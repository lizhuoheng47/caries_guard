package com.cariesguard.report.interfaces.command;

public record RagKbRebuildCommand(
        String kbCode,
        String kbName,
        String kbTypeCode,
        String knowledgeVersion) {
}
