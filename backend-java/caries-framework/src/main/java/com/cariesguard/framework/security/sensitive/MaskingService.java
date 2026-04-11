package com.cariesguard.framework.security.sensitive;

public interface MaskingService {

    String maskName(String name);

    String maskPhone(String phone);

    String maskIdCard(String idCard);

    String maskBirthDate(String birthDate);

    String maskGeneric(String value);
}
