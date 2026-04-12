package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.VisitDetailModel;
import com.cariesguard.patient.domain.model.VisitSummaryModel;
import java.util.Optional;

public interface VisitQueryRepository {

    Optional<VisitDetailModel> findVisitDetail(Long visitId);

    PageQueryResult<VisitSummaryModel> pageVisits(Long orgId,
                                                  int pageNo,
                                                  int pageSize,
                                                  Long patientId,
                                                  Long doctorUserId,
                                                  String visitTypeCode);
}
