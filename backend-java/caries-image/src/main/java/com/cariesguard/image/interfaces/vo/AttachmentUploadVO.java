package com.cariesguard.image.interfaces.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record AttachmentUploadVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long attachmentId,
        String fileName,
        String bucketName,
        String objectKey,
        String md5) {
}
