package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.PasswordResetTokenModel;
import com.cariesguard.system.domain.repository.PasswordResetTokenRepository;
import com.cariesguard.system.infrastructure.dataobject.SysPasswordResetTokenDO;
import com.cariesguard.system.infrastructure.mapper.SysPasswordResetTokenMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final SysPasswordResetTokenMapper mapper;

    public PasswordResetTokenRepositoryImpl(SysPasswordResetTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void invalidateActiveTokens(Long userId, LocalDateTime now) {
        SysPasswordResetTokenDO update = new SysPasswordResetTokenDO();
        update.setUsedAt(now);
        mapper.update(update, Wrappers.<SysPasswordResetTokenDO>lambdaUpdate()
                .eq(SysPasswordResetTokenDO::getUserId, userId)
                .isNull(SysPasswordResetTokenDO::getUsedAt));
    }

    @Override
    public void create(PasswordResetTokenModel model) {
        SysPasswordResetTokenDO row = new SysPasswordResetTokenDO();
        row.setUserId(model.userId());
        row.setUsername(model.username());
        row.setCodeSalt(model.codeSalt());
        row.setCodeHash(model.codeHash());
        row.setExpiresAt(model.expiresAt());
        row.setAttemptCount(model.attemptCount());
        row.setRequestIp(model.requestIp());
        row.setCreatedAt(model.createdAt());
        mapper.insert(row);
    }

    @Override
    public Optional<PasswordResetTokenModel> findLatestActive(Long userId, LocalDateTime now) {
        SysPasswordResetTokenDO row = mapper.selectOne(Wrappers.<SysPasswordResetTokenDO>lambdaQuery()
                .eq(SysPasswordResetTokenDO::getUserId, userId)
                .isNull(SysPasswordResetTokenDO::getUsedAt)
                .gt(SysPasswordResetTokenDO::getExpiresAt, now)
                .orderByDesc(SysPasswordResetTokenDO::getCreatedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(row).map(this::toModel);
    }

    @Override
    public void incrementAttempts(Long tokenId) {
        mapper.update(null, Wrappers.<SysPasswordResetTokenDO>lambdaUpdate()
                .eq(SysPasswordResetTokenDO::getId, tokenId)
                .setSql("attempt_count = attempt_count + 1"));
    }

    @Override
    public void markUsed(Long tokenId, LocalDateTime usedAt) {
        SysPasswordResetTokenDO update = new SysPasswordResetTokenDO();
        update.setId(tokenId);
        update.setUsedAt(usedAt);
        mapper.updateById(update);
    }

    private PasswordResetTokenModel toModel(SysPasswordResetTokenDO row) {
        return new PasswordResetTokenModel(
                row.getId(),
                row.getUserId(),
                row.getUsername(),
                row.getCodeSalt(),
                row.getCodeHash(),
                row.getExpiresAt(),
                row.getUsedAt(),
                row.getAttemptCount() == null ? 0 : row.getAttemptCount(),
                row.getRequestIp(),
                row.getCreatedAt());
    }
}
