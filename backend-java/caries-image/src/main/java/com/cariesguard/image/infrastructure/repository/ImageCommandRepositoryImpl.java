package com.cariesguard.image.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.image.domain.model.AttachmentDuplicateModel;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
import com.cariesguard.image.domain.model.AttachmentUploadModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageAssociationModel;
import com.cariesguard.image.domain.model.ImageManagedModel;
import com.cariesguard.image.domain.model.ImageQualityCheckCreateModel;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.infrastructure.dataobject.MedAttachmentDO;
import com.cariesguard.image.infrastructure.dataobject.MedCaseDO;
import com.cariesguard.image.infrastructure.dataobject.MedImageFileDO;
import com.cariesguard.image.infrastructure.dataobject.MedImageQualityCheckDO;
import com.cariesguard.image.infrastructure.mapper.ImageCaseMapper;
import com.cariesguard.image.infrastructure.mapper.ImageFileMapper;
import com.cariesguard.image.infrastructure.mapper.MedAttachmentMapper;
import com.cariesguard.image.infrastructure.mapper.MedImageQualityCheckMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ImageCommandRepositoryImpl implements ImageCommandRepository {

    private final MedAttachmentMapper medAttachmentMapper;
    private final ImageCaseMapper imageCaseMapper;
    private final ImageFileMapper imageFileMapper;
    private final MedImageQualityCheckMapper medImageQualityCheckMapper;

    public ImageCommandRepositoryImpl(MedAttachmentMapper medAttachmentMapper,
                                      ImageCaseMapper imageCaseMapper,
                                      ImageFileMapper imageFileMapper,
                                      MedImageQualityCheckMapper medImageQualityCheckMapper) {
        this.medAttachmentMapper = medAttachmentMapper;
        this.imageCaseMapper = imageCaseMapper;
        this.imageFileMapper = imageFileMapper;
        this.medImageQualityCheckMapper = medImageQualityCheckMapper;
    }

    @Override
    public Optional<AttachmentDuplicateModel> findAttachmentByMd5(Long orgId, String md5) {
        MedAttachmentDO attachment = medAttachmentMapper.selectOne(new LambdaQueryWrapper<MedAttachmentDO>()
                .eq(MedAttachmentDO::getOrgId, orgId)
                .eq(MedAttachmentDO::getMd5, md5)
                .eq(MedAttachmentDO::getDeletedFlag, 0L)
                .eq(MedAttachmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return attachment == null ? Optional.empty() : Optional.of(new AttachmentDuplicateModel(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getOriginalName(),
                attachment.getBucketName(),
                attachment.getObjectKey(),
                attachment.getMd5(),
                attachment.getContentType(),
                attachment.getFileSizeBytes()));
    }

    @Override
    public void createAttachment(AttachmentUploadModel model) {
        MedAttachmentDO entity = new MedAttachmentDO();
        entity.setId(model.attachmentId());
        entity.setBizModuleCode(model.bizModuleCode());
        entity.setBizId(model.bizId());
        entity.setFileCategoryCode(model.fileCategoryCode());
        entity.setFileName(model.fileName());
        entity.setOriginalName(model.originalName());
        entity.setBucketName(model.bucketName());
        entity.setObjectKey(model.objectKey());
        entity.setContentType(model.contentType());
        entity.setFileExt(model.fileExt());
        entity.setFileSizeBytes(model.fileSizeBytes());
        entity.setMd5(model.md5());
        entity.setStorageProviderCode(model.storageProviderCode());
        entity.setVisibilityCode(model.visibilityCode());
        entity.setUploadUserId(model.uploadUserId());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        medAttachmentMapper.insert(entity);
    }

    @Override
    public Optional<AttachmentViewModel> findAttachment(Long attachmentId) {
        MedAttachmentDO attachment = medAttachmentMapper.selectOne(new LambdaQueryWrapper<MedAttachmentDO>()
                .eq(MedAttachmentDO::getId, attachmentId)
                .eq(MedAttachmentDO::getDeletedFlag, 0L)
                .eq(MedAttachmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return attachment == null ? Optional.empty() : Optional.of(new AttachmentViewModel(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getOriginalName(),
                attachment.getBucketName(),
                attachment.getObjectKey(),
                attachment.getContentType(),
                attachment.getMd5(),
                attachment.getFileSizeBytes(),
                attachment.getOrgId()));
    }

    @Override
    public Optional<AttachmentOwnerCaseModel> findCase(Long caseId) {
        MedCaseDO medicalCase = imageCaseMapper.selectOne(new LambdaQueryWrapper<MedCaseDO>()
                .eq(MedCaseDO::getId, caseId)
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .eq(MedCaseDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return medicalCase == null ? Optional.empty() : Optional.of(new AttachmentOwnerCaseModel(
                medicalCase.getId(),
                medicalCase.getCaseNo(),
                medicalCase.getVisitId(),
                medicalCase.getPatientId(),
                medicalCase.getOrgId(),
                medicalCase.getCaseStatusCode()));
    }

    @Override
    public int nextImageIndexNo(Long caseId, String imageTypeCode) {
        return imageFileMapper.selectList(new LambdaQueryWrapper<MedImageFileDO>()
                        .eq(MedImageFileDO::getCaseId, caseId)
                        .eq(MedImageFileDO::getImageTypeCode, imageTypeCode)
                        .eq(MedImageFileDO::getDeletedFlag, 0L))
                .size() + 1;
    }

    @Override
    public void createImage(ImageAssociationModel model) {
        MedImageFileDO entity = new MedImageFileDO();
        entity.setId(model.imageId());
        entity.setCaseId(model.caseId());
        entity.setVisitId(model.visitId());
        entity.setPatientId(model.patientId());
        entity.setAttachmentId(model.attachmentId());
        entity.setImageTypeCode(model.imageTypeCode());
        entity.setImageSourceCode(model.imageSourceCode());
        entity.setShootingTime(model.shootingTime());
        entity.setBodyPositionCode(model.bodyPositionCode());
        entity.setImageIndexNo(model.imageIndexNo());
        entity.setQualityStatusCode(model.qualityStatusCode());
        entity.setIsPrimary(model.primaryFlag());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        imageFileMapper.insert(entity);
    }

    @Override
    public void clearPrimaryFlag(Long caseId, String imageTypeCode, Long operatorUserId) {
        imageFileMapper.update(null, new LambdaUpdateWrapper<MedImageFileDO>()
                .eq(MedImageFileDO::getCaseId, caseId)
                .eq(MedImageFileDO::getImageTypeCode, imageTypeCode)
                .eq(MedImageFileDO::getDeletedFlag, 0L)
                .set(MedImageFileDO::getIsPrimary, "0")
                .set(MedImageFileDO::getUpdatedBy, operatorUserId));
    }

    @Override
    public Optional<ImageManagedModel> findImage(Long imageId) {
        MedImageFileDO image = imageFileMapper.selectOne(new LambdaQueryWrapper<MedImageFileDO>()
                .eq(MedImageFileDO::getId, imageId)
                .eq(MedImageFileDO::getDeletedFlag, 0L)
                .eq(MedImageFileDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return image == null ? Optional.empty() : Optional.of(new ImageManagedModel(
                image.getId(),
                image.getCaseId(),
                image.getPatientId(),
                image.getAttachmentId(),
                image.getOrgId()));
    }

    @Override
    public void saveQualityCheck(ImageQualityCheckCreateModel model) {
        medImageQualityCheckMapper.update(null, new LambdaUpdateWrapper<MedImageQualityCheckDO>()
                .eq(MedImageQualityCheckDO::getImageId, model.imageId())
                .eq(MedImageQualityCheckDO::getDeletedFlag, 0L)
                .set(MedImageQualityCheckDO::getCurrentFlag, "0")
                .set(MedImageQualityCheckDO::getUpdatedBy, model.operatorUserId()));

        MedImageQualityCheckDO entity = new MedImageQualityCheckDO();
        entity.setId(model.checkId());
        entity.setImageId(model.imageId());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setCheckTypeCode(model.checkTypeCode());
        entity.setCheckResultCode(model.checkResultCode());
        entity.setQualityScore(model.qualityScore());
        entity.setBlurScore(model.blurScore());
        entity.setExposureScore(model.exposureScore());
        entity.setIntegrityScore(model.integrityScore());
        entity.setOcclusionScore(model.occlusionScore());
        entity.setIssueCodesJson(model.issueCodesJson());
        entity.setSuggestionText(model.suggestionText());
        entity.setCurrentFlag(model.currentFlag());
        entity.setCheckedBy(model.checkedBy());
        entity.setCheckedAt(model.checkedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        medImageQualityCheckMapper.insert(entity);

        imageFileMapper.update(null, new LambdaUpdateWrapper<MedImageFileDO>()
                .eq(MedImageFileDO::getId, model.imageId())
                .eq(MedImageFileDO::getDeletedFlag, 0L)
                .set(MedImageFileDO::getQualityStatusCode, model.checkResultCode())
                .set(MedImageFileDO::getUpdatedBy, model.operatorUserId()));
    }
}
