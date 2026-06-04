package com.farmland.intel.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM helper for storing sensitive values such as user-provided AI API keys.
 *
 * The encryption key is intentionally derived from the application itself so
 * users can save their own AI keys without requiring a server environment
 * variable. This protects against plain-text database exposure; deployments that
 * need stricter key management can replace this with an external key provider.
 */
public final class CryptoUtils {

    private CryptoUtils() {}

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_SEED = "bangbang-agro-user-ai-config-key-v1";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 16;

    public static String encrypt(String plainText) {
        if (!hasText(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt sensitive value", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (!hasText(cipherText)) {
            return cipherText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            if (decoded.length < GCM_IV_LENGTH + 16) {
                return cipherText;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }

    public static boolean isEncrypted(String text) {
        if (!hasText(text)) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return decoded.length >= GCM_IV_LENGTH + 16;
        } catch (Exception e) {
            return false;
        }
    }

    private static SecretKeySpec keySpec() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(KEY_SEED.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[AES_KEY_LENGTH];
        System.arraycopy(digest, 0, key, 0, AES_KEY_LENGTH);
        return new SecretKeySpec(key, ALGORITHM);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
