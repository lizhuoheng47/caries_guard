package com.cariesguard.framework.security.sensitive;

import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SensitiveDataFacade {

    private final CryptoService cryptoService;
    private final HashService hashService;
    private final MaskingService maskingService;

    public SensitiveDataFacade(CryptoService cryptoService,
                               HashService hashService,
                               MaskingService maskingService) {
        this.cryptoService = cryptoService;
        this.hashService = hashService;
        this.maskingService = maskingService;
    }

    public ProtectedValue protectName(String plainText) {
        return protect(plainText, maskingService::maskName);
    }

    public ProtectedValue protectPhone(String plainText) {
        return protect(plainText, maskingService::maskPhone);
    }

    public ProtectedValue protectIdCard(String plainText) {
        return protect(plainText, maskingService::maskIdCard);
    }

    public ProtectedValue protectBirthDate(String plainText) {
        return protect(plainText, maskingService::maskBirthDate);
    }

    public ProtectedValue protectGeneric(String plainText) {
        return protect(plainText, maskingService::maskGeneric);
    }

    private ProtectedValue protect(String plainText, Function<String, String> maskingFunction) {
        if (!StringUtils.hasText(plainText)) {
            return new ProtectedValue(null, null, null);
        }
        return new ProtectedValue(
                cryptoService.encrypt(plainText),
                hashService.normalizeThenHash(plainText),
                maskingFunction.apply(plainText));
    }
}
