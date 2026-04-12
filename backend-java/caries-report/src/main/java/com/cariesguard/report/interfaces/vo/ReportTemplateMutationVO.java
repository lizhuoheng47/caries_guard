package com.cariesguard.report.interfaces.vo;

public record ReportTemplateMutationVO(
        Long templateId,
        String templateCode,
        Integer versionNo,
        String status) {
}

