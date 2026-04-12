package com.cariesguard.patient.interfaces.vo;

public record CaseStatusTransitionVO(
        Long caseId,
        String fromStatusCode,
        String toStatusCode) {
}
