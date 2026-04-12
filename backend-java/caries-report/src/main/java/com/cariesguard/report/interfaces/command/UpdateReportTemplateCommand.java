package com.cariesguard.report.interfaces.command;

public record UpdateReportTemplateCommand(
        String templateName,
        String templateContent,
        String status,
        String remark) {
}

