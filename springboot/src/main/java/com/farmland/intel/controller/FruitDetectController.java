package com.farmland.intel.controller;

import com.farmland.intel.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 果蔬成熟度检测接口
 * 转发请求到Python YOLO检测服务
 */
@RestController
@RequestMapping("/fruit-detect")
public class FruitDetectController {
    private static final Logger log = LoggerFactory.getLogger(FruitDetectController.class);

    // Python检测服务地址
    private static final String PYTHON_API_URL = "http://localhost:5000/api/detect";
    private static final String PYTHON_HEALTH_URL = "http://localhost:5000/api/health";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 上传图片进行果蔬成熟度检测
     * @param file 上传的图片文件
     * @return 检测结果
     */
    @PostMapping("/analyze")
    public Result analyze(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return Result.error("400", "请上传图片文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("400", "只支持图片文件");
            }

            // 构建multipart请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 创建文件资源
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用Python API
            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && "200".equals(responseBody.get("code"))) {
                return Result.success(responseBody.get("data"));
            } else {
                String msg = responseBody != null ? (String) responseBody.get("msg") : "检测失败";
                return Result.error("500", msg);
            }

        } catch (Exception e) {
            log.error("Fruit detect analyze failed", e);
            return Result.error("500", "检测服务异常，请确保Python检测服务已启动: " + e.getMessage());
        }
    }

    /**
     * 检查Python检测服务状态
     */
    @GetMapping("/health")
    public Result healthCheck() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(PYTHON_HEALTH_URL, Map.class);
            Map responseBody = response.getBody();
            if (responseBody != null && "200".equals(responseBody.get("code"))) {
                return Result.success(responseBody.get("data"));
            }
            return Result.error("500", "检测服务状态异常");
        } catch (Exception e) {
            return Result.error("500", "检测服务未启动，请先运行 TomatoDetection/api_server.py");
        }
    }
}
