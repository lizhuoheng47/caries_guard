package com.cariesguard.analysis.infrastructure.client;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiCallbackSignatureVerifier {

    private final AnalysisProperties analysisProperties;

    public AiCallbackSignatureVerifier(AnalysisProperties analysisProperties) {
        this.analysisProperties = analysisProperties;
    }

    public void verify(String rawBody, String timestamp, String signature) {
        if (!StringUtils.hasText(rawBody) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "AI callback signature headers are required");
        }
        long callbackEpochSeconds;
        try {
            callbackEpochSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "AI callback timestamp is invalid");
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - callbackEpochSeconds) > analysisProperties.getCallbackAllowedClockSkewSeconds()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "AI callback timestamp is expired");
        }
        String expectedSignature = sign(rawBody, timestamp);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "AI callback signature is invalid");
        }
    }

    private String sign(String rawBody, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(analysisProperties.getCallbackSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }
}
