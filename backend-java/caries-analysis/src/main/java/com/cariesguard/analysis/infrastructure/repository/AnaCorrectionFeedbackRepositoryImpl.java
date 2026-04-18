package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.model.CorrectionFeedbackExportCandidateModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaCorrectionFeedbackDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaCorrectionFeedbackMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
public class AnaCorrectionFeedbackRepositoryImpl implements AnaCorrectionFeedbackRepository {

    private final AnaCorrectionFeedbackMapper anaCorrectionFeedbackMapper;

    public AnaCorrectionFeedbackRepositoryImpl(AnaCorrectionFeedbackMapper anaCorrectionFeedbackMapper) {
        this.anaCorrectionFeedbackMapper = anaCorrectionFeedbackMapper;
    }

    @Override
    public void save(CorrectionFeedbackCreateModel model) {
        AnaCorrectionFeedbackDO entity = new AnaCorrectionFeedbackDO();
        entity.setId(model.feedbackId());
        entity.setCaseId(model.caseId());
        entity.setDiagnosisId(model.diagnosisId());
        entity.setSourceImageId(model.sourceImageId());
        entity.setSourceAttachmentId(model.sourceAttachmentId());
        entity.setDoctorUserId(model.doctorUserId());
        entity.setOriginalInferenceJson(model.originalInferenceJson());
        entity.setCorrectedTruthJson(model.correctedTruthJson());
        entity.setFeedbackTypeCode(model.feedbackTypeCode());
        entity.setIsExportedForTrain(model.exportCandidateFlag());
        entity.setExportCandidateFlag(model.exportCandidateFlag());
        entity.setExportedSnapshotNo(model.exportedSnapshotNo());
        entity.setTrainingCandidateFlag(model.trainingCandidateFlag());
        entity.setDesensitizedExportFlag(model.desensitizedExportFlag());
        entity.setDatasetSnapshotNo(model.exportedSnapshotNo());
        entity.setReviewStatusCode(model.reviewStatusCode());
        entity.setReviewedBy(model.reviewedBy());
        entity.setReviewedAt(model.reviewedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.doctorUserId());
        anaCorrectionFeedbackMapper.insert(entity);
    }

    @Override
    public List<CorrectionFeedbackExportCandidateModel> listTrainingCandidates(Long orgId, int limit) {
        LambdaQueryWrapper<AnaCorrectionFeedbackDO> wrapper = new LambdaQueryWrapper<AnaCorrectionFeedbackDO>()
                .eq(AnaCorrectionFeedbackDO::getTrainingCandidateFlag, "1")
                .eq(AnaCorrectionFeedbackDO::getExportCandidateFlag, "1")
                .eq(AnaCorrectionFeedbackDO::getStatus, "ACTIVE")
                .eq(AnaCorrectionFeedbackDO::getDeletedFlag, 0L)
                .eq(AnaCorrectionFeedbackDO::getReviewStatusCode, "APPROVED")
                .and(item -> item.isNull(AnaCorrectionFeedbackDO::getExportedSnapshotNo)
                        .or()
                        .eq(AnaCorrectionFeedbackDO::getExportedSnapshotNo, ""))
                .eq(orgId != null, AnaCorrectionFeedbackDO::getOrgId, orgId)
                .orderByAsc(AnaCorrectionFeedbackDO::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return anaCorrectionFeedbackMapper.selectList(wrapper).stream()
                .map(this::toCandidateModel)
                .toList();
    }

    @Override
    public void markExported(List<Long> feedbackIds, String snapshotNo) {
        if (CollectionUtils.isEmpty(feedbackIds)) {
            return;
        }
        AnaCorrectionFeedbackDO update = new AnaCorrectionFeedbackDO();
        update.setIsExportedForTrain("1");
        update.setExportedSnapshotNo(snapshotNo);
        update.setDesensitizedExportFlag("1");
        update.setDatasetSnapshotNo(snapshotNo);
        LambdaUpdateWrapper<AnaCorrectionFeedbackDO> wrapper = new LambdaUpdateWrapper<AnaCorrectionFeedbackDO>()
                .in(AnaCorrectionFeedbackDO::getId, feedbackIds)
                .eq(AnaCorrectionFeedbackDO::getDeletedFlag, 0L);
        anaCorrectionFeedbackMapper.update(update, wrapper);
    }

    @Override
    public int reviewFeedbacks(List<Long> feedbackIds,
                               String reviewStatusCode,
                               String trainingCandidateFlag,
                               Long reviewerUserId,
                               LocalDateTime reviewedAt,
                               Long orgId) {
        if (CollectionUtils.isEmpty(feedbackIds)) {
            return 0;
        }
        AnaCorrectionFeedbackDO update = new AnaCorrectionFeedbackDO();
        update.setReviewStatusCode(reviewStatusCode);
        update.setReviewedBy(reviewerUserId);
        update.setReviewedAt(reviewedAt);
        if (StringUtils.hasText(trainingCandidateFlag)) {
            update.setTrainingCandidateFlag(trainingCandidateFlag);
        }
        LambdaUpdateWrapper<AnaCorrectionFeedbackDO> wrapper = new LambdaUpdateWrapper<AnaCorrectionFeedbackDO>()
                .in(AnaCorrectionFeedbackDO::getId, feedbackIds)
                .eq(AnaCorrectionFeedbackDO::getStatus, "ACTIVE")
                .eq(AnaCorrectionFeedbackDO::getDeletedFlag, 0L)
                .and(item -> item.isNull(AnaCorrectionFeedbackDO::getExportedSnapshotNo)
                        .or()
                        .eq(AnaCorrectionFeedbackDO::getExportedSnapshotNo, ""))
                .eq(orgId != null, AnaCorrectionFeedbackDO::getOrgId, orgId);
        return anaCorrectionFeedbackMapper.update(update, wrapper);
    }

    private CorrectionFeedbackExportCandidateModel toCandidateModel(AnaCorrectionFeedbackDO entity) {
        return new CorrectionFeedbackExportCandidateModel(
                entity.getId(),
                entity.getCaseId(),
                entity.getDiagnosisId(),
                entity.getSourceImageId(),
                entity.getSourceAttachmentId(),
                entity.getDoctorUserId(),
                entity.getOriginalInferenceJson(),
                entity.getCorrectedTruthJson(),
                entity.getFeedbackTypeCode(),
                entity.getReviewStatusCode(),
                entity.getOrgId(),
                entity.getCreatedAt());
    }
}
