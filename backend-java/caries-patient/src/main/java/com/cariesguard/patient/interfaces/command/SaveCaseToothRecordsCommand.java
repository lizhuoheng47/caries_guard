package com.cariesguard.patient.interfaces.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaveCaseToothRecordsCommand(
        @NotEmpty List<@Valid ToothRecordItemCommand> toothRecords) {
}
