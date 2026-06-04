package com.farmland.intel.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.farmland.intel.common.Result;
import com.farmland.intel.entity.AiConfig;
import com.farmland.intel.entity.SensorReading;
import com.farmland.intel.entity.User;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.service.ChatModelFactory;
import com.farmland.intel.service.IAiConfigService;
import com.farmland.intel.service.IOneNetService;
import com.farmland.intel.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    @Autowired
    private SensorReadingMapper sensorReadingMapper;

    @Autowired(required = false)
    private IOneNetService oneNetService;

    @Autowired
    private ChatModelFactory chatModelFactory;

    @Autowired
    private IAiConfigService aiConfigService;

    @Value("${amap.web-key:}")
    private String webKey;

    @Value("${amap.js-key:}")
    private String jsKey;

    @Value("${amap.js-security-key:}")
    private String jsSecurityKey;

    @Value("${amap.city:430800}")
    private String defaultCity;

    @PostMapping("/ask")
    public Result chatWithAI(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        Map<String, Object> response = new HashMap<>();
        response.put("answer", askWithCurrentUserConfig(question, buildAgriSystemPrompt()));
        return Result.success(response);
    }

    @PostMapping({"/ai-proxy", "/qwen-proxy"})
    public Map<String, Object> aiProxy(@RequestBody Map<String, String> params) {
        String prompt = params.get("prompt");
        String systemPrompt = params.getOrDefault(
                "systemPrompt",
                "你是农业数据分析助手，请根据用户要求输出简洁、可执行的结果。"
        );

        Map<String, Object> response = new HashMap<>();
        if (prompt == null || prompt.trim().isEmpty()) {
            response.put("code", 400);
            response.put("message", "prompt 不能为空");
            response.put("data", null);
            return response;
        }

        try {
            String content = askWithCurrentUserConfig(prompt, systemPrompt);
            if (content.startsWith("请先在个人中心配置")) {
                response.put("code", 400);
                response.put("message", content);
                response.put("data", null);
                return response;
            }
            return buildProxySuccessResponse(response, content);
        } catch (Exception e) {
            log.error("AI proxy call failed", e);
            response.put("code", 500);
            response.put("message", "AI 服务暂时不可用，请稍后重试");
            response.put("data", null);
            return response;
        }
    }

    private String askWithCurrentUserConfig(String prompt, String systemPrompt) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return "请先登录后再使用 AI 功能。";
        }

        AiConfig aiConfig = aiConfigService.getByUserId(user.getId());
        if (aiConfig == null || aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            return "请先在个人中心配置自己的 AI API Key。";
        }

        aiConfig.setUserId(user.getId());
        if (aiConfig.getTemperature() == null) {
            aiConfig.setTemperature(new BigDecimal("0.7"));
        }

        ChatClient client = chatModelFactory.getChatClient(aiConfig);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(prompt));

        String content = client.prompt()
                .messages(messages)
                .call()
                .content();

        log.info("AI proxy provider={}, model={}, responseLength={}",
                aiConfig.getProvider(), aiConfig.getModelName(), content != null ? content.length() : 0);
        return content != null ? content.trim() : "AI 未返回有效响应。";
    }

    private String buildAgriSystemPrompt() {
        double indoorTemp = 25.0;
        double indoorHumidity = 60.0;

        boolean gotFromOneNet = false;
        if (oneNetService != null) {
            try {
                Map<String, Object> oneNetData = oneNetService.getDeviceData();
                Object successObj = oneNetData.getOrDefault("success", false);
                if (Boolean.TRUE.equals(successObj)) {
                    Object tempObj = oneNetData.get("temperature");
                    Object humObj = oneNetData.get("humidity");
                    if (tempObj != null && humObj != null) {
                        indoorTemp = Double.parseDouble(tempObj.toString());
                        indoorHumidity = Double.parseDouble(humObj.toString());
                        gotFromOneNet = true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (!gotFromOneNet) {
            try {
                SensorReading latest = sensorReadingMapper.selectLatest();
                if (latest != null) {
                    indoorTemp = latest.getTemperature();
                    indoorHumidity = latest.getHumidity();
                }
            } catch (Exception ignored) {
            }
        }

        double outdoorTemp = 20.0;
        try {
            String apiKey = (webKey != null && !webKey.isEmpty()) ? webKey : jsKey;
            if (apiKey != null && !apiKey.isEmpty()) {
                String res = HttpUtil.get(buildWeatherUrl(apiKey));
                JSONObject json = JSONUtil.parseObj(res);
                if ("1".equals(json.getStr("status"))) {
                    JSONArray lives = json.getJSONArray("lives");
                    if (lives != null && !lives.isEmpty()) {
                        outdoorTemp = Double.parseDouble(lives.getJSONObject(0).getStr("temperature"));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "你是一位资深农业专家。当前环境数据如下：\n"
                + "室内温度：" + indoorTemp + "°C\n"
                + "室内湿度：" + indoorHumidity + "%\n"
                + "室外温度：" + outdoorTemp + "°C\n"
                + "请根据上述环境数据回答用户问题。若数据存在风险，请优先给出预警和具体处置建议。";
    }

    private String buildWeatherUrl(String apiKey) {
        StringBuilder url = new StringBuilder("https://restapi.amap.com/v3/weather/weatherInfo")
                .append("?city=").append(defaultCity)
                .append("&key=").append(apiKey)
                .append("&extensions=base")
                .append("&sdk=server");
        if ((webKey == null || webKey.isEmpty()) && jsSecurityKey != null && !jsSecurityKey.isEmpty()) {
            url.append("&jscode=").append(jsSecurityKey);
        }
        return url.toString();
    }

    private Map<String, Object> buildProxySuccessResponse(Map<String, Object> response, String content) {
        if (content != null) {
            content = stripMarkdownFence(content.trim());
            try {
                JSONObject jsonContent = JSONUtil.parseObj(content);
                response.put("code", 200);
                response.put("message", "success");
                response.put("data", jsonContent);
            } catch (Exception e) {
                response.put("code", 200);
                response.put("message", "success");
                response.put("data", content);
            }
        } else {
            response.put("code", 500);
            response.put("message", "AI 未返回有效响应");
            response.put("data", null);
        }
        return response;
    }

    private String stripMarkdownFence(String content) {
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }
}
