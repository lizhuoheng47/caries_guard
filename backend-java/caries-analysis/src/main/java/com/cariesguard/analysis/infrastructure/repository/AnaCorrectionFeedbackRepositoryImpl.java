package com.cariesguard.analysis.infrastructure.repository;

import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;
import com.cariesguard.analysis.domain.repository.AnaCorrectionFeedbackRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaCorrectionFeedbackDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaCorrectionFeedbackMapper;
import org.springframework.stereotype.Repository;

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
        entity.setDoctorUserId(model.doctorUserId());
        entity.setOriginalInferenceJson(model.originalInferenceJson());
        entity.setCorrectedTruthJson(model.correctedTruthJson());
        entity.setFeedbackTypeCode(model.feedbackTypeCode());
        entity.setIsExportedForTrain(model.exportedForTrainFlag());
        entity.setTrainingCandidateFlag(model.trainingCandidateFlag());
        entity.setDesensitizedExportFlag(model.desensitizedExportFlag());
        entity.setDatasetSnapshotNo(model.datasetSnapshotNo());
        entity.setReviewStatusCode(model.reviewStatusCode());
        entity.setReviewedBy(model.reviewedBy());
        entity.setReviewedAt(model.reviewedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.doctorUserId());
        anaCorrectionFeedbackMapper.insert(entity);
    }
}

