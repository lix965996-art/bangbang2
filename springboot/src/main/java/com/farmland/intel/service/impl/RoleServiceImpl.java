package com.farmland.intel.service.impl;

import com.farmland.intel.entity.Menu;
import com.farmland.intel.entity.Role;
import com.farmland.intel.entity.RoleMenu;
import com.farmland.intel.mapper.RoleMapper;
import com.farmland.intel.mapper.RoleMenuMapper;
import com.farmland.intel.service.IMenuService;
import com.farmland.intel.service.IRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author
 * @since 2022-02-10
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Resource
    private IMenuService menuService;

    @Transactional
    @Override
    public void setRoleMenu(Integer roleId, List<Integer> menuIds) {
        // 先删除当前角色id所有的绑定关系
        roleMenuMapper.deleteByRoleId(roleId);

        // 再把前端传过来的菜单id数组绑定到当前的这个角色id上去
        // 使用 Set 记录已插入的菜单ID，防止重复插入
        Set<Integer> insertedIds = new HashSet<>(menuIds);
        for (Integer menuId : menuIds) {
            Menu menu = menuService.getById(menuId);
            if (menu == null) {
                continue;
            }
            if (menu.getPid() != null && !insertedIds.contains(menu.getPid())) {
                // 二级菜单 并且传过来的menuId数组里面没有它的父级id，补上父级id
                RoleMenu parentRoleMenu = new RoleMenu();
                parentRoleMenu.setRoleId(roleId);
                parentRoleMenu.setMenuId(menu.getPid());
                roleMenuMapper.insert(parentRoleMenu);
                insertedIds.add(menu.getPid());
            }
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }

    @Override
    public List<Integer> getRoleMenu(Integer roleId) {

        return roleMenuMapper.selectByRoleId(roleId);
    }

}
