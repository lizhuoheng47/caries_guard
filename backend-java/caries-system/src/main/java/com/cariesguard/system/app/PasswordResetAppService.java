package com.cariesguard.system.app;

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
import com.cariesguard.system.interfaces.vo.PasswordResetConfirmVO;
import com.cariesguard.system.interfaces.vo.PasswordResetRequestVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetAppService {

    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final String GENERIC_MESSAGE =
            "If the account is eligible, a password reset code has been created.";

    private final SystemUserAuthRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final HashService hashService;
    private final CryptoService cryptoService;
    private final Optional<PasswordResetCodeSender> codeSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetAppService(SystemUserAuthRepository userRepository,
                                   PasswordResetTokenRepository tokenRepository,
                                   PasswordResetProperties properties,
                                   PasswordEncoder passwordEncoder,
                                   HashService hashService,
                                   CryptoService cryptoService,
                                   Optional<PasswordResetCodeSender> codeSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.hashService = hashService;
        this.cryptoService = cryptoService;
        this.codeSender = codeSender;
    }

    @Transactional
    public PasswordResetRequestVO requestReset(RequestPasswordResetCommand command, String requestIp) {
        requireEnabled();
        String username = normalizeUsername(command.getUsername());
        Optional<SystemUserAuthModel> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty() || !"ACTIVE".equals(optionalUser.get().status())) {
            return new PasswordResetRequestVO(GENERIC_MESSAGE, ttlSeconds(), null, null);
        }

        SystemUserAuthModel user = optionalUser.get();
        PasswordResetEmailModel emailTarget = null;
        boolean localDelivery = isLocalDelivery();
        if (!localDelivery) {
            if (!"SMTP".equalsIgnoreCase(properties.getDeliveryMode()) || codeSender.isEmpty()) {
                return new PasswordResetRequestVO(GENERIC_MESSAGE, ttlSeconds(), null, null);
            }
            emailTarget = userRepository.findPasswordResetEmail(user.userId()).orElse(null);
            if (emailTarget == null) {
                return new PasswordResetRequestVO(GENERIC_MESSAGE, ttlSeconds(), null, null);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String code = String.format("%06d", 100000 + secureRandom.nextInt(900000));
        String salt = UUID.randomUUID().toString().replace("-", "");
        String codeHash = hashService.hmacSha256(salt + ":" + code);
        tokenRepository.invalidateActiveTokens(user.userId(), now);
        tokenRepository.create(new PasswordResetTokenModel(
                null,
                user.userId(),
                user.username(),
                salt,
                codeHash,
                now.plusMinutes(Math.max(1, properties.getCodeTtlMinutes())),
                null,
                0,
                requestIp,
                now));

        if (!localDelivery) {
            String recipient = cryptoService.decrypt(emailTarget.encryptedEmail());
            codeSender.orElseThrow().send(
                    recipient,
                    user.username(),
                    code,
                    Math.max(1, properties.getCodeTtlMinutes()));
        }

        return new PasswordResetRequestVO(
                GENERIC_MESSAGE,
                ttlSeconds(),
                localDelivery ? "LOCAL_DEVELOPMENT" : emailTarget.maskedEmail(),
                localDelivery ? code : null);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PasswordResetConfirmVO confirmReset(ConfirmPasswordResetCommand command) {
        requireEnabled();
        validateNewPassword(command);
        String username = normalizeUsername(command.getUsername());
        SystemUserAuthModel user = userRepository.findByUsername(username)
                .filter(item -> "ACTIVE".equals(item.status()))
                .orElseThrow(this::invalidCode);
        LocalDateTime now = LocalDateTime.now();
        PasswordResetTokenModel token = tokenRepository.findLatestActive(user.userId(), now)
                .orElseThrow(this::invalidCode);
        if (token.attemptCount() >= Math.max(1, properties.getMaxAttempts())) {
            tokenRepository.markUsed(token.id(), now);
            throw invalidCode();
        }

        String suppliedHash = hashService.hmacSha256(token.codeSalt() + ":" + command.getVerificationCode());
        if (!secureEquals(token.codeHash(), suppliedHash)) {
            if (token.attemptCount() + 1 >= Math.max(1, properties.getMaxAttempts())) {
                tokenRepository.markUsed(token.id(), now);
            } else {
                tokenRepository.incrementAttempts(token.id());
            }
            throw invalidCode();
        }

        userRepository.updatePassword(user.userId(), passwordEncoder.encode(command.getNewPassword()), now);
        tokenRepository.markUsed(token.id(), now);
        tokenRepository.invalidateActiveTokens(user.userId(), now);
        return new PasswordResetConfirmVO(true, "Password has been reset. Please sign in with the new password.");
    }

    private void validateNewPassword(ConfirmPasswordResetCommand command) {
        if (!command.getNewPassword().equals(command.getConfirmPassword())) {
            throw new BusinessException("A0412", "The two password entries do not match");
        }
        String password = command.getNewPassword();
        if (!LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
            throw new BusinessException("A0412", "Password must contain at least one letter and one digit");
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException("A0411", "Password reset is disabled");
        }
    }

    private long ttlSeconds() {
        return Math.max(1, properties.getCodeTtlMinutes()) * 60L;
    }

    private boolean isLocalDelivery() {
        return properties.isDevelopmentCodeEnabled()
                && "LOCAL".equalsIgnoreCase(properties.getDeliveryMode());
    }

    private static String normalizeUsername(String value) {
        return value == null ? "" : value.trim();
    }

    private BusinessException invalidCode() {
        return new BusinessException("A0410", "Verification code is invalid or expired");
    }

    private static boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
