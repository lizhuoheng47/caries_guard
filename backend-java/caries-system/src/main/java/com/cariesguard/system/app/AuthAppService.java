package com.cariesguard.system.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.jwt.JwtTokenProvider;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.SystemAuthenticatedUserFactory;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemPermissionRepository;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import com.cariesguard.system.interfaces.command.LoginCommand;
import com.cariesguard.system.interfaces.vo.CurrentUserVO;
import com.cariesguard.system.interfaces.vo.CurrentUserPermissionsVO;
import com.cariesguard.system.interfaces.vo.LoginTokenVO;
import com.cariesguard.system.interfaces.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthAppService {

    private final SystemUserAuthRepository systemUserAuthRepository;
    private final SystemPermissionRepository systemPermissionRepository;
    private final LoginAuditService loginAuditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CompetitionExposureService competitionExposureService;

    public AuthAppService(SystemUserAuthRepository systemUserAuthRepository,
                          SystemPermissionRepository systemPermissionRepository,
                          LoginAuditService loginAuditService,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          CompetitionExposureService competitionExposureService) {
        this.systemUserAuthRepository = systemUserAuthRepository;
        this.systemPermissionRepository = systemPermissionRepository;
        this.loginAuditService = loginAuditService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.competitionExposureService = competitionExposureService;
    }

    public LoginTokenVO login(LoginCommand command, HttpServletRequest request) {
        SystemUserAuthModel user = systemUserAuthRepository.findByUsername(command.getUsername())
                .orElse(null);
        if (user == null) {
            loginAuditService.recordFailure(command.getUsername(), request, CommonErrorCode.INVALID_CREDENTIALS.message());
            throw new BusinessException(CommonErrorCode.INVALID_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.status())) {
            loginAuditService.recordFailure(user, request, CommonErrorCode.ACCOUNT_DISABLED.message());
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(command.getPassword(), user.passwordHash())) {
            loginAuditService.recordFailure(user, request, CommonErrorCode.INVALID_CREDENTIALS.message());
            throw new BusinessException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        AuthenticatedUser principal = SystemAuthenticatedUserFactory.fromModel(user);
        systemUserAuthRepository.markLoginSuccess(user.userId(), LocalDateTime.now());
        loginAuditService.recordSuccess(user, request);
        return new LoginTokenVO(
                jwtTokenProvider.createAccessToken(principal),
                jwtTokenProvider.getAccessTokenExpireSeconds(),
                new LoginUserVO(
                        user.userId(),
                        user.username(),
                        user.nickName(),
                        user.userTypeCode(),
                        user.orgId()));
    }

    public CurrentUserVO currentUser() {
        SystemUserAuthModel user = loadCurrentUser();
        List<String> permissions = competitionExposureService.filterPermissions(
                systemPermissionRepository.findPermissionCodesByUserId(user.userId()));
        return new CurrentUserVO(
                user.userId(),
                user.username(),
                user.nickName(),
                user.userTypeCode(),
                user.orgId(),
                user.roleCodes(),
                permissions);
    }

    public CurrentUserPermissionsVO currentPermissions() {
        SystemUserAuthModel user = loadCurrentUser();
        return new CurrentUserPermissionsVO(
                user.userId(),
                user.roleCodes(),
                competitionExposureService.filterPermissions(
                        systemPermissionRepository.findPermissionCodesByUserId(user.userId())));
    }

    private SystemUserAuthModel loadCurrentUser() {
        AuthenticatedUser principal = SecurityContextUtils.currentUser();
        return systemUserAuthRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
    }
}
