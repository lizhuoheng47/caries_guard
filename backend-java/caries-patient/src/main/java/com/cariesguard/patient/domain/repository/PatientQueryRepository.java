package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.PatientDetailModel;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.PatientSummaryModel;
import java.util.Optional;

public interface PatientQueryRepository {

    Optional<PatientDetailModel> findPatientDetail(Long patientId);

    PageQueryResult<PatientSummaryModel> pagePatients(Long orgId,
                                                      int pageNo,
                                                      int pageSize,
                                                      String keyword,
                                                      String sourceCode,
                                                      String status);
}
