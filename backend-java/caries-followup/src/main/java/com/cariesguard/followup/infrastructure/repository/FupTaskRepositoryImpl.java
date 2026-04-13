package com.cariesguard.followup.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.FupTaskModel;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.infrastructure.dataobject.FupTaskDO;
import com.cariesguard.followup.infrastructure.mapper.FupTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class FupTaskRepositoryImpl implements FupTaskRepository {

    private static final Set<String> TERMINAL_STATUSES = Set.of("DONE", "CANCELLED");

    private final FupTaskMapper fupTaskMapper;

    public FupTaskRepositoryImpl(FupTaskMapper fupTaskMapper) {
        this.fupTaskMapper = fupTaskMapper;
    }

    @Override
    public void create(FupTaskCreateModel model) {
        FupTaskDO entity = new FupTaskDO();
        entity.setId(model.taskId());
        entity.setTaskNo(model.taskNo());
        entity.setPlanId(model.planId());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setTaskTypeCode(model.taskTypeCode());
        entity.setTaskStatusCode(model.taskStatusCode());
        entity.setAssignedToUserId(model.assignedToUserId());
        entity.setDueDate(model.dueDate());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        fupTaskMapper.insert(entity);
    }

    @Override
    public Optional<FupTaskModel> findById(Long taskId) {
        FupTaskDO entity = fupTaskMapper.selectOne(new LambdaQueryWrapper<FupTaskDO>()
                .eq(FupTaskDO::getId, taskId)
                .eq(FupTaskDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public List<FupTaskModel> listByPlanId(Long planId) {
        return fupTaskMapper.selectList(new LambdaQueryWrapper<FupTaskDO>()
                        .eq(FupTaskDO::getPlanId, planId)
                        .eq(FupTaskDO::getDeletedFlag, 0L)
                        .orderByAsc(FupTaskDO::getDueDate)
                        .orderByDesc(FupTaskDO::getCreatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<FupTaskModel> listByCaseId(Long caseId) {
        return fupTaskMapper.selectList(new LambdaQueryWrapper<FupTaskDO>()
                        .eq(FupTaskDO::getCaseId, caseId)
                        .eq(FupTaskDO::getDeletedFlag, 0L)
                        .orderByDesc(FupTaskDO::getCreatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void updateStatus(Long taskId, String taskStatusCode, LocalDateTime completedAt, Long operatorUserId) {
        fupTaskMapper.update(null, new LambdaUpdateWrapper<FupTaskDO>()
                .eq(FupTaskDO::getId, taskId)
                .eq(FupTaskDO::getDeletedFlag, 0L)
                .set(FupTaskDO::getTaskStatusCode, taskStatusCode)
                .set(FupTaskDO::getCompletedAt, completedAt)
                .set(FupTaskDO::getUpdatedBy, operatorUserId));
    }

    @Override
    public void assignTask(Long taskId, Long assignedToUserId, Long operatorUserId) {
        fupTaskMapper.update(null, new LambdaUpdateWrapper<FupTaskDO>()
                .eq(FupTaskDO::getId, taskId)
                .eq(FupTaskDO::getDeletedFlag, 0L)
                .set(FupTaskDO::getAssignedToUserId, assignedToUserId)
                .set(FupTaskDO::getUpdatedBy, operatorUserId));
    }

    @Override
    public boolean allTasksDoneOrCancelled(Long planId) {
        Long activeCount = fupTaskMapper.selectCount(new LambdaQueryWrapper<FupTaskDO>()
                .eq(FupTaskDO::getPlanId, planId)
                .eq(FupTaskDO::getDeletedFlag, 0L)
                .notIn(FupTaskDO::getTaskStatusCode, TERMINAL_STATUSES));
        return activeCount == null || activeCount == 0;
    }

    private FupTaskModel toModel(FupTaskDO entity) {
        return new FupTaskModel(
                entity.getId(),
                entity.getTaskNo(),
                entity.getPlanId(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getTaskTypeCode(),
                entity.getTaskStatusCode(),
                entity.getAssignedToUserId(),
                entity.getDueDate(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getOrgId(),
                entity.getRemark(),
                entity.getCreatedAt());
    }
}
