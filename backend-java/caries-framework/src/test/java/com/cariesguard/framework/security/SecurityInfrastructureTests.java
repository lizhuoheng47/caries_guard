package com.cariesguard.framework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.DefaultCryptoService;
import com.cariesguard.framework.security.sensitive.DefaultHashService;
import com.cariesguard.framework.security.sensitive.DefaultMaskingService;
import com.cariesguard.framework.security.sensitive.ProtectedValue;
import com.cariesguard.framework.security.sensitive.SensitiveDataFacade;
import com.cariesguard.framework.security.sensitive.SensitiveDataProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityInfrastructureTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldProtectSensitiveValueWithEncryptionHashAndMask() {
        SensitiveDataProperties properties = new SensitiveDataProperties();
        properties.setCryptoSecret("test-sensitive-crypto-secret");
        properties.setHashSecret("test-sensitive-hash-secret");

        DefaultCryptoService cryptoService = new DefaultCryptoService(properties);
        cryptoService.init();
        SensitiveDataFacade facade = new SensitiveDataFacade(
                cryptoService,
                new DefaultHashService(properties),
                new DefaultMaskingService());

        ProtectedValue protectedValue = facade.protectPhone("13812345678");

        assertNotEquals("13812345678", protectedValue.encrypted());
        assertEquals("138****5678", protectedValue.masked());
        assertEquals("13812345678", cryptoService.decrypt(protectedValue.encrypted()));
    }

    @Test
    void shouldResolveAllScopeForAdminUser() {
        AuthenticatedUser user = new AuthenticatedUser(
                1L,
                100001L,
                "admin",
                "encoded",
                "Admin",
                true,
                List.of("SYS_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        assertEquals(DataScopeType.ALL, SecurityContextUtils.currentDataScope().scopeType());
    }
}
