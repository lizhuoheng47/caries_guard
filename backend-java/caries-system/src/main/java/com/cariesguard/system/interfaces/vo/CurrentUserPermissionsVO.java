package com.cariesguard.system.interfaces.vo;

import java.util.List;

public record CurrentUserPermissionsVO(
        Long userId,
        List<String> roles,
        List<String> permissions) {
}
