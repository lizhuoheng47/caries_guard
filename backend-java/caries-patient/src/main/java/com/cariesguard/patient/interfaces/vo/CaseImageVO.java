package com.cariesguard.patient.interfaces.vo;

public record CaseImageVO(
        Long imageId,
        String imageTypeCode,
        String qualityStatusCode,
        String primaryFlag) {
}
