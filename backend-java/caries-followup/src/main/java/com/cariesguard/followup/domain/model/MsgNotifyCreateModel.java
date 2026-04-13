package com.cariesguard.followup.domain.model;

public record MsgNotifyCreateModel(
        Long notifyId,
        String bizModuleCode,
        Long bizId,
        Long receiverUserId,
        String notifyTypeCode,
        String channelCode,
        String title,
        String contentSummary,
        String sendStatusCode,
        Long orgId,
        Long operatorUserId) {
}
