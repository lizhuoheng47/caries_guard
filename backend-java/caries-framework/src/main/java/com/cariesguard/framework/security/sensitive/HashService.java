package com.cariesguard.framework.security.sensitive;

public interface HashService {

    String hmacSha256(String value);

    String normalizeThenHash(String value);
}
