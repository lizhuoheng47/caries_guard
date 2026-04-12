package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.RiskAssessmentCreateModel;

public interface MedRiskAssessmentRecordRepository {

    void save(RiskAssessmentCreateModel model);
}
