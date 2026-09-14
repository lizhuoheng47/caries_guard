package com.cariesguard.analysis.infrastructure.client;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InternalSegmentationClient {
    private static final Pattern REQUEST_ID = Pattern.compile("[a-f0-9]{32}");
    private static final Set<String> ASSET_PREFIXES = Set.of("mask_", "overlay_", "heatmap_");
    private final AnalysisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public InternalSegmentationClient(AnalysisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode health() { return send(baseRequest("/ai/v1/segment/health").GET().build()); }

    public JsonNode segment(byte[] body, String contentType) {
        HttpRequest request = baseRequest("/ai/v1/segment")
                .header("Content-Type", contentType)
                .timeout(Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        JsonNode response = send(request);
        JsonNode data = response.path("data");
        String requestId = data.path("requestId").asText();
        JsonNode assets = data.path("assets");
        if (REQUEST_ID.matcher(requestId).matches() && assets instanceof ObjectNode assetObject) {
            rewriteAsset(assetObject, "maskUrl", requestId);
            rewriteAsset(assetObject, "overlayUrl", requestId);
            rewriteAsset(assetObject, "heatmapUrl", requestId);
        }
        return response;
    }

    public AssetBody getAsset(String requestId, String fileName) {
        validateAssetPath(requestId, fileName);
        try {
            HttpResponse<byte[]> response = client.send(
                    baseRequest("/ai/v1/segment-assets/" + requestId + "/" + fileName)
                            .timeout(Duration.ofSeconds(30)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw downstream("Segmentation asset is unavailable");
            return new AssetBody(response.body(), response.headers().firstValue("Content-Type").orElse("image/png"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw downstream("Segmentation asset request was interrupted");
        } catch (Exception exception) {
            throw downstream("Segmentation asset request failed");
        }
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw downstream("Python inference service returned HTTP " + response.statusCode());
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw downstream("Python inference request was interrupted");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw downstream("Python inference service is unavailable");
        }
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder(uri(path)).header("X-Internal-Api-Key", properties.getInternalApiKey());
    }

    private URI uri(String path) { return URI.create(properties.getInferenceBaseUrl().replaceAll("/+$", "") + path); }

    private void rewriteAsset(ObjectNode assets, String field, String requestId) {
        String source = assets.path(field).asText();
        String fileName = source.substring(source.lastIndexOf('/') + 1);
        validateAssetPath(requestId, fileName);
        assets.put(field, "/api/v1/segmentation/assets/" + requestId + "/" + fileName);
    }

    private void validateAssetPath(String requestId, String fileName) {
        boolean validName = fileName != null && fileName.endsWith(".png") && ASSET_PREFIXES.stream().anyMatch(fileName::startsWith);
        if (!REQUEST_ID.matcher(requestId == null ? "" : requestId).matches() || !validName || fileName.contains("..")) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Invalid segmentation asset path");
        }
    }

    private BusinessException downstream(String message) {
        return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), message);
    }

    public record AssetBody(byte[] bytes, String contentType) {}
}
