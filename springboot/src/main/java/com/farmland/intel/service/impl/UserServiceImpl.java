package com.farmland.intel.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.farmland.intel.utils.PasswordUtils;
import com.farmland.intel.utils.TokenUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Log LOG = Log.get();

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private IMenuService menuService;

    @Override
    public UserDTO login(UserDTO userDTO) {
        User one = getUserInfo(userDTO);
        if (one == null) {
            throw new ServiceException(Constants.CODE_600, "用户名或密码错误");
        }

        BeanUtil.copyProperties(one, userDTO, true);
        userDTO.setToken(TokenUtils.genToken(one.getId().toString()));
        userDTO.setPassword(null);
        userDTO.setMenus(getRoleMenus(one.getRole()));
        return userDTO;
    }

    @Override
    public User register(UserDTO userDTO) {
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, userDTO.getUsername()));
        if (one != null) {
            throw new ServiceException(Constants.CODE_600, "用户已存在");
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            one = new User();
            BeanUtil.copyProperties(userDTO, one, true);
            one.setPassword(PasswordUtils.encode(userDTO.getPassword()));
            one.setRole(RoleEnum.ROLE_USER.toString());
            one.setUid(generateUniqueUid());
            try {
                save(one);
                one.setPassword(null);
                return one;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // UID 冲突，重试（数据库 UNIQUE 约束兜底）
                LOG.warn("UID冲突，重试第{}次", attempt + 1);
            }
        }
        throw new ServiceException(Constants.CODE_500, "注册失败，请稍后重试");
    }

    /**
     * 生成唯一6位数字UID（100000-999999），冲突时重试。
     * 利用数据库 UNIQUE 约束捕获 DuplicateKeyException，避免 TOCTOU 竞态。
     */
    private String generateUniqueUid() {
        java.util.Random random = new java.util.Random();
        int maxRetries = 20;
        for (int i = 0; i < maxRetries; i++) {
            String uid = String.valueOf(100000 + random.nextInt(900000));
            // 先快速查询，减少不必要的DB写入冲突
            if (count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("uid", uid)) > 0) {
                continue;
            }
            return uid;
        }
        throw new com.farmland.intel.exception.ServiceException(
                com.farmland.intel.common.Constants.CODE_500, "UID生成失败，请稍后重试");
    }

    // 用于时序攻击缓解的占位 BCrypt hash（非公开示例值）
    private static final String PLACEHOLDER_HASH = "$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQkW.gFqJyqSu3A8tUnSjmYjMoMOeO";

    @Override
    public void updatePassword(UserPasswordDTO userPasswordDTO) {
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, userPasswordDTO.getUsername()));
        if (user == null) {
            PasswordUtils.matches(userPasswordDTO.getPassword(), PLACEHOLDER_HASH);
            throw new ServiceException(Constants.CODE_600, "密码错误");
        }
        if (!PasswordUtils.matches(userPasswordDTO.getPassword(), user.getPassword())) {
            throw new ServiceException(Constants.CODE_600, "密码错误");
        }
        user.setPassword(PasswordUtils.encode(userPasswordDTO.getNewPassword()));
        updateById(user);
    }

    private User getUserInfo(UserDTO userDTO) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userDTO.getUsername());
        User one;
        try {
            one = getOne(queryWrapper);
        } catch (Exception e) {
            LOG.error(e);
            throw new ServiceException(Constants.CODE_500, "系统错误");
        }

        if (one == null) {
            // 防止用户枚举时序攻击：用户名不存在时也执行等时bcrypt
            PasswordUtils.matches(userDTO.getPassword(), PLACEHOLDER_HASH);
            return null;
        }
        if (!PasswordUtils.matches(userDTO.getPassword(), one.getPassword())) {
            return null;
        }

        // 检查账号是否被封禁
        if (one.getStatus() != null && one.getStatus() == 1) {
            throw new ServiceException(Constants.CODE_600, "您已被管理员拉黑，请稍后再试！");
        }

        if (PasswordUtils.needsEncoding(one.getPassword())) {
            one.setPassword(PasswordUtils.encode(userDTO.getPassword()));
            updateById(one);
        }
        return one;
    }

    private List<Menu> getRoleMenus(String roleFlag) {
        if (roleFlag == null) {
            return new ArrayList<>();
        }
        Integer roleId = roleMapper.selectByFlag(roleFlag);
        if (roleId == null) {
            return new ArrayList<>();
        }
        List<Integer> menuIds = roleMenuMapper.selectByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Menu> menus = menuService.findMenus("");
        List<Menu> roleMenus = new ArrayList<>();
        for (Menu menu : menus) {
            if (menuIds.contains(menu.getId())) {
                // 创建防御性副本，避免修改共享的Menu对象
                Menu copy = new Menu();
                copy.setId(menu.getId());
                copy.setName(menu.getName());
                copy.setPath(menu.getPath());
                copy.setIcon(menu.getIcon());
                copy.setPagePath(menu.getPagePath());
                copy.setDescription(menu.getDescription());
                copy.setPid(menu.getPid());
                copy.setSortNum(menu.getSortNum());
                // 过滤子菜单，只保留当前角色有权访问的
                List<Menu> children = menu.getChildren();
                if (children != null) {
                    List<Menu> filteredChildren = new ArrayList<>();
                    for (Menu child : children) {
                        if (menuIds.contains(child.getId())) {
                            filteredChildren.add(child);
                        }
                    }
                    copy.setChildren(filteredChildren);
                }
                roleMenus.add(copy);
            }
        }
        return roleMenus;
    }
}
