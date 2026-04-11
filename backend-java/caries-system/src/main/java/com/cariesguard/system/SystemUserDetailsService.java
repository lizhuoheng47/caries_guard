package com.cariesguard.system;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemUserAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SystemUserDetailsService implements UserDetailsService {

    private final SystemUserAuthRepository systemUserAuthRepository;

    public SystemUserDetailsService(SystemUserAuthRepository systemUserAuthRepository) {
        this.systemUserAuthRepository = systemUserAuthRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemUserAuthModel user = systemUserAuthRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED);
        }
        return SystemAuthenticatedUserFactory.fromModel(user);
    }
}
