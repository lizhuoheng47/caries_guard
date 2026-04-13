package com.cariesguard.followup.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.followup.domain.model.FupRecordCreateModel;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.domain.repository.FupRecordRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.domain.service.FollowupDomainService;
import com.cariesguard.followup.interfaces.command.CreateFollowupRecordCommand;
import com.cariesguard.followup.interfaces.vo.FollowupRecordVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowupRecordAppService {

    private static final Logger log = LoggerFactory.getLogger(FollowupRecordAppService.class);

    private final FupRecordRepository fupRecordRepository;
    private final FupTaskRepository fupTaskRepository;
    private final FollowupDomainService followupDomainService;

    public FollowupRecordAppService(FupRecordRepository fupRecordRepository,
                                    FupTaskRepository fupTaskRepository,
                                    FollowupDomainService followupDomainService) {
        this.fupRecordRepository = fupRecordRepository;
        this.fupTaskRepository = fupTaskRepository;
        this.followupDomainService = followupDomainService;
    }

    /**
     * 新增随访记录，同时将关联任务标记为完成。
     * 若 followNext=true，在原计划下自动派生下一个任务。
     */
    @Transactional
    public FollowupRecordVO addRecord(CreateFollowupRecordCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupTaskModel task = fupTaskRepository.findById(command.taskId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Task not found"));
        ensureOrgAccess(operator, task.orgId());

        LocalDateTime now = LocalDateTime.now();
        long recordId = IdWorker.getId();
        String followNextFlag = Boolean.TRUE.equals(command.followNext()) ? "1" : "0";

        fupRecordRepository.create(new FupRecordCreateModel(
                recordId,
                followupDomainService.buildRecordNo(recordId),
                task.taskId(),
                task.planId(),
                task.caseId(),
                task.patientId(),
                followupDomainService.normalizeFollowupMethod(command.followupMethodCode()),
                followupDomainService.normalizeContactResult(command.contactResultCode()),
                followNextFlag,
                command.nextIntervalDays(),
                command.outcomeSummary(),
                command.doctorNotes(),
                now,
                task.orgId(),
                command.remark(),
                operator.getUserId()));

        // 回填任务完成
        fupTaskRepository.updateStatus(task.taskId(), "DONE", now, operator.getUserId());

        // 若建议继续随访，在原计划下派生下一个任务
        if ("1".equals(followNextFlag)) {
            deriveNextTask(task, command.nextIntervalDays(), operator.getUserId());
        }

        return fupRecordRepository.findById(recordId).map(this::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.SYSTEM_ERROR));
    }

    public List<FollowupRecordVO> listByTask(Long taskId) {
        return fupRecordRepository.listByTaskId(taskId).stream().map(this::toVO).toList();
    }

    public List<FollowupRecordVO> listByCase(Long caseId) {
        return fupRecordRepository.listByCaseId(caseId).stream().map(this::toVO).toList();
    }

    private void deriveNextTask(FupTaskModel completedTask, Integer nextIntervalDays, Long operatorUserId) {
        int interval = nextIntervalDays != null && nextIntervalDays > 0 ? nextIntervalDays : 30;
        long newTaskId = IdWorker.getId();
        try {
            fupTaskRepository.create(new FupTaskCreateModel(
                    newTaskId,
                    followupDomainService.buildTaskNo(newTaskId),
                    completedTask.planId(),
                    completedTask.caseId(),
                    completedTask.patientId(),
                    "FOLLOW_CONTACT",
                    "TODO",
                    completedTask.assignedToUserId(),
                    followupDomainService.resolveFirstTaskDueDate(interval),
                    completedTask.orgId(),
                    "Derived from record followNext=true",
                    operatorUserId));
            log.info("Derived next task {} for plan {}", newTaskId, completedTask.planId());
        } catch (Exception e) {
            log.warn("Failed to derive next task for planId={}: {}", completedTask.planId(), e.getMessage());
        }
    }

    private FollowupRecordVO toVO(com.cariesguard.followup.domain.model.FupRecordModel model) {
        return new FollowupRecordVO(
                model.recordId(),
                model.recordNo(),
                model.taskId(),
                model.planId(),
                model.caseId(),
                model.followupMethodCode(),
                model.contactResultCode(),
                "1".equals(model.followNextFlag()),
                model.nextIntervalDays(),
                model.outcomeSummary(),
                model.doctorNotes(),
                model.recordedAt(),
                model.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
