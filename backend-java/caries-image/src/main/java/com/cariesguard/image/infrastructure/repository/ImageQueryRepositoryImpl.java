package com.cariesguard.image.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageQualityCheckModel;
import com.cariesguard.image.domain.model.ImageViewModel;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.infrastructure.dataobject.MedAttachmentDO;
import com.cariesguard.image.infrastructure.dataobject.MedImageFileDO;
import com.cariesguard.image.infrastructure.dataobject.MedImageQualityCheckDO;
import com.cariesguard.image.infrastructure.mapper.ImageFileMapper;
import com.cariesguard.image.infrastructure.mapper.MedAttachmentMapper;
import com.cariesguard.image.infrastructure.mapper.MedImageQualityCheckMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ImageQueryRepositoryImpl implements ImageQueryRepository {

    private final ImageFileMapper imageFileMapper;
    private final MedAttachmentMapper medAttachmentMapper;
    private final MedImageQualityCheckMapper medImageQualityCheckMapper;

    public ImageQueryRepositoryImpl(ImageFileMapper imageFileMapper,
                                    MedAttachmentMapper medAttachmentMapper,
                                    MedImageQualityCheckMapper medImageQualityCheckMapper) {
        this.imageFileMapper = imageFileMapper;
        this.medAttachmentMapper = medAttachmentMapper;
        this.medImageQualityCheckMapper = medImageQualityCheckMapper;
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
    public Optional<ImageViewModel> findImage(Long imageId) {
        MedImageFileDO image = imageFileMapper.selectOne(new LambdaQueryWrapper<MedImageFileDO>()
                .eq(MedImageFileDO::getId, imageId)
                .eq(MedImageFileDO::getDeletedFlag, 0L)
                .eq(MedImageFileDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return image == null ? Optional.empty() : Optional.of(toImageViewModel(image));
    }

    @Override
    public List<ImageViewModel> listImagesByCaseId(Long caseId) {
        return imageFileMapper.selectList(new LambdaQueryWrapper<MedImageFileDO>()
                        .eq(MedImageFileDO::getCaseId, caseId)
                        .eq(MedImageFileDO::getDeletedFlag, 0L)
                        .eq(MedImageFileDO::getStatus, "ACTIVE")
                        .orderByDesc(MedImageFileDO::getIsPrimary)
                        .orderByAsc(MedImageFileDO::getImageIndexNo)
                        .orderByAsc(MedImageFileDO::getId))
                .stream()
                .map(this::toImageViewModel)
                .toList();
    }

    @Override
    public Optional<ImageQualityCheckModel> findCurrentQualityCheck(Long imageId) {
        MedImageQualityCheckDO qualityCheck = medImageQualityCheckMapper.selectOne(new LambdaQueryWrapper<MedImageQualityCheckDO>()
                .eq(MedImageQualityCheckDO::getImageId, imageId)
                .eq(MedImageQualityCheckDO::getCurrentFlag, "1")
                .eq(MedImageQualityCheckDO::getDeletedFlag, 0L)
                .eq(MedImageQualityCheckDO::getStatus, "ACTIVE")
                .orderByDesc(MedImageQualityCheckDO::getCheckedAt)
                .last("LIMIT 1"));
        return qualityCheck == null ? Optional.empty() : Optional.of(new ImageQualityCheckModel(
                qualityCheck.getImageId(),
                qualityCheck.getCheckTypeCode(),
                qualityCheck.getCheckResultCode(),
                qualityCheck.getQualityScore(),
                qualityCheck.getBlurScore(),
                qualityCheck.getExposureScore(),
                qualityCheck.getIntegrityScore(),
                qualityCheck.getOcclusionScore(),
                qualityCheck.getIssueCodesJson(),
                qualityCheck.getSuggestionText(),
                qualityCheck.getCheckedAt()));
    }

    private ImageViewModel toImageViewModel(MedImageFileDO image) {
        MedAttachmentDO attachment = medAttachmentMapper.selectById(image.getAttachmentId());
        return new ImageViewModel(
                image.getId(),
                image.getAttachmentId(),
                attachment == null ? null : attachment.getOriginalName(),
                attachment == null ? null : attachment.getBucketName(),
                attachment == null ? null : attachment.getObjectKey(),
                image.getImageTypeCode(),
                image.getImageSourceCode(),
                image.getQualityStatusCode(),
                image.getIsPrimary(),
                image.getShootingTime(),
                image.getBodyPositionCode());
    }
}
