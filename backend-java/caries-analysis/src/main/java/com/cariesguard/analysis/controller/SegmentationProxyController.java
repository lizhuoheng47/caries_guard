package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.infrastructure.client.InternalSegmentationClient;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/segmentation")
public class SegmentationProxyController {
    private static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "application/dicom");
    private final InternalSegmentationClient client;

    public SegmentationProxyController(InternalSegmentationClient client) { this.client = client; }

    @GetMapping("/health")
    @RequirePermission("analysis:view")
    public JsonNode health() { return client.health(); }

    @PostMapping
    @RequirePermission("analysis:create")
    public JsonNode segment(@RequestBody byte[] body,
                            @RequestHeader(value = "Content-Type", defaultValue = "application/octet-stream") String contentType) {
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase();
        if (!CONTENT_TYPES.contains(normalized)) throw new IllegalArgumentException("Unsupported image Content-Type");
        return client.segment(body, normalized);
    }

    @GetMapping("/assets/{requestId}/{fileName:.+}")
    @RequirePermission("analysis:view")
    public ResponseEntity<byte[]> asset(@PathVariable String requestId, @PathVariable String fileName) {
        var asset = client.getAsset(requestId, fileName);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .body(asset.bytes());
    }
}
