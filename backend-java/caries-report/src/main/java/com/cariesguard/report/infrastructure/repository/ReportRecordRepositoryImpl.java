package com.cariesguard.report.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.report.domain.model.ReportAttachmentCreateModel;
import com.cariesguard.report.domain.model.ReportAttachmentModel;
import com.cariesguard.report.domain.model.ReportGenerateModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.infrastructure.dataobject.ReportAttachmentDO;
import com.cariesguard.report.infrastructure.dataobject.RptRecordDO;
import com.cariesguard.report.infrastructure.mapper.ReportAttachmentMapper;
import com.cariesguard.report.infrastructure.mapper.RptRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRecordRepositoryImpl implements ReportRecordRepository {

    private final RptRecordMapper rptRecordMapper;
    private final ReportAttachmentMapper reportAttachmentMapper;

    public ReportRecordRepositoryImpl(RptRecordMapper rptRecordMapper, ReportAttachmentMapper reportAttachmentMapper) {
        this.rptRecordMapper = rptRecordMapper;
        this.reportAttachmentMapper = reportAttachmentMapper;
    }

    @Override
    public int nextVersionNo(Long caseId, String reportTypeCode) {
        RptRecordDO latest = rptRecordMapper.selectOne(new LambdaQueryWrapper<RptRecordDO>()
                .eq(RptRecordDO::getCaseId, caseId)
                .eq(RptRecordDO::getReportTypeCode, reportTypeCode)
                .eq(RptRecordDO::getDeletedFlag, 0L)
                .eq(RptRecordDO::getStatus, "ACTIVE")
                .orderByDesc(RptRecordDO::getVersionNo)
                .orderByDesc(RptRecordDO::getId)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    @Override
    public void create(ReportGenerateModel model) {
        RptRecordDO entity = new RptRecordDO();
        entity.setId(model.reportId());
        entity.setReportNo(model.reportNo());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setSourceSummaryId(model.sourceSummaryId());
        entity.setSourceRiskAssessmentId(model.sourceRiskAssessmentId());
        entity.setSourceCorrectionId(model.sourceCorrectionId());
        entity.setReportTypeCode(model.reportTypeCode());
        entity.setReportStatusCode(model.reportStatusCode());
        entity.setVersionNo(model.versionNo());
        entity.setSummaryText(model.summaryText());
        entity.setGeneratedAt(model.generatedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        rptRecordMapper.insert(entity);
    }

    @Override
    public void updateArchiveInfo(Long reportId,
                                  Long attachmentId,
                                  String reportStatusCode,
                                  LocalDateTime generatedAt,
                                  Long operatorUserId) {
        rptRecordMapper.update(null, new LambdaUpdateWrapper<RptRecordDO>()
                .eq(RptRecordDO::getId, reportId)
                .eq(RptRecordDO::getDeletedFlag, 0L)
                .set(RptRecordDO::getAttachmentId, attachmentId)
                .set(RptRecordDO::getReportStatusCode, reportStatusCode)
                .set(RptRecordDO::getGeneratedAt, generatedAt)
                .set(RptRecordDO::getUpdatedBy, operatorUserId));
    }

    @Override
    public Optional<ReportRecordModel> findById(Long reportId) {
        RptRecordDO entity = rptRecordMapper.selectOne(new LambdaQueryWrapper<RptRecordDO>()
                .eq(RptRecordDO::getId, reportId)
                .eq(RptRecordDO::getDeletedFlag, 0L)
                .eq(RptRecordDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(toModel(entity));
    }

    @Override
    public List<ReportRecordModel> listByCaseId(Long caseId) {
        return rptRecordMapper.selectList(new LambdaQueryWrapper<RptRecordDO>()
                        .eq(RptRecordDO::getCaseId, caseId)
                        .eq(RptRecordDO::getDeletedFlag, 0L)
                        .eq(RptRecordDO::getStatus, "ACTIVE")
                        .orderByDesc(RptRecordDO::getVersionNo)
                        .orderByDesc(RptRecordDO::getCreatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void createAttachment(ReportAttachmentCreateModel model) {
        ReportAttachmentDO entity = new ReportAttachmentDO();
        entity.setId(model.attachmentId());
        entity.setBizModuleCode(model.bizModuleCode());
        entity.setBizId(model.bizId());
        entity.setFileCategoryCode(model.fileCategoryCode());
        entity.setAssetTypeCode(model.assetTypeCode());
        entity.setSourceAttachmentId(model.sourceAttachmentId());
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
        entity.setRetentionPolicyCode(model.retentionPolicyCode());
        entity.setExpiredAt(model.expiredAt());
        entity.setIntegrityStatusCode(model.integrityStatusCode());
        entity.setMetadataJson(model.metadataJson());
        entity.setUploadUserId(model.uploadUserId());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        reportAttachmentMapper.insert(entity);
    }

    @Override
    public Optional<ReportAttachmentModel> findAttachment(Long attachmentId) {
        ReportAttachmentDO entity = reportAttachmentMapper.selectOne(new LambdaQueryWrapper<ReportAttachmentDO>()
                .eq(ReportAttachmentDO::getId, attachmentId)
                .eq(ReportAttachmentDO::getDeletedFlag, 0L)
                .eq(ReportAttachmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportAttachmentModel(
                entity.getId(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getOriginalName(),
                entity.getContentType(),
                entity.getOrgId(),
                entity.getStatus()));
    }

    private ReportRecordModel toModel(RptRecordDO entity) {
        return new ReportRecordModel(
                entity.getId(),
                entity.getReportNo(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getAttachmentId(),
                entity.getSourceSummaryId(),
                entity.getSourceRiskAssessmentId(),
                entity.getSourceCorrectionId(),
                entity.getReportTypeCode(),
                entity.getReportStatusCode(),
                entity.getVersionNo(),
                entity.getSummaryText(),
                entity.getGeneratedAt(),
                entity.getSignedAt(),
                entity.getOrgId(),
                entity.getCreatedAt());
    }
}
