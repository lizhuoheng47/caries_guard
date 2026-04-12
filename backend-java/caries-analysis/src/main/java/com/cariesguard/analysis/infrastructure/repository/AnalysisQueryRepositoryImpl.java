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
        return entity == null ? Optional.empty() : Optional.of(new AnalysisTaskViewModel(
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
                entity.getOrgId()));
    }
}
