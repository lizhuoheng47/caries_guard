package com.cariesguard.followup.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.domain.repository.FupRecordRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.interfaces.vo.FollowupPlanVO;
import com.cariesguard.followup.interfaces.vo.FollowupRecordVO;
import com.cariesguard.followup.interfaces.vo.FollowupTaskVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FollowupQueryService {

    private final FupPlanRepository fupPlanRepository;
    private final FupTaskRepository fupTaskRepository;
    private final FupRecordRepository fupRecordRepository;

    public FollowupQueryService(FupPlanRepository fupPlanRepository,
                                FupTaskRepository fupTaskRepository,
                                FupRecordRepository fupRecordRepository) {
        this.fupPlanRepository = fupPlanRepository;
        this.fupTaskRepository = fupTaskRepository;
        this.fupRecordRepository = fupRecordRepository;
    }

    public List<FollowupPlanVO> listCasePlans(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        var plans = fupPlanRepository.listByCaseId(caseId);
        ensureOrgAccess(operator, plans.stream().findFirst().map(p -> p.orgId()).orElse(null));
        return plans.stream()
                .map(p -> new FollowupPlanVO(
                        p.planId(), p.planNo(), p.caseId(), p.patientId(),
                        p.planTypeCode(), p.planStatusCode(), p.nextFollowupDate(),
                        p.intervalDays(), p.ownerUserId(), p.triggerSourceCode(),
                        p.triggerRefId(), p.remark(), p.createdAt()))
                .toList();
    }

    public List<FollowupTaskVO> listCaseTasks(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        var tasks = fupTaskRepository.listByCaseId(caseId);
        ensureOrgAccess(operator, tasks.stream().findFirst().map(t -> t.orgId()).orElse(null));
        return tasks.stream()
                .map(t -> new FollowupTaskVO(
                        t.taskId(), t.taskNo(), t.planId(), t.caseId(),
                        t.taskTypeCode(), t.taskStatusCode(), t.assignedToUserId(),
                        t.dueDate(), t.startedAt(), t.completedAt(),
                        t.remark(), t.createdAt()))
                .toList();
    }

    public List<FollowupRecordVO> listCaseRecords(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        var records = fupRecordRepository.listByCaseId(caseId);
        ensureOrgAccess(operator, records.stream().findFirst().map(r -> r.orgId()).orElse(null));
        return records.stream()
                .map(r -> new FollowupRecordVO(
                        r.recordId(), r.recordNo(), r.taskId(), r.planId(), r.caseId(),
                        r.followupMethodCode(), r.contactResultCode(),
                        "1".equals(r.followNextFlag()), r.nextIntervalDays(),
                        r.outcomeSummary(), r.doctorNotes(), r.recordedAt(), r.createdAt()))
                .toList();
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (recordOrgId == null) {
            return;
        }
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
