package com.cariesguard.dashboard.infrastructure.repository;

import com.cariesguard.dashboard.interfaces.query.DashboardRangeQuery;
import com.cariesguard.dashboard.interfaces.vo.BacklogSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.CaseStatusDistributionVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.dashboard.interfaces.vo.FollowupTaskSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
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
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(1) FROM pat_patient WHERE org_id = ? AND deleted_flag = 0) AS patient_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0) AS case_count,
                    (SELECT COUNT(DISTINCT case_id) FROM ana_task_record WHERE org_id = ? AND deleted_flag = 0 AND task_status_code = 'SUCCESS') AS analyzed_case_count,
                    (SELECT COUNT(DISTINCT case_id) FROM rpt_record WHERE org_id = ? AND deleted_flag = 0) AS generated_report_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0 AND (case_status_code = 'FOLLOWUP_REQUIRED' OR followup_required_flag = '1')) AS followup_required_case_count,
                    (SELECT COUNT(1) FROM med_case WHERE org_id = ? AND deleted_flag = 0 AND case_status_code = 'CLOSED') AS closed_case_count
                """, orgId, orgId, orgId, orgId, orgId, orgId);
        return new DashboardOverviewVO(
                longValue(row.get("patient_count")),
                longValue(row.get("case_count")),
                longValue(row.get("analyzed_case_count")),
                longValue(row.get("generated_report_count")),
                longValue(row.get("followup_required_case_count")),
                longValue(row.get("closed_case_count")));
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
                     WHERE org_id = ? AND deleted_flag = 0 AND created_at >= ? AND task_status_code = 'FAILED') AS failed_task_count
                """, orgId, orgId, recentThreshold, orgId, recentThreshold, orgId, recentThreshold);
        long recentTaskCount = longValue(row.get("recent_task_count"));
        long successTaskCount = longValue(row.get("success_task_count"));
        long failedTaskCount = longValue(row.get("failed_task_count"));
        return new ModelRuntimeVO(
                stringValue(row.get("current_model_version")),
                recentTaskCount,
                successTaskCount,
                failedTaskCount,
                rate(successTaskCount, recentTaskCount));
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
