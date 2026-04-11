package com.cariesguard.framework.security.sensitive;

public interface CryptoService {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
