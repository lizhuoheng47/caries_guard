package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.followup.app.FollowupTriggerService;
import com.cariesguard.followup.interfaces.vo.FollowupTriggerResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FollowupTriggerIdempotencyE2ETest extends AnalysisReportE2EBaseTest {

    @Autowired
    private FollowupTriggerService followupTriggerService;

    @Test
    void shouldNotCreateDuplicateActivePlanOrFirstTaskWhenTriggerReplayed() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e followup idempotency");

        callbackSuccess(task, fixture);
        long reportId = generateDoctorReport(fixture, "high risk confirmed");

        assertThat(count("SELECT COUNT(1) FROM fup_plan WHERE case_id = ? AND trigger_source_code = 'RISK_HIGH' AND trigger_ref_id = ?",
                fixture.caseId(), reportId)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM fup_plan
                WHERE case_id = ?
                  AND trigger_source_code = 'RISK_HIGH'
                  AND trigger_ref_id = ?
                  AND plan_status_code IN ('PLANNED', 'ACTIVE')
                """, fixture.caseId(), reportId)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM fup_task
                WHERE case_id = ?
                  AND plan_id IN (
                      SELECT id
                      FROM fup_plan
                      WHERE case_id = ?
                        AND trigger_source_code = 'RISK_HIGH'
                        AND trigger_ref_id = ?
                  )
                """, fixture.caseId(), fixture.caseId(), reportId)).isEqualTo(1);

        FollowupTriggerResultVO replayResult = followupTriggerService.triggerFromReport(
                fixture.caseId(),
                fixture.patientId(),
                fixture.orgId(),
                reportId,
                "HIGH",
                "1",
                30,
                fixture.operatorUserId());

        assertThat(replayResult.triggered()).isFalse();
        assertThat(replayResult.skipReason()).isEqualTo("Followup plan already exists for this trigger");
        assertThat(count("SELECT COUNT(1) FROM fup_plan WHERE case_id = ? AND trigger_source_code = 'RISK_HIGH' AND trigger_ref_id = ?",
                fixture.caseId(), reportId)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM fup_task
                WHERE case_id = ?
                  AND plan_id IN (
                      SELECT id
                      FROM fup_plan
                      WHERE case_id = ?
                        AND trigger_source_code = 'RISK_HIGH'
                        AND trigger_ref_id = ?
                  )
                """, fixture.caseId(), fixture.caseId(), reportId)).isEqualTo(1);
    }
}
