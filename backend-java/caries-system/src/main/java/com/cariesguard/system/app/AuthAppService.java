package com.cariesguard.system.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.jwt.JwtTokenProvider;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import com.cariesguard.system.interfaces.command.LoginCommand;
import com.cariesguard.system.interfaces.vo.CurrentUserVO;
import com.cariesguard.system.interfaces.vo.LoginTokenVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthAppService {

    private final SystemUserAuthRepository systemUserAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthAppService(SystemUserAuthRepository systemUserAuthRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.systemUserAuthRepository = systemUserAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginTokenVO login(LoginCommand command) {
        SystemUserAuthModel user = systemUserAuthRepository.findByUsername(command.getUsername())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_CREDENTIALS));
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(command.getPassword(), user.passwordHash())) {
            throw new BusinessException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        AuthenticatedUser authenticatedUser = toAuthenticatedUser(user);
        String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
        return new LoginTokenVO("Bearer", accessToken, jwtTokenProvider.getAccessTokenExpireSeconds());
    }

    public CurrentUserVO currentUser() {
        AuthenticatedUser currentUser = SecurityContextUtils.currentUser();
        SystemUserAuthModel user = systemUserAuthRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
        return new CurrentUserVO(
                user.userId(),
                user.orgId(),
                user.username(),
                user.displayName(),
                user.userTypeCode(),
                user.roleCodes());
    }

    public AuthenticatedUser toAuthenticatedUser(SystemUserAuthModel user) {
        return new AuthenticatedUser(
                user.userId(),
                user.orgId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                "ACTIVE".equals(user.status()),
                user.roleCodes());
    }
}
