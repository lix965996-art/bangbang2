package com.farmland.intel.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;

import java.util.Objects;

public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String encode(String rawPassword) {
        if (StrUtil.isBlank(rawPassword)) {
            return rawPassword;
        }

        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (StrUtil.isBlank(rawPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        if (isEncoded(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return Objects.equals(rawPassword, storedPassword);
    }

    public static boolean needsEncoding(String password) {
        return StrUtil.isNotBlank(password) && !isEncoded(password);
    }

    public static boolean isEncoded(String password) {
        return StrUtil.startWithAny(password, "$2a$", "$2b$", "$2y$");
    }
}
