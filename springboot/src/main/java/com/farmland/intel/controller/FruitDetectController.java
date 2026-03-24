package com.farmland.intel.controller;

import com.farmland.intel.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/fruit-detect")
public class FruitDetectController {
    private static final Logger log = LoggerFactory.getLogger(FruitDetectController.class);

    @Value("${fruit-detect.api-url:http://localhost:5000/api/detect}")
    private String pythonApiUrl;

    @Value("${fruit-detect.health-url:http://localhost:5000/api/health}")
    private String pythonHealthUrl;

    private final RestTemplate restTemplate;

    public FruitDetectController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/analyze")
    public Result analyze(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("400", "请上传图片文件");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("400", "只支持图片文件");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    pythonApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && "200".equals(responseBody.get("code"))) {
                return Result.success(responseBody.get("data"));
            }

            String msg = responseBody != null ? (String) responseBody.get("msg") : "检测失败";
            return Result.error("500", msg);
        } catch (Exception e) {
            log.error("Fruit detect analyze failed", e);
            return Result.error("500", "检测服务异常，请确认 Python 检测服务已启动: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public Result healthCheck() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(pythonHealthUrl, Map.class);
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
