package com.cariesguard.report.infrastructure.client;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.report.config.RagProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class HttpRagAdminClient {

    private static final String SUCCESS_CODE = "00000";

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpRagAdminClient(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMillis())))
                .build();
    }

    public Object get(String path, Map<String, ?> queryParams, String traceId) {
        return exchange("GET", path, queryParams, null, "application/json", traceId);
    }

    public Object post(String path, Object payload, String traceId) {
        return exchange("POST", path, null, serialize(payload), "application/json", traceId);
    }

    public Object put(String path, Object payload, String traceId) {
        return exchange("PUT", path, null, serialize(payload), "application/json", traceId);
    }

    public Object multipart(String path, MultipartFile file, Map<String, ?> formFields, String traceId) {
        String boundary = "----CariesGuardBoundary" + UUID.randomUUID();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (Map.Entry<String, ?> entry : formFields.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                output.write(String.valueOf(entry.getValue()).getBytes(StandardCharsets.UTF_8));
                output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Type: " + (StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream") + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write(file.getBytes());
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return exchange("POST", path, null, output.toByteArray(), "multipart/form-data; boundary=" + boundary, traceId);
        } catch (IOException exception) {
            throw externalError("RAG upload proxy failed: " + exception.getMessage());
        }
    }

    private Object exchange(String method, String path, Map<String, ?> queryParams, byte[] body, String contentType, String traceId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint(path, queryParams)))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMillis(Math.max(1, properties.getRequestTimeoutMillis())))
                    .header("Accept", "application/json");
            if (StringUtils.hasText(traceId)) {
                builder.header("X-Trace-Id", traceId);
            }
            if (body != null) {
                builder.header("Content-Type", contentType).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw externalError("RAG admin service returned HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw externalError("RAG admin invocation was interrupted");
        } catch (IOException exception) {
            throw externalError("RAG admin invocation failed: " + exception.getMessage());
        }
    }

    private Object parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!SUCCESS_CODE.equals(root.path("code").asText())) {
            throw externalError(root.path("message").asText("RAG admin service returned failure"));
        }
        JsonNode data = root.path("data");
        return objectMapper.convertValue(data, new TypeReference<Object>() { });
    }

    private byte[] serialize(Object payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (IOException exception) {
            throw externalError("Failed to serialize request payload: " + exception.getMessage());
        }
    }

    private String endpoint(String path, Map<String, ?> queryParams) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw externalError("RAG base URL is not configured");
        }
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        StringBuilder endpoint = new StringBuilder(baseUrl).append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                endpoint.append(first ? "?" : "&");
                first = false;
                endpoint.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                endpoint.append("=");
                endpoint.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
        }
        return endpoint.toString();
    }

    public Map<String, Object> linkedMap() {
        return new LinkedHashMap<>();
    }

    private static BusinessException externalError(String message) {
        return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), message);
    }
}
