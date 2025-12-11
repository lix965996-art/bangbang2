package com.farmland.intel.service.impl;
import com.farmland.intel.service.IDeepSeekService;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
@Service
public class DeepSeekServiceImpl implements IDeepSeekService {
    private static final String API_KEY = "sk-1ac381a76063448ca44f0aa6d02b1123";
    // DeepSeek 官方接口地址
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    @Override
    public String askAgriExpert(String userQuestion, double indoorTemp, double indoorHumidity, double outdoorTemp) {
        try {
            // 1. 构建“人设”和“环境背景” (System Prompt)
            // 这是 AI 能够结合硬件数据的关键！
            String systemPrompt = "你是一位拥有20年经验的资深农业专家。当前大棚的实时环境数据如下：\n" +
                    "【室内温度】：" + indoorTemp + "°C\n" +
                    "【室内湿度】：" + indoorHumidity + "%\n" +
                    "【室外温度】：" + outdoorTemp + "°C\n" +
                    "请根据上述环境数据回答用户的问题。\n" +
                    "注意：如果温湿度处于危险范围（如湿度>80%或温度>35°C），请在回答开头优先给出红色预警和具体补救措施（如开窗、补水等）。回答风格要专业、简洁。";
            // 2. 构建请求参数 (OpenAI 接口格式)
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat"); // 指定模型版本
            payload.put("temperature", 0.7);       // 创意程度 (0.7比较平衡)

            // 消息历史
            JSONArray messages = new JSONArray();

            // -> 系统角色 (System)
            messages.add(new JSONObject().put("role", "system").put("content", systemPrompt));

            // -> 用户角色 (User)
            messages.add(new JSONObject().put("role", "user").put("content", userQuestion));

            payload.put("messages", messages);

            // 3. 发送 HTTP POST 请求 (使用 Hutool 工具链)
            // 设置超时时间为 60秒，因为 AI 思考需要时间
            String result = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(payload))
                    .timeout(60000)
                    .execute()
                    .body();

            // 4. 解析返回的 JSON 结果
            JSONObject jsonResult = JSONUtil.parseObj(result);

            // 检查是否有错误信息
            if (jsonResult.containsKey("error")) {
                String errorMsg = jsonResult.getJSONObject("error").getStr("message");
                return "DeepSeek 调用失败: " + errorMsg;
            }

            // 提取 AI 回复的核心文本
            return jsonResult.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

        } catch (Exception e) {
            e.printStackTrace();
            // 生产环境建议记录日志，这里直接返回友好提示
            return "AI 农艺师正在田间巡逻（连接超时或网络异常），请稍后再试。";
        }
    }
}