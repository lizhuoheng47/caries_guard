package com.cariesguard.report.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    void buildSummaryTextShouldIncludePatientExplanationForPatientReport() {
        ReportRenderDataModel renderData = new ReportRenderDataModel(
                "CASE-1",
                1L,
                2L,
                "PATIENT",
                List.of(),
                List.of(),
                List.of(),
                "C2",
                null,
                1,
                1,
                "HIGH",
                30,
                "1",
                List.of(),
                null,
                "RAG patient explanation",
                LocalDateTime.now());

        assertThat(service.buildSummaryText(renderData)).contains("patientExplanation=RAG patient explanation");
    }
}
