package com.cariesguard.report.infrastructure.client;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.report.config.RagProperties;
import com.cariesguard.report.domain.client.RagClient;
import com.cariesguard.report.domain.model.RagAnswerModel;
import com.cariesguard.report.domain.model.RagCitationModel;
import com.cariesguard.report.domain.model.RagDoctorQaRequestModel;
import com.cariesguard.report.domain.model.RagPatientExplanationRequestModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpRagClient implements RagClient {

    private static final String SUCCESS_CODE = "00000";

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpRagClient(RagProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(timeout(properties.getConnectTimeoutMillis()))
                .build());
    }

    HttpRagClient(RagProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public RagAnswerModel doctorQa(RagDoctorQaRequestModel request) {
        return post("/rag/doctor-qa", request);
    }

    @Override
    public RagAnswerModel patientExplanation(RagPatientExplanationRequestModel request) {
        return post("/rag/patient-explanation", request);
    }

    private RagAnswerModel post(String path, Object requestPayload) {
        try {
            String requestBody = objectMapper.writeValueAsString(requestPayload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint(path)))
                    .timeout(timeout(properties.getRequestTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw externalError("RAG service returned HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw externalError("RAG service invocation was interrupted");
        } catch (IOException | IllegalArgumentException exception) {
            throw externalError("RAG service invocation failed: " + exception.getMessage());
        }
    }

    private RagAnswerModel parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String code = text(root, "code");
        if (!SUCCESS_CODE.equals(code)) {
            String message = text(root, "message");
            throw externalError(StringUtils.hasText(message) ? message : "RAG service returned failure code " + code);
        }
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            throw externalError("RAG service returned empty data");
        }

        List<RagCitationModel> citations = new ArrayList<>();
        JsonNode citationNodes = data.path("citations");
        if (citationNodes.isArray()) {
            for (JsonNode citation : citationNodes) {
                citations.add(new RagCitationModel(
                        intValue(citation, "rankNo"),
                        longValue(citation, "docId"),
                        text(citation, "docTitle"),
                        longValue(citation, "chunkId"),
                        doubleValue(citation, "score"),
                        text(citation, "sourceUri"),
                        text(citation, "chunkText")));
            }
        }

        return new RagAnswerModel(
                text(data, "sessionNo"),
                text(data, "requestNo"),
                text(data, "answerText"),
                citations,
                text(data, "knowledgeVersion"),
                text(data, "modelName"),
                text(data, "safetyFlag"),
                intValue(data, "latencyMs"),
                false);
    }

    private String endpoint(String path) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw externalError("RAG base URL is not configured");
        }
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private static Duration timeout(long millis) {
        return Duration.ofMillis(Math.max(1, millis));
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer intValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private static Double doubleValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private static BusinessException externalError(String message) {
        return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), message);
    }
}
