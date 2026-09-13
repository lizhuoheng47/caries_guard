package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.sensitive.HashService;
import com.cariesguard.framework.security.sensitive.CryptoService;
import com.cariesguard.system.config.PasswordResetProperties;
import com.cariesguard.system.domain.model.PasswordResetTokenModel;
import com.cariesguard.system.domain.model.PasswordResetEmailModel;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.PasswordResetTokenRepository;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import com.cariesguard.system.domain.service.PasswordResetCodeSender;
import com.cariesguard.system.interfaces.command.ConfirmPasswordResetCommand;
import com.cariesguard.system.interfaces.command.RequestPasswordResetCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetAppServiceTests {

    @Mock private SystemUserAuthRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HashService hashService;
    @Mock private CryptoService cryptoService;

    private PasswordResetAppService service;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setEnabled(true);
        properties.setDevelopmentCodeEnabled(true);
        properties.setDeliveryMode("LOCAL");
        properties.setCodeTtlMinutes(10);
        properties.setMaxAttempts(5);
        service = new PasswordResetAppService(
                userRepository,
                tokenRepository,
                properties,
                passwordEncoder,
                hashService,
                cryptoService,
                Optional.<PasswordResetCodeSender>empty());
    }

    @Test
    void requestShouldCreateHashedOneTimeCodeForActiveUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser()));
        when(hashService.hmacSha256(any())).thenReturn("stored-hash");
        RequestPasswordResetCommand command = new RequestPasswordResetCommand();
        command.setUsername(" admin ");

        var response = service.requestReset(command, "127.0.0.1");

        assertThat(response.expiresInSeconds()).isEqualTo(600L);
        assertThat(response.developmentCode()).matches("\\d{6}");
        assertThat(response.deliveryMasked()).isEqualTo("LOCAL_DEVELOPMENT");
        ArgumentCaptor<PasswordResetTokenModel> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenModel.class);
        verify(tokenRepository).invalidateActiveTokens(eq(100001L), any(LocalDateTime.class));
        verify(tokenRepository).create(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().codeHash()).isEqualTo("stored-hash");
        assertThat(tokenCaptor.getValue().requestIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void smtpRequestShouldDecryptAddressAndSendCodeWithoutReturningIt() {
        PasswordResetProperties smtpProperties = new PasswordResetProperties();
        smtpProperties.setEnabled(true);
        smtpProperties.setDevelopmentCodeEnabled(false);
        smtpProperties.setDeliveryMode("SMTP");
        smtpProperties.setCodeTtlMinutes(10);
        PasswordResetCodeSender sender = org.mockito.Mockito.mock(PasswordResetCodeSender.class);
        PasswordResetAppService smtpService = new PasswordResetAppService(
                userRepository,
                tokenRepository,
                smtpProperties,
                passwordEncoder,
                hashService,
                cryptoService,
                Optional.of(sender));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser()));
        when(userRepository.findPasswordResetEmail(100001L))
                .thenReturn(Optional.of(new PasswordResetEmailModel("email-cipher", "a***@example.com")));
        when(cryptoService.decrypt("email-cipher")).thenReturn("admin@example.com");
        when(hashService.hmacSha256(any())).thenReturn("stored-hash");
        RequestPasswordResetCommand command = new RequestPasswordResetCommand();
        command.setUsername("admin");

        var response = smtpService.requestReset(command, "127.0.0.1");

        assertThat(response.developmentCode()).isNull();
        assertThat(response.deliveryMasked()).isEqualTo("a***@example.com");
        verify(sender).send(
                eq("admin@example.com"),
                eq("admin"),
                org.mockito.ArgumentMatchers.matches("\\d{6}"),
                eq(10));
    }

    @Test
    void confirmShouldRejectWrongCodeAndCountAttempt() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser()));
        when(tokenRepository.findLatestActive(eq(100001L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeToken("expected-hash", 0)));
        when(hashService.hmacSha256("salt:000000")).thenReturn("wrong-hash");

        assertThatThrownBy(() -> service.confirmReset(confirmCommand("000000")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("A0410"));
        verify(tokenRepository).incrementAttempts(9L);
    }

    @Test
    void confirmShouldUpdatePasswordAndConsumeCode() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser()));
        when(tokenRepository.findLatestActive(eq(100001L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeToken("expected-hash", 0)));
        when(hashService.hmacSha256("salt:123456")).thenReturn("expected-hash");
        when(passwordEncoder.encode("NewPassword8")).thenReturn("bcrypt-hash");

        var response = service.confirmReset(confirmCommand("123456"));

        assertThat(response.reset()).isTrue();
        verify(userRepository).updatePassword(eq(100001L), eq("bcrypt-hash"), any(LocalDateTime.class));
        verify(tokenRepository).markUsed(eq(9L), any(LocalDateTime.class));
        verify(tokenRepository).invalidateActiveTokens(eq(100001L), any(LocalDateTime.class));
    }

    @Test
    void fifthWrongCodeShouldConsumeTokenImmediately() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser()));
        when(tokenRepository.findLatestActive(eq(100001L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeToken("expected-hash", 4)));
        when(hashService.hmacSha256("salt:000000")).thenReturn("wrong-hash");

        assertThatThrownBy(() -> service.confirmReset(confirmCommand("000000")))
                .isInstanceOf(BusinessException.class);

        verify(tokenRepository).markUsed(eq(9L), any(LocalDateTime.class));
        verify(tokenRepository, never()).incrementAttempts(9L);
    }

    private ConfirmPasswordResetCommand confirmCommand(String code) {
        ConfirmPasswordResetCommand command = new ConfirmPasswordResetCommand();
        command.setUsername("admin");
        command.setVerificationCode(code);
        command.setNewPassword("NewPassword8");
        command.setConfirmPassword("NewPassword8");
        return command;
    }

    private PasswordResetTokenModel activeToken(String codeHash, int attempts) {
        return new PasswordResetTokenModel(
                9L, 100001L, "admin", "salt", codeHash,
                LocalDateTime.now().plusMinutes(5), null, attempts,
                "127.0.0.1", LocalDateTime.now());
    }

    private SystemUserAuthModel activeUser() {
        return new SystemUserAuthModel(
                100001L, 100001L, 100001L, "U100001", "admin", "old-hash",
                "Admin", "Admin", "138****0000", "admin@demo.local", null,
                "ADMIN", "UNKNOWN", null, null, "ACTIVE", List.of("SYS_ADMIN"));
    }
}
