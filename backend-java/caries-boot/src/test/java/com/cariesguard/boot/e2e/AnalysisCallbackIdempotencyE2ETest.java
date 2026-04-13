package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class AnalysisCallbackIdempotencyE2ETest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldBeIdempotentWhenSuccessCallbackIsRepeated() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e idempotency");

        JsonNode first = callbackSuccess(task, fixture);
        assertThat(first.path("data").path("taskStatusCode").asText()).isEqualTo("SUCCESS");
        assertThat(callbackIsIdempotent(first)).isFalse();

        long summaryCountAfterFirst = count("SELECT COUNT(1) FROM ana_result_summary WHERE task_id = ?", task.taskId());
        long assetCountAfterFirst = count("SELECT COUNT(1) FROM ana_visual_asset WHERE task_id = ?", task.taskId());
        long riskCountAfterFirst = count("SELECT COUNT(1) FROM med_risk_assessment_record WHERE case_id = ?", fixture.caseId());
        long transitionCountAfterFirst = count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'ANALYZING'
                  AND to_status_code = 'REVIEW_PENDING'
                  AND change_reason_code = 'AI_CALLBACK_SUCCESS'
                """, fixture.caseId());

        JsonNode second = callbackSuccess(task, fixture);
        assertThat(second.path("data").path("taskStatusCode").asText()).isEqualTo("SUCCESS");
        assertThat(callbackIsIdempotent(second)).isTrue();
        assertThat(queryString("SELECT task_status_code FROM ana_task_record WHERE task_no = ?", task.taskNo())).isEqualTo("SUCCESS");

        assertThat(count("SELECT COUNT(1) FROM ana_result_summary WHERE task_id = ?", task.taskId())).isEqualTo(summaryCountAfterFirst);
        assertThat(count("SELECT COUNT(1) FROM ana_visual_asset WHERE task_id = ?", task.taskId())).isEqualTo(assetCountAfterFirst);
        assertThat(count("SELECT COUNT(1) FROM med_risk_assessment_record WHERE case_id = ?", fixture.caseId())).isEqualTo(riskCountAfterFirst);
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'ANALYZING'
                  AND to_status_code = 'REVIEW_PENDING'
                  AND change_reason_code = 'AI_CALLBACK_SUCCESS'
                """, fixture.caseId())).isEqualTo(transitionCountAfterFirst);
    }
}
