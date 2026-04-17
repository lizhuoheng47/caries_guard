package com.cariesguard.report.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.infrastructure.dataobject.ReportAttachmentDO;
import com.cariesguard.report.infrastructure.dataobject.ReportCaseDO;
import com.cariesguard.report.infrastructure.dataobject.ReportCorrectionFeedbackDO;
import com.cariesguard.report.infrastructure.dataobject.ReportImageFileDO;
import com.cariesguard.report.infrastructure.dataobject.ReportResultSummaryDO;
import com.cariesguard.report.infrastructure.dataobject.ReportRiskAssessmentDO;
import com.cariesguard.report.infrastructure.dataobject.ReportToothRecordDO;
import com.cariesguard.report.infrastructure.dataobject.ReportVisualAssetDO;
import com.cariesguard.report.infrastructure.mapper.ReportAttachmentMapper;
import com.cariesguard.report.infrastructure.mapper.ReportCaseMapper;
import com.cariesguard.report.infrastructure.mapper.ReportCorrectionFeedbackMapper;
import com.cariesguard.report.infrastructure.mapper.ReportImageFileMapper;
import com.cariesguard.report.infrastructure.mapper.ReportResultSummaryMapper;
import com.cariesguard.report.infrastructure.mapper.ReportRiskAssessmentMapper;
import com.cariesguard.report.infrastructure.mapper.ReportToothRecordMapper;
import com.cariesguard.report.infrastructure.mapper.ReportVisualAssetMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ReportSourceQueryRepositoryImpl implements ReportSourceQueryRepository {

    private final ReportCaseMapper reportCaseMapper;
    private final ReportImageFileMapper reportImageFileMapper;
    private final ReportAttachmentMapper reportAttachmentMapper;
    private final ReportResultSummaryMapper reportResultSummaryMapper;
    private final ReportRiskAssessmentMapper reportRiskAssessmentMapper;
    private final ReportCorrectionFeedbackMapper reportCorrectionFeedbackMapper;
    private final ReportToothRecordMapper reportToothRecordMapper;
    private final ReportVisualAssetMapper reportVisualAssetMapper;

    public ReportSourceQueryRepositoryImpl(ReportCaseMapper reportCaseMapper,
                                           ReportImageFileMapper reportImageFileMapper,
                                           ReportAttachmentMapper reportAttachmentMapper,
                                           ReportResultSummaryMapper reportResultSummaryMapper,
                                           ReportRiskAssessmentMapper reportRiskAssessmentMapper,
                                           ReportCorrectionFeedbackMapper reportCorrectionFeedbackMapper,
                                           ReportToothRecordMapper reportToothRecordMapper,
                                           ReportVisualAssetMapper reportVisualAssetMapper) {
        this.reportCaseMapper = reportCaseMapper;
        this.reportImageFileMapper = reportImageFileMapper;
        this.reportAttachmentMapper = reportAttachmentMapper;
        this.reportResultSummaryMapper = reportResultSummaryMapper;
        this.reportRiskAssessmentMapper = reportRiskAssessmentMapper;
        this.reportCorrectionFeedbackMapper = reportCorrectionFeedbackMapper;
        this.reportToothRecordMapper = reportToothRecordMapper;
        this.reportVisualAssetMapper = reportVisualAssetMapper;
    }

    @Override
    public Optional<ReportCaseModel> findCase(Long caseId) {
        ReportCaseDO entity = reportCaseMapper.selectOne(new LambdaQueryWrapper<ReportCaseDO>()
                .eq(ReportCaseDO::getId, caseId)
                .eq(ReportCaseDO::getDeletedFlag, 0L)
                .eq(ReportCaseDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportCaseModel(
                entity.getId(),
                entity.getCaseNo(),
                entity.getPatientId(),
                entity.getCaseStatusCode(),
                entity.getOrgId()));
    }

    @Override
    public List<ReportImageModel> listCaseImages(Long caseId) {
        return reportImageFileMapper.selectList(new LambdaQueryWrapper<ReportImageFileDO>()
                        .eq(ReportImageFileDO::getCaseId, caseId)
                        .eq(ReportImageFileDO::getDeletedFlag, 0L)
                        .eq(ReportImageFileDO::getStatus, "ACTIVE")
                        .orderByDesc(ReportImageFileDO::getIsPrimary)
                        .orderByAsc(ReportImageFileDO::getId))
                .stream()
                .map(this::toImageModel)
                .toList();
    }

    @Override
    public List<ReportToothRecordModel> listToothRecords(Long caseId) {
        return reportToothRecordMapper.selectList(new LambdaQueryWrapper<ReportToothRecordDO>()
                        .eq(ReportToothRecordDO::getCaseId, caseId)
                        .eq(ReportToothRecordDO::getDeletedFlag, 0L)
                        .eq(ReportToothRecordDO::getStatus, "ACTIVE")
                        .orderByAsc(ReportToothRecordDO::getSortOrder)
                        .orderByAsc(ReportToothRecordDO::getToothCode)
                        .orderByAsc(ReportToothRecordDO::getId))
                .stream()
                .map(entity -> new ReportToothRecordModel(
                        entity.getId(),
                        entity.getSourceImageId(),
                        entity.getToothCode(),
                        entity.getToothSurfaceCode(),
                        entity.getIssueTypeCode(),
                        entity.getSeverityCode(),
                        entity.getFindingDesc(),
                        entity.getSuggestion(),
                        entity.getSortOrder()))
                .toList();
    }

    @Override
    public Optional<ReportAnalysisSummaryModel> findLatestSummary(Long caseId) {
        ReportResultSummaryDO entity = reportResultSummaryMapper.selectOne(new LambdaQueryWrapper<ReportResultSummaryDO>()
                .eq(ReportResultSummaryDO::getCaseId, caseId)
                .eq(ReportResultSummaryDO::getDeletedFlag, 0L)
                .eq(ReportResultSummaryDO::getStatus, "ACTIVE")
                .orderByDesc(ReportResultSummaryDO::getId)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportAnalysisSummaryModel(
                entity.getId(),
                entity.getTaskId(),
                entity.getRawResultJson(),
                entity.getOverallHighestSeverity(),
                entity.getUncertaintyScore(),
                entity.getReviewSuggestedFlag(),
                entity.getLesionCount(),
                entity.getAbnormalToothCount()));
    }

    @Override
    public Optional<ReportAnalysisSummaryModel> findSummaryById(Long summaryId) {
        if (summaryId == null) {
            return Optional.empty();
        }
        ReportResultSummaryDO entity = reportResultSummaryMapper.selectOne(new LambdaQueryWrapper<ReportResultSummaryDO>()
                .eq(ReportResultSummaryDO::getId, summaryId)
                .eq(ReportResultSummaryDO::getDeletedFlag, 0L)
                .eq(ReportResultSummaryDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportAnalysisSummaryModel(
                entity.getId(),
                entity.getTaskId(),
                entity.getRawResultJson(),
                entity.getOverallHighestSeverity(),
                entity.getUncertaintyScore(),
                entity.getReviewSuggestedFlag(),
                entity.getLesionCount(),
                entity.getAbnormalToothCount()));
    }

    @Override
    public List<ReportVisualAssetModel> listVisualAssetsByTaskId(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        List<ReportVisualAssetDO> assets = reportVisualAssetMapper.selectList(new LambdaQueryWrapper<ReportVisualAssetDO>()
                .eq(ReportVisualAssetDO::getTaskId, taskId)
                .eq(ReportVisualAssetDO::getDeletedFlag, 0L)
                .eq(ReportVisualAssetDO::getStatus, "ACTIVE")
                .orderByAsc(ReportVisualAssetDO::getSortOrder)
                .orderByAsc(ReportVisualAssetDO::getId));
        if (assets.isEmpty()) {
            return List.of();
        }
        Set<Long> attachmentIds = assets.stream()
                .map(ReportVisualAssetDO::getAttachmentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ReportAttachmentDO> attachmentIndex = attachmentIds.isEmpty()
                ? Map.of()
                : reportAttachmentMapper.selectList(new LambdaQueryWrapper<ReportAttachmentDO>()
                        .in(ReportAttachmentDO::getId, attachmentIds)
                        .eq(ReportAttachmentDO::getDeletedFlag, 0L)
                        .eq(ReportAttachmentDO::getStatus, "ACTIVE"))
                        .stream()
                        .collect(Collectors.toMap(ReportAttachmentDO::getId, Function.identity(), (a, b) -> a, HashMap::new));
        return assets.stream()
                .map(asset -> {
                    ReportAttachmentDO attachment = asset.getAttachmentId() == null ? null : attachmentIndex.get(asset.getAttachmentId());
                    return new ReportVisualAssetModel(
                            asset.getId(),
                            asset.getTaskId(),
                            asset.getAssetTypeCode(),
                            asset.getAttachmentId(),
                            asset.getRelatedImageId(),
                            asset.getSourceAttachmentId(),
                            asset.getToothCode(),
                            asset.getSortOrder(),
                            attachment == null ? null : attachment.getBucketName(),
                            attachment == null ? null : attachment.getObjectKey(),
                            attachment == null ? null : attachment.getContentType(),
                            attachment == null ? null : attachment.getOriginalName());
                })
                .toList();
    }

    @Override
    public Optional<ReportRiskAssessmentModel> findLatestRiskAssessment(Long caseId) {
        ReportRiskAssessmentDO entity = reportRiskAssessmentMapper.selectOne(new LambdaQueryWrapper<ReportRiskAssessmentDO>()
                .eq(ReportRiskAssessmentDO::getCaseId, caseId)
                .eq(ReportRiskAssessmentDO::getDeletedFlag, 0L)
                .eq(ReportRiskAssessmentDO::getStatus, "ACTIVE")
                .orderByDesc(ReportRiskAssessmentDO::getAssessedAt)
                .orderByDesc(ReportRiskAssessmentDO::getId)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportRiskAssessmentModel(
                entity.getId(),
                entity.getOverallRiskLevelCode(),
                entity.getAssessmentReportJson(),
                entity.getRecommendedCycleDays(),
                entity.getAssessedAt()));
    }

    @Override
    public Optional<ReportRiskAssessmentModel> findRiskAssessmentById(Long riskAssessmentId) {
        if (riskAssessmentId == null) {
            return Optional.empty();
        }
        ReportRiskAssessmentDO entity = reportRiskAssessmentMapper.selectOne(new LambdaQueryWrapper<ReportRiskAssessmentDO>()
                .eq(ReportRiskAssessmentDO::getId, riskAssessmentId)
                .eq(ReportRiskAssessmentDO::getDeletedFlag, 0L)
                .eq(ReportRiskAssessmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportRiskAssessmentModel(
                entity.getId(),
                entity.getOverallRiskLevelCode(),
                entity.getAssessmentReportJson(),
                entity.getRecommendedCycleDays(),
                entity.getAssessedAt()));
    }

    @Override
    public Optional<ReportCorrectionModel> findLatestCorrection(Long caseId) {
        ReportCorrectionFeedbackDO entity = reportCorrectionFeedbackMapper.selectOne(new LambdaQueryWrapper<ReportCorrectionFeedbackDO>()
                .eq(ReportCorrectionFeedbackDO::getCaseId, caseId)
                .eq(ReportCorrectionFeedbackDO::getDeletedFlag, 0L)
                .eq(ReportCorrectionFeedbackDO::getStatus, "ACTIVE")
                .orderByDesc(ReportCorrectionFeedbackDO::getCreatedAt)
                .orderByDesc(ReportCorrectionFeedbackDO::getId)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportCorrectionModel(
                entity.getId(),
                entity.getFeedbackTypeCode(),
                entity.getCorrectedTruthJson(),
                entity.getCreatedAt()));
    }

    @Override
    public Optional<ReportCorrectionModel> findCorrectionById(Long correctionId) {
        if (correctionId == null) {
            return Optional.empty();
        }
        ReportCorrectionFeedbackDO entity = reportCorrectionFeedbackMapper.selectOne(new LambdaQueryWrapper<ReportCorrectionFeedbackDO>()
                .eq(ReportCorrectionFeedbackDO::getId, correctionId)
                .eq(ReportCorrectionFeedbackDO::getDeletedFlag, 0L)
                .eq(ReportCorrectionFeedbackDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new ReportCorrectionModel(
                entity.getId(),
                entity.getFeedbackTypeCode(),
                entity.getCorrectedTruthJson(),
                entity.getCreatedAt()));
    }

    @Override
    public List<ReportCorrectionModel> listCorrections(Long caseId) {
        return reportCorrectionFeedbackMapper.selectList(new LambdaQueryWrapper<ReportCorrectionFeedbackDO>()
                        .eq(ReportCorrectionFeedbackDO::getCaseId, caseId)
                        .eq(ReportCorrectionFeedbackDO::getDeletedFlag, 0L)
                        .eq(ReportCorrectionFeedbackDO::getStatus, "ACTIVE")
                        .orderByAsc(ReportCorrectionFeedbackDO::getCreatedAt)
                        .orderByAsc(ReportCorrectionFeedbackDO::getId))
                .stream()
                .map(entity -> new ReportCorrectionModel(
                        entity.getId(),
                        entity.getFeedbackTypeCode(),
                        entity.getCorrectedTruthJson(),
                        entity.getCreatedAt()))
                .toList();
    }

    private ReportImageModel toImageModel(ReportImageFileDO entity) {
        ReportAttachmentDO attachment = reportAttachmentMapper.selectOne(new LambdaQueryWrapper<ReportAttachmentDO>()
                .eq(ReportAttachmentDO::getId, entity.getAttachmentId())
                .eq(ReportAttachmentDO::getDeletedFlag, 0L)
                .eq(ReportAttachmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        if (attachment == null) {
            return new ReportImageModel(
                    entity.getId(),
                    entity.getAttachmentId(),
                    entity.getImageTypeCode(),
                    entity.getQualityStatusCode(),
                    entity.getIsPrimary(),
                    null,
                    null,
                    null);
        }
        return new ReportImageModel(
                entity.getId(),
                entity.getAttachmentId(),
                entity.getImageTypeCode(),
                entity.getQualityStatusCode(),
                entity.getIsPrimary(),
                attachment.getBucketName(),
                attachment.getObjectKey(),
                attachment.getOriginalName());
    }
}
