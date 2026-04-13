package com.cariesguard.followup.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.domain.model.MsgNotifyCreateModel;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.domain.repository.MsgNotifyRepository;
import com.cariesguard.followup.domain.service.FollowupDomainService;
import com.cariesguard.followup.interfaces.command.CreateFollowupTaskCommand;
import com.cariesguard.followup.interfaces.command.UpdateFollowupTaskStatusCommand;
import com.cariesguard.followup.interfaces.vo.FollowupTaskVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FollowupTaskAppService {

    private static final Logger log = LoggerFactory.getLogger(FollowupTaskAppService.class);

    private static final Set<String> VALID_TASK_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "DONE", "OVERDUE", "CANCELLED");
    private static final Set<String> TERMINAL_STATUSES = Set.of("DONE", "CANCELLED");

    private final FupTaskRepository fupTaskRepository;
    private final FupPlanRepository fupPlanRepository;
    private final MsgNotifyRepository msgNotifyRepository;
    private final FollowupDomainService followupDomainService;

    public FollowupTaskAppService(FupTaskRepository fupTaskRepository,
                                  FupPlanRepository fupPlanRepository,
                                  MsgNotifyRepository msgNotifyRepository,
                                  FollowupDomainService followupDomainService) {
        this.fupTaskRepository = fupTaskRepository;
        this.fupPlanRepository = fupPlanRepository;
        this.msgNotifyRepository = msgNotifyRepository;
        this.followupDomainService = followupDomainService;
    }

    @Transactional
    public FollowupTaskVO createTask(Long caseId, CreateFollowupTaskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        var plan = fupPlanRepository.findById(command.planId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Followup plan not found"));
        ensureOrgAccess(operator, plan.orgId());
        if (TERMINAL_STATUSES.contains(plan.planStatusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Cannot add task to a closed or cancelled plan");
        }

        long taskId = IdWorker.getId();
        String taskTypeCode = StringUtils.hasText(command.taskTypeCode()) ? command.taskTypeCode() : "FOLLOW_CONTACT";
        fupTaskRepository.create(new FupTaskCreateModel(
                taskId,
                followupDomainService.buildTaskNo(taskId),
                plan.planId(),
                caseId,
                plan.patientId(),
                taskTypeCode,
                "TODO",
                command.assignedToUserId(),
                command.dueDate(),
                plan.orgId(),
                command.remark(),
                operator.getUserId()));

        return fupTaskRepository.findById(taskId).map(this::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.SYSTEM_ERROR));
    }

    @Transactional
    public FollowupTaskVO updateTaskStatus(Long taskId, UpdateFollowupTaskStatusCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupTaskModel task = fupTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Task not found"));
        ensureOrgAccess(operator, task.orgId());

        String target = command.targetStatusCode().trim().toUpperCase();
        if (!VALID_TASK_STATUSES.contains(target)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Invalid task status: " + target);
        }
        if (TERMINAL_STATUSES.contains(task.taskStatusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Task is already in terminal status");
        }

        LocalDateTime completedAt = "DONE".equals(target) ? LocalDateTime.now() : null;
        fupTaskRepository.updateStatus(taskId, target, completedAt, operator.getUserId());

        if ("DONE".equals(target)) {
            autoClosePlanIfAllDone(task.planId(), operator.getUserId());
        }

        if ("OVERDUE".equals(target)) {
            recordOverdueNotify(task, operator.getUserId());
        }

        return fupTaskRepository.findById(taskId).map(this::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.SYSTEM_ERROR));
    }

    @Transactional
    public void assignTask(Long taskId, Long assigneeUserId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupTaskModel task = fupTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Task not found"));
        ensureOrgAccess(operator, task.orgId());
        fupTaskRepository.assignTask(taskId, assigneeUserId, operator.getUserId());
    }

    public List<FollowupTaskVO> listByPlan(Long planId) {
        return fupTaskRepository.listByPlanId(planId).stream().map(this::toVO).toList();
    }

    public List<FollowupTaskVO> listByCase(Long caseId) {
        return fupTaskRepository.listByCaseId(caseId).stream().map(this::toVO).toList();
    }

    public FollowupTaskVO getTask(Long taskId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupTaskModel task = fupTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Task not found"));
        ensureOrgAccess(operator, task.orgId());
        return toVO(task);
    }

    private void autoClosePlanIfAllDone(Long planId, Long operatorUserId) {
        if (fupTaskRepository.allTasksDoneOrCancelled(planId)) {
            fupPlanRepository.updateStatus(planId, "DONE", operatorUserId);
            log.info("Plan {} auto-closed: all tasks done or cancelled", planId);
        }
    }

    private void recordOverdueNotify(FupTaskModel task, Long operatorUserId) {
        try {
            long notifyId = IdWorker.getId();
            msgNotifyRepository.create(new MsgNotifyCreateModel(
                    notifyId,
                    "FOLLOWUP",
                    task.taskId(),
                    task.assignedToUserId() != null ? task.assignedToUserId() : operatorUserId,
                    "ALERT",
                    "IN_APP",
                    "随访任务已逾期",
                    "任务 " + task.taskNo() + " 已超过截止日期，请尽快处理",
                    "PENDING",
                    task.orgId(),
                    operatorUserId));
        } catch (Exception e) {
            log.warn("Failed to record overdue notify for taskId={}: {}", task.taskId(), e.getMessage());
        }
    }

    private FollowupTaskVO toVO(FupTaskModel model) {
        return new FollowupTaskVO(
                model.taskId(),
                model.taskNo(),
                model.planId(),
                model.caseId(),
                model.taskTypeCode(),
                model.taskStatusCode(),
                model.assignedToUserId(),
                model.dueDate(),
                model.startedAt(),
                model.completedAt(),
                model.remark(),
                model.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
