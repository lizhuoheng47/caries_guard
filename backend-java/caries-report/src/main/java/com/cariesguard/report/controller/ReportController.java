package com.cariesguard.report.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.report.app.ReportAppService;
import com.cariesguard.report.app.ReportQueryAppService;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportDetailVO;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import com.cariesguard.report.interfaces.vo.ReportListItemVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportAppService reportAppService;
    private final ReportQueryAppService reportQueryAppService;

    public ReportController(ReportAppService reportAppService, ReportQueryAppService reportQueryAppService) {
        this.reportAppService = reportAppService;
        this.reportQueryAppService = reportQueryAppService;
    }

    @PostMapping("/api/v1/cases/{caseId}/reports")
    @RequirePermission("report:generate")
    public ApiResponse<ReportGenerateResultVO> generateReport(@PathVariable Long caseId,
                                                              @Valid @RequestBody GenerateReportCommand command) {
        return ApiResponse.success(reportAppService.generateReport(caseId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/cases/{caseId}/reports")
    @RequirePermission("report:view")
    public ApiResponse<List<ReportListItemVO>> listCaseReports(@PathVariable Long caseId) {
        return ApiResponse.success(reportQueryAppService.listCaseReports(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/reports/{reportId}")
    @RequirePermission("report:view")
    public ApiResponse<ReportDetailVO> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(reportQueryAppService.getReport(reportId), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/api/v1/reports/{reportId}/export")
    @RequirePermission("report:export")
    public ApiResponse<ReportExportResultVO> exportReport(@PathVariable Long reportId,
                                                          @RequestBody(required = false) ExportReportCommand command) {
        return ApiResponse.success(reportAppService.exportReport(reportId, command), TraceIdUtils.currentTraceId());
    }
}

