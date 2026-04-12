package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.CaseStatusUpdateModel;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import java.util.Optional;

public interface VisitCaseCommandRepository {

    Optional<PatientOwnedModel> findPatient(Long patientId);

    Optional<VisitOwnedModel> findVisit(Long visitId);

    void createVisit(VisitCreateModel model);

    void createCase(CaseCreateModel model, CaseStatusLogCreateModel statusLog);

    Optional<CaseManagedModel> findManagedCase(Long caseId);

    boolean hasActiveImage(Long caseId);

    boolean hasAiSummary(Long caseId);

    void updateCaseStatus(CaseStatusUpdateModel model);

    void appendCaseStatusLog(CaseStatusLogCreateModel model);
}
