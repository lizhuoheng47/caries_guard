package com.cariesguard.report.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.report.app.ReportTemplateAppService;
import com.cariesguard.report.interfaces.command.CreateReportTemplateCommand;
import com.cariesguard.report.interfaces.command.UpdateReportTemplateCommand;
import com.cariesguard.report.interfaces.query.ReportTemplateListQuery;
import com.cariesguard.report.interfaces.vo.ReportTemplateMutationVO;
import com.cariesguard.report.interfaces.vo.ReportTemplateVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/report-templates")
public class ReportTemplateController {

    private final ReportTemplateAppService reportTemplateAppService;

    public ReportTemplateController(ReportTemplateAppService reportTemplateAppService) {
        this.reportTemplateAppService = reportTemplateAppService;
    }

    @PostMapping
    @RequirePermission("report:template:manage")
    public ApiResponse<ReportTemplateMutationVO> createTemplate(@Valid @RequestBody CreateReportTemplateCommand command) {
        return ApiResponse.success(reportTemplateAppService.createTemplate(command), TraceIdUtils.currentTraceId());
    }

    @PutMapping("/{templateId}")
    @RequirePermission("report:template:manage")
    public ApiResponse<ReportTemplateMutationVO> updateTemplate(@PathVariable Long templateId,
                                                                @RequestBody UpdateReportTemplateCommand command) {
        return ApiResponse.success(reportTemplateAppService.updateTemplate(templateId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping
    @RequirePermission("report:template:view")
    public ApiResponse<List<ReportTemplateVO>> listTemplates(@RequestParam(required = false) String reportTypeCode) {
        return ApiResponse.success(reportTemplateAppService.listTemplates(new ReportTemplateListQuery(reportTypeCode)),
                TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{templateId}")
    @RequirePermission("report:template:view")
    public ApiResponse<ReportTemplateVO> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(reportTemplateAppService.getTemplate(templateId), TraceIdUtils.currentTraceId());
    }
}

