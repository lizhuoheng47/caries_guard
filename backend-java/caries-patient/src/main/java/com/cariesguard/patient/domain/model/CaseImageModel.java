package com.cariesguard.patient.domain.model;

public record CaseImageModel(
        Long imageId,
        String imageTypeCode,
        String qualityStatusCode,
        String primaryFlag) {
}
