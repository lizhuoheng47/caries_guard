package com.cariesguard.report.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ReportDomainServiceTests {

    private final ReportDomainService service = new ReportDomainService();

    @Test
    void normalizeReportTypeShouldUppercase() {
        assertThat(service.normalizeReportType("doctor")).isEqualTo("DOCTOR");
    }

    @Test
    void normalizeReportTypeShouldRejectUnknownType() {
        assertThatThrownBy(() -> service.normalizeReportType("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ensureCaseStatusAllowedShouldRejectInvalidStatus() {
        assertThatThrownBy(() -> service.ensureCaseStatusAllowed("ANALYZING"))
                .isInstanceOf(BusinessException.class);
    }
}

