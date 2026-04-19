package com.cariesguard.patient.interfaces.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record VisitMutationVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long visitId,
        String visitNo) {
}
