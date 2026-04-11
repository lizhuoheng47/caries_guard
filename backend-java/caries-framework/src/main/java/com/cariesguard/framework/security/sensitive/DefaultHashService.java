package com.cariesguard.framework.security.sensitive;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultHashService implements HashService {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SensitiveDataProperties properties;

    public DefaultHashService(SensitiveDataProperties properties) {
        this.properties = properties;
    }

    @Override
    public String hmacSha256(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (!StringUtils.hasText(properties.getHashSecret())) {
            throw new IllegalStateException("Sensitive hash secret is empty");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash sensitive data", exception);
        }
    }

    @Override
    public String normalizeThenHash(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return hmacSha256(value.trim().replace(" ", ""));
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xFF;
            chars[index * 2] = HEX[value >>> 4];
            chars[index * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(chars);
    }
}
