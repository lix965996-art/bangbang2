package com.farmland.intel.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.entity.User;

public final class DataPermissionUtils {

    private DataPermissionUtils() {
    }

    public static <T> void applyUserMatchPermission(QueryWrapper<T> queryWrapper, String columnName, User currentUser) {
        if (queryWrapper == null || currentUser == null || "ROLE_ADMIN".equals(currentUser.getRole())) {
            return;
        }

        String username = StrUtil.trimToEmpty(currentUser.getUsername());
        String nickname = StrUtil.trimToEmpty(currentUser.getNickname());
        if (StrUtil.isBlank(username) && StrUtil.isBlank(nickname)) {
            queryWrapper.eq(columnName, "__NO_MATCH__");
            return;
        }

        if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(nickname) && !StrUtil.equals(username, nickname)) {
            queryWrapper.and(wrapper -> wrapper.eq(columnName, username).or().eq(columnName, nickname));
            return;
        }

        queryWrapper.eq(columnName, StrUtil.blankToDefault(username, nickname));
    }

    public static String resolveUserDisplayName(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(currentUser.getNickname()), StrUtil.trim(currentUser.getUsername()));
    }
}
