package com.cariesguard.followup.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.followup.domain.model.FollowupCaseModel;
import com.cariesguard.followup.domain.repository.FollowupCaseRepository;
import com.cariesguard.followup.infrastructure.dataobject.FollowupCaseDO;
import com.cariesguard.followup.infrastructure.mapper.FollowupCaseMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FollowupCaseRepositoryImpl implements FollowupCaseRepository {

    private final FollowupCaseMapper followupCaseMapper;

    public FollowupCaseRepositoryImpl(FollowupCaseMapper followupCaseMapper) {
        this.followupCaseMapper = followupCaseMapper;
    }

    @Override
    public Optional<FollowupCaseModel> findCase(Long caseId) {
        FollowupCaseDO entity = followupCaseMapper.selectOne(new LambdaQueryWrapper<FollowupCaseDO>()
                .eq(FollowupCaseDO::getId, caseId)
                .eq(FollowupCaseDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(new FollowupCaseModel(
                entity.getId(),
                entity.getPatientId(),
                entity.getCaseStatusCode(),
                entity.getOrgId()));
    }
}
