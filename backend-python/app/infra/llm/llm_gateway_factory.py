from __future__ import annotations

from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, TimeoutError, wait

from app.core.config import Settings
from app.infra.llm.base_llm_client import BaseLlmClient, LlmResult
from app.infra.llm.openai_compatible_llm_client import OpenAiCompatibleLlmClient
from app.infra.llm.template_llm_client import TemplateLlmClient


class FallbackLlmClient:
    def __init__(self, primary: BaseLlmClient, fallback: BaseLlmClient) -> None:
        self.primary = primary
        self.fallback = fallback

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None):
        try:
            return self.primary.generate(scene=scene, query=query, evidence=evidence, context_text=context_text)
        except Exception:
            return self.fallback.generate(scene=scene, query=query, evidence=evidence, context_text=context_text)

    def resolve_profile(self, scene: str) -> dict[str, str]:
        if hasattr(self.primary, "resolve_profile"):
            return self.primary.resolve_profile(scene)
        if hasattr(self.fallback, "resolve_profile"):
            return self.fallback.resolve_profile(scene)
        return {}


class HedgedLlmClient:
    """Issue a secondary request when the primary request exceeds a delay budget."""

    def __init__(
        self,
        primary: BaseLlmClient,
        hedge: BaseLlmClient,
        *,
        hedge_delay_ms: int,
        primary_profile: dict[str, str],
        hedge_profile: dict[str, str],
    ) -> None:
        self.primary = primary
        self.hedge = hedge
        self.hedge_delay_ms = max(0, int(hedge_delay_ms))
        self.primary_profile = primary_profile
        self.hedge_profile = hedge_profile
        self._executor = ThreadPoolExecutor(max_workers=2)

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        primary_future = self._executor.submit(
            self.primary.generate,
            scene=scene,
            query=query,
            evidence=evidence,
            context_text=context_text,
        )
        try:
            return primary_future.result(timeout=self.hedge_delay_ms / 1000.0)
        except TimeoutError:
            pass
        except Exception as primary_error:
            hedge_future = self._executor.submit(
                self.hedge.generate,
                scene=scene,
                query=query,
                evidence=evidence,
                context_text=context_text,
            )
            try:
                return hedge_future.result()
            except Exception as hedge_error:
                raise RuntimeError(
                    f"Hedged LLM failed: primary={primary_error.__class__.__name__}, "
                    f"secondary={hedge_error.__class__.__name__}"
                ) from hedge_error

        hedge_future = self._executor.submit(
            self.hedge.generate,
            scene=scene,
            query=query,
            evidence=evidence,
            context_text=context_text,
        )
        done, pending = wait([primary_future, hedge_future], return_when=FIRST_COMPLETED)

        first_error: Exception | None = None
        for future in done:
            try:
                return future.result()
            except Exception as exc:
                first_error = exc

        for future in pending:
            try:
                return future.result()
            except Exception as second_error:
                first = first_error.__class__.__name__ if first_error is not None else "UnknownError"
                raise RuntimeError(
                    f"Hedged LLM failed: first={first}, second={second_error.__class__.__name__}"
                ) from second_error

        raise RuntimeError("Hedged LLM failed without result") from first_error

    def resolve_profile(self, scene: str) -> dict[str, str]:
        profile = dict(self.primary_profile)
        profile["hedgeProviderCode"] = self.hedge_profile.get("providerCode", "")
        profile["hedgeModelName"] = self.hedge_profile.get("modelName", "")
        profile["hedgeEnabled"] = "true"
        return profile


