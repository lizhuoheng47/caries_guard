package com.cariesguard.framework.security.sensitive;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultMaskingService implements MaskingService {

    @Override
    public String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(Math.max(1, name.length() - 1));
    }

    @Override
    public String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        if (phone.length() <= 7) {
            return maskGeneric(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return null;
        }
        if (idCard.length() <= 8) {
            return maskGeneric(idCard);
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }

    @Override
    public String maskBirthDate(String birthDate) {
        if (!StringUtils.hasText(birthDate)) {
            return null;
        }
        if (birthDate.length() >= 7) {
            return birthDate.substring(0, 7) + "-**";
        }
        return maskGeneric(birthDate);
    }

    @Override
    public String maskGeneric(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }
}
