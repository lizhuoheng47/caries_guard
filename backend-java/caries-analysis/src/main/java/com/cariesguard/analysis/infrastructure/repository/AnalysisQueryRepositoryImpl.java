package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnalysisQueryRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaTaskRecordDO;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisTaskRecordMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnalysisQueryRepositoryImpl implements AnalysisQueryRepository {

    private final AnalysisTaskRecordMapper analysisTaskRecordMapper;

    public AnalysisQueryRepositoryImpl(AnalysisTaskRecordMapper analysisTaskRecordMapper) {
        this.analysisTaskRecordMapper = analysisTaskRecordMapper;
    }

    @Override
    public Optional<AnalysisTaskViewModel> findTask(Long taskId) {
        AnaTaskRecordDO entity = analysisTaskRecordMapper.selectOne(new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getId, taskId)
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(toView(entity));
    }

    @Override
    public long countTasks(Long caseId, String taskStatusCode) {
        LambdaQueryWrapper<AnaTaskRecordDO> wrapper = new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE");
        if (caseId != null) {
            wrapper.eq(AnaTaskRecordDO::getCaseId, caseId);
        }
        if (taskStatusCode != null && !taskStatusCode.isBlank()) {
            wrapper.eq(AnaTaskRecordDO::getTaskStatusCode, taskStatusCode.trim());
        }
        return analysisTaskRecordMapper.selectCount(wrapper);
    }

    @Override
    public java.util.List<AnalysisTaskViewModel> pageTasks(Long caseId, String taskStatusCode, int offset, int limit) {
        LambdaQueryWrapper<AnaTaskRecordDO> wrapper = new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE");
        if (caseId != null) {
            wrapper.eq(AnaTaskRecordDO::getCaseId, caseId);
        }
        if (taskStatusCode != null && !taskStatusCode.isBlank()) {
            wrapper.eq(AnaTaskRecordDO::getTaskStatusCode, taskStatusCode.trim());
        }
        wrapper.orderByDesc(AnaTaskRecordDO::getCreatedAt)
                .last("LIMIT " + Math.max(offset, 0) + "," + Math.max(limit, 1));
        return analysisTaskRecordMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    private AnalysisTaskViewModel toView(AnaTaskRecordDO entity) {
        return new AnalysisTaskViewModel(
                entity.getId(),
                entity.getTaskNo(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getModelVersion(),
                entity.getTaskTypeCode(),
                entity.getTaskStatusCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getOrgId(),
                entity.getRetryFromTaskId(),
                entity.getTraceId(),
                entity.getInferenceMillis());
    }
}
