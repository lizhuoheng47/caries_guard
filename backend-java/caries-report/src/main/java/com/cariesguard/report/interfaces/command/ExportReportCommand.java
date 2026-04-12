package com.cariesguard.report.interfaces.command;

public record ExportReportCommand(
        String exportTypeCode,
        String exportChannelCode) {
}

