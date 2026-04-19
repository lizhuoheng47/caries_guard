package com.cariesguard.patient.interfaces.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record PatientMutationVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long patientId,
        String patientNo) {
}
