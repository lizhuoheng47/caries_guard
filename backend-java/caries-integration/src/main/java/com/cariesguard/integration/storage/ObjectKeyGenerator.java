package com.cariesguard.integration.storage;

import java.time.LocalDate;

public interface ObjectKeyGenerator {

    String generate(String bizModule, String bizId, String originalFileName, LocalDate date);
}
