package com.cariesguard.report.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.report.app.RagKbAppService;
import com.cariesguard.report.interfaces.command.RagKbImportTextCommand;
import com.cariesguard.report.interfaces.command.RagKbRebuildCommand;
import com.cariesguard.report.interfaces.command.RagKbUpdateCommand;
import com.cariesguard.report.interfaces.command.RagVersionActionCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/kb")
public class RagKbController {

    private final RagKbAppService ragKbAppService;

    public RagKbController(RagKbAppService ragKbAppService) {
        this.ragKbAppService = ragKbAppService;
    }

    @GetMapping("/overview")
    @RequirePermission("report:view")
    public ApiResponse<Object> overview(@RequestParam(required = false) String kbCode) {
        return ApiResponse.success(ragKbAppService.overview(kbCode), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/documents")
    @RequirePermission("report:view")
    public ApiResponse<Object> documents(
            @RequestParam(required = false) String kbCode,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(ragKbAppService.documents(kbCode, keyword), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/documents/{id}")
    @RequirePermission("report:view")
    public ApiResponse<Object> documentDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(ragKbAppService.documentDetail(id), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/import-text")
    @RequirePermission("report:view")
    public ApiResponse<Object> importText(@Valid @RequestBody RagKbImportTextCommand command) {
        return ApiResponse.success(ragKbAppService.importText(command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/upload")
    @RequirePermission("report:view")
    public ApiResponse<Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String kbCode,
            @RequestParam(required = false) String kbName,
            @RequestParam(required = false, defaultValue = "PATIENT_GUIDE") String kbTypeCode,
            @RequestParam(required = false) String docTitle,
            @RequestParam(required = false, defaultValue = "UPLOAD") String docSourceCode,
            @RequestParam(required = false) String sourceUri,
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String docVersion,
            @RequestParam(required = false) String changeSummary) {
        return ApiResponse.success(
                ragKbAppService.upload(file, kbCode, kbName, kbTypeCode, docTitle, docSourceCode, sourceUri, docNo, docVersion, changeSummary),
                TraceIdUtils.currentTraceId());
    }

    @PutMapping("/documents/{id}")
    @RequirePermission("report:view")
    public ApiResponse<Object> update(@PathVariable("id") Long id, @Valid @RequestBody RagKbUpdateCommand command) {
        return ApiResponse.success(ragKbAppService.update(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/{id}/submit-review")
    @RequirePermission("report:view")
    public ApiResponse<Object> submitReview(@PathVariable("id") Long id, @Valid @RequestBody RagVersionActionCommand command) {
        return ApiResponse.success(ragKbAppService.submitReview(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/{id}/approve")
    @RequirePermission("report:view")
    public ApiResponse<Object> approve(@PathVariable("id") Long id, @Valid @RequestBody RagVersionActionCommand command) {
        return ApiResponse.success(ragKbAppService.approve(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/{id}/reject")
    @RequirePermission("report:view")
    public ApiResponse<Object> reject(@PathVariable("id") Long id, @Valid @RequestBody RagVersionActionCommand command) {
        return ApiResponse.success(ragKbAppService.reject(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/{id}/publish")
    @RequirePermission("report:view")
    public ApiResponse<Object> publish(@PathVariable("id") Long id, @Valid @RequestBody RagVersionActionCommand command) {
        return ApiResponse.success(ragKbAppService.publish(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/documents/{id}/rollback")
    @RequirePermission("report:view")
    public ApiResponse<Object> rollback(@PathVariable("id") Long id, @Valid @RequestBody RagVersionActionCommand command) {
        return ApiResponse.success(ragKbAppService.rollback(id, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/rebuild")
    @RequirePermission("report:view")
    public ApiResponse<Object> rebuild(@RequestBody RagKbRebuildCommand command) {
        return ApiResponse.success(ragKbAppService.rebuild(command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/rebuild-jobs")
    @RequirePermission("report:view")
    public ApiResponse<Object> rebuildJobs(@RequestParam(required = false) String kbCode) {
        return ApiResponse.success(ragKbAppService.rebuildJobs(kbCode), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/ingest-jobs")
    @RequirePermission("report:view")
    public ApiResponse<Object> ingestJobs() {
        return ApiResponse.success(ragKbAppService.ingestJobs(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/graph-stats")
    @RequirePermission("report:view")
    public ApiResponse<Object> graphStats(@RequestParam(required = false) String kbCode) {
        return ApiResponse.success(ragKbAppService.graphStats(kbCode), TraceIdUtils.currentTraceId());
    }
}
