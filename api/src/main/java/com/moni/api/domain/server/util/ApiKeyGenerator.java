package com.moni.api.domain.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.crypto.keygen.KeyGenerators;

public class ApiKeyGenerator {

    private static final int KEY_BYTE_LENGTH = 32;

    private ApiKeyGenerator() {
    }

    public static String generateApiKey() {
        byte[] secureBytes = KeyGenerators.secureRandom(KEY_BYTE_LENGTH).generateKey();
        return HexFormat.of().formatHex(secureBytes);
    }

    public static String hash(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new IllegalArgumentException("API Key는 비어있거나 공백일 수 없습니다.");
        }
        try {
            byte[] encodedHash = MessageDigest.getInstance("SHA-256")
                    .digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 암호화 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
