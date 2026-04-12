package com.cariesguard.patient.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.VisitDetailModel;
import com.cariesguard.patient.domain.model.VisitSummaryModel;
import com.cariesguard.patient.domain.repository.VisitQueryRepository;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.cariesguard.patient.interfaces.vo.VisitDetailVO;
import com.cariesguard.patient.interfaces.vo.VisitListItemVO;
import org.springframework.stereotype.Service;

@Service
public class VisitQueryAppService {

    private final VisitQueryRepository visitQueryRepository;

    public VisitQueryAppService(VisitQueryRepository visitQueryRepository) {
        this.visitQueryRepository = visitQueryRepository;
    }

    public VisitDetailVO getVisit(Long visitId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        VisitDetailModel visit = visitQueryRepository.findVisitDetail(visitId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Visit does not exist"));
        ensureOrgAccess(operator, visit.orgId());
        return new VisitDetailVO(
                visit.visitId(),
                visit.visitNo(),
                visit.patientId(),
                visit.departmentId(),
                visit.doctorUserId(),
                visit.visitTypeCode(),
                visit.visitDate(),
                visit.complaint(),
                visit.triageLevelCode(),
                visit.sourceChannelCode(),
                visit.status(),
                visit.remark());
    }

    public PageResultVO<VisitListItemVO> pageVisits(int pageNo,
                                                    int pageSize,
                                                    Long patientId,
                                                    Long doctorUserId,
                                                    String visitTypeCode) {
        PageQueryResult<VisitSummaryModel> result = visitQueryRepository.pageVisits(
                currentOrgScope(),
                pageNo,
                pageSize,
                patientId,
                doctorUserId,
                visitTypeCode);
        return new PageResultVO<>(
                result.records().stream().map(this::toVisitListItemVO).toList(),
                result.total(),
                result.pageNo(),
                result.pageSize());
    }

    private VisitListItemVO toVisitListItemVO(VisitSummaryModel model) {
        return new VisitListItemVO(
                model.visitId(),
                model.visitNo(),
                model.patientId(),
                model.doctorUserId(),
                model.visitTypeCode(),
                model.visitDate(),
                model.triageLevelCode(),
                model.status());
    }

    private Long currentOrgScope() {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        return operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
