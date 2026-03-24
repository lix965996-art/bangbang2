package com.farmland.intel.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.config.interceptor.AuthAccess;
import com.farmland.intel.controller.dto.UserDTO;
import com.farmland.intel.controller.dto.UserPasswordDTO;
import com.farmland.intel.entity.User;
import com.farmland.intel.exception.ServiceException;
import com.farmland.intel.service.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        return Result.success(userService.login(userDTO));
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        userDTO.setNickname(userDTO.getUsername());
        return Result.success(userService.register(userDTO));
    }

    @PostMapping
    public Result save(@RequestBody User user) {
        String username = user.getUsername();
        if (StrUtil.isBlank(username)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        if (StrUtil.isBlank(user.getNickname())) {
            user.setNickname(username);
        }
        if (user.getId() != null) {
            user.setPassword(null);
        } else {
            String rawPassword = StrUtil.blankToDefault(user.getPassword(), "123");
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        return Result.success(userService.saveOrUpdate(user));
    }

    @PostMapping("/password")
    public Result password(@RequestBody UserPasswordDTO userPasswordDTO) {
        userService.updatePassword(userPasswordDTO);
        return Result.success();
    }

    @AuthAccess
    @PutMapping("/reset")
    public Result reset(@RequestBody UserPasswordDTO userPasswordDTO) {
        if (StrUtil.isBlank(userPasswordDTO.getUsername()) || StrUtil.isBlank(userPasswordDTO.getPhone())) {
            throw new ServiceException(Constants.CODE_400, "参数异常");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userPasswordDTO.getUsername());
        queryWrapper.eq("phone", userPasswordDTO.getPhone());
        List<User> list = userService.list(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            return Result.error(Constants.CODE_404, "用户不存在或手机号错误");
        }
        User user = list.get(0);
        user.setPassword(passwordEncoder.encode("123"));
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(userService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(userService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(userService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/username/{username}")
    public Result findByUsername(@PathVariable String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return Result.success(userService.getOne(queryWrapper));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String email,
                           @RequestParam(defaultValue = "") String address) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        if (StrUtil.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        if (StrUtil.isNotBlank(email)) {
            queryWrapper.like("email", email);
        }
        if (StrUtil.isNotBlank(address)) {
            queryWrapper.like("address", address);
        }

        return Result.success(userService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        List<User> list = userService.list();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("nickname", "昵称");
        writer.addHeaderAlias("email", "邮箱");
        writer.addHeaderAlias("phone", "电话");
        writer.addHeaderAlias("address", "地址");
        writer.addHeaderAlias("createTime", "创建时间");
        writer.addHeaderAlias("avatarUrl", "头像");

        List<Map<String, Object>> exportRows = CollUtil.newArrayList();
        for (User user : list) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            row.put("email", user.getEmail());
            row.put("phone", user.getPhone());
            row.put("address", user.getAddress());
            row.put("createTime", user.getCreateTime());
            row.put("avatarUrl", user.getAvatarUrl());
            exportRows.add(row);
        }
        writer.write(exportRows, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("用户信息", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        out.close();
        writer.close();
    }

    @PostMapping("/import")
    public Result imp(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error(Constants.CODE_400, "导入文件不能为空");
        }
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        List<List<Object>> rows = reader.read(1);
        List<User> users = CollUtil.newArrayList();
        for (List<Object> row : rows) {
            User user = new User();
            user.setUsername(getCellValue(row, 0));
            if (StrUtil.isBlank(user.getUsername())) {
                continue;
            }
            String rawPassword = StrUtil.blankToDefault(getCellValue(row, 1), "123");
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setNickname(StrUtil.blankToDefault(getCellValue(row, 2), user.getUsername()));
            user.setEmail(getCellValue(row, 3));
            user.setPhone(getCellValue(row, 4));
            user.setAddress(getCellValue(row, 5));
            user.setAvatarUrl(getCellValue(row, 6));
            user.setRole("ROLE_USER");
            users.add(user);
        }

        if (CollUtil.isEmpty(users)) {
            return Result.error(Constants.CODE_400, "未读取到有效用户数据");
        }

        userService.saveBatch(users);
        return Result.success(true);
    }

    private String getCellValue(List<Object> row, int index) {
        if (row == null || row.size() <= index || row.get(index) == null) {
            return null;
        }
        return row.get(index).toString().trim();
    }
}
