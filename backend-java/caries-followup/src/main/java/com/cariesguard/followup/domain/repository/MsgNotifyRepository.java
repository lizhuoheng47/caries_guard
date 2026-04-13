package com.cariesguard.followup.domain.repository;

import com.cariesguard.followup.domain.model.MsgNotifyCreateModel;

public interface MsgNotifyRepository {

    void create(MsgNotifyCreateModel model);
}