class RoutedLlmClient:
    def __init__(
        self,
        default_client: BaseLlmClient,
        scene_clients: dict[str, BaseLlmClient],
        scene_profiles: dict[str, dict[str, str]],
    ) -> None:
        self.default_client = default_client
        self.scene_clients = scene_clients
        self.scene_profiles = scene_profiles

    @staticmethod
    def _normalize_scene(scene: str) -> str:
        value = (scene or "").strip().upper()
        if value == "PATIENT_EXPLAIN":
            return "PATIENT_EXPLAIN"
        return "DOCTOR_QA"

    def resolve_profile(self, scene: str) -> dict[str, str]:
        key = self._normalize_scene(scene)
        return self.scene_profiles.get(key, self.scene_profiles.get("DOCTOR_QA", {}))

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        key = self._normalize_scene(scene)
        client = self.scene_clients.get(key, self.default_client)
        result = client.generate(scene=scene, query=query, evidence=evidence, context_text=context_text)
        profile = self.resolve_profile(scene)
        provider = result.provider or profile.get("providerCode")
        model = result.model or profile.get("modelName")
        if provider == result.provider and model == result.model:
            return result
        return LlmResult(
            answer_text=result.answer_text,
            prompt_text=result.prompt_text,
            provider=provider,
            model=model,
            latency_ms=result.latency_ms,
            usage=result.usage,
            finish_reason=result.finish_reason,
        )


def _build_client_with_profile(settings: Settings, profile: dict[str, str]) -> BaseLlmClient:
    provider = (profile.get("providerCode") or "MOCK").strip().upper()
    model_name = profile.get("modelName") or settings.llm_model_name
    base_url = profile.get("baseUrl") or settings.llm_base_url
    api_key = profile.get("apiKey") or settings.llm_api_key
    if provider == "MOCK":
        return TemplateLlmClient(settings, provider_code=provider, model_name=model_name)
    if provider in {"OPENAI", "OPENAI_COMPATIBLE", "DASHSCOPE", "DEEPSEEK", "QWEN"}:
        primary = OpenAiCompatibleLlmClient(
            settings,
            provider_code=provider,
            model_name=model_name,
            base_url=base_url,
            api_key=api_key,
        )
        if settings.llm_enable_fallback_mock and settings.ai_runtime_mode != "real":
            fallback_model = f"{model_name}-fallback-mock"
            return FallbackLlmClient(primary, TemplateLlmClient(settings, model_name=fallback_model))
        return primary
    raise ValueError(f"Unsupported CG_LLM_PROVIDER_CODE={provider}")


def _profile_identity(profile: dict[str, str]) -> tuple[str, str, str]:
    return (
        (profile.get("providerCode") or "").strip().upper(),
        (profile.get("modelName") or "").strip(),
        (profile.get("baseUrl") or "").strip(),
    )


def _maybe_wrap_hedged_client(
    settings: Settings,
    primary_client: BaseLlmClient,
    primary_profile: dict[str, str],
) -> BaseLlmClient:
    if not settings.llm_hedge_enabled:
        return primary_client
    hedge_profile = settings.get_llm_hedge_profile()
    if _profile_identity(primary_profile) == _profile_identity(hedge_profile):
        return primary_client
    hedge_client = _build_client_with_profile(settings, hedge_profile)
    return HedgedLlmClient(
        primary=primary_client,
        hedge=hedge_client,
        hedge_delay_ms=settings.llm_hedge_delay_ms,
        primary_profile=primary_profile,
        hedge_profile=hedge_profile,
    )


def create_llm_client(settings: Settings) -> BaseLlmClient:
    default_profile = settings.get_llm_profile_for_scene("DOCTOR_QA")
    default_client = _build_client_with_profile(settings, default_profile)
    default_client = _maybe_wrap_hedged_client(settings, default_client, default_profile)
    if not settings.llm_scene_routing_enabled:
        return default_client
    scene_profiles = {
        "DOCTOR_QA": settings.get_llm_profile_for_scene("DOCTOR_QA"),
        "PATIENT_EXPLAIN": settings.get_llm_profile_for_scene("PATIENT_EXPLAIN"),
    }
    scene_clients = {
        scene: _maybe_wrap_hedged_client(
            settings,
            _build_client_with_profile(settings, profile),
            profile,
        )
        for scene, profile in scene_profiles.items()
    }
    return RoutedLlmClient(default_client=default_client, scene_clients=scene_clients, scene_profiles=scene_profiles)
