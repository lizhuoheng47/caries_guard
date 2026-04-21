from __future__ import annotations

from app.core.config import Settings
from app.infra.llm.base_llm_client import LlmResult


class TemplateLlmClient:
    def __init__(
        self,
        settings: Settings,
        *,
        provider_code: str = "MOCK",
        model_name: str = "template-llm-v1",
    ) -> None:
        self.settings = settings
        self.provider_code = provider_code
        self.model_name = model_name

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        if self.settings.ai_runtime_mode in {"real", "competition"}:
            # Although 'competition' usually uses hybrid, the doc says:
            # "若 competition-hybrid / real 模式仍命中该 client，则直接抛错"
            # Note: in my previous turn I updated competition.env to 'hybrid'.
            # I will check if runtime_mode is 'real' or if provider is specifically restricted.
            pass

        # Strict check as per doc
        if self.settings.ai_runtime_mode == "real" or self.settings.app_env == "competition":
             raise RuntimeError(f"TemplateLlmClient (MOCK) is forbidden in {self.settings.ai_runtime_mode} mode")

        prompt = self._build_prompt(scene, query, evidence, context_text)
        if not evidence:
            answer = (
                "当前知识库没有检索到足够依据。建议补充已审核知识文档后再生成结论；"
                "如涉及疼痛、肿胀、牙体缺损或高风险因素，应由口腔医生线下复核。"
            )
            return LlmResult(answer_text=answer, prompt_text=prompt, provider=self.provider_code, model=self.model_name)

        citations = " ".join(f"[{index}]" for index in range(1, len(evidence) + 1))
        if scene == "PATIENT_EXPLAIN":
            lead = "根据本次检索到的已审核资料，可以向患者这样解释："
            advice = "请保持日常清洁，控制含糖饮食，并按风险等级安排复查；若症状加重，应及时就诊。"
        else:
            lead = "基于已审核知识片段，医生端参考回答如下："
            advice = "该回答仅用于辅助复核，应结合影像、病史、检查和医生判断形成最终意见。"
        snippets = "；".join(item["chunk_text"].strip().replace("\n", " ")[:120] for item in evidence[:3])
        answer = f"{lead}{snippets}。{advice} 依据：{citations}"
        return LlmResult(answer_text=answer, prompt_text=prompt, provider=self.provider_code, model=self.model_name)

    def resolve_profile(self, scene: str) -> dict[str, str]:
        return {
            "providerCode": self.provider_code,
            "modelName": self.model_name,
            "baseUrl": "",
            "apiKey": "",
        }

    @staticmethod
    def _build_prompt(scene: str, query: str, evidence: list[dict], context_text: str | None) -> str:
        evidence_text = "\n".join(
            f"[{index}] {item.get('doc_title') or '未命名文档'}: {item['chunk_text']}"
            for index, item in enumerate(evidence, start=1)
        )
        return (
            "你是 CariesGuard 的受控口腔健康文本助手。\n"
            "禁止伪造引用，禁止承诺诊断结果，必须保守表达。\n"
            f"场景: {scene}\n"
            f"问题: {query}\n"
            f"上下文: {context_text or '无'}\n"
            f"证据:\n{evidence_text or '无命中'}"
        )
