package com.cariesguard.image.interfaces.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CaseImageMutationVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long imageId,
        String qualityStatusCode) {
}
