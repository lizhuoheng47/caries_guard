package com.cariesguard.image.domain.repository;

import com.cariesguard.image.domain.model.AttachmentDuplicateModel;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
import com.cariesguard.image.domain.model.AttachmentUploadModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageAssociationModel;
import com.cariesguard.image.domain.model.ImageManagedModel;
import com.cariesguard.image.domain.model.ImageQualityCheckCreateModel;
import java.util.Optional;

public interface ImageCommandRepository {

    Optional<AttachmentDuplicateModel> findAttachmentByMd5(Long orgId,
                                                           String md5,
                                                           String bizModuleCode,
                                                           Long bizId,
                                                           String fileCategoryCode);

    default Optional<AttachmentDuplicateModel> findAttachmentByMd5(Long orgId, String md5) {
        return findAttachmentByMd5(orgId, md5, null, null, null);
    }

    void createAttachment(AttachmentUploadModel model);

    Optional<AttachmentViewModel> findAttachment(Long attachmentId);

    Optional<AttachmentViewModel> findAttachmentByObject(String bucketName, String objectKey);

    Optional<AttachmentOwnerCaseModel> findCase(Long caseId);

    int nextImageIndexNo(Long caseId, String imageTypeCode);

    void createImage(ImageAssociationModel model);

    void clearPrimaryFlag(Long caseId, String imageTypeCode, Long operatorUserId);

    Optional<ImageManagedModel> findImage(Long imageId);

    void saveQualityCheck(ImageQualityCheckCreateModel model);
}
