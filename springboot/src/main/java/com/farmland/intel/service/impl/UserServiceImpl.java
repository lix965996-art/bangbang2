package com.farmland.intel.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.RoleEnum;
import com.farmland.intel.controller.dto.UserDTO;
import com.farmland.intel.controller.dto.UserPasswordDTO;
import com.farmland.intel.entity.Menu;
import com.farmland.intel.entity.User;
import com.farmland.intel.exception.ServiceException;
import com.farmland.intel.mapper.RoleMapper;
import com.farmland.intel.mapper.RoleMenuMapper;
import com.farmland.intel.mapper.UserMapper;
import com.farmland.intel.service.IMenuService;
import com.farmland.intel.service.IUserService;
import com.farmland.intel.utils.TokenUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Log LOG = Log.get();

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private IMenuService menuService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDTO login(UserDTO userDTO) {
        User user = getUserByUsername(userDTO.getUsername());
        if (user == null || !matchesPassword(userDTO.getPassword(), user)) {
            throw new ServiceException(Constants.CODE_600, "用户名或密码错误");
        }

        upgradePlaintextPasswordIfNeeded(user, userDTO.getPassword());
        BeanUtil.copyProperties(user, userDTO, true);
        userDTO.setPassword(null);
        userDTO.setToken(TokenUtils.genToken(user.getId().toString(), user.getPassword()));
        userDTO.setMenus(getRoleMenus(user.getRole()));
        return userDTO;
    }

    @Override
    public User register(UserDTO userDTO) {
        User existedUser = getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, userDTO.getUsername()));
        if (existedUser != null) {
            throw new ServiceException(Constants.CODE_600, "用户已存在");
        }

        User user = new User();
        BeanUtil.copyProperties(userDTO, user, true);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(RoleEnum.ROLE_USER.toString());
        save(user);
        return user;
    }

    @Override
    public void updatePassword(UserPasswordDTO userPasswordDTO) {
        User user = getUserByUsername(userPasswordDTO.getUsername());
        if (user == null || !matchesPassword(userPasswordDTO.getPassword(), user)) {
            throw new ServiceException(Constants.CODE_600, "密码错误");
        }
        user.setPassword(passwordEncoder.encode(userPasswordDTO.getNewPassword()));
        updateById(user);
    }

    private User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        try {
            return getOne(queryWrapper);
        } catch (Exception e) {
            LOG.error(e);
            throw new ServiceException(Constants.CODE_500, "系统错误");
        }
    }

    private boolean matchesPassword(String rawPassword, User user) {
        String storedPassword = user == null ? null : user.getPassword();
        if (StrUtil.isBlank(rawPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        if (!isEncodedPassword(storedPassword)) {
            return storedPassword.equals(rawPassword);
        }
        try {
            return passwordEncoder.matches(rawPassword, storedPassword);
        } catch (IllegalArgumentException ex) {
            LOG.warn("Invalid encoded password for user: {}", user.getUsername());
            return false;
        }
    }

    private void upgradePlaintextPasswordIfNeeded(User user, String rawPassword) {
        if (user == null || isEncodedPassword(user.getPassword()) || !StrUtil.equals(user.getPassword(), rawPassword)) {
            return;
        }
        try {
            user.setPassword(passwordEncoder.encode(rawPassword));
            updateById(user);
        } catch (Exception ex) {
            LOG.error("Failed to upgrade plaintext password for user: {}", user.getUsername(), ex);
        }
    }

    private boolean isEncodedPassword(String password) {
        return StrUtil.isNotBlank(password) && password.startsWith("$2");
    }

    private List<Menu> getRoleMenus(String roleFlag) {
        Integer roleId = roleMapper.selectByFlag(roleFlag);
        if (roleId == null) {
            return new ArrayList<>();
        }

        List<Integer> menuIds = roleMenuMapper.selectByRoleId(roleId);
        List<Menu> menus = menuService.findMenus("");
        List<Menu> roleMenus = new ArrayList<>();
        for (Menu menu : menus) {
            if (menuIds.contains(menu.getId())) {
                roleMenus.add(menu);
            }
            List<Menu> children = menu.getChildren();
            if (children != null) {
                children.removeIf(child -> !menuIds.contains(child.getId()));
            }
        }
        return roleMenus;
    }
}
