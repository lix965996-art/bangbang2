package com.farmland.intel.controller;

// 1. 注意这里：引入的是通义千问【接口】
import com.farmland.intel.service.IQwenService;
import com.farmland.intel.service.IOneNetService;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.entity.SensorReading;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    // 2. 注意这里：注入类型必须是【接口 IQwenService】
    @Autowired
    private IQwenService qwenService;

    @Autowired
    private SensorReadingMapper sensorReadingMapper;

    @Autowired(required = false)
    private IOneNetService oneNetService;

    @Value("${amap.web-key:}")
    private String webKey;

    @Value("${amap.js-key:}")
    private String jsKey;

    @Value("${amap.city:430800}")
    private String defaultCity;

    @Value("${qwen.api-key:sk-ac55e3c1bff04024ab56bd6066ba5662}")
    private String qwenApiKey;

    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String qwenApiUrl;

    @Value("${qwen.model:qwen-max}")
    private String qwenModel;

    @PostMapping("/ask")
    public Map<String, Object> chatWithAI(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        Map<String, Object> response = new HashMap<>();

        // 1. 获取室内温湿度 (STM32)
        double indoorTemp = 25.0;
        double indoorHumidity = 60.0;
        
        // 优先尝试从OneNET获取
        boolean gotFromOneNet = false;
        if (oneNetService != null) {
            try {
                Map<String, Object> oneNetData = oneNetService.getDeviceData();
                if ((Boolean) oneNetData.getOrDefault("success", false)) {
                    indoorTemp = Double.parseDouble(oneNetData.get("temperature").toString());
                    indoorHumidity = Double.parseDouble(oneNetData.get("humidity").toString());
                    gotFromOneNet = true;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        
        // 如果OneNET失败，从数据库获取最新
        if (!gotFromOneNet) {
            try {
                SensorReading latest = sensorReadingMapper.selectLatest();
                if (latest != null) {
                    indoorTemp = latest.getTemperature();
                    indoorHumidity = latest.getHumidity();
                }
            } catch (Exception e) {
                // ignore
            }
        }

        // 2. 获取室外温度 (高德API)
        double outdoorTemp = 20.0; // 默认值
        try {
            String apiKey = (webKey != null && !webKey.isEmpty()) ? webKey : jsKey;
            if (apiKey != null && !apiKey.isEmpty()) {
                String url = "https://restapi.amap.com/v3/weather/weatherInfo?city=" + defaultCity + "&key=" + apiKey + "&extensions=base";
                String res = HttpUtil.get(url);
                JSONObject json = JSONUtil.parseObj(res);
                if ("1".equals(json.getStr("status"))) {
                    JSONArray lives = json.getJSONArray("lives");
                    if (lives != null && !lives.isEmpty()) {
                        outdoorTemp = Double.parseDouble(lives.getJSONObject(0).getStr("temperature"));
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // 3. 调用接口定义的方法
        System.out.println("🤖 AI Chat 正在获取环境数据...");
        System.out.println("   [室内] 温度: " + indoorTemp + "°C, 湿度: " + indoorHumidity + "%");
        System.out.println("   [室外] 温度: " + outdoorTemp + "°C");
        
        String answer = qwenService.askAgriExpert(question, indoorTemp, indoorHumidity, outdoorTemp);

        response.put("code", 200);
        response.put("answer", answer);
        return response;
    }

    /**
     * 通义千问API代理接口（解决前端CORS问题）
     * 用于前端需要直接调用通义千问进行分析的场景
     * @param params 包含 prompt 和可选的 systemPrompt
     * @return 通义千问的响应结果
     */
    @PostMapping("/qwen-proxy")
    public Map<String, Object> qwenProxy(@RequestBody Map<String, String> params) {
        String prompt = params.get("prompt");
        String systemPrompt = params.getOrDefault("systemPrompt", "你是一个农业专家，善于分析农田数据并给出产量和市场预测。请以JSON格式返回结果。");
        Map<String, Object> response = new HashMap<>();

        if (prompt == null || prompt.trim().isEmpty()) {
            response.put("code", 400);
            response.put("message", "prompt不能为空");
            return response;
        }

        try {
            // 构建通义千问请求格式
            JSONObject payload = new JSONObject();
            payload.put("model", qwenModel);
            
            // 构建input
            JSONObject input = new JSONObject();
            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.add(new JSONObject().put("role", "user").put("content", prompt));
            input.put("messages", messages);
            payload.put("input", input);
            
            // 构建parameters
            JSONObject parameters = new JSONObject();
            parameters.put("temperature", 0.7);
            parameters.put("result_format", "message");
            payload.put("parameters", parameters);

            // 发送请求
            String result = HttpRequest.post(qwenApiUrl)
                    .header("Authorization", "Bearer " + qwenApiKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(60000)
                    .execute()
                    .body();

            log.info("通义千问代理响应: {}", result);

            // 解析返回的 JSON 结果
            JSONObject jsonResult = JSONUtil.parseObj(result);

            // 检查是否有错误信息
            if (jsonResult.containsKey("code") && !"200".equals(jsonResult.getStr("code"))) {
                String errorMsg = jsonResult.getStr("message", "未知错误");
                response.put("code", 500);
                response.put("message", "通义千问调用失败: " + errorMsg);
                response.put("data", null);
                return response;
            }

            // 提取 AI 回复的核心文本
            String content = null;
            if (jsonResult.containsKey("output")) {
                JSONObject output = jsonResult.getJSONObject("output");
                if (output.containsKey("choices")) {
                    JSONArray choices = output.getJSONArray("choices");
                    if (!choices.isEmpty()) {
                        content = choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getStr("content");
                    }
                }
                // 兼容旧格式
                if (content == null && output.containsKey("text")) {
                    content = output.getStr("text");
                }
            }

            if (content != null) {
                // 清理markdown代码块标记
                content = content.trim();
                if (content.startsWith("```json")) {
                    content = content.substring(7);
                } else if (content.startsWith("```")) {
                    content = content.substring(3);
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
                content = content.trim();

                // 尝试解析为JSON
                try {
                    JSONObject jsonContent = JSONUtil.parseObj(content);
                    response.put("code", 200);
                    response.put("message", "success");
                    response.put("data", jsonContent);
                } catch (Exception e) {
                    // 如果不是JSON，直接返回文本
                    response.put("code", 200);
                    response.put("message", "success");
                    response.put("data", content);
                }
            } else {
                response.put("code", 500);
                response.put("message", "AI 未返回有效响应");
                response.put("data", null);
            }

        } catch (Exception e) {
            log.error("通义千问代理调用异常", e);
            response.put("code", 500);
            response.put("message", "AI 服务调用异常: " + e.getMessage());
            response.put("data", null);
        }

        return response;
    }
}