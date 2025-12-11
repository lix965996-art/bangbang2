package com.farmland.intel.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图API代理控制器
 * 解决前端调用高德API的跨域问题
 */
@RestController
@RequestMapping("/amap")
public class AmapProxyController {

    @Value("${amap.web-key:ad1347855b35e84080018cf5c811d3e7}")
    private String webApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 输入提示接口代理 - 根据输入关键字返回地点提示
     * @param keywords 搜索关键词
     * @param city 城市名称
     * @return 输入提示结果
     */
    @GetMapping("/inputtips")
    @ResponseBody
    public Map<String, Object> inputTips(
            @RequestParam String keywords,
            @RequestParam(required = false, defaultValue = "张家界") String city) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查关键词
            if (keywords == null || keywords.trim().isEmpty()) {
                result.put("status", "0");
                result.put("info", "请输入关键词");
                result.put("tips", new ArrayList<>());
                return result;
            }
            
            System.out.println("🔍 输入提示请求 - 关键词: " + keywords + ", 城市: " + city);
            System.out.println("🔑 使用API Key: " + (webApiKey != null ? webApiKey.substring(0, 8) + "..." : "null"));
            
            // 构建请求URL
            String url = "https://restapi.amap.com/v3/assistant/inputtips";
            String requestUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("key", webApiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("city", city)
                    .queryParam("citylimit", "false")
                    .queryParam("datatype", "all")
                    .queryParam("output", "json")
                    .build()
                    .toUriString();
            
            System.out.println("🌐 请求高德API: " + requestUrl);
            
            // 调用高德API
            ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            System.out.println("📥 高德API响应: " + responseBody);
            
            if (responseBody != null && "1".equals(String.valueOf(responseBody.get("status")))) {
                // 成功返回
                result.put("status", "1");
                result.put("info", "OK");
                Object tips = responseBody.get("tips");
                result.put("tips", tips != null ? tips : new ArrayList<>());
                if (tips != null && tips instanceof List) {
                    System.out.println("✅ 输入提示成功，返回 " + ((List)tips).size() + " 条结果");
                }
            } else {
                // 失败返回
                result.put("status", "0");
                result.put("info", responseBody != null ? responseBody.get("info") : "调用失败");
                result.put("tips", new ArrayList<>());
                System.err.println("❌ 高德API返回失败: " + responseBody);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "0");
            result.put("info", "服务器错误: " + e.getMessage());
            result.put("tips", new ArrayList<>());
            System.err.println("❌ 代理请求异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 地理编码接口代理（地址转坐标）
     * @param address 地址
     * @param city 城市
     * @return 地理编码结果
     */
    @GetMapping("/geocode")
    public Map<String, Object> geocode(
            @RequestParam String address,
            @RequestParam(required = false) String city) {
        
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", webApiKey)
                    .queryParam("address", address)
                    .queryParam("output", "json");
            
            if (city != null && !city.isEmpty()) {
                builder.queryParam("city", city);
            }
            
            String url = builder.build().encode().toUriString();
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && "1".equals(responseBody.get("status"))) {
                return responseBody;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "0");
                errorResult.put("info", responseBody != null ? responseBody.get("info") : "调用失败");
                return errorResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "0");
            errorResult.put("info", "代理请求失败：" + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 逆地理编码接口代理（坐标转地址）
     * @param location 经纬度坐标，格式：经度,纬度
     * @param extensions 返回结果控制：base（基础）或all（详细）
     * @return 逆地理编码结果
     */
    @GetMapping("/regeocode")
    public Map<String, Object> regeocode(
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "base") String extensions) {
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                    .queryParam("key", webApiKey)
                    .queryParam("location", location)
                    .queryParam("extensions", extensions)
                    .queryParam("output", "json")
                    .build()
                    .encode()
                    .toUriString();
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && "1".equals(responseBody.get("status"))) {
                return responseBody;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "0");
                errorResult.put("info", responseBody != null ? responseBody.get("info") : "调用失败");
                return errorResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "0");
            errorResult.put("info", "代理请求失败：" + e.getMessage());
            return errorResult;
        }
    }
}
