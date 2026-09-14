package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.domain.repository.AnaReviewDraftRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaReviewDraftDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaReviewDraftMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnaReviewDraftRepositoryImpl implements AnaReviewDraftRepository {
    private final AnaReviewDraftMapper mapper;

    public AnaReviewDraftRepositoryImpl(AnaReviewDraftMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AnaReviewDraftDO> findByTaskAndDoctor(Long taskId, Long doctorUserId) {
        AnaReviewDraftDO entity = mapper.selectOne(new LambdaQueryWrapper<AnaReviewDraftDO>()
                .eq(AnaReviewDraftDO::getTaskId, taskId)
                .eq(AnaReviewDraftDO::getDoctorUserId, doctorUserId)
                .eq(AnaReviewDraftDO::getStatus, "ACTIVE")
                .eq(AnaReviewDraftDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity);
    }

    @Override
    public AnaReviewDraftDO saveDraft(AnaReviewDraftDO draft) {
        Optional<AnaReviewDraftDO> existing = findByTaskAndDoctor(draft.getTaskId(), draft.getDoctorUserId());
        if (existing.isEmpty()) {
            draft.setId(IdWorker.getId());
            draft.setVersionNo(1);
            draft.setDraftStatusCode("DRAFT");
            draft.setStatus("ACTIVE");
            draft.setDeletedFlag(0L);
            draft.setCreatedBy(draft.getDoctorUserId());
            draft.setUpdatedBy(draft.getDoctorUserId());
            mapper.insert(draft);
            return findByTaskAndDoctor(draft.getTaskId(), draft.getDoctorUserId()).orElse(draft);
        }

        AnaReviewDraftDO current = existing.get();
        if ("SUBMITTED".equals(current.getDraftStatusCode())) {
            return current;
        }
        mapper.update(null, new LambdaUpdateWrapper<AnaReviewDraftDO>()
                .eq(AnaReviewDraftDO::getId, current.getId())
                .eq(AnaReviewDraftDO::getDraftStatusCode, "DRAFT")
                .set(AnaReviewDraftDO::getRevisedGrade, draft.getRevisedGrade())
                .set(AnaReviewDraftDO::getRevisedDetectionsJson, draft.getRevisedDetectionsJson())
                .set(AnaReviewDraftDO::getReasonTagsJson, draft.getReasonTagsJson())
                .set(AnaReviewDraftDO::getNote, draft.getNote())
                .set(AnaReviewDraftDO::getVersionNo, current.getVersionNo() + 1)
                .set(AnaReviewDraftDO::getUpdatedBy, draft.getDoctorUserId()));
        return findByTaskAndDoctor(draft.getTaskId(), draft.getDoctorUserId()).orElse(current);
    }

    @Override
    public void markSubmitted(Long draftId, Long feedbackId, Long operatorUserId) {
        mapper.update(null, new LambdaUpdateWrapper<AnaReviewDraftDO>()
                .eq(AnaReviewDraftDO::getId, draftId)
                .eq(AnaReviewDraftDO::getDraftStatusCode, "DRAFT")
                .set(AnaReviewDraftDO::getDraftStatusCode, "SUBMITTED")
                .set(AnaReviewDraftDO::getSubmittedFeedbackId, feedbackId)
                .set(AnaReviewDraftDO::getUpdatedBy, operatorUserId));
    }
}
