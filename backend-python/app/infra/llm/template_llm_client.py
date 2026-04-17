from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class LlmResult:
    answer_text: str
    prompt_text: str


class TemplateLlmClient:
    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        prompt = self._build_prompt(scene, query, evidence, context_text)
        if not evidence:
            answer = (
                "当前知识库没有检索到足够依据。建议补充已审核知识文档后再生成结论；"
                "如涉及疼痛、肿胀、牙体缺损或高风险因素，应由口腔医生线下复核。"
            )
            return LlmResult(answer_text=answer, prompt_text=prompt)

        citations = " ".join(f"[{index}]" for index in range(1, len(evidence) + 1))
        if scene == "PATIENT_EXPLAIN":
            lead = "根据本次检索到的已审核资料，可以向患者这样解释："
            advice = "请保持日常清洁，控制含糖饮食，并按风险等级安排复查；若症状加重，应及时就诊。"
        else:
            lead = "基于已审核知识片段，医生端参考回答如下："
            advice = "该回答仅用于辅助复核，应结合影像、病史、检查和医生判断形成最终意见。"
        snippets = "；".join(item["chunk_text"].strip().replace("\n", " ")[:120] for item in evidence[:3])
        answer = f"{lead}{snippets}。{advice} 依据：{citations}"
        return LlmResult(answer_text=answer, prompt_text=prompt)

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
