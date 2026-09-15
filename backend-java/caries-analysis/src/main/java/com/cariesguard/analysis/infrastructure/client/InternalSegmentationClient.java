package com.cariesguard.analysis.infrastructure.client;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class InternalSegmentationClient {
    private final AnalysisProperties properties;
    private final ObjectMapper objectMapper;
    // Uvicorn 仅处理 HTTP/1.1；禁止 JDK HttpClient 对带请求体的推理调用尝试 h2c 升级。
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
        return send(request);
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

    private BusinessException downstream(String message) {
        return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), message);
    }
}
