package com.farmland.intel.controller;

// 1. 注意这里：引入的是你刚才新建的【接口】
import com.farmland.intel.service.IDeepSeekService;
import com.farmland.intel.service.IOneNetService;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.entity.SensorReading;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // 2. 注意这里：注入类型必须是【接口 IDeepSeekService】
    @Autowired
    private IDeepSeekService deepSeekService;

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
        
        String answer = deepSeekService.askAgriExpert(question, indoorTemp, indoorHumidity, outdoorTemp);

        response.put("code", 200);
        response.put("answer", answer);
        return response;
    }
}