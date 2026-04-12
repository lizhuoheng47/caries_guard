package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.CaseStatusUpdateModel;
import com.cariesguard.patient.domain.model.CaseToothRecordCreateModel;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import java.time.LocalDateTime;
import java.util.List;
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

    void replaceDiagnoses(Long caseId,
                          Long operatorUserId,
                          LocalDateTime diagnosisTime,
                          List<CaseDiagnosisCreateModel> diagnoses);

    void replaceToothRecords(Long caseId,
                             Long operatorUserId,
                             List<CaseToothRecordCreateModel> toothRecords);
}
