package com.farmland.intel.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.entity.Files;
import com.farmland.intel.exception.ServiceException;
import com.farmland.intel.mapper.FileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${files.upload.path}")
    private String fileUploadPath;

    @Value("${files.upload.allowed-types:}")
    private String allowedTypes;

    @Resource
    private FileMapper fileMapper;

    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "文件不能为空");
        }

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "file");
        String type = FileUtil.extName(originalFilename).toLowerCase(Locale.ROOT);
        validateFileType(type);

        String fileUUID = IdUtil.fastSimpleUUID() + StrUtil.DOT + type;
        Path uploadFile = resolveUploadPath(fileUUID);
        java.nio.file.Files.createDirectories(uploadFile.getParent());

        String md5 = SecureUtil.md5(file.getInputStream());
        Files dbFile = getFileByMd5(md5);
        String url;
        if (dbFile != null) {
            url = normalizeFileUrl(dbFile.getUrl(), fileUUID);
        } else {
            file.transferTo(uploadFile.toFile());
            url = buildFileUrl(fileUUID);
        }

        Files saveFile = new Files();
        saveFile.setName(originalFilename);
        saveFile.setType(type);
        saveFile.setSize(file.getSize() / 1024);
        saveFile.setUrl(url);
        saveFile.setMd5(md5);
        fileMapper.insert(saveFile);
        return url;
    }

    @GetMapping("/{fileUUID}")
    public void download(@PathVariable String fileUUID, HttpServletResponse response) throws IOException {
        Path uploadFile = resolveUploadPath(fileUUID);
        if (!java.nio.file.Files.exists(uploadFile) || java.nio.file.Files.isDirectory(uploadFile)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(uploadFile.getFileName().toString(), "UTF-8"));
        response.setContentType("application/octet-stream");

        try (ServletOutputStream os = response.getOutputStream()) {
            java.nio.file.Files.copy(uploadFile, os);
            os.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody Files files) {
        return Result.success(fileMapper.updateById(files));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Files files = fileMapper.selectById(id);
        if (files == null) {
            return Result.error(Constants.CODE_404, "文件不存在");
        }
        files.setIsDelete(true);
        fileMapper.updateById(files);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error(Constants.CODE_400, "删除列表不能为空");
        }
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        List<Files> files = fileMapper.selectList(queryWrapper);
        for (Files file : files) {
            if (file != null) {
                file.setIsDelete(true);
                fileMapper.updateById(file);
            }
        }
        return Result.success();
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String name) {
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderByDesc("id");
        if (StrUtil.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        return Result.success(fileMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper));
    }

    private Files getFileByMd5(String md5) {
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5", md5);
        List<Files> filesList = fileMapper.selectList(queryWrapper);
        return filesList.isEmpty() ? null : filesList.get(0);
    }

    private String buildFileUrl(String fileUUID) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/file/")
                .path(fileUUID)
                .toUriString();
    }

    private String normalizeFileUrl(String storedUrl, String fallbackFileUUID) {
        String currentContextPath = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        if (StrUtil.isBlank(storedUrl)) {
            return currentContextPath + "/file/" + fallbackFileUUID;
        }
        String filePath = StrUtil.subAfter(storedUrl, "/file/", true);
        if (StrUtil.isBlank(filePath)) {
            return currentContextPath + "/file/" + fallbackFileUUID;
        }
        return currentContextPath + "/file/" + filePath;
    }

    private void validateFileType(String type) {
        if (StrUtil.isBlank(type)) {
            throw new ServiceException(Constants.CODE_400, "文件缺少后缀名");
        }
        if (StrUtil.isBlank(allowedTypes)) {
            return;
        }
        Set<String> allowList = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        if (!allowList.contains(type)) {
            throw new ServiceException(Constants.CODE_400, "不支持的文件类型");
        }
    }

    private Path resolveUploadPath(String fileName) {
        if (StrUtil.isBlank(fileName)
                || fileName.contains("..")
                || fileName.contains("/")
                || fileName.contains("\\")) {
            throw new ServiceException(Constants.CODE_400, "文件名不合法");
        }

        Path uploadDir = Paths.get(fileUploadPath).toAbsolutePath().normalize();
        Path resolvedPath = uploadDir.resolve(fileName).normalize();
        if (!resolvedPath.startsWith(uploadDir)) {
            throw new ServiceException(Constants.CODE_400, "文件路径不合法");
        }
        return resolvedPath;
    }
}
