package com.cariesguard.image.interfaces.vo;

public record AttachmentAccessVO(
        String accessUrl,
        long expireAt) {
}
