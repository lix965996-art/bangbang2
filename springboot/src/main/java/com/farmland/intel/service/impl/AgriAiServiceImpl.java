package com.farmland.intel.service.impl;

import com.farmland.intel.service.IAgriAiService;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.farmland.intel.entity.AiConfig;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.IAiConfigService;
import com.farmland.intel.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 兼容旧接口的农业问答服务。实际模型使用当前登录用户在个人中心保存的 AI 配置。
 */
@Service
@Slf4j
public class AgriAiServiceImpl implements IAgriAiService {

    @Autowired(required = false)
    private IAiConfigService aiConfigService;

    @Override
    public String askAgriExpert(String userQuestion, double indoorTemp, double indoorHumidity, double outdoorTemp) {
        try {
            AiConfig cfg = resolveCurrentUserConfig();
            if (cfg == null || !StringUtils.hasText(cfg.getApiKey())) {
                return "请先在个人中心配置自己的 AI API Key。";
            }
            if (!StringUtils.hasText(cfg.getBaseUrl()) || !StringUtils.hasText(cfg.getModelName())) {
                return "AI 配置不完整，请检查 Base URL 和模型名称。";
            }

            // 1. 构建"人设"和"环境背景" (System Prompt)
            String systemPrompt = "你是一位拥有20年经验的资深农业专家。当前大棚的实时环境数据如下：\n" +
                    "【室内温度】：" + indoorTemp + "°C\n" +
                    "【室内湿度】：" + indoorHumidity + "%\n" +
                    "【室外温度】：" + outdoorTemp + "°C\n" +
                    "请根据上述环境数据回答用户的问题。\n" +
                    "注意：如果温湿度处于危险范围（如湿度>80%或温度>35°C），请在回答开头优先给出红色预警和具体补救措施（如开窗、补水等）。回答风格要专业、简洁。";
            
            // 2. 构建 OpenAI 兼容请求格式
            JSONObject payload = new JSONObject();
            payload.put("model", cfg.getModelName());

            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.add(new JSONObject().put("role", "user").put("content", userQuestion));
            payload.put("messages", messages);
            payload.put("temperature", 0.7);

            // 3. 发送 HTTP POST 请求
            String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            cn.hutool.http.HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(payload.toString())
                    .timeout(60000)
                    .execute();
            String result;
            try {
                result = response.body();
            } finally {
                response.close();
            }

            log.info("AI 农业问答响应: provider={}, model={}, length={}",
                    cfg.getProvider(), cfg.getModelName(), result != null ? result.length() : 0);

            // 4. 解析返回的 JSON 结果
            JSONObject jsonResult = JSONUtil.parseObj(result);

            // 检查是否有错误信息
            if (jsonResult.containsKey("code") && !"200".equals(jsonResult.getStr("code"))) {
                String errorMsg = jsonResult.getStr("message", "未知错误");
                return "AI 调用失败: " + errorMsg;
            }

            JSONArray choices = jsonResult.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content", "AI 未返回有效响应，请稍后再试。");
            }
            
            return "AI 未返回有效响应，请稍后再试。";

        } catch (Exception e) {
            log.error("AI 农业问答调用异常", e);
            return "AI 服务暂时不可用，请稍后再试。";
        }
    }

    private AiConfig resolveCurrentUserConfig() {
        User user = TokenUtils.getCurrentUser();
        if (user == null || aiConfigService == null) {
            return null;
        }
        return aiConfigService.getByUserId(user.getId());
    }
}
