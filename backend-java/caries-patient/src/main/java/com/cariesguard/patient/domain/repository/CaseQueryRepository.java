package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.CaseDetailModel;
import com.cariesguard.patient.domain.model.CaseSummaryModel;
import com.cariesguard.patient.domain.model.PageQueryResult;
import java.util.Optional;

public interface CaseQueryRepository {

    Optional<CaseDetailModel> findCaseDetail(Long caseId);

    PageQueryResult<CaseSummaryModel> pageCases(Long orgId,
                                                int pageNo,
                                                int pageSize,
                                                Long patientId,
                                                String caseStatusCode,
                                                Long attendingDoctorId);
}
