package com.farmland.intel.service.impl;

import com.farmland.intel.entity.SensorReading;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.service.IOneNetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OneNET物联网平台服务实现
 * 提供STM32设备数据获取和控制功能
 */
@Service
@ConditionalOnProperty(prefix = "onenet", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OneNetServiceImpl implements IOneNetService {
    
    private static final Logger log = LoggerFactory.getLogger(OneNetServiceImpl.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
        new ParameterizedTypeReference<Map<String, Object>>() {};
    
    @Autowired
    private SensorReadingMapper sensorReadingMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${onenet.api-url:https://iot-api.heclouds.com}")
    private String apiUrl;
    
    @Value("${onenet.author-key:}")
    private String authorKey;

    @Value("${onenet.user-id:}")
    private String userId;

    @Value("${onenet.version:2022-05-01}")
    private String version;

    @Value("${onenet.product-id:}")
    private String productId;

    @Value("${onenet.device-name:}")
    private String deviceName;

    // 新设备配置（水泵控制设备）
    @Value("${onenet.new-product-id:}")
    private String newProductId;

    @Value("${onenet.new-device-name:}")
    private String newDeviceName;
    
    /**
     * 生成OneNET认证Token（根据key.js实现）
     * 
     * 参考代码：
     * const access_key = Buffer.from(params.author_key, "base64")
     * const key = et + "\n" + method + "\n" + res + "\n" + version
     * let sign = crypto.createHmac('sha1', access_key).update(key).digest().toString('base64')
     */
    private String generateToken() {
        try {
            // 1. 计算过期时间（1年后，完全按照key.js）
            long et = (System.currentTimeMillis() + 365L * 24 * 3600 * 1000) / 1000;
            String res = "userid/" + userId;
            String method = "sha1";  // 关键：使用SHA1，不是SHA256！
            
            // 2. 构建待签名字符串（完全按照key.js的格式：et + "\n" + method + "\n" + res + "\n" + version）
            String key = et + "\n" + method + "\n" + res + "\n" + version;
            
            log.debug("待签名字符串: {}", key);
            
            // 3. 关键：将author_key从Base64解码后作为密钥（与key.js一致）
            // JavaScript: Buffer.from(params.author_key, "base64")
            byte[] accessKey = Base64.getDecoder().decode(authorKey);
            log.debug("密钥解码成功，长度: {} bytes", accessKey.length);
            
            // 4. 使用HmacSHA1签名（与key.js的crypto.createHmac('sha1', access_key)一致）
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(accessKey, "HmacSHA1");
            mac.init(secretKey);
            
            byte[] hash = mac.doFinal(key.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(hash);
            
            log.debug("签名生成成功");
            
            // 5. URL编码（与key.js的encodeURIComponent一致）
            String encodedRes = java.net.URLEncoder.encode(res, "UTF-8");
            String encodedSign = java.net.URLEncoder.encode(sign, "UTF-8");
            
            // 6. 拼接Token（完全按照key.js的格式）
            String token = "version=" + version 
                         + "&res=" + encodedRes 
                         + "&et=" + et 
                         + "&method=" + method 
                         + "&sign=" + encodedSign;
            
            log.debug("Token生成成功");
            
            return token;
        } catch (Exception e) {
            log.error("生成Token失败", e);
            return "";
        }
    }
    
    @Override
    public Map<String, Object> getDeviceData() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        try {
            String url = apiUrl + "/thingmodel/query-device-property?product_id=" + productId + "&device_name=" + deviceName;
            String token = generateToken();
            
            log.debug("正在调用OneNET API: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = exchangeForMap(url, HttpMethod.GET, entity);
            
            log.debug("OneNET响应状态: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                
                // 检查是否有错误码
                Object code = body.get("code");
                if (code != null && !code.toString().equals("0")) {
                    log.error("OneNET返回错误码: {}, 错误信息: {}", code, body.get("msg"));
                    result.put("error", "OneNET返回错误: " + body.get("msg"));
                    return result;
                }
                
                Object dataObj = body.get("data");
                
                if (dataObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> dataList = (java.util.List<Map<String, Object>>) dataObj;
                    log.debug("数据列表长度: {}", dataList.size());
                    
                    if (dataList.size() >= 3) {
                        // data[0] = 湿度, data[1] = LED, data[2] = 温度
                        Map<String, Object> humi = dataList.get(0);
                        Map<String, Object> led = dataList.get(1);
                        Map<String, Object> temp = dataList.get(2);
                        
                        Double temperature = Double.parseDouble(temp.get("value").toString());
                        Double humidity = Double.parseDouble(humi.get("value").toString());
                        Integer ledState = "true".equals(led.get("value").toString()) ? 1 : 0;
                        
                        log.debug("STM32数据解析成功 - 温度: {}℃, 湿度: {}%", temperature, humidity);
                        
                        result.put("success", true);
                        result.put("temperature", temperature);
                        result.put("humidity", humidity);
                        result.put("led", ledState);
                        result.put("online", true);
                    } else {
                        log.warn("数据列表长度不足: {}", dataList.size());
                        result.put("error", "数据格式错误");
                    }
                } else {
                    log.warn("data字段不是List类型: {}", (dataObj != null ? dataObj.getClass().getName() : "null"));
                    result.put("error", "数据格式错误");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP客户端错误: {} - {}, 响应内容: {}", e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
            result.put("success", false);
            result.put("online", false);
            result.put("error", "HTTP错误 " + e.getStatusCode() + ": " + e.getMessage());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("网络连接失败: {}", e.getMessage());
            result.put("success", false);
            result.put("online", false);
            result.put("error", "网络连接失败，无法访问OneNET平台");
        } catch (Exception e) {
            log.error("从OneNET获取数据失败", e);
            result.put("success", false);
            result.put("online", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public boolean controlLed(boolean led) {
        try {
            String url = apiUrl + "/thingmodel/set-device-property";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", generateToken());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("product_id", productId);
            requestBody.put("device_name", deviceName);
            
            Map<String, Object> params = new HashMap<>();
            params.put("led", led);
            requestBody.put("params", params);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = exchangeForMap(url, HttpMethod.POST, entity);
            
            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                log.debug("OneNET LED控制成功: {}", led ? "开启" : "关闭");
            } else {
                log.warn("OneNET LED控制失败，状态码: {}", response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            log.error("控制LED失败", e);
            return false;
        }
    }
    
    @Override
    public boolean syncDataToDatabase() {
        try {
            Map<String, Object> data = getDeviceData();
            
            if ((Boolean) data.getOrDefault("success", false)) {
                SensorReading reading = new SensorReading();
                reading.setTemperature((Double) data.get("temperature"));
                reading.setHumidity((Double) data.get("humidity"));
                reading.setLed((Integer) data.get("led"));
                reading.setDeviceName("STM32-001");
                reading.setCreatedAt(LocalDateTime.now());
                
                sensorReadingMapper.insert(reading);
                
                log.debug("[定时任务] STM32数据已同步到数据库: T={}℃, H={}%", reading.getTemperature(), reading.getHumidity());
                return true;
            } else {
                log.warn("[定时任务] 获取设备数据失败，无法同步");
            }
        } catch (Exception e) {
            log.error("[定时任务] 同步数据到数据库异常", e);
        }
        
        return false;
    }

@Override
public boolean controlBump(boolean bump) {
    try {
        String url = apiUrl + "/thingmodel/set-device-property";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", generateToken());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("product_id", newProductId);  // 使用新设备的product_id
        requestBody.put("device_name", newDeviceName);
        
        Map<String, Object> params = new HashMap<>();
        params.put("bump", bump);  // 水泵控制参数
        requestBody.put("params", params);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        log.debug("正在控制水泵: {} -> {}", newProductId, bump ? "开启" : "关闭");
        
        ResponseEntity<Map<String, Object>> response = exchangeForMap(url, HttpMethod.POST, entity);
        
        boolean success = response.getStatusCode() == HttpStatus.OK;
        if (success) {
            log.debug("OneNET 水泵控制成功: {}", bump ? "开启" : "关闭");
        } else {
            log.warn("OneNET 水泵控制失败，状态码: {}", response.getStatusCode());
        }
        return success;
    } catch (Exception e) {
        log.error("控制水泵失败", e);
        return false;
    }
}

@Override
public Map<String, Object> getNewDeviceData() {
    Map<String, Object> result = new HashMap<>();
    result.put("success", false);
    
    try {
        String url = apiUrl + "/thingmodel/query-device-property?product_id=" + newProductId + "&device_name=" + newDeviceName;
        String token = generateToken();
        
        log.debug("正在获取新设备数据: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map<String, Object>> response = exchangeForMap(url, HttpMethod.GET, entity);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            Object dataObj = body.get("data");
            
            if (dataObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> dataList = (java.util.List<Map<String, Object>>) dataObj;
                
                // 根据你提供的代码：data[0]=bump, data[1]=humi, data[2]=led, data[3]=temp
                if (dataList.size() >= 4) {
                    Double temperature = Double.parseDouble(dataList.get(3).get("value").toString());
                    Double humidity = Double.parseDouble(dataList.get(1).get("value").toString());
                    boolean bumpState = "true".equals(dataList.get(0).get("value").toString());
                    boolean ledState = "true".equals(dataList.get(2).get("value").toString());
                    
                    log.debug("新设备数据解析成功 - 温度: {}℃, 湿度: {}%, 水泵: {}", temperature, humidity, bumpState ? "开" : "关");
                    
                    result.put("success", true);
                    result.put("temperature", temperature);
                    result.put("humidity", humidity);
                    result.put("bump", bumpState);
                    result.put("led", ledState);
                    result.put("online", true);
                }
            }
        }
    } catch (Exception e) {
        log.error("获取新设备数据失败", e);
        result.put("error", e.getMessage());
    }
    
    return result;
    }
    
    @Override
    public boolean syncNewDeviceDataToDatabase() {
        try {
            Map<String, Object> data = getNewDeviceData();
            
            if ((Boolean) data.getOrDefault("success", false)) {
                SensorReading reading = new SensorReading();
                reading.setTemperature((Double) data.get("temperature"));
                reading.setHumidity((Double) data.get("humidity"));
                // 使用bump状态作为led字段（复用现有字段）
                reading.setLed((Boolean) data.get("bump") ? 1 : 0);
                reading.setDeviceName("STM32-PUMP");  // 新设备名称
                reading.setCreatedAt(LocalDateTime.now());
                
                sensorReadingMapper.insert(reading);
                
                log.debug("[定时任务] 新设备数据已同步到数据库: T={}℃, H={}%, 水泵={}", 
                    reading.getTemperature(), reading.getHumidity(), 
                    (Boolean) data.get("bump") ? "开" : "关");
                return true;
            } else {
                log.warn("[定时任务] 获取新设备数据失败，无法同步");
            }
        } catch (Exception e) {
            log.error("[定时任务] 同步新设备数据到数据库异常", e);
        }
        
        return false;
    }

    private ResponseEntity<Map<String, Object>> exchangeForMap(String url, HttpMethod method, HttpEntity<?> entity) {
        return restTemplate.exchange(url, method, entity, MAP_RESPONSE_TYPE);
    }
}



