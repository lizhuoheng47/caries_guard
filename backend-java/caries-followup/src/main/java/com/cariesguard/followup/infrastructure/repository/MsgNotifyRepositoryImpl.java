package com.cariesguard.followup.infrastructure.repository;

import com.cariesguard.followup.domain.model.MsgNotifyCreateModel;
import com.cariesguard.followup.domain.repository.MsgNotifyRepository;
import com.cariesguard.followup.infrastructure.dataobject.MsgNotifyRecordDO;
import com.cariesguard.followup.infrastructure.mapper.MsgNotifyRecordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MsgNotifyRepositoryImpl implements MsgNotifyRepository {

    private final MsgNotifyRecordMapper msgNotifyRecordMapper;

    public MsgNotifyRepositoryImpl(MsgNotifyRecordMapper msgNotifyRecordMapper) {
        this.msgNotifyRecordMapper = msgNotifyRecordMapper;
    }

    @Override
    public void create(MsgNotifyCreateModel model) {
        MsgNotifyRecordDO entity = new MsgNotifyRecordDO();
        entity.setId(model.notifyId());
        entity.setBizModuleCode(model.bizModuleCode());
        entity.setBizId(model.bizId());
        entity.setReceiverUserId(model.receiverUserId());
        entity.setNotifyTypeCode(model.notifyTypeCode());
        entity.setChannelCode(model.channelCode());
        entity.setTitle(model.title());
        entity.setContentSummary(model.contentSummary());
        entity.setSendStatusCode(model.sendStatusCode());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        msgNotifyRecordMapper.insert(entity);
    }
}
