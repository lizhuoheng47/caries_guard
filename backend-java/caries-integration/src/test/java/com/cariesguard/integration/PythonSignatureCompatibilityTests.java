package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.infrastructure.client.AiCallbackSignatureVerifier;
import com.cariesguard.common.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PythonSignatureCompatibilityTests {

    @Test
    void javaVerifierShouldAcceptPythonGeneratedSignature() throws Exception {
        String secret = "p4-python-signature-secret";
        String body = "{\"taskNo\":\"TASK-PY-001\",\"taskStatusCode\":\"SUCCESS\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        String signature = runPythonSign(secret, timestamp, body);
        Assumptions.assumeTrue(signature != null && !signature.isBlank(),
                "Python runtime is unavailable, skip real-signature compatibility test");

        AnalysisProperties properties = new AnalysisProperties();
        properties.setCallbackSecret(secret);
        properties.setCallbackAllowedClockSkewSeconds(300);
        AiCallbackSignatureVerifier verifier = new AiCallbackSignatureVerifier(properties);

        verifier.verify(body, timestamp, signature);
    }

    @Test
    void javaVerifierShouldRejectSignatureFromDifferentSecret() throws Exception {
        String body = "{\"taskNo\":\"TASK-PY-002\",\"taskStatusCode\":\"SUCCESS\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signatureFromWrongSecret = runPythonSign("wrong-secret", timestamp, body);
        Assumptions.assumeTrue(signatureFromWrongSecret != null && !signatureFromWrongSecret.isBlank(),
                "Python runtime is unavailable, skip real-signature compatibility test");

        AnalysisProperties properties = new AnalysisProperties();
        properties.setCallbackSecret("correct-secret");
        properties.setCallbackAllowedClockSkewSeconds(300);
        AiCallbackSignatureVerifier verifier = new AiCallbackSignatureVerifier(properties);

        assertThatThrownBy(() -> verifier.verify(body, timestamp, signatureFromWrongSecret))
                .isInstanceOf(BusinessException.class);
    }

    private String runPythonSign(String secret, String timestamp, String body) throws IOException, InterruptedException {
        String script = "import base64,hmac,hashlib,sys;"
                + "secret=sys.argv[1].encode('utf-8');"
                + "timestamp=sys.argv[2];"
                + "body=sys.stdin.read();"
                + "digest=hmac.new(secret,(timestamp+'.'+body).encode('utf-8'),hashlib.sha256).digest();"
                + "print(base64.urlsafe_b64encode(digest).rstrip(b'=').decode('utf-8'))";

        String signature = runPython(List.of("python", "-c", script, secret, timestamp), body);
        if (signature != null) {
            return signature;
        }
        return runPython(List.of("py", "-3", "-c", script, secret, timestamp), body);
    }

    private String runPython(List<String> command, String body) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException exception) {
            return null;
        }
        process.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
        process.getOutputStream().close();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        int exit = process.waitFor();
        if (exit != 0) {
            return null;
        }
        String combined = new String(stdout, StandardCharsets.UTF_8) + "\n" + new String(stderr, StandardCharsets.UTF_8);
        String candidate = null;
        for (String line : combined.split("\\R")) {
            String text = line.trim();
            if (text.matches("^[A-Za-z0-9_-]{20,}$")) {
                candidate = text;
            }
        }
        return candidate;
    }
}
