package com.farmland.intel.service.impl;

import com.farmland.intel.entity.SensorReading;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.service.IOneNetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class OneNetServiceImpl implements IOneNetService {

    private static final Logger log = LoggerFactory.getLogger(OneNetServiceImpl.class);

    @Autowired
    private SensorReadingMapper sensorReadingMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${onenet.api-url:https://iot-api.heclouds.com}")
    private String apiUrl;

    @Value("${onenet.author-key:}")
    private String authorKey;

    @Value("${onenet.user-id:470386}")
    private String userId;

    @Value("${onenet.version:2022-05-01}")
    private String version;

    @Value("${onenet.product-id:CzL61ga8FI}")
    private String productId;

    @Value("${onenet.device-name:d1}")
    private String deviceName;

    @Value("${onenet.new-product-id:KK57iNOm8d}")
    private String newProductId;

    @Value("${onenet.new-device-name:d1}")
    private String newDeviceName;

    private String generateToken() {
        try {
            if (!hasText(authorKey)) {
                log.debug("OneNET author key is empty");
                return "";
            }

            long expireAt = (System.currentTimeMillis() + 365L * 24 * 3600 * 1000) / 1000;
            String resource = "userid/" + userId;
            String method = "sha1";
            String key = expireAt + "\n" + method + "\n" + resource + "\n" + version;

            byte[] accessKey = Base64.getDecoder().decode(authorKey);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(accessKey, "HmacSHA1"));

            String sign = Base64.getEncoder().encodeToString(mac.doFinal(key.getBytes(StandardCharsets.UTF_8)));
            return "version=" + version
                    + "&res=" + URLEncoder.encode(resource, "UTF-8")
                    + "&et=" + expireAt
                    + "&method=" + method
                    + "&sign=" + URLEncoder.encode(sign, "UTF-8");
        } catch (Exception e) {
            log.error("Failed to generate OneNET token", e);
            return "";
        }
    }

    @Override
    public Map<String, Object> getDeviceData() {
        Map<String, Object> result = createFailureResult();

        try {
            ResponseEntity<Map<String, Object>> response = queryDeviceProperties(productId, deviceName, result);
            if (response == null) {
                return result;
            }

            java.util.List<Map<String, Object>> dataList = extractDataList(response.getBody(), 3, result);
            if (dataList == null) {
                return result;
            }

            Double humidity = toDoubleValue(dataList.get(0), "humidity", result);
            Integer ledState = toSwitchValue(dataList.get(1), "led", result);
            Double temperature = toDoubleValue(dataList.get(2), "temperature", result);
            if (humidity == null || ledState == null || temperature == null) {
                return result;
            }

            result.put("success", true);
            result.put("online", true);
            result.put("temperature", temperature);
            result.put("humidity", humidity);
            result.put("led", ledState);
            return result;
        } catch (HttpClientErrorException e) {
            log.error("OneNET old device request failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            result.put("error", "OneNET request failed: " + e.getStatusCode());
            return result;
        } catch (ResourceAccessException e) {
            log.error("OneNET old device connection failed", e);
            result.put("error", "OneNET connection failed");
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch old device data from OneNET", e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public boolean controlLed(boolean led) {
        return controlProperty(productId, deviceName, "led", led);
    }

    @Override
    public boolean syncDataToDatabase() {
        try {
            Map<String, Object> data = getDeviceData();
            if (!Boolean.TRUE.equals(data.get("success"))) {
                log.debug("Skip old OneNET sync because device data is unavailable: {}", data.get("error"));
                return false;
            }

            SensorReading reading = new SensorReading();
            reading.setTemperature((Double) data.get("temperature"));
            reading.setHumidity((Double) data.get("humidity"));
            reading.setLed((Integer) data.get("led"));
            reading.setDeviceName("STM32-001");
            reading.setCreatedAt(LocalDateTime.now());
            sensorReadingMapper.insert(reading);
            return true;
        } catch (Exception e) {
            log.error("Failed to sync old OneNET device data", e);
            return false;
        }
    }

    @Override
    public boolean controlBump(boolean bump) {
        return controlProperty(newProductId, newDeviceName, "bump", bump);
    }

    @Override
    public Map<String, Object> getNewDeviceData() {
        Map<String, Object> result = createFailureResult();

        try {
            ResponseEntity<Map<String, Object>> response = queryDeviceProperties(newProductId, newDeviceName, result);
            if (response == null) {
                return result;
            }

            java.util.List<Map<String, Object>> dataList = extractDataList(response.getBody(), 4, result);
            if (dataList == null) {
                return result;
            }

            Integer bumpState = toSwitchValue(dataList.get(0), "bump", result);
            Double humidity = toDoubleValue(dataList.get(1), "humidity", result);
            Integer ledState = toSwitchValue(dataList.get(2), "led", result);
            Double temperature = toDoubleValue(dataList.get(3), "temperature", result);
            if (bumpState == null || humidity == null || ledState == null || temperature == null) {
                return result;
            }

            result.put("success", true);
            result.put("online", true);
            result.put("temperature", temperature);
            result.put("humidity", humidity);
            result.put("bump", bumpState == 1);
            result.put("led", ledState == 1);
            return result;
        } catch (HttpClientErrorException e) {
            log.error("OneNET new device request failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            result.put("error", "OneNET request failed: " + e.getStatusCode());
            return result;
        } catch (ResourceAccessException e) {
            log.error("OneNET new device connection failed", e);
            result.put("error", "OneNET connection failed");
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch new device data from OneNET", e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public boolean syncNewDeviceDataToDatabase() {
        try {
            Map<String, Object> data = getNewDeviceData();
            if (!Boolean.TRUE.equals(data.get("success"))) {
                log.debug("Skip new OneNET sync because device data is unavailable: {}", data.get("error"));
                return false;
            }

            SensorReading reading = new SensorReading();
            reading.setTemperature((Double) data.get("temperature"));
            reading.setHumidity((Double) data.get("humidity"));
            reading.setLed(Boolean.TRUE.equals(data.get("bump")) ? 1 : 0);
            reading.setDeviceName("STM32-PUMP");
            reading.setCreatedAt(LocalDateTime.now());
            sensorReadingMapper.insert(reading);
            return true;
        } catch (Exception e) {
            log.error("Failed to sync new OneNET device data", e);
            return false;
        }
    }

    private boolean controlProperty(String currentProductId, String currentDeviceName, String propertyName, boolean value) {
        try {
            if (!hasText(currentProductId) || !hasText(currentDeviceName)) {
                log.warn("Skip OneNET control because device config is missing: productId={}, deviceName={}", currentProductId, currentDeviceName);
                return false;
            }

            String token = generateToken();
            if (!hasText(token)) {
                log.warn("Skip OneNET control because token is empty");
                return false;
            }

            String url = apiUrl + "/thingmodel/set-device-property";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", token);

            Map<String, Object> params = new HashMap<>();
            params.put(propertyName, value);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("product_id", currentProductId);
            requestBody.put("device_name", currentDeviceName);
            requestBody.put("params", params);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("OneNET control failed with status {}", response.getStatusCode());
                return false;
            }

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return true;
            }

            Object code = body.get("code");
            return code == null || "0".equals(String.valueOf(code));
        } catch (Exception e) {
            log.error("Failed to control OneNET property {}", propertyName, e);
            return false;
        }
    }

    private ResponseEntity<Map<String, Object>> queryDeviceProperties(String currentProductId,
                                                                      String currentDeviceName,
                                                                      Map<String, Object> result) {
        if (!hasText(currentProductId) || !hasText(currentDeviceName)) {
            result.put("error", "OneNET device config is missing");
            return null;
        }

        String token = generateToken();
        if (!hasText(token)) {
            result.put("error", "OneNET token is empty");
            return null;
        }

        String url = apiUrl + "/thingmodel/query-device-property?product_id=" + currentProductId
                + "&device_name=" + currentDeviceName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            result.put("error", "OneNET response is empty");
            return null;
        }

        Object code = response.getBody().get("code");
        if (code != null && !"0".equals(String.valueOf(code))) {
            result.put("error", "OneNET response error: " + response.getBody().get("msg"));
            return null;
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Map<String, Object>> extractDataList(Map<String, Object> body,
                                                                int minSize,
                                                                Map<String, Object> result) {
        Object dataObj = body.get("data");
        if (!(dataObj instanceof java.util.List)) {
            result.put("error", "OneNET data format is invalid");
            return null;
        }

        java.util.List<Map<String, Object>> dataList = (java.util.List<Map<String, Object>>) dataObj;
        if (dataList.size() < minSize) {
            result.put("error", "OneNET data list is too short");
            return null;
        }

        return dataList;
    }

    private Double toDoubleValue(Map<String, Object> item, String fieldName, Map<String, Object> result) {
        Object value = readValue(item, fieldName, result);
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            result.put("error", fieldName + " value is invalid: " + value);
            return null;
        }
    }

    private Integer toSwitchValue(Map<String, Object> item, String fieldName, Map<String, Object> result) {
        Object value = readValue(item, fieldName, result);
        if (value == null) {
            return null;
        }
        return isTrueValue(value) ? 1 : 0;
    }

    private Object readValue(Map<String, Object> item, String fieldName, Map<String, Object> result) {
        if (item == null) {
            result.put("error", fieldName + " item is missing");
            return null;
        }

        Object value = item.get("value");
        if (value == null) {
            result.put("error", fieldName + " value is missing");
            return null;
        }

        return value;
    }

    private boolean isTrueValue(Object value) {
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "on".equalsIgnoreCase(text);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Map<String, Object> createFailureResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("online", false);
        return result;
    }
}
