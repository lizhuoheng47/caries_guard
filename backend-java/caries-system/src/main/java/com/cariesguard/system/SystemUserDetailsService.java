package com.cariesguard.system;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.app.AuthAppService;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SystemUserDetailsService implements UserDetailsService {

    private final SystemUserAuthRepository systemUserAuthRepository;
    private final AuthAppService authAppService;

    public SystemUserDetailsService(SystemUserAuthRepository systemUserAuthRepository,
                                    AuthAppService authAppService) {
        this.systemUserAuthRepository = systemUserAuthRepository;
        this.authAppService = authAppService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemUserAuthModel user = systemUserAuthRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED);
        }
        return authAppService.toAuthenticatedUser(user);
    }
}
