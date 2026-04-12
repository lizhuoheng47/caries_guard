package com.cariesguard.image.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final AttachmentAppService attachmentAppService;

    public FileController(AttachmentAppService attachmentAppService) {
        this.attachmentAppService = attachmentAppService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("image:upload")
    public ApiResponse<AttachmentUploadVO> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(attachmentAppService.upload(file), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{attachmentId}/access-url")
    @RequirePermission("image:read")
    public ApiResponse<AttachmentAccessVO> accessUrl(@PathVariable Long attachmentId, HttpServletRequest request) {
        return ApiResponse.success(attachmentAppService.createAccessUrl(attachmentId, request), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long attachmentId,
                                            @RequestParam Long expireAt,
                                            @RequestParam String signature) {
        StoredObjectResource storedObject = attachmentAppService.loadContent(attachmentId, expireAt, signature);
        MediaType mediaType = MediaType.parseMediaType(storedObject.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(storedObject.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(storedObject.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(storedObject.resource());
    }
}
