package com.farmland.intel.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.farmland.intel.service.IQwenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class QwenServiceImpl implements IQwenService {

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;

    @Value("${qwen.model:qwen-max}")
    private String model;

    @Override
    public String askAgriExpert(String userQuestion, double indoorTemp, double indoorHumidity, double outdoorTemp) {
        if (!StringUtils.hasText(apiKey)) {
            return "AI 服务未配置，请先设置 qwen.api-key。";
        }

        try {
            String systemPrompt = "你是一位拥有10年经验的资深农业专家。当前大棚的实时环境数据如下：\n"
                    + "【室内温度】：" + indoorTemp + "°C\n"
                    + "【室内湿度】：" + indoorHumidity + "%\n"
                    + "【室外温度】：" + outdoorTemp + "°C\n"
                    + "请根据上述环境数据回答用户的问题。\n"
                    + "注意：如果温湿度处于危险范围（如湿度>80%或温度>35°C），请在回答开头优先给出预警和具体补救措施。";

            JSONObject payload = new JSONObject();
            payload.put("model", model);

            JSONObject input = new JSONObject();
            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.add(new JSONObject().put("role", "user").put("content", userQuestion));
            input.put("messages", messages);
            payload.put("input", input);

            JSONObject parameters = new JSONObject();
            parameters.put("temperature", 0.7);
            parameters.put("result_format", "message");
            payload.put("parameters", parameters);

            String result = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(60000)
                    .execute()
                    .body();

            log.info("通义千问响应: {}", result);

            JSONObject jsonResult = JSONUtil.parseObj(result);
            if (jsonResult.containsKey("code") && !"200".equals(jsonResult.getStr("code"))) {
                return "通义千问调用失败: " + jsonResult.getStr("message", "未知错误");
            }

            if (jsonResult.containsKey("output")) {
                JSONObject output = jsonResult.getJSONObject("output");
                if (output.containsKey("choices")) {
                    JSONArray choices = output.getJSONArray("choices");
                    if (!choices.isEmpty()) {
                        return choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getStr("content");
                    }
                }
                if (output.containsKey("text")) {
                    return output.getStr("text");
                }
            }

            return "AI 未返回有效响应，请稍后再试。";
        } catch (Exception e) {
            log.error("通义千问调用异常", e);
            return "AI 服务调用失败，请稍后再试。";
        }
    }
}
