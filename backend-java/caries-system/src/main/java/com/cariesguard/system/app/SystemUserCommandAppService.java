package com.cariesguard.system.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.ProtectedValue;
import com.cariesguard.framework.security.sensitive.SensitiveDataFacade;
import com.cariesguard.system.domain.model.SystemManagedUserModel;
import com.cariesguard.system.domain.model.SystemUserUpsertModel;
import com.cariesguard.system.domain.repository.SystemUserCommandRepository;
import com.cariesguard.system.interfaces.command.CreateSystemUserCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemUserCommand;
import com.cariesguard.system.interfaces.vo.SystemUserMutationVO;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemUserCommandAppService {

    private final SystemUserCommandRepository systemUserCommandRepository;
    private final SensitiveDataFacade sensitiveDataFacade;
    private final PasswordEncoder passwordEncoder;

    public SystemUserCommandAppService(SystemUserCommandRepository systemUserCommandRepository,
                                       SensitiveDataFacade sensitiveDataFacade,
                                       PasswordEncoder passwordEncoder) {
        this.systemUserCommandRepository = systemUserCommandRepository;
        this.sensitiveDataFacade = sensitiveDataFacade;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SystemUserMutationVO createUser(CreateSystemUserCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        Set<Long> roleIds = normalizeRoleIds(command.getRoleIds());
        validateDept(command.getDeptId(), operator.getOrgId());
        validateRoles(roleIds, operator.getOrgId());
        validateUsername(command.getUsername(), null);

        String userNo = resolveUserNo(command.getUserNo(), null, null);
        long userId = IdWorker.getId();
        SystemUserUpsertModel model = buildModel(
                userId,
                operator,
                operator.getOrgId(),
                command.getDeptId(),
                userNo,
                command.getUsername(),
                command.getPassword(),
                command.getRealName(),
                command.getNickName(),
                command.getUserTypeCode(),
                command.getGenderCode(),
                command.getPhone(),
                command.getEmail(),
                command.getAvatarUrl(),
                command.getCertificateTypeCode(),
                command.getCertificateNo(),
                defaultStatus(command.getStatus()),
                command.getRemark(),
                roleIds,
                true,
                null);
        systemUserCommandRepository.createUser(model);
        return new SystemUserMutationVO(model.userId(), model.userNo(), model.username(), model.status(), roleIds.stream().toList());
    }

    @Transactional
    public SystemUserMutationVO updateUser(Long userId, UpdateSystemUserCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        SystemManagedUserModel existing = systemUserCommandRepository.findManagedUser(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "System user does not exist"));
        if (!existing.orgId().equals(operator.getOrgId()) && !operator.hasAnyRole("ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        Set<Long> roleIds = normalizeRoleIds(command.getRoleIds());
        validateDept(command.getDeptId(), existing.orgId());
        validateRoles(roleIds, existing.orgId());
        validateUsername(command.getUsername(), userId);

        String userNo = resolveUserNo(command.getUserNo(), existing.userNo(), userId);
        SystemUserUpsertModel model = buildModel(
                userId,
                operator,
                existing.orgId(),
                command.getDeptId(),
                userNo,
                command.getUsername(),
                command.getPassword(),
                command.getRealName(),
                command.getNickName(),
                command.getUserTypeCode(),
                command.getGenderCode(),
                command.getPhone(),
                command.getEmail(),
                command.getAvatarUrl(),
                command.getCertificateTypeCode(),
                command.getCertificateNo(),
                command.getStatus(),
                command.getRemark(),
                roleIds,
                StringUtils.hasText(command.getPassword()),
                existing.passwordHash());
        systemUserCommandRepository.updateUser(model);
        return new SystemUserMutationVO(model.userId(), model.userNo(), model.username(), model.status(), roleIds.stream().toList());
    }

    private void validateDept(Long deptId, Long orgId) {
        if (deptId != null && !systemUserCommandRepository.existsActiveDept(deptId, orgId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Department does not exist or is inactive");
        }
    }

    private void validateRoles(Set<Long> roleIds, Long orgId) {
        if (systemUserCommandRepository.findActiveRoleIds(roleIds, orgId).size() != roleIds.size()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Role selection contains invalid or inactive records");
        }
    }

    private void validateUsername(String username, Long excludeUserId) {
        if (systemUserCommandRepository.existsUsername(username, excludeUserId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Username already exists");
        }
    }

    private String resolveUserNo(String requestedUserNo, String currentUserNo, Long excludeUserId) {
        String candidate = StringUtils.hasText(requestedUserNo)
                ? requestedUserNo.trim()
                : currentUserNo;
        if (!StringUtils.hasText(candidate)) {
            candidate = "U" + IdWorker.getIdStr();
        }
        if (systemUserCommandRepository.existsUserNo(candidate, excludeUserId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "User number already exists");
        }
        return candidate;
    }

    private Set<Long> normalizeRoleIds(java.util.List<Long> roleIds) {
        return new LinkedHashSet<>(roleIds);
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "ACTIVE";
    }

    private SystemUserUpsertModel buildModel(Long userId,
                                             AuthenticatedUser operator,
                                             Long orgId,
                                             Long deptId,
                                             String userNo,
                                             String username,
                                             String password,
                                             String realName,
                                             String nickName,
                                             String userTypeCode,
                                             String genderCode,
                                             String phone,
                                             String email,
                                             String avatarUrl,
                                             String certificateTypeCode,
                                             String certificateNo,
                                             String status,
                                             String remark,
                                             Set<Long> roleIds,
                                             boolean resetPassword,
                                             String currentPasswordHash) {
        ProtectedValue realNameValue = sensitiveDataFacade.protectName(realName);
        ProtectedValue phoneValue = sensitiveDataFacade.protectPhone(phone);
        ProtectedValue emailValue = sensitiveDataFacade.protectGeneric(email);
        ProtectedValue certificateValue = sensitiveDataFacade.protectIdCard(certificateNo);
        String passwordHash = resetPassword
                ? passwordEncoder.encode(password)
                : currentPasswordHash;
        LocalDateTime pwdUpdatedAt = resetPassword ? LocalDateTime.now() : null;
        return new SystemUserUpsertModel(
                userId,
                deptId,
                userNo,
                username,
                passwordHash,
                realNameValue.encrypted(),
                realNameValue.hash(),
                realNameValue.masked(),
                StringUtils.hasText(nickName) ? nickName : realNameValue.masked(),
                userTypeCode,
                StringUtils.hasText(genderCode) ? genderCode : "UNKNOWN",
                phoneValue.encrypted(),
                phoneValue.hash(),
                phoneValue.masked(),
                emailValue.encrypted(),
                emailValue.hash(),
                emailValue.masked(),
                avatarUrl,
                certificateTypeCode,
                certificateValue.encrypted(),
                certificateValue.hash(),
                certificateValue.masked(),
                pwdUpdatedAt,
                orgId,
                status,
                remark,
                operator.getUserId(),
                roleIds);
    }
}
