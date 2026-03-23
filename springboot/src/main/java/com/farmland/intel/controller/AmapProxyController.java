package com.farmland.intel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/amap")
public class AmapProxyController {

    private static final Logger log = LoggerFactory.getLogger(AmapProxyController.class);

    @Value("${amap.web-key:}")
    private String webApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/inputtips")
    public Map<String, Object> inputTips(
            @RequestParam String keywords,
            @RequestParam(required = false, defaultValue = "张家界") String city) {

        Map<String, Object> result = new HashMap<>();
        if (keywords == null || keywords.trim().isEmpty()) {
            result.put("status", "0");
            result.put("info", "请输入关键词");
            result.put("tips", new ArrayList<>());
            return result;
        }
        if (webApiKey == null || webApiKey.trim().isEmpty()) {
            result.put("status", "0");
            result.put("info", "高德 Web API Key 未配置");
            result.put("tips", new ArrayList<>());
            return result;
        }

        try {
            String requestUrl = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/assistant/inputtips")
                    .queryParam("key", webApiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("city", city)
                    .queryParam("citylimit", "false")
                    .queryParam("datatype", "all")
                    .queryParam("output", "json")
                    .build()
                    .encode()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && "1".equals(String.valueOf(responseBody.get("status")))) {
                result.put("status", "1");
                result.put("info", "OK");
                result.put("tips", responseBody.getOrDefault("tips", new ArrayList<>()));
                return result;
            }

            log.warn("Amap input tips failed: keywords={}, city={}", keywords, city);
            result.put("status", "0");
            result.put("info", responseBody != null ? responseBody.get("info") : "调用失败");
            result.put("tips", new ArrayList<>());
            return result;
        } catch (Exception e) {
            log.error("Amap input tips proxy error", e);
            result.put("status", "0");
            result.put("info", "服务异常: " + e.getMessage());
            result.put("tips", new ArrayList<>());
            return result;
        }
    }

    @GetMapping("/geocode")
    public Map<String, Object> geocode(
            @RequestParam String address,
            @RequestParam(required = false) String city) {

        if (webApiKey == null || webApiKey.trim().isEmpty()) {
            return error("高德 Web API Key 未配置");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", webApiKey)
                    .queryParam("address", address)
                    .queryParam("output", "json");

            if (city != null && !city.isEmpty()) {
                builder.queryParam("city", city);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    builder.build().encode().toUriString(),
                    Map.class
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && "1".equals(String.valueOf(responseBody.get("status")))) {
                return responseBody;
            }
            return error(responseBody != null ? String.valueOf(responseBody.get("info")) : "调用失败");
        } catch (Exception e) {
            log.error("Amap geocode proxy error", e);
            return error("代理请求失败: " + e.getMessage());
        }
    }

    @GetMapping("/regeocode")
    public Map<String, Object> regeocode(
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "base") String extensions) {

        if (webApiKey == null || webApiKey.trim().isEmpty()) {
            return error("高德 Web API Key 未配置");
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                    .queryParam("key", webApiKey)
                    .queryParam("location", location)
                    .queryParam("extensions", extensions)
                    .queryParam("output", "json")
                    .build()
                    .encode()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && "1".equals(String.valueOf(responseBody.get("status")))) {
                return responseBody;
            }
            return error(responseBody != null ? String.valueOf(responseBody.get("info")) : "调用失败");
        } catch (Exception e) {
            log.error("Amap regeocode proxy error", e);
            return error("代理请求失败: " + e.getMessage());
        }
    }

    private Map<String, Object> error(String info) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "0");
        errorResult.put("info", info);
        if (!errorResult.containsKey("tips")) {
            errorResult.put("tips", new ArrayList<>());
        }
        return errorResult;
    }
}
