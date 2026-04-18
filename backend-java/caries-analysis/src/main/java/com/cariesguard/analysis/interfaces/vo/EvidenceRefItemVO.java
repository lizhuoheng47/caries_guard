package com.cariesguard.analysis.interfaces.vo;

/**
 * 单条证据引用 — 用于 evidenceRefs 分类展示.
 */
public record EvidenceRefItemVO(
        /** 引用类型: IMAGE / KNOWLEDGE / CLINICAL / MODEL */
        String refType,
        /** 引用编码 */
        String refCode,
        /** 摘要描述 */
        String summary,
        /** 来源 */
        String source) {
}
