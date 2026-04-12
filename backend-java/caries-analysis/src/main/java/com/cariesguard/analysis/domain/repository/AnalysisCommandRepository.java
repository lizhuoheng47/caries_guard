package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import java.util.List;
import java.util.Optional;

public interface AnalysisCommandRepository {

    Optional<AnalysisCaseModel> findCase(Long caseId);

    Optional<AnalysisPatientModel> findPatient(Long patientId);

    List<AnalysisImageModel> listImages(Long caseId, List<Long> imageIds);

    void createTask(AnalysisTaskCreateModel model);
}
