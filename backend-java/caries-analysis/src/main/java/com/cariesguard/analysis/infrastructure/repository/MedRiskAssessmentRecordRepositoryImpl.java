package com.cariesguard.analysis.infrastructure.repository;

import com.cariesguard.analysis.domain.model.RiskAssessmentCreateModel;
import com.cariesguard.analysis.domain.repository.MedRiskAssessmentRecordRepository;
import com.cariesguard.analysis.infrastructure.dataobject.MedRiskAssessmentRecordDO;
import com.cariesguard.analysis.infrastructure.mapper.MedRiskAssessmentRecordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MedRiskAssessmentRecordRepositoryImpl implements MedRiskAssessmentRecordRepository {

    private final MedRiskAssessmentRecordMapper medRiskAssessmentRecordMapper;

    public MedRiskAssessmentRecordRepositoryImpl(MedRiskAssessmentRecordMapper medRiskAssessmentRecordMapper) {
        this.medRiskAssessmentRecordMapper = medRiskAssessmentRecordMapper;
    }

    @Override
    public void save(RiskAssessmentCreateModel model) {
        MedRiskAssessmentRecordDO entity = new MedRiskAssessmentRecordDO();
        entity.setId(model.recordId());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setTaskId(model.taskId());
        entity.setOverallRiskLevelCode(model.overallRiskLevelCode());
        entity.setRiskScore(model.riskScore());
        entity.setAssessmentReportJson(model.assessmentReportJson());
        entity.setRecommendedCycleDays(model.recommendedCycleDays());
        entity.setVersionNo(model.versionNo());
        entity.setAssessedAt(model.assessedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        medRiskAssessmentRecordMapper.insert(entity);
    }
}
