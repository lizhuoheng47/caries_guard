package com.cariesguard.dashboard.infrastructure.repository;

import com.cariesguard.dashboard.interfaces.query.DashboardRangeQuery;
import com.cariesguard.dashboard.interfaces.vo.BacklogSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.CaseStatusDistributionVO;
import com.cariesguard.dashboard.interfaces.vo.CorrectionFeedbackStatsVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.dashboard.interfaces.vo.FollowupTaskSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.dashboard.interfaces.vo.ModelVersionRuntimeVO;
import com.cariesguard.dashboard.interfaces.vo.RiskLevelDistributionVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardTrendPointVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardStatsRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final JdbcTemplate jdbcTemplate;

    public DashboardStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardOverviewVO queryOverview(Long orgId) {
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(30);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(1) FROM pat_patient WHERE org_id = ? AND deleted_flag = 0) AS patient_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0) AS case_count,
                    (SELECT COUNT(DISTINCT case_id) FROM ana_task_record WHERE org_id = ? AND deleted_flag = 0 AND task_status_code = 'SUCCESS') AS analyzed_case_count,
                    (SELECT COUNT(DISTINCT case_id) FROM rpt_record WHERE org_id = ? AND deleted_flag = 0) AS generated_report_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0 AND (case_status_code = 'FOLLOWUP_REQUIRED' OR followup_required_flag = '1')) AS followup_required_case_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0 AND case_status_code = 'CLOSED') AS closed_case_count,
                    (SELECT COUNT(1)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ?) AS today_analysis_task_count,
                    (SELECT AVG(inference_millis)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ? AND inference_millis IS NOT NULL) AS average_inference_millis,
                    (
                        SELECT COUNT(1)
                        FROM rag_request_log
                        WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ?
                    ) AS today_rag_request_count
                """, orgId, orgId, orgId, orgId, orgId, orgId, orgId, todayStart, orgId, recentThreshold, orgId, todayStart);
        Map<String, Object> aiRow = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(s.id) AS summary_count,
                    SUM(CASE WHEN s.uncertainty_score IS NOT NULL AND s.uncertainty_score >= 0.5000 THEN 1 ELSE 0 END) AS high_uncertainty_count
                FROM ana_result_summary s
                INNER JOIN ana_task_record t ON t.id = s.task_id
                WHERE t.org_id = ?
                  AND t.deleted_flag = 0
                  AND s.deleted_flag = 0
                  AND t.created_at >= ?
                """, orgId, recentThreshold);
        Map<String, Object> reviewRow = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(1) AS review_required_count,
                    SUM(CASE WHEN c.case_status_code IN ('REPORT_READY', 'FOLLOWUP_REQUIRED', 'CLOSED') THEN 1 ELSE 0 END) AS review_passed_count
                FROM (
                    SELECT s.case_id, MAX(s.id) AS max_id
                    FROM ana_result_summary s
                    INNER JOIN ana_task_record t ON t.id = s.task_id
                    WHERE t.org_id = ?
                      AND t.deleted_flag = 0
                      AND s.deleted_flag = 0
                      AND s.review_suggested_flag = '1'
                      AND t.created_at >= ?
                    GROUP BY s.case_id
                ) latest
                INNER JOIN med_case c ON c.id = latest.case_id
                WHERE c.org_id = ? AND c.deleted_flag = 0
                """, orgId, recentThreshold, orgId);
        Map<String, Object> knowledgeRow = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(1) AS rag_request_count,
                    COUNT(DISTINCT rl.request_id) AS hit_request_count
                FROM rag_request_log r
                LEFT JOIN rag_retrieval_log rl ON rl.request_id = r.id
                WHERE r.org_id = ?
                  AND r.deleted_flag = 0
                  AND r.created_at >= ?
                """, orgId, recentThreshold);
        Map<String, Object> feedbackRow = jdbcTemplate.queryForMap("""
                SELECT
                    SUM(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(corrected_truth_json, '$.feedbackGovernance.acceptedAiConclusion')) = 'true' THEN 1 ELSE 0 END) AS ai_accepted_count,
                    SUM(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(corrected_truth_json, '$.feedbackGovernance.acceptedAiConclusion')) = 'false' THEN 1 ELSE 0 END) AS ai_corrected_count
                FROM ana_correction_feedback
                WHERE org_id = ?
                  AND deleted_flag = 0
                  AND status = 'ACTIVE'
                  AND created_at >= ?
                """, orgId, recentThreshold);
        long summaryCount = longValue(aiRow.get("summary_count"));
        long reviewRequiredCount = longValue(reviewRow.get("review_required_count"));
        long ragRequestCount = longValue(knowledgeRow.get("rag_request_count"));
        long aiAcceptedCount = longValue(feedbackRow.get("ai_accepted_count"));
        long aiCorrectedCount = longValue(feedbackRow.get("ai_corrected_count"));
        return new DashboardOverviewVO(
                longValue(row.get("patient_count")),
                longValue(row.get("case_count")),
                longValue(row.get("analyzed_case_count")),
                longValue(row.get("generated_report_count")),
                longValue(row.get("followup_required_case_count")),
                longValue(row.get("closed_case_count")),
                longValue(row.get("today_analysis_task_count")),
                decimalValue(row.get("average_inference_millis")),
                rate(longValue(aiRow.get("high_uncertainty_count")), summaryCount),
                rate(longValue(reviewRow.get("review_passed_count")), reviewRequiredCount),
                longValue(row.get("today_rag_request_count")),
                rate(longValue(knowledgeRow.get("hit_request_count")), ragRequestCount),
                rate(aiAcceptedCount, aiAcceptedCount + aiCorrectedCount));
    }

    public CaseStatusDistributionVO queryCaseStatusDistribution(Long orgId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    SUM(CASE WHEN case_status_code = 'CREATED' THEN 1 ELSE 0 END) AS created_count,
                    SUM(CASE WHEN case_status_code = 'QC_PENDING' THEN 1 ELSE 0 END) AS qc_pending_count,
                    SUM(CASE WHEN case_status_code = 'ANALYZING' THEN 1 ELSE 0 END) AS analyzing_count,
                    SUM(CASE WHEN case_status_code = 'REVIEW_PENDING' THEN 1 ELSE 0 END) AS review_pending_count,
                    SUM(CASE WHEN case_status_code = 'REPORT_READY' THEN 1 ELSE 0 END) AS report_ready_count,
                    SUM(CASE WHEN case_status_code = 'FOLLOWUP_REQUIRED' THEN 1 ELSE 0 END) AS followup_required_count,
                    SUM(CASE WHEN case_status_code = 'CLOSED' THEN 1 ELSE 0 END) AS closed_count,
                    SUM(CASE WHEN case_status_code = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count
                FROM med_case
                WHERE org_id = ? AND deleted_flag = 0
                """, orgId);
        return new CaseStatusDistributionVO(
                longValue(row.get("created_count")),
                longValue(row.get("qc_pending_count")),
                longValue(row.get("analyzing_count")),
                longValue(row.get("review_pending_count")),
                longValue(row.get("report_ready_count")),
                longValue(row.get("followup_required_count")),
                longValue(row.get("closed_count")),
                longValue(row.get("cancelled_count")));
    }

    public RiskLevelDistributionVO queryRiskLevelDistribution(Long orgId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    SUM(CASE WHEN latest.overall_risk_level_code = 'HIGH' THEN 1 ELSE 0 END) AS high_risk_count,
                    SUM(CASE WHEN latest.overall_risk_level_code = 'MEDIUM' THEN 1 ELSE 0 END) AS medium_risk_count,
                    SUM(CASE WHEN latest.overall_risk_level_code = 'LOW' THEN 1 ELSE 0 END) AS low_risk_count
                FROM (
                    SELECT r.case_id, r.overall_risk_level_code
                    FROM med_risk_assessment_record r
                    INNER JOIN (
                        SELECT case_id, MAX(id) AS max_id
                        FROM med_risk_assessment_record
                        WHERE org_id = ? AND deleted_flag = 0
                        GROUP BY case_id
                    ) current_record ON current_record.max_id = r.id
                    WHERE r.org_id = ? AND r.deleted_flag = 0
                ) latest
                """, orgId, orgId);
        return new RiskLevelDistributionVO(
                longValue(row.get("high_risk_count")),
                longValue(row.get("medium_risk_count")),
                longValue(row.get("low_risk_count")));
    }

    public FollowupTaskSummaryVO queryFollowupTaskSummary(Long orgId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    SUM(CASE WHEN task_status_code = 'TODO' THEN 1 ELSE 0 END) AS todo_count,
                    SUM(CASE WHEN task_status_code = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_count,
                    SUM(CASE WHEN task_status_code = 'DONE' THEN 1 ELSE 0 END) AS done_count,
                    SUM(CASE WHEN task_status_code = 'OVERDUE' THEN 1 ELSE 0 END) AS overdue_count
                FROM fup_task
                WHERE org_id = ? AND deleted_flag = 0
                """, orgId);
        long todoCount = longValue(row.get("todo_count"));
        long inProgressCount = longValue(row.get("in_progress_count"));
        long doneCount = longValue(row.get("done_count"));
        long overdueCount = longValue(row.get("overdue_count"));
        long denominator = todoCount + inProgressCount + doneCount + overdueCount;
        return new FollowupTaskSummaryVO(
                todoCount,
                inProgressCount,
                doneCount,
                overdueCount,
                rate(doneCount, denominator),
                rate(overdueCount, denominator));
    }

    public BacklogSummaryVO queryBacklogSummary(Long orgId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0 AND case_status_code = 'REVIEW_PENDING') AS review_pending_case_count,
                    (SELECT COUNT(1) FROM fup_task WHERE org_id = ? AND deleted_flag = 0 AND task_status_code IN ('TODO', 'IN_PROGRESS')) AS todo_followup_task_count,
                    (SELECT COUNT(1) FROM fup_task WHERE org_id = ? AND deleted_flag = 0 AND task_status_code = 'OVERDUE') AS overdue_followup_task_count,
                    (
                        SELECT COUNT(1)
                        FROM med_case c
                        INNER JOIN (
                            SELECT case_id, MAX(id) AS max_id
                            FROM med_risk_assessment_record
                            WHERE org_id = ? AND deleted_flag = 0
                            GROUP BY case_id
                        ) current_risk ON current_risk.case_id = c.id
                        INNER JOIN med_risk_assessment_record r ON r.id = current_risk.max_id
                        WHERE c.org_id = ?
                          AND c.deleted_flag = 0
                          AND c.case_status_code NOT IN ('CLOSED', 'CANCELLED')
                          AND r.overall_risk_level_code = 'HIGH'
                    ) AS high_risk_pending_case_count
                """, orgId, orgId, orgId, orgId, orgId);
        return new BacklogSummaryVO(
                longValue(row.get("review_pending_case_count")),
                longValue(row.get("todo_followup_task_count")),
                longValue(row.get("overdue_followup_task_count")),
                longValue(row.get("high_risk_pending_case_count")));
    }

    public ModelRuntimeVO queryModelRuntime(Long orgId) {
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(30);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT model_version
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND task_status_code = 'SUCCESS'
                     ORDER BY completed_at DESC, id DESC
                     LIMIT 1) AS current_model_version,
                    (SELECT COUNT(1)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ?) AS recent_task_count,
                    (SELECT COUNT(1)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ? AND task_status_code = 'SUCCESS') AS success_task_count,
                    (SELECT COUNT(1)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ? AND task_status_code = 'FAILED') AS failed_task_count,
                    (SELECT AVG(inference_millis)
                     FROM ana_task_record
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ? AND inference_millis IS NOT NULL) AS average_inference_millis
                """, orgId, orgId, recentThreshold, orgId, recentThreshold, orgId, recentThreshold, orgId, recentThreshold);
        Map<String, Object> qualityRow = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(s.id) AS summary_count,
                    SUM(CASE WHEN s.uncertainty_score IS NOT NULL AND s.uncertainty_score >= 0.5000 THEN 1 ELSE 0 END) AS high_uncertainty_count,
                    SUM(CASE WHEN s.review_suggested_flag = '1' THEN 1 ELSE 0 END) AS review_suggested_count,
                    (SELECT COUNT(1)
                     FROM ana_correction_feedback f
                     WHERE f.org_id = ? AND f.deleted_flag = 0 AND f.created_at >= ?) AS correction_feedback_count
                FROM ana_result_summary s
                INNER JOIN ana_task_record t ON t.id = s.task_id
                WHERE t.org_id = ?
                  AND t.deleted_flag = 0
                  AND s.deleted_flag = 0
                  AND t.created_at >= ?
                """, orgId, recentThreshold, orgId, recentThreshold);
        List<ModelVersionRuntimeVO> modelVersions = jdbcTemplate.queryForList("""
                SELECT
                    COALESCE(NULLIF(model_version, ''), 'UNKNOWN') AS model_version,
                    COUNT(1) AS task_count,
                    SUM(CASE WHEN task_status_code = 'SUCCESS' THEN 1 ELSE 0 END) AS success_task_count,
                    SUM(CASE WHEN task_status_code = 'FAILED' THEN 1 ELSE 0 END) AS failed_task_count,
                    AVG(inference_millis) AS average_inference_millis
                FROM ana_task_record
                WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ?
                GROUP BY COALESCE(NULLIF(model_version, ''), 'UNKNOWN')
                ORDER BY task_count DESC, model_version ASC
                """, orgId, recentThreshold).stream().map(item -> {
            long taskCount = longValue(item.get("task_count"));
            long successTaskCount = longValue(item.get("success_task_count"));
            return new ModelVersionRuntimeVO(
                    stringValue(item.get("model_version")),
                    taskCount,
                    successTaskCount,
                    longValue(item.get("failed_task_count")),
                    rate(successTaskCount, taskCount),
                    decimalValue(item.get("average_inference_millis")));
        }).toList();
        long recentTaskCount = longValue(row.get("recent_task_count"));
        long successTaskCount = longValue(row.get("success_task_count"));
        long failedTaskCount = longValue(row.get("failed_task_count"));
        long summaryCount = longValue(qualityRow.get("summary_count"));
        return new ModelRuntimeVO(
                stringValue(row.get("current_model_version")),
                recentTaskCount,
                successTaskCount,
                failedTaskCount,
                rate(successTaskCount, recentTaskCount),
                decimalValue(row.get("average_inference_millis")),
                rate(longValue(qualityRow.get("high_uncertainty_count")), summaryCount),
                rate(longValue(qualityRow.get("review_suggested_count")), summaryCount),
                longValue(qualityRow.get("correction_feedback_count")),
                modelVersions);
    }

    public CorrectionFeedbackStatsVO queryCorrectionFeedbackStats(Long orgId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(1) AS total_feedback_count,
                    SUM(CASE WHEN training_candidate_flag = '1' THEN 1 ELSE 0 END) AS training_candidate_count,
                    SUM(CASE WHEN review_status_code = 'PENDING' THEN 1 ELSE 0 END) AS pending_review_count,
                    SUM(CASE WHEN review_status_code = 'APPROVED' THEN 1 ELSE 0 END) AS approved_review_count,
                    SUM(CASE WHEN review_status_code = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_review_count,
                    SUM(CASE WHEN desensitized_export_flag = '1' OR exported_snapshot_no IS NOT NULL THEN 1 ELSE 0 END) AS exported_sample_count,
                    SUM(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(corrected_truth_json, '$.feedbackGovernance.acceptedAiConclusion')) = 'true' THEN 1 ELSE 0 END) AS ai_accepted_count,
                    SUM(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(corrected_truth_json, '$.feedbackGovernance.acceptedAiConclusion')) = 'false' THEN 1 ELSE 0 END) AS ai_corrected_count
                FROM ana_correction_feedback
                WHERE org_id = ?
                  AND deleted_flag = 0
                  AND status = 'ACTIVE'
                """, orgId);
        long aiAcceptedCount = longValue(row.get("ai_accepted_count"));
        long aiCorrectedCount = longValue(row.get("ai_corrected_count"));
        return new CorrectionFeedbackStatsVO(
                longValue(row.get("total_feedback_count")),
                longValue(row.get("training_candidate_count")),
                longValue(row.get("pending_review_count")),
                longValue(row.get("approved_review_count")),
                longValue(row.get("rejected_review_count")),
                longValue(row.get("exported_sample_count")),
                aiAcceptedCount,
                aiCorrectedCount,
                rate(aiCorrectedCount, aiAcceptedCount + aiCorrectedCount));
    }

    public List<DashboardTrendPointVO> queryTrend(Long orgId, DashboardRangeQuery rangeQuery) {
        LocalDateTime startDateTime = rangeQuery.startDate().atStartOfDay();
        LocalDateTime endExclusive = rangeQuery.endDate().plusDays(1).atStartOfDay();
        Map<LocalDate, Long> newCaseCountMap = queryCountByDate("""
                SELECT DATE(created_at) AS stat_date, COUNT(1) AS total_count
                FROM med_case
                WHERE org_id = ? AND deleted_flag = 0
                  AND created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                """, orgId, startDateTime, endExclusive);
        Map<LocalDate, Long> analysisCompletedCountMap = queryCountByDate("""
                SELECT DATE(completed_at) AS stat_date, COUNT(1) AS total_count
                FROM ana_task_record
                WHERE org_id = ? AND deleted_flag = 0 AND task_status_code = 'SUCCESS'
                  AND completed_at IS NOT NULL
                  AND completed_at >= ? AND completed_at < ?
                GROUP BY DATE(completed_at)
                """, orgId, startDateTime, endExclusive);
        Map<LocalDate, Long> reportGeneratedCountMap = queryCountByDate("""
                SELECT DATE(created_at) AS stat_date, COUNT(1) AS total_count
                FROM rpt_record
                WHERE org_id = ? AND deleted_flag = 0
                  AND created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                """, orgId, startDateTime, endExclusive);
        Map<LocalDate, Long> followupTriggeredCountMap = queryCountByDate("""
                SELECT DATE(created_at) AS stat_date, COUNT(1) AS total_count
                FROM fup_plan
                WHERE org_id = ? AND deleted_flag = 0
                  AND created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                """, orgId, startDateTime, endExclusive);

        List<DashboardTrendPointVO> trend = new ArrayList<>();
        for (LocalDate date = rangeQuery.startDate(); !date.isAfter(rangeQuery.endDate()); date = date.plusDays(1)) {
            trend.add(new DashboardTrendPointVO(
                    date,
                    newCaseCountMap.getOrDefault(date, 0L),
                    analysisCompletedCountMap.getOrDefault(date, 0L),
                    reportGeneratedCountMap.getOrDefault(date, 0L),
                    followupTriggeredCountMap.getOrDefault(date, 0L)));
        }
        return trend;
    }

    private long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private Map<LocalDate, Long> queryCountByDate(String sql, Long orgId, LocalDateTime startDateTime, LocalDateTime endExclusive) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, orgId, startDateTime, endExclusive);
        Map<LocalDate, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object statDate = row.get("stat_date");
            if (statDate == null) {
                continue;
            }
            LocalDate date = ((Date) statDate).toLocalDate();
            countMap.put(date, longValue(row.get("total_count")));
        }
        return countMap;
    }
}




