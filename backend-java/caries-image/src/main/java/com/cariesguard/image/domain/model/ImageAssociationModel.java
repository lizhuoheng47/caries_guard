package com.cariesguard.image.domain.model;

import java.time.LocalDateTime;

public record ImageAssociationModel(
        Long imageId,
        Long caseId,
        Long visitId,
        Long patientId,
        Long attachmentId,
        String imageTypeCode,
        String imageSourceCode,
        LocalDateTime shootingTime,
        String bodyPositionCode,
        Integer imageIndexNo,
        String qualityStatusCode,
        String sourceDeviceCode,
        String captureBatchNo,
        String primaryFlag,
        Long orgId,
        String status,
        String remark,
        Long operatorUserId) {

    public ImageAssociationModel(Long imageId,
                                 Long caseId,
                                 Long visitId,
                                 Long patientId,
                                 Long attachmentId,
                                 String imageTypeCode,
                                 String imageSourceCode,
                                 LocalDateTime shootingTime,
                                 String bodyPositionCode,
                                 Integer imageIndexNo,
                                 String qualityStatusCode,
                                 String primaryFlag,
                                 Long orgId,
                                 String status,
                                 String remark,
                                 Long operatorUserId) {
        this(imageId, caseId, visitId, patientId, attachmentId, imageTypeCode, imageSourceCode, shootingTime,
                bodyPositionCode, imageIndexNo, qualityStatusCode, null, null, primaryFlag, orgId, status,
                remark, operatorUserId);
    }
}
