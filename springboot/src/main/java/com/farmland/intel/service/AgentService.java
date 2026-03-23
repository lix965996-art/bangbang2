package com.farmland.intel.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.farmland.intel.agent.AgentAction;
import com.farmland.intel.agent.AgentActionResult;
import com.farmland.intel.agent.AgentPlan;
import com.farmland.intel.entity.Statistic;
import com.farmland.intel.entity.Sales;
import com.farmland.intel.entity.Purchase;
import com.farmland.intel.entity.Inventory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 编排服务：使用 Function Calling 实现智能农业助手
 * 支持工具调用：查询农田数据、获取设备状态、控制设备等
 */
@Service
@Slf4j
public class AgentService {

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;

    @Value("${qwen.model:qwen-max}")
    private String model;

    @Autowired(required = false)
    private IOneNetService oneNetService;

    @Autowired
    private IStatisticService statisticService;
    
    @Autowired(required = false)
    private ISalesService salesService;
    
    @Autowired(required = false)
    private IPurchaseService purchaseService;
    
    @Autowired(required = false)
    private IInventoryService inventoryService;
    
    @Autowired(required = false)
    private IUserService userService;
    
    @Autowired(required = false)
    private IRoleService roleService;
    
    @Autowired(required = false)
    private INoticeService noticeService;
    
    @Autowired(required = false)
    private IOnlineSaleService onlineSaleService;
    
    @Autowired
    private ModelCallLogger modelCallLogger;
    
    // 最大工具调用轮次，防止无限循环
    private static final int MAX_TOOL_ROUNDS = 5;

    /**
     * 使用 Function Calling 构建智能计划
     * AI 可以主动调用工具获取实时数据，然后基于数据做出智能判断
     */
    public AgentPlan buildPlan(String userQuestion) {
        AgentPlan plan = new AgentPlan();
        if (!StringUtils.hasText(userQuestion)) {
            plan.setAdvice("请输入问题或指令。");
            return plan;
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("通义千问 API Key 未配置，使用规则兜底计划");
            return fallbackPlan(userQuestion);
        }

        try {
            // 使用 Function Calling 进行多轮对话
            String result = callQwenWithFunctionCalling(userQuestion);
            if (StringUtils.hasText(result)) {
                AgentPlan parsed = parsePlan(result);
                if (parsed != null && parsed.getAdvice() != null) {
                    String advice = parsed.getAdvice();
                    // 检查AI是否只是说"正在调用"或"将调用"而没有返回实际数据
                    if (advice.contains("正在") || advice.contains("将调用") || advice.contains("我将") || 
                        advice.contains("跳转") || advice.length() < 50) {
                        log.info("AI回答不完整，使用兜底逻辑");
                        return fallbackPlan(userQuestion);
                    }
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.error("调用通义千问生成计划失败，使用兜底逻辑", e);
        }

        return fallbackPlan(userQuestion);
    }
    
    /**
     * 使用 Function Calling 进行多轮对话
     * AI 会根据需要调用工具获取数据，然后给出最终回答
     */
    private String callQwenWithFunctionCalling(String userQuestion) {
        // 构建消息列表
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject()
            .put("role", "system")
            .put("content", buildSystemPrompt()));
        messages.add(new JSONObject()
            .put("role", "user")
            .put("content", userQuestion));
        
        // 构建工具定义
        JSONArray tools = buildTools();
        
        int round = 0;
        int totalToolCalls = 0;
        
        while (round < MAX_TOOL_ROUNDS) {
            round++;
            log.info("Function Calling 第 {} 轮", round);
            
            // 调用 API
            JSONObject response = callQwenAPI(messages, tools);
            if (response == null) {
                log.error("API 调用失败");
                modelCallLogger.logApiError(model, userQuestion, "NULL_RESPONSE", "API 返回 null");
                return null;
            }
            
            // 检查是否有错误
            if (response.containsKey("code") && !"200".equals(response.getStr("code"))) {
                String errorCode = response.getStr("code");
                String errorMsg = response.getStr("message");
                log.error("API 返回错误: code={}, message={}", errorCode, errorMsg);
                modelCallLogger.logApiError(model, userQuestion, errorCode, errorMsg);
                return null;
            }
            
            // 解析响应
            JSONObject output = response.getJSONObject("output");
            if (output == null) {
                log.error("响应中没有 output 字段: {}", response);
                modelCallLogger.logApiError(model, userQuestion, "NO_OUTPUT", "响应中没有 output 字段");
                return null;
            }
            
            JSONArray choices = output.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("响应中没有 choices: {}", response);
                modelCallLogger.logApiError(model, userQuestion, "NO_CHOICES", "响应中没有 choices");
                return null;
            }
            
            JSONObject choice = choices.getJSONObject(0);
            String finishReason = choice.getStr("finish_reason");
            JSONObject message = choice.getJSONObject("message");
            
            // 检查是否需要调用工具
            if ("tool_calls".equals(finishReason) && message.containsKey("tool_calls")) {
                JSONArray toolCalls = message.getJSONArray("tool_calls");
                int toolCallCount = toolCalls.size();
                totalToolCalls += toolCallCount;
                log.info("AI 请求调用 {} 个工具", toolCallCount);
                
                // 记录工具调用日志
                modelCallLogger.logModelCall(model, userQuestion, response, true, toolCallCount);
                
                // 将 assistant 消息添加到历史
                messages.add(message);
                
                // 执行每个工具调用
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject toolCall = toolCalls.getJSONObject(i);
                    String toolCallId = toolCall.getStr("id");
                    JSONObject function = toolCall.getJSONObject("function");
                    String functionName = function.getStr("name");
                    String argumentsStr = function.getStr("arguments");
                    
                    log.info("执行工具: {} 参数: {}", functionName, argumentsStr);
                    
                    // 执行工具并获取结果
                    String toolResult = executeToolCall(functionName, argumentsStr);
                    log.info("工具 {} 返回: {}", functionName, toolResult);
                    
                    // 将工具结果添加到消息
                    messages.add(new JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", toolCallId)
                        .put("content", toolResult));
                }
            } else {
                // AI 完成思考，返回最终结果
                String content = message.getStr("content");
                log.info("AI 最终回答: {}", content);
                
                // 记录最终回答日志
                modelCallLogger.logModelCall(model, userQuestion, response, true, totalToolCalls);
                
                return cleanJsonContent(content);
            }
        }
        
        log.warn("达到最大工具调用轮次 {}", MAX_TOOL_ROUNDS);
        modelCallLogger.logApiError(model, userQuestion, "MAX_ROUNDS", "达到最大工具调用轮次: " + MAX_TOOL_ROUNDS);
        return null;
    }
    
    /**
     * 调用通义千问 API（支持 Function Calling）
     * DashScope API: tools 放在顶层
     */
    private JSONObject callQwenAPI(JSONArray messages, JSONArray tools) {
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        
        JSONObject input = new JSONObject();
        input.put("messages", messages);
        payload.put("input", input);
        
        JSONObject parameters = new JSONObject();
        parameters.put("temperature", 0.3);
        parameters.put("result_format", "message");
        payload.put("parameters", parameters);
        
        // DashScope API: tools 放在顶层
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
        }
        
        log.info("调用通义千问 API, 请求体: {}", payload.toString());
        
        try {
            String responseStr = HttpRequest.post(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(payload.toString())
                .timeout(60000)
                .execute()
                .body();
            
            log.info("通义千问响应: {}", responseStr);
            return JSONUtil.parseObj(responseStr);
        } catch (Exception e) {
            log.error("调用通义千问 API 异常", e);
            return null;
        }
    }
    
    /**
     * 执行工具调用
     */
    private String executeToolCall(String functionName, String argumentsStr) {
        try {
            JSONObject args = StringUtils.hasText(argumentsStr) ? JSONUtil.parseObj(argumentsStr) : new JSONObject();
            
            switch (functionName) {
                case "get_all_farms":
                    return getAllFarmsData();
                    
                case "get_farm_detail":
                    String farmName = args.getStr("farm_name");
                    return getFarmDetail(farmName);
                    
                case "get_farms_need_irrigation":
                    int threshold = args.getInt("threshold", 30);
                    return getFarmsNeedIrrigation(threshold);
                    
                case "get_device_status":
                    return getDeviceStatus();
                    
                case "get_environment_data":
                    return getEnvironmentData();
                    
                case "get_sales_data":
                    return getSalesData();
                    
                case "get_purchase_data":
                    return getPurchaseData();
                    
                case "get_inventory_data":
                    return getInventoryData();
                    
                case "get_profit_analysis":
                    return getProfitAnalysis();
                    
                case "get_user_statistics":
                    return getUserStatistics();
                    
                case "get_system_overview":
                    return getSystemOverview();
                    
                case "get_online_sale_data":
                    return getOnlineSaleData();
                    
                case "get_business_health_score":
                    return getBusinessHealthScore();
                    
                case "get_trend_analysis":
                    return getTrendAnalysis();
                    
                case "get_comprehensive_report":
                    return getComprehensiveReport();
                    
                default:
                    return "{\"error\": \"未知工具: " + functionName + "\"}";
            }
        } catch (Exception e) {
            log.error("执行工具 {} 失败", functionName, e);
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }
    
    // ==================== 工具实现 ====================
    
    /**
     * 获取所有农田数据
     */
    private String getAllFarmsData() {
        List<Statistic> farms = statisticService.list();
        JSONArray result = new JSONArray();
        for (Statistic farm : farms) {
            result.add(farmToJson(farm));
        }
        return result.toString();
    }
    
    /**
     * 获取指定农田详情
     */
    private String getFarmDetail(String farmName) {
        List<Statistic> farms = statisticService.list();
        for (Statistic farm : farms) {
            if (farm.getFarm() != null && farm.getFarm().contains(farmName)) {
                return farmToJson(farm).toString();
            }
        }
        return "{\"error\": \"未找到农田: " + farmName + "\"}";
    }
    
    /**
     * 获取需要灌溉的农田（土壤湿度低于阈值）
     */
    private String getFarmsNeedIrrigation(int threshold) {
        List<Statistic> farms = statisticService.list();
        JSONArray result = new JSONArray();
        for (Statistic farm : farms) {
            Integer soilHumidity = farm.getSoilhumidity();
            if (soilHumidity != null && soilHumidity < threshold) {
                JSONObject item = farmToJson(farm);
                item.put("need_irrigation", true);
                item.put("urgency", soilHumidity < 20 ? "紧急" : "一般");
                result.add(item);
            }
        }
        if (result.isEmpty()) {
            return "{\"message\": \"所有农田土壤湿度正常，无需灌溉\", \"threshold\": " + threshold + "}";
        }
        return result.toString();
    }
    
    /**
     * 获取设备状态
     */
    private String getDeviceStatus() {
        if (oneNetService == null) {
            return "{\"error\": \"OneNET 服务不可用\"}";
        }
        try {
            Map<String, Object> data = oneNetService.getDeviceData();
            return JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            return "{\"error\": \"获取设备状态失败: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 获取环境数据（温湿度等）
     */
    private String getEnvironmentData() {
        JSONObject result = new JSONObject();
        
        // 从 OneNET 获取实时数据
        if (oneNetService != null) {
            try {
                Map<String, Object> data = oneNetService.getDeviceData();
                result.put("realtime", data);
            } catch (Exception e) {
                result.put("realtime_error", e.getMessage());
            }
        }
        
        // 从数据库获取各农田的环境数据
        List<Statistic> farms = statisticService.list();
        JSONArray farmEnvs = new JSONArray();
        for (Statistic farm : farms) {
            JSONObject env = new JSONObject();
            env.put("farm_name", farm.getFarm());
            env.put("temperature", farm.getTemperature());
            env.put("air_humidity", farm.getAirhumidity());
            env.put("soil_humidity", farm.getSoilhumidity());
            env.put("light", farm.getLight());
            env.put("co2", farm.getCarbon());
            env.put("ph", farm.getPh());
            farmEnvs.add(env);
        }
        result.put("farms", farmEnvs);
        
        return result.toString();
    }
    
    /**
     * 获取销售收入数据
     */
    private String getSalesData() {
        JSONObject result = new JSONObject();
        
        if (salesService == null) {
            result.put("error", "销售服务未配置");
            return result.toString();
        }
        
        try {
            List<Sales> salesList = salesService.list();
            BigDecimal totalIncome = BigDecimal.ZERO;
            JSONArray records = new JSONArray();
            
            for (Sales sale : salesList) {
                JSONObject record = new JSONObject();
                record.put("id", sale.getId());
                record.put("product", sale.getProduct());
                record.put("price", sale.getPrice());
                record.put("number", sale.getNumber());
                record.put("buyer", sale.getBuyer());
                record.put("shipper", sale.getShipper());
                
                // 计算单笔收入
                if (sale.getPrice() != null && sale.getNumber() != null) {
                    BigDecimal income = sale.getPrice().multiply(new BigDecimal(sale.getNumber()));
                    record.put("income", income);
                    totalIncome = totalIncome.add(income);
                }
                records.add(record);
            }
            
            result.put("total_records", salesList.size());
            result.put("total_income", totalIncome);
            result.put("records", records);
        } catch (Exception e) {
            result.put("error", "获取销售数据失败: " + e.getMessage());
        }
        
        return result.toString();
    }
    
    /**
     * 获取采购数据
     */
    private String getPurchaseData() {
        JSONObject result = new JSONObject();
        
        if (purchaseService == null) {
            result.put("error", "采购服务未配置");
            return result.toString();
        }
        
        try {
            List<Purchase> purchaseList = purchaseService.list();
            BigDecimal totalCost = BigDecimal.ZERO;
            JSONArray records = new JSONArray();
            
            for (Purchase purchase : purchaseList) {
                JSONObject record = new JSONObject();
                record.put("id", purchase.getId());
                record.put("product", purchase.getProduct());
                record.put("price", purchase.getPrice());
                record.put("number", purchase.getNumber());
                record.put("provider", purchase.getProvider());
                record.put("purchaser", purchase.getPurchaser());
                
                // 计算单笔支出
                if (purchase.getPrice() != null && purchase.getNumber() != null) {
                    BigDecimal cost = purchase.getPrice().multiply(new BigDecimal(purchase.getNumber()));
                    record.put("cost", cost);
                    totalCost = totalCost.add(cost);
                }
                records.add(record);
            }
            
            result.put("total_records", purchaseList.size());
            result.put("total_cost", totalCost);
            result.put("records", records);
        } catch (Exception e) {
            result.put("error", "获取采购数据失败: " + e.getMessage());
        }
        
        return result.toString();
    }
    
    /**
     * 获取库存数据（当前拥有的物资）
     */
    private String getInventoryData() {
        JSONObject result = new JSONObject();
        
        if (inventoryService == null) {
            result.put("error", "库存服务未配置");
            return result.toString();
        }
        
        try {
            List<Inventory> inventoryList = inventoryService.list();
            int totalItems = inventoryList.size();
            int lowStockCount = 0;
            JSONArray records = new JSONArray();
            
            for (Inventory item : inventoryList) {
                JSONObject record = new JSONObject();
                record.put("id", item.getId());
                record.put("name", item.getProduce());
                record.put("warehouse", item.getWarehouse());
                record.put("region", item.getRegion());
                record.put("number", item.getNumber());
                record.put("safe_stock", item.getSafeStock());
                record.put("keeper", item.getKeeper());
                record.put("remark", item.getRemark());
                
                // 判断是否低库存
                int stock = item.getNumber() != null ? item.getNumber() : 0;
                int safeStock = item.getSafeStock() != null ? item.getSafeStock() : 10;
                if (stock < safeStock) {
                    record.put("is_low_stock", true);
                    lowStockCount++;
                } else {
                    record.put("is_low_stock", false);
                }
                
                records.add(record);
            }
            
            result.put("total_items", totalItems);
            result.put("low_stock_count", lowStockCount);
            result.put("records", records);
        } catch (Exception e) {
            result.put("error", "获取库存数据失败: " + e.getMessage());
        }
        
        return result.toString();
    }
    
    /**
     * 获取利润分析数据（销售收入 - 采购成本 = 毛利润）
     */
    private String getProfitAnalysis() {
        JSONObject result = new JSONObject();
        
        try {
            // 计算销售总收入
            BigDecimal totalIncome = BigDecimal.ZERO;
            int salesCount = 0;
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                salesCount = salesList.size();
                for (Sales sale : salesList) {
                    if (sale.getPrice() != null && sale.getNumber() != null) {
                        totalIncome = totalIncome.add(sale.getPrice().multiply(new BigDecimal(sale.getNumber())));
                    }
                }
            }
            
            // 计算采购总成本
            BigDecimal totalCost = BigDecimal.ZERO;
            int purchaseCount = 0;
            if (purchaseService != null) {
                List<Purchase> purchaseList = purchaseService.list();
                purchaseCount = purchaseList.size();
                for (Purchase purchase : purchaseList) {
                    if (purchase.getPrice() != null && purchase.getNumber() != null) {
                        totalCost = totalCost.add(purchase.getPrice().multiply(new BigDecimal(purchase.getNumber())));
                    }
                }
            }
            
            // 计算毛利润
            BigDecimal grossProfit = totalIncome.subtract(totalCost);
            
            // 计算利润率
            BigDecimal profitRate = BigDecimal.ZERO;
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = grossProfit.multiply(new BigDecimal(100)).divide(totalIncome, 2, BigDecimal.ROUND_HALF_UP);
            }
            
            result.put("total_income", totalIncome);
            result.put("total_cost", totalCost);
            result.put("gross_profit", grossProfit);
            result.put("profit_rate", profitRate.toString() + "%");
            result.put("sales_count", salesCount);
            result.put("purchase_count", purchaseCount);
            result.put("is_profitable", grossProfit.compareTo(BigDecimal.ZERO) > 0);
            
        } catch (Exception e) {
            result.put("error", "获取利润分析失败: " + e.getMessage());
        }
        
        return result.toString();
    }
    
    /**
     * 将农田实体转换为 JSON
     */
    private JSONObject farmToJson(Statistic farm) {
        JSONObject json = new JSONObject();
        json.put("id", farm.getId());
        json.put("name", farm.getFarm());
        json.put("crop", farm.getCrop());
        json.put("area", farm.getArea());
        json.put("state", farm.getState());
        json.put("temperature", farm.getTemperature());
        json.put("air_humidity", farm.getAirhumidity());
        json.put("soil_humidity", farm.getSoilhumidity());
        json.put("light", farm.getLight());
        json.put("co2", farm.getCarbon());
        json.put("ph", farm.getPh());
        json.put("pump", farm.getPump());
        json.put("led", farm.getFilllight());
        json.put("keeper", farm.getKeeper());
        return json;
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return "你是一个专业的智能农业助手，拥有20年农业经验和数据分析能力。你的职责是：\n" +
               "1. 主动调用工具获取农田实时数据、财务数据、系统运营数据\n" +
               "2. 基于真实数据进行专业分析，给出详细、有数据支撑的深度洞察\n" +
               "3. 从多个维度综合评估，提供战略性建议和优化方案\n" +
               "4. 在必要时生成设备控制指令\n\n" +
               "【核心数据工具 - 基础数据获取】\n" +
               "1. get_all_farms - 获取所有农田环境数据（温湿度、光照、pH等）\n" +
               "2. get_sales_data - 获取销售记录和收入数据\n" +
               "3. get_purchase_data - 获取采购记录和成本支出\n" +
               "4. get_inventory_data - 获取仓库库存和物资情况\n" +
               "5. get_profit_analysis - ★核心★ 自动计算利润（收入-成本=利润）\n" +
               "6. get_device_status - 获取IoT设备状态（水泵、补光灯等）\n" +
               "7. get_environment_data - 获取环境监测汇总数据\n\n" +
               "【系统管理工具 - 全局运营数据】\n" +
               "8. get_user_statistics - 获取用户统计（总用户数、角色分布）\n" +
               "9. get_system_overview - ★推荐★ 获取系统全局概览（农田、用户、销售、库存、公告等全局统计）\n" +
               "10. get_online_sale_data - 获取在线销售平台的订单和收入数据\n\n" +
               "【深度分析工具 - AI智能分析】\n" +
               "11. get_business_health_score - ★强大★ 综合评估农场经营健康度（利润率、库存、资金流、环境4大维度打分）\n" +
               "12. get_trend_analysis - 分析历史趋势并预测未来走向\n" +
               "13. get_comprehensive_report - ★最全面★ 生成完整的经营分析报告（整合所有数据的深度洞察）\n\n" +
               "【智能工具选择策略】\n" +
               "- 用户问\"整体情况\"、\"经营如何\"、\"系统概况\" → 优先调用 get_system_overview 或 get_comprehensive_report\n" +
               "- 用户问\"经营健康度\"、\"评分\"、\"诊断\" → 调用 get_business_health_score\n" +
               "- 用户问\"趋势\"、\"预测\"、\"未来\" → 调用 get_trend_analysis\n" +
               "- 用户问\"利润\"、\"赚了多少\" → 调用 get_profit_analysis\n" +
               "- 用户问\"用户\"、\"系统管理\" → 调用 get_user_statistics\n" +
               "- 用户需要全面分析 → 调用 get_comprehensive_report（整合财务+运营+环境+库存）\n\n" +
               "【重要规则 - 必须遵守！】\n" +
               "- ★核心原则：用户问数据相关问题时，必须先调用工具获取真实数据，然后在advice中给出分析结果\n" +
               "- 多使用深度分析工具（health_score, trend_analysis, comprehensive_report）提供更有价值的洞察\n" +
               "- 当用户询问库存、物资、预警时 → 必须调用 get_inventory_data\n" +
               "- 当用户询问成本、采购、支出时 → 必须调用 get_purchase_data\n" +
               "- advice 字段必须包含从工具获取的真实数据和分析，不要只说\"正在跳转\"或\"请查看\"\n" +
               "- 只有当用户明确说\"打开XX页面\"、\"跳转到XX\"时才使用 navigate 动作\n\n" +
               "【输出格式】必须是 JSON：\n" +
               "{\n" +
               "  \"advice\": \"详细的分析结果和建议（必须包含从工具获取的真实数据）\",\n" +
               "  \"actions\": [{\"id\": \"action_1\", \"type\": \"irrigation_on\", \"title\": \"开启灌溉\", \"description\": \"描述\", \"riskLevel\": \"medium\"}]\n" +
               "}\n\n" +
               "【可用动作类型】\n" +
               "- irrigation_on: 开启灌溉（当土壤湿度<30%时建议）\n" +
               "- irrigation_off: 关闭灌溉\n" +
               "- led_on: 开启补光灯（当光照<1000lux时建议）\n" +
               "- led_off: 关闭补光灯\n" +
               "- navigate: 页面跳转，route 必须以 / 开头\n\n" +
               "【页面路由映射表】\n" +
               "- /fruit-detect: 果蔬双检、果蔬检测、AI识别、病虫害检测\n" +
               "- /farmmap3d: 3D沙盘、3D界面、三维地图\n" +
               "- /dashbordnew: 环境监测、传感器数据、温湿度\n" +
               "- /home: 首页、主页\n" +
               "- /bigscreen: 大屏、可视化大屏\n" +
               "- /statistic: 农田信息、农田统计\n" +
               "- /farm-map-gaode: 地理地图、高德地图、GIS地图\n" +
               "- /purchase: 物资采购、采购管理\n" +
               "- /inventory: 物资库存、仓库管理\n" +
               "- /sales: 出售账单、销售记录\n" +
               "- /online-sale: 在线销售、农作物销售\n\n" +
               "【示例】用户问：今年赚了多少钱？\n" +
               "正确做法：调用get_profit_analysis工具，返回：{\"advice\": \"根据财务数据分析：销售总收入46025元，采购成本10750元，毛利润35275元，利润率76.64%。经营状况良好！\", \"actions\": []}";
    }
    
    /**
     * 构建工具定义
     */
    private static JSONArray buildTools() {
        JSONArray tools = new JSONArray();
        
        // 工具1: 获取所有农田数据
        tools.add(buildTool(
            "get_all_farms",
            "获取所有农田的基本信息和环境数据，包括名称、作物、面积、土壤湿度、温度等",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具2: 获取指定农田详情
        tools.add(buildTool(
            "get_farm_detail",
            "获取指定农田的详细信息",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                    .put("farm_name", new JSONObject()
                        .put("type", "string")
                        .put("description", "农田名称或关键字")))
                .put("required", new JSONArray().put("farm_name"))
        ));
        
        // 工具3: 获取需要灌溉的农田
        tools.add(buildTool(
            "get_farms_need_irrigation",
            "查询土壤湿度低于阈值、需要灌溉的农田列表",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                    .put("threshold", new JSONObject()
                        .put("type", "integer")
                        .put("description", "土壤湿度阈值，低于此值需要灌溉，默认30%")))
                .put("required", new JSONArray())
        ));
        
        // 工具4: 获取设备状态
        tools.add(buildTool(
            "get_device_status",
            "获取 IoT 设备的实时状态，包括水泵、补光灯等",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具5: 获取环境数据
        tools.add(buildTool(
            "get_environment_data",
            "获取所有农田的环境监测数据，包括温度、湿度、光照、CO2等",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具6: 获取销售收入数据
        tools.add(buildTool(
            "get_sales_data",
            "获取农产品销售数据和收入统计，包括今年收入、销售记录等",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具7: 获取采购数据
        tools.add(buildTool(
            "get_purchase_data",
            "获取物资采购数据（投入成本），包括购买的农资、机械设备等的支出记录",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具8: 获取库存数据
        tools.add(buildTool(
            "get_inventory_data",
            "获取仓库库存数据（当前拥有的物资），包括农资、工具、设备的库存数量和预警状态",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具9: 利润分析
        tools.add(buildTool(
            "get_profit_analysis",
            "获取经营利润分析，自动计算：销售总收入 - 采购总成本 = 毛利润，以及利润率",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具10: 用户统计
        tools.add(buildTool(
            "get_user_statistics",
            "获取系统用户统计数据，包括总用户数、角色分布、活跃情况等",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具11: 系统概览
        tools.add(buildTool(
            "get_system_overview",
            "获取系统整体运营概览，包括农田、用户、销售、库存、公告等全局统计",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具12: 在线销售数据
        tools.add(buildTool(
            "get_online_sale_data",
            "获取在线销售平台的农作物销售数据和订单统计",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具13: 经营健康度评分
        tools.add(buildTool(
            "get_business_health_score",
            "综合评估农场经营健康度，从利润率、库存周转、资金流等多维度打分并给出诊断建议",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具14: 趋势分析
        tools.add(buildTool(
            "get_trend_analysis",
            "分析销售、采购、库存的历史趋势，预测未来走向并给出优化建议",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        // 工具15: 综合分析报告
        tools.add(buildTool(
            "get_comprehensive_report",
            "生成全面的经营分析报告，整合财务、运营、环境、库存等全部数据的深度洞察",
            new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray())
        ));
        
        return tools;
    }
    
    /**
     * 构建单个工具定义
     */
    private static JSONObject buildTool(String name, String description, JSONObject parameters) {
        return new JSONObject()
            .put("type", "function")
            .put("function", new JSONObject()
                .put("name", name)
                .put("description", description)
                .put("parameters", parameters));
    }
    
    /**
     * 清理 JSON 内容中的 markdown 标记
     */
    private String cleanJsonContent(String content) {
        if (content == null) return null;
        content = content.trim();
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

    /**
     * 执行已经确认的动作，所有动作都在白名单内。
     */
    public List<AgentActionResult> executeActions(List<AgentAction> actions) {
        List<AgentActionResult> results = new ArrayList<>();
        if (actions == null || actions.isEmpty()) {
            return results;
        }

        for (AgentAction action : actions) {
            AgentActionResult result = new AgentActionResult();
            result.setId(action.getId());
            result.setType(action.getType());
            result.setStatus("skipped");
            result.setMessage("动作类型未处理");

            try {
                switch (action.getType()) {
                    case "navigate":
                        result.setStatus("pending-client");
                        result.setRoute(action.getRoute());
                        result.setMessage("前端已确认，准备跳转");
                        break;
                    case "irrigation_on":
                        if (oneNetService != null && oneNetService.controlBump(true)) {
                            result.setStatus("success");
                            result.setMessage("已开启智能灌溉（水泵）");
                        } else {
                            result.setStatus("failed");
                            result.setMessage("灌溉开启失败，OneNET 不可用或调用异常");
                        }
                        break;
                    case "irrigation_off":
                        if (oneNetService != null && oneNetService.controlBump(false)) {
                            result.setStatus("success");
                            result.setMessage("已关闭智能灌溉（水泵）");
                        } else {
                            result.setStatus("failed");
                            result.setMessage("灌溉关闭失败，OneNET 不可用或调用异常");
                        }
                        break;
                    case "led_on":
                        if (oneNetService != null && oneNetService.controlLed(true)) {
                            result.setStatus("success");
                            result.setMessage("补光灯已开启");
                        } else {
                            result.setStatus("failed");
                            result.setMessage("补光灯开启失败，OneNET 不可用或调用异常");
                        }
                        break;
                    case "led_off":
                        if (oneNetService != null && oneNetService.controlLed(false)) {
                            result.setStatus("success");
                            result.setMessage("补光灯已关闭");
                        } else {
                            result.setStatus("failed");
                            result.setMessage("补光灯关闭失败，OneNET 不可用或调用异常");
                        }
                        break;
                    case "create_farm":
                        // 创建新农田
                        try {
                            Map<String, Object> params = action.getParams();
                            Statistic newFarm = new Statistic();
                            
                            String address = params.getOrDefault("address", "").toString();
                            newFarm.setAddress(address);
                            
                            // 根据地址匹配预设的地理坐标
                            Map<String, Object> geoData = getPresetGeoLocation(address);
                            
                            // 优先使用预设的建议名称，否则使用用户指定的名称
                            String farmName = params.getOrDefault("name", "新农田").toString();
                            if (geoData != null && geoData.get("suggestedName") != null) {
                                farmName = geoData.get("suggestedName").toString();
                            }
                            newFarm.setFarm(farmName);
                            
                            newFarm.setArea(params.getOrDefault("area", "100").toString());
                            newFarm.setCrop(params.getOrDefault("crop", "未种植").toString());
                            
                            // 从地址中提取区县信息
                            String district = extractDistrictFromAddress(address);
                            if (district != null && !district.isEmpty()) {
                                newFarm.setDistrict(district);
                            }
                            
                            // 填充地理坐标
                            if (geoData != null) {
                                newFarm.setCenterLng(new java.math.BigDecimal(geoData.get("centerLng").toString()));
                                newFarm.setCenterLat(new java.math.BigDecimal(geoData.get("centerLat").toString()));
                                newFarm.setCoordinates(geoData.get("coordinates").toString());
                            }
                            
                            newFarm.setState("正常");
                            newFarm.setSoilhumidity(60);
                            newFarm.setAirhumidity(50);
                            newFarm.setTemperature(new java.math.BigDecimal("25.0"));
                            
                            statisticService.save(newFarm);
                            result.setStatus("success");
                            result.setMessage("农田【" + newFarm.getFarm() + "】创建成功！ID: " + newFarm.getId());
                            log.info("Agent创建农田成功: {}", newFarm.getFarm());
                        } catch (Exception e) {
                            result.setStatus("failed");
                            result.setMessage("创建农田失败: " + e.getMessage());
                            log.error("Agent创建农田失败", e);
                        }
                        break;
                    case "delete_farm":
                        // 删除农田
                        try {
                            Map<String, Object> params = action.getParams();
                            Object farmIdObj = params.get("farmId");
                            if (farmIdObj != null) {
                                Integer farmId = Integer.parseInt(farmIdObj.toString());
                                Statistic farm = statisticService.getById(farmId);
                                if (farm != null) {
                                    statisticService.removeById(farmId);
                                    result.setStatus("success");
                                    result.setMessage("农田【" + farm.getFarm() + "】已删除");
                                    log.info("Agent删除农田成功: ID={}", farmId);
                                } else {
                                    result.setStatus("failed");
                                    result.setMessage("未找到ID为" + farmId + "的农田");
                                }
                            } else {
                                result.setStatus("failed");
                                result.setMessage("未指定要删除的农田ID");
                            }
                        } catch (Exception e) {
                            result.setStatus("failed");
                            result.setMessage("删除农田失败: " + e.getMessage());
                        }
                        break;
                    case "drone_spray":
                        // 飞防消杀
                        try {
                            Map<String, Object> params = action.getParams();
                            String farmName = params != null ? params.getOrDefault("farmName", "农田").toString() : "农田";
                            
                            // 模拟启动无人机飞防消杀
                            result.setStatus("success");
                            result.setMessage("已启动【" + farmName + "】的飞防消杀作业，无人机正在执行任务...");
                            log.info("Agent启动飞防消杀: {}", farmName);
                        } catch (Exception e) {
                            result.setStatus("failed");
                            result.setMessage("飞防消杀启动失败: " + e.getMessage());
                        }
                        break;
                    default:
                        result.setStatus("skipped");
                        result.setMessage("不支持的动作类型: " + action.getType());
                        break;
                }
            } catch (Exception e) {
                log.error("执行动作失败: {}", action.getId(), e);
                result.setStatus("failed");
                result.setMessage("执行异常: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    private AgentPlan parsePlan(String content) {
        try {
            JSONObject obj = JSONUtil.parseObj(content);
            AgentPlan plan = new AgentPlan();
            plan.setAdvice(obj.getStr("advice", "好的，我来帮您处理。"));

            List<AgentAction> actions = new ArrayList<>();
            JSONArray arr = obj.getJSONArray("actions");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    AgentAction action = new AgentAction();
                    action.setId(item.getStr("id", "action-" + i));
                    action.setType(item.getStr("type"));
                    action.setTitle(item.getStr("title", "未命名动作"));
                    action.setDescription(item.getStr("description", ""));
                    action.setRoute(item.getStr("route", null));
                    action.setTarget(item.getStr("target", null));
                    action.setRiskLevel(item.getStr("riskLevel", "medium"));
                    if (item.containsKey("params")) {
                        action.setParams(item.getJSONObject("params"));
                    }

                    // 白名单校验
                    if (isAllowedType(action.getType())) {
                        actions.add(action);
                    } else {
                        log.warn("丢弃非白名单动作: {}", action.getType());
                    }
                }
            }
            plan.setActions(actions);
            return plan;
        } catch (Exception e) {
            log.error("解析 Agent 计划失败: {}", content, e);
            return null;
        }
    }

    private boolean isAllowedType(String type) {
        return Arrays.asList("navigate", "irrigation_on", "irrigation_off", "led_on", "led_off", 
                            "create_farm", "delete_farm", "update_farm", "drone_spray")
                .contains(type);
    }

    private AgentPlan fallbackPlan(String userQuestion) {
        AgentPlan plan = new AgentPlan();
        List<AgentAction> actions = new ArrayList<>();

        String lower = userQuestion.toLowerCase(Locale.ROOT);
        
        // 优先匹配：指定田地的操作（灌溉、飞防消杀等）
        String targetFarmName = extractFarmName(userQuestion);
        if (targetFarmName != null) {
            List<Statistic> farms = statisticService.list();
            Statistic targetFarm = null;
            
            for (Statistic farm : farms) {
                if (farm.getFarm() != null && 
                    (farm.getFarm().contains(targetFarmName) || matchFarmNumber(farm.getFarm(), targetFarmName))) {
                    targetFarm = farm;
                    break;
                }
            }
            
            if (targetFarm != null) {
                // 飞防消杀
                if (lower.contains("飞防") || lower.contains("消杀") || lower.contains("打药") || lower.contains("喷药") || lower.contains("除虫")) {
                    StringBuilder advice = new StringBuilder();
                    advice.append("好的，即将为【").append(targetFarm.getFarm()).append("】执行飞防消杀。\n\n");
                    advice.append("作物：").append(targetFarm.getCrop() != null ? targetFarm.getCrop() : "未知").append("\n");
                    advice.append("面积：").append(targetFarm.getArea() != null ? targetFarm.getArea() : "未知").append("亩\n\n");
                    advice.append("确认后将启动无人机进行飞防作业。");
                    
                    plan.setAdvice(advice.toString());
                    
                    // 添加导航到该田地
                    AgentAction navAction = new AgentAction();
                    navAction.setId("nav_farm_" + System.currentTimeMillis());
                    navAction.setType("navigate");
                    navAction.setTitle("跳转到" + targetFarm.getFarm());
                    navAction.setDescription("在3D地图中定位到" + targetFarm.getFarm());
                    navAction.setRoute("/farmmap3d?farmId=" + targetFarm.getId());
                    navAction.setRiskLevel("low");
                    actions.add(navAction);
                    
                    // 添加飞防消杀动作
                    AgentAction sprayAction = new AgentAction();
                    sprayAction.setId("drone_spray_" + System.currentTimeMillis());
                    sprayAction.setType("drone_spray");
                    sprayAction.setTitle("飞防消杀：" + targetFarm.getFarm());
                    sprayAction.setDescription("为【" + targetFarm.getFarm() + "】启动无人机飞防消杀");
                    sprayAction.setRiskLevel("medium");
                    Map<String, Object> params = new HashMap<>();
                    params.put("farmId", targetFarm.getId());
                    params.put("farmName", targetFarm.getFarm());
                    sprayAction.setParams(params);
                    actions.add(sprayAction);
                    
                    plan.setActions(actions);
                    return plan;
                }
                
                // 灌溉/浇水
                if (lower.contains("灌溉") || lower.contains("浇水") || lower.contains("浇")) {
                    StringBuilder advice = new StringBuilder();
                    advice.append("好的，即将为【").append(targetFarm.getFarm()).append("】开启智能灌溉。\n\n");
                    advice.append("当前土壤湿度：").append(targetFarm.getSoilhumidity() != null ? targetFarm.getSoilhumidity() + "%" : "未知").append("\n\n");
                    advice.append("确认后将启动灌溉系统。");
                    
                    plan.setAdvice(advice.toString());
                    
                    // 添加导航到该田地
                    AgentAction navAction = new AgentAction();
                    navAction.setId("nav_farm_" + System.currentTimeMillis());
                    navAction.setType("navigate");
                    navAction.setTitle("跳转到" + targetFarm.getFarm());
                    navAction.setDescription("在3D地图中定位到" + targetFarm.getFarm());
                    navAction.setRoute("/farmmap3d?farmId=" + targetFarm.getId());
                    navAction.setRiskLevel("low");
                    actions.add(navAction);
                    
                    // 添加灌溉动作
                    AgentAction irrigation = new AgentAction();
                    irrigation.setId("irrigation_on_" + System.currentTimeMillis());
                    irrigation.setType("irrigation_on");
                    irrigation.setTitle("智能灌溉：" + targetFarm.getFarm());
                    irrigation.setDescription("为【" + targetFarm.getFarm() + "】开启智能灌溉");
                    irrigation.setRiskLevel("medium");
                    Map<String, Object> params = new HashMap<>();
                    params.put("farmId", targetFarm.getId());
                    params.put("farmName", targetFarm.getFarm());
                    irrigation.setParams(params);
                    actions.add(irrigation);
                    
                    plan.setActions(actions);
                    return plan;
                }
            }
        }
        
        // 优先匹配：查询需要浇水的农田（必须在导航之前）
        if ((lower.contains("需要浇水") || lower.contains("需要灌溉") || lower.contains("要浇水") || lower.contains("要灌溉") ||
             lower.contains("缺水") || lower.contains("干旱")) ||
            ((lower.contains("浇水") || lower.contains("灌溉")) && (lower.contains("哪") || lower.contains("有没有") || lower.contains("看看")))) {
            
            List<Statistic> farms = statisticService.list();
            List<Statistic> needWater = farms.stream()
                .filter(f -> f.getSoilhumidity() != null && f.getSoilhumidity() < 30)
                .collect(Collectors.toList());
            
            StringBuilder advice = new StringBuilder();
            if (!needWater.isEmpty()) {
                advice.append("检查了一下，有 ").append(needWater.size()).append(" 块地需要浇水：\n\n");
                for (Statistic farm : needWater) {
                    advice.append(farm.getFarm()).append("：土壤湿度 ")
                          .append(farm.getSoilhumidity()).append("%，有点干了\n");
                }
                advice.append("\n要不要帮您开启灌溉？");
                
                // 添加灌溉动作
                AgentAction irrigation = new AgentAction();
                irrigation.setId("irrigation_on_" + System.currentTimeMillis());
                irrigation.setType("irrigation_on");
                irrigation.setTitle("开启灌溉");
                irrigation.setDescription("为缺水农田开启灌溉系统");
                irrigation.setRiskLevel("medium");
                actions.add(irrigation);
            } else {
                advice.append("检查了一下，所有农田的土壤湿度都正常，暂时不需要浇水。");
            }
            
            plan.setAdvice(advice.toString());
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：新建/创建农田（必须在导航之前，因为"帮我去新建"会触发导航）
        if ((lower.contains("新增") || lower.contains("添加") || lower.contains("创建") || lower.contains("新建")) &&
            (lower.contains("农田") || lower.contains("土地") || lower.contains("田地") || lower.contains("地块") || lower.contains("田"))) {
            
            String crop = extractCropName(userQuestion);
            String area = extractArea(userQuestion);
            String address = extractAddress(userQuestion);
            
            // 优先使用预设地点的建议名称
            String farmName = extractNewFarmName(userQuestion);
            if (address != null && !address.isEmpty()) {
                Map<String, Object> geoData = getPresetGeoLocation(address);
                if (geoData != null && geoData.get("suggestedName") != null) {
                    farmName = geoData.get("suggestedName").toString();
                }
            }
            
            StringBuilder advice = new StringBuilder();
            advice.append("好的，即将为您创建新农田。\n\n");
            advice.append("农田名称：").append(farmName).append("\n");
            advice.append("面积：").append(area).append("亩\n");
            if (!"作物".equals(crop)) {
                advice.append("作物：").append(crop).append("\n");
            }
            if (address != null && !address.isEmpty()) {
                advice.append("位置：").append(address).append("\n");
            }
            advice.append("\n确认后将创建该农田。");
            
            plan.setAdvice(advice.toString());
            
            AgentAction createAction = new AgentAction();
            createAction.setId("create_farm_" + System.currentTimeMillis());
            createAction.setType("create_farm");
            createAction.setTitle("创建农田：" + farmName);
            createAction.setDescription("新建一块名为【" + farmName + "】的农田");
            createAction.setRiskLevel("medium");
            
            Map<String, Object> params = new HashMap<>();
            params.put("name", farmName);
            params.put("area", area);
            params.put("crop", crop);
            if (address != null && !address.isEmpty()) {
                params.put("address", address);
            }
            createAction.setParams(params);
            
            actions.add(createAction);
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：导航意图（带我去、打开、看看 + 页面名称）
        if (lower.contains("带我") || lower.contains("打开") || lower.contains("跳转") || 
            lower.contains("进入") || lower.contains("去") || lower.contains("看看") || lower.contains("查看")) {
            
            String route = null;
            String title = null;
            
            // 3D地图/3D界面
            if (lower.contains("3d") || lower.contains("三维") || lower.contains("立体")) {
                route = "/farmmap3d";
                title = "3D农田地图";
            }
            // GIS地图/指挥地图
            else if (lower.contains("gis") || lower.contains("指挥") || lower.contains("卫星")) {
                route = "/farm-map-gaode";
                title = "GIS指挥地图";
            }
            // 地图（通用）
            else if (lower.contains("地图") && (lower.contains("田") || lower.contains("地"))) {
                route = "/farm-map-gaode";
                title = "农田地图";
            }
            // 大屏/数据大屏/环境监测
            else if (lower.contains("大屏") || lower.contains("监测") || lower.contains("环境")) {
                route = "/dashbordnew";
                title = "环境监测大屏";
            }
            // 农田管理/田地管理
            else if ((lower.contains("农田") || lower.contains("田地") || lower.contains("地块")) && 
                     (lower.contains("管理") || lower.contains("列表") || lower.contains("看看"))) {
                route = "/farmland";
                title = "农田管理";
            }
            // 销售/订单
            else if (lower.contains("销售") || lower.contains("订单")) {
                route = "/sale";
                title = "销售管理";
            }
            // 采购
            else if (lower.contains("采购") || lower.contains("进货")) {
                route = "/purchase";
                title = "采购管理";
            }
            // 库存
            else if (lower.contains("库存") || lower.contains("仓库")) {
                route = "/inventory";
                title = "库存管理";
            }
            // 果蔬识别
            else if (lower.contains("识别") || lower.contains("果蔬") || lower.contains("检测")) {
                route = "/fruit-detect";
                title = "果蔬识别";
            }
            
            if (route != null) {
                plan.setAdvice("好的，正在为您打开" + title + "...");
                
                AgentAction navAction = new AgentAction();
                navAction.setId("nav_" + System.currentTimeMillis());
                navAction.setType("navigate");
                navAction.setTitle(title);
                navAction.setDescription("打开" + title);
                navAction.setRoute(route);
                navAction.setRiskLevel("low");
                actions.add(navAction);
                
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 优先匹配：简单问候语
        if (lower.matches("^(你好|您好|hi|hello|嗨|哈喽|早上好|下午好|晚上好|早安|晚安|在吗|在不在|你在吗)[!！。？?]*$")) {
            List<Statistic> farms = statisticService.list();
            StringBuilder greeting = new StringBuilder();
            greeting.append("你好！我是您的智能农情助手。\n\n");
            greeting.append("目前系统共管理 ").append(farms.size()).append(" 块农田。\n\n");
            greeting.append("我可以帮您查询农田数据、控制设备、分析经营情况等。\n\n");
            greeting.append("请问您需要了解什么？");
            
            plan.setAdvice(greeting.toString());
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：感谢语
        if (lower.matches("^(谢谢|感谢|多谢|谢了|thx|thanks|thank you)[!！。]*$")) {
            plan.setAdvice("不客气！有任何农业问题随时问我 😊");
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：查看作物统计、种了什么、作物最多
        if ((lower.contains("作物") || lower.contains("种植") || lower.contains("种了") || lower.contains("种什么")) &&
            (lower.contains("什么") || lower.contains("哪些") || lower.contains("多少") || lower.contains("最多") || 
             lower.contains("统计") || lower.contains("看看") || lower.contains("查看"))) {
            
            List<Statistic> farms = statisticService.list();
            Map<String, Integer> cropCount = new LinkedHashMap<>();
            
            for (Statistic farm : farms) {
                String crop = farm.getCrop();
                if (crop != null && !crop.trim().isEmpty()) {
                    // 处理多作物（逗号分隔）
                    String[] crops = crop.split("[,，]");
                    for (String c : crops) {
                        c = c.trim();
                        if (!c.isEmpty()) {
                            cropCount.put(c, cropCount.getOrDefault(c, 0) + 1);
                        }
                    }
                }
            }
            
            StringBuilder advice = new StringBuilder();
            if (cropCount.isEmpty()) {
                advice.append("目前还没有作物种植记录。");
            } else {
                // 按数量排序
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(cropCount.entrySet());
                sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                
                String topCrop = sorted.get(0).getKey();
                int topCount = sorted.get(0).getValue();
                
                advice.append("目前种植最多的是").append(topCrop).append("，共有 ").append(topCount).append(" 块地。\n\n");
                
                if (sorted.size() > 1) {
                    advice.append("其他作物：");
                    for (int i = 1; i < sorted.size(); i++) {
                        Map.Entry<String, Integer> entry = sorted.get(i);
                        if (i > 1) advice.append("、");
                        advice.append(entry.getKey()).append(" ").append(entry.getValue()).append("块");
                    }
                    advice.append("。\n\n");
                }
                
                advice.append("总共 ").append(farms.size()).append(" 块农田，种植了 ").append(cropCount.size()).append(" 种作物。");
            }
            
            plan.setAdvice(advice.toString());
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：查看农田概况、有多少地、农田情况
        if ((lower.contains("农田") || lower.contains("地块") || lower.contains("土地") || lower.contains("田地")) &&
            (lower.contains("多少") || lower.contains("几块") || lower.contains("情况") || lower.contains("概况") || 
             lower.contains("统计") || lower.contains("看看") || lower.contains("有哪些"))) {
            
            List<Statistic> farms = statisticService.list();
            
            // 计算总面积
            double totalArea = farms.stream()
                .filter(f -> f.getArea() != null)
                .mapToDouble(f -> {
                    try { return Double.parseDouble(f.getArea()); } 
                    catch (Exception e) { return 0; }
                })
                .sum();
            
            StringBuilder advice = new StringBuilder();
            advice.append("您目前共有 ").append(farms.size()).append(" 块农田，总面积约 ").append(String.format("%.1f", totalArea)).append(" 亩。\n\n");
            
            if (!farms.isEmpty()) {
                int showCount = Math.min(5, farms.size());
                for (int i = 0; i < showCount; i++) {
                    Statistic farm = farms.get(i);
                    advice.append(farm.getFarm() != null ? farm.getFarm() : "未命名");
                    if (farm.getCrop() != null) {
                        advice.append("，种植").append(farm.getCrop());
                    }
                    if (farm.getArea() != null) {
                        advice.append("，").append(farm.getArea()).append("亩");
                    }
                    advice.append("。\n");
                }
                if (farms.size() > 5) {
                    advice.append("还有其他 ").append(farms.size() - 5).append(" 块农田。\n");
                }
            }
            
            plan.setAdvice(advice.toString());
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：录入位置、标记位置、设置地址
        if ((lower.contains("录入") || lower.contains("标记") || lower.contains("设置") || lower.contains("添加") || lower.contains("修改")) &&
            (lower.contains("位置") || lower.contains("地址") || lower.contains("坐标") || lower.contains("地图"))) {
            
            StringBuilder advice = new StringBuilder();
            advice.append("好的，我来帮您录入农田位置。\n\n");
            advice.append("请到农田管理页面，点击对应农田的【编辑】按钮，然后点击【在地图上标记】来设置位置。\n\n");
            advice.append("或者您可以告诉我要编辑哪块农田，我帮您跳转。");
            
            plan.setAdvice(advice.toString());
            
            // 添加导航动作
            AgentAction navAction = new AgentAction();
            navAction.setId("nav_to_farmland_" + System.currentTimeMillis());
            navAction.setType("navigate");
            navAction.setTitle("农田管理");
            navAction.setDescription("打开农田管理页面进行位置录入");
            navAction.setRoute("/farmland");
            navAction.setRiskLevel("low");
            actions.add(navAction);
            
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：删除农田
        if ((lower.contains("删除") || lower.contains("移除") || lower.contains("去掉")) &&
            (lower.contains("农田") || lower.contains("土地") || lower.contains("田地") || lower.contains("地块"))) {
            
            String targetFarm = extractFarmName(userQuestion);
            List<Statistic> farms = statisticService.list();
            
            if (targetFarm != null) {
                Statistic matchedFarm = null;
                for (Statistic farm : farms) {
                    if (farm.getFarm() != null && 
                        (farm.getFarm().contains(targetFarm) || matchFarmNumber(farm.getFarm(), targetFarm))) {
                        matchedFarm = farm;
                        break;
                    }
                }
                
                if (matchedFarm != null) {
                    plan.setAdvice("⚠️ 您确定要删除【" + matchedFarm.getFarm() + "】吗？\n\n此操作不可撤销，该农田的所有数据将被永久删除。");
                    
                    AgentAction deleteAction = new AgentAction();
                    deleteAction.setId("delete_farm_" + matchedFarm.getId());
                    deleteAction.setType("delete_farm");
                    deleteAction.setTitle("删除农田：" + matchedFarm.getFarm());
                    deleteAction.setDescription("永久删除【" + matchedFarm.getFarm() + "】及其所有数据");
                    deleteAction.setRiskLevel("high");
                    
                    Map<String, Object> params = new HashMap<>();
                    params.put("farmId", matchedFarm.getId());
                    params.put("farmName", matchedFarm.getFarm());
                    deleteAction.setParams(params);
                    
                    actions.add(deleteAction);
                } else {
                    plan.setAdvice("未找到【" + targetFarm + "】这块农田。\n\n您可以说：删除1号田、删除XX农田 等。");
                }
            } else {
                plan.setAdvice("请告诉我要删除哪块农田，例如：删除1号田、删除测试农田 等。");
            }
            
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：利润、收入、赚钱相关查询
        if (lower.contains("利润") || lower.contains("净利") || lower.contains("盈利") || lower.contains("赚") || 
            lower.contains("可以赚") || lower.contains("能赚") || lower.contains("今年收入") ||
            (lower.contains("收入") && (lower.contains("多少") || lower.contains("支出"))) || 
            (lower.contains("采购") && lower.contains("多少"))) {
            // 计算利润：收入 - 支出
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            
            // 获取销售收入
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                for (Sales sale : salesList) {
                    if (sale.getPrice() != null && sale.getNumber() != null) {
                        totalIncome = totalIncome.add(sale.getPrice().multiply(new BigDecimal(sale.getNumber())));
                    }
                }
            }
            
            // 获取采购支出
            if (purchaseService != null) {
                List<Purchase> purchaseList = purchaseService.list();
                for (Purchase purchase : purchaseList) {
                    if (purchase.getPrice() != null && purchase.getNumber() != null) {
                        totalCost = totalCost.add(purchase.getPrice().multiply(new BigDecimal(purchase.getNumber())));
                    }
                }
            }
            
            BigDecimal profit = totalIncome.subtract(totalCost);
            StringBuilder advice = new StringBuilder();
            advice.append("根据系统财务数据分析：\n\n");
            advice.append("销售总收入：¥").append(totalIncome.setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
            advice.append("采购总支出：¥").append(totalCost.setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
            advice.append("净利润：¥").append(profit.setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n\n");
            
            if (profit.compareTo(BigDecimal.ZERO) > 0) {
                advice.append("经营状况良好，处于盈利状态。");
            } else if (profit.compareTo(BigDecimal.ZERO) < 0) {
                advice.append("当前处于亏损状态，建议优化采购成本或提升销售收入。");
            } else {
                advice.append("收支平衡。");
            }
            
            plan.setAdvice(advice.toString());
            plan.setActions(actions);
            return plan;
        }
        
        // 优先匹配：作物种植建议（温度适合种XX吗、能种XX吗）
        if ((lower.contains("适合") || lower.contains("能种") || lower.contains("可以种") || lower.contains("能不能种")) &&
            (lower.contains("种") || lower.contains("植") || lower.contains("养"))) {
            
            // 提取作物名称
            String crop = extractCropName(userQuestion);
            
            // 获取当前环境数据
            List<Statistic> farms = statisticService.list();
            if (!farms.isEmpty()) {
                // 计算平均温度和湿度
                double avgTemp = farms.stream()
                    .filter(f -> f.getTemperature() != null)
                    .mapToDouble(f -> f.getTemperature().doubleValue())
                    .average()
                    .orElse(25.0);
                double avgHumidity = farms.stream()
                    .filter(f -> f.getSoilhumidity() != null)
                    .mapToDouble(f -> f.getSoilhumidity().doubleValue())
                    .average()
                    .orElse(50.0);
                
                StringBuilder advice = new StringBuilder();
                advice.append("根据当前环境数据分析：\n\n");
                advice.append("🌡️ 平均温度：").append(String.format("%.1f", avgTemp)).append("°C\n");
                advice.append("💧 平均土壤湿度：").append(String.format("%.0f", avgHumidity)).append("%\n\n");
                
                // 根据作物给出建议
                String cropAdvice = getCropGrowthAdvice(crop, avgTemp, avgHumidity);
                advice.append(cropAdvice);
                
                plan.setAdvice(advice.toString());
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 采购数据查询
        if (lower.contains("采购") && !lower.contains("跳转") && !lower.contains("界面") && !lower.contains("页面")) {
            if (purchaseService != null) {
                List<Purchase> purchaseList = purchaseService.list();
                BigDecimal totalCost = BigDecimal.ZERO;
                
                StringBuilder advice = new StringBuilder();
                advice.append("根据采购记录，共有 ").append(purchaseList.size()).append(" 条采购记录：\n\n");
                
                int showCount = Math.min(5, purchaseList.size());
                for (int i = 0; i < showCount; i++) {
                    Purchase p = purchaseList.get(i);
                    advice.append(i + 1).append(". ").append(p.getProduct());
                    if (p.getPrice() != null && p.getNumber() != null) {
                        BigDecimal cost = p.getPrice().multiply(new BigDecimal(p.getNumber()));
                        totalCost = totalCost.add(cost);
                        advice.append(" - ¥").append(cost.setScale(2, BigDecimal.ROUND_HALF_UP));
                    }
                    advice.append("\n");
                }
                
                if (purchaseList.size() > 5) {
                    advice.append("...(还有").append(purchaseList.size() - 5).append("条记录)\n");
                }
                advice.append("\n采购总支出：¥").append(totalCost.setScale(2, BigDecimal.ROUND_HALF_UP));
                
                plan.setAdvice(advice.toString());
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 销售/收入数据查询
        if ((lower.contains("销售") || lower.contains("收入") || lower.contains("账单")) && 
            !lower.contains("跳转") && !lower.contains("界面") && !lower.contains("页面")) {
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                BigDecimal totalIncome = BigDecimal.ZERO;
                
                StringBuilder advice = new StringBuilder();
                advice.append("根据销售记录，共有 ").append(salesList.size()).append(" 条销售记录：\n\n");
                
                int showCount = Math.min(5, salesList.size());
                for (int i = 0; i < showCount; i++) {
                    Sales s = salesList.get(i);
                    advice.append(i + 1).append(". ").append(s.getProduct());
                    if (s.getPrice() != null && s.getNumber() != null) {
                        BigDecimal income = s.getPrice().multiply(new BigDecimal(s.getNumber()));
                        totalIncome = totalIncome.add(income);
                        advice.append(" - ¥").append(income.setScale(2, BigDecimal.ROUND_HALF_UP));
                    }
                    advice.append("\n");
                }
                
                if (salesList.size() > 5) {
                    advice.append("...(还有").append(salesList.size() - 5).append("条记录)\n");
                }
                advice.append("\n销售总收入：¥").append(totalIncome.setScale(2, BigDecimal.ROUND_HALF_UP));
                
                plan.setAdvice(advice.toString());
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 农田数量查询（避免与上面的利润查询冲突）
        if ((lower.contains("几块") || lower.contains("有哪些")) && (lower.contains("农场") || lower.contains("农田"))) {
            // 查询数据库获取农田信息
            List<Statistic> farms = statisticService.list();
            if (farms != null && !farms.isEmpty()) {
                StringBuilder advice = new StringBuilder();
                advice.append("根据系统数据，您目前共有 ").append(farms.size()).append(" 块农田：\n\n");
                for (int i = 0; i < farms.size(); i++) {
                    Statistic farm = farms.get(i);
                    advice.append(i + 1).append(". ").append(farm.getFarm());
                    if (farm.getCrop() != null) {
                        advice.append(" - 种植").append(farm.getCrop());
                    }
                    if (farm.getArea() != null) {
                        advice.append("，面积").append(farm.getArea()).append("亩");
                    }
                    if (farm.getSoilhumidity() != null) {
                        advice.append("，土壤湿度").append(farm.getSoilhumidity()).append("%");
                    }
                    advice.append("\n");
                }
                plan.setAdvice(advice.toString());
                plan.setActions(actions);
                return plan;
            } else {
                plan.setAdvice("系统中暂无农田数据，请先添加农田信息。");
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 查询需要浇水的农田
        if (lower.contains("需要浇水") || lower.contains("需要灌溉") || lower.contains("哪些") && (lower.contains("浇水") || lower.contains("灌溉"))) {
            List<Statistic> farms = statisticService.list();
            List<Statistic> needWater = farms.stream()
                .filter(f -> f.getSoilhumidity() != null && f.getSoilhumidity() < 30)
                .collect(Collectors.toList());
            
            if (!needWater.isEmpty()) {
                StringBuilder advice = new StringBuilder();
                advice.append("我看了一下，这几块地有点干了，需要浇浇水：\n\n");
                for (Statistic farm : needWater) {
                    advice.append("🌾 ").append(farm.getFarm()).append("：土壤湿度只有 ")
                          .append(farm.getSoilhumidity()).append("%，有点缺水了\n");
                }
                plan.setAdvice(advice.toString());
                plan.setActions(actions);
                return plan;
            } else {
                plan.setAdvice("我看了一下，所有农田的土壤湿度都挺好的，暂时不用浇水哦~");
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 查询缺水/灌溉相关
        if (lower.contains("缺水") || lower.contains("干旱")) {
            List<Statistic> farms = statisticService.list();
            List<Statistic> needWater = farms.stream()
                .filter(f -> f.getSoilhumidity() != null && f.getSoilhumidity() < 30)
                .collect(Collectors.toList());
            
            if (!needWater.isEmpty()) {
                StringBuilder advice = new StringBuilder();
                advice.append("发现有 ").append(needWater.size()).append(" 块地有点干了：\n\n");
                for (Statistic farm : needWater) {
                    advice.append("🌾 ").append(farm.getFarm()).append("：湿度只有 ")
                          .append(farm.getSoilhumidity()).append("%（正常要30%以上）\n");
                }
                advice.append("\n建议赶紧浇浇水，别让庄稼渴着了！");
                plan.setAdvice(advice.toString());
                
                // 添加灌溉动作
                AgentAction irrigation = new AgentAction();
                irrigation.setId("irrigation_on");
                irrigation.setType("irrigation_on");
                irrigation.setTitle("开启灌溉");
                irrigation.setDescription("为缺水农田开启灌溉");
                irrigation.setRiskLevel("medium");
                actions.add(irrigation);
            } else {
                plan.setAdvice("放心吧，所有地的湿度都正常，暂时不用浇水。");
            }
            plan.setActions(actions);
            return plan;
        }
        
        // 优先处理：给指定农田浇水的命令
        if ((lower.contains("浇水") || lower.contains("灌溉")) && 
            (lower.contains("给") || lower.contains("帮") || lower.contains("开"))) {
            
            // 尝试提取农田名称（支持多种格式：二号田、2号田、田地2、农田二等）
            String targetFarm = extractFarmName(userQuestion);
            List<Statistic> farms = statisticService.list();
            
            if (targetFarm != null) {
                // 查找匹配的农田
                Statistic matchedFarm = null;
                for (Statistic farm : farms) {
                    if (farm.getFarm() != null && 
                        (farm.getFarm().contains(targetFarm) || 
                         matchFarmNumber(farm.getFarm(), targetFarm))) {
                        matchedFarm = farm;
                        break;
                    }
                }
                
                if (matchedFarm != null) {
                    StringBuilder advice = new StringBuilder();
                    advice.append("好的，为您准备给【").append(matchedFarm.getFarm()).append("】浇水。\n");
                    advice.append("当前土壤湿度：").append(matchedFarm.getSoilhumidity()).append("%\n");
                    if (matchedFarm.getSoilhumidity() != null && matchedFarm.getSoilhumidity() >= 30) {
                        advice.append("（提示：当前湿度正常，确定要浇水吗？）");
                    }
                    plan.setAdvice(advice.toString());
                    
                    AgentAction irrigation = new AgentAction();
                    irrigation.setId("irrigation_on");
                    irrigation.setType("irrigation_on");
                    irrigation.setTitle("给" + matchedFarm.getFarm() + "浇水");
                    irrigation.setDescription("开启灌溉系统为 " + matchedFarm.getFarm() + " 浇水");
                    irrigation.setRiskLevel("medium");
                    actions.add(irrigation);
                    plan.setActions(actions);
                    return plan;
                } else {
                    plan.setAdvice("抱歉，没找到【" + targetFarm + "】这块地。\n您可以说：帮我给1号田浇水、给二号田地灌溉 等。");
                    plan.setActions(actions);
                    return plan;
                }
            } else {
                // 没有指定具体农田，检查哪些需要浇水
                List<Statistic> needWater = farms.stream()
                    .filter(f -> f.getSoilhumidity() != null && f.getSoilhumidity() < 30)
                    .collect(Collectors.toList());
                
                if (!needWater.isEmpty()) {
                    StringBuilder advice = new StringBuilder();
                    advice.append("检测到以下农田需要浇水：\n");
                    for (Statistic farm : needWater) {
                        advice.append("• ").append(farm.getFarm()).append("（湿度").append(farm.getSoilhumidity()).append("%）\n");
                    }
                    advice.append("\n确认后将开启灌溉系统。");
                    plan.setAdvice(advice.toString());
                    
                    AgentAction irrigation = new AgentAction();
                    irrigation.setId("irrigation_on");
                    irrigation.setType("irrigation_on");
                    irrigation.setTitle("开启灌溉");
                    irrigation.setDescription("为缺水农田开启灌溉");
                    irrigation.setRiskLevel("medium");
                    actions.add(irrigation);
                } else {
                    plan.setAdvice("所有农田湿度正常，暂时不需要浇水。\n如需强制浇水，请指定农田名称，如：帮我给1号田浇水");
                }
                plan.setActions(actions);
                return plan;
            }
        }
        
        // 默认兜底逻辑
        plan.setAdvice("好的，我来帮您处理这个请求。");
        if (lower.contains("浇水") || lower.contains("灌溉")) {
            // 已在上面处理，这里是兜底
            plan.setAdvice("您想给哪块地浇水呢？可以说：帮我给1号田浇水");
        }
        else if (lower.contains("关水") || (lower.contains("停") && lower.contains("水"))) {
            plan.setAdvice("好的，帮您准备关水操作了。");
            AgentAction irrigationOff = new AgentAction();
            irrigationOff.setId("irrigation_off");
            irrigationOff.setType("irrigation_off");
            irrigationOff.setTitle("关闭灌溉");
            irrigationOff.setDescription("关闭水泵");
            irrigationOff.setRiskLevel("low");
            actions.add(irrigationOff);
        }
        else if (lower.contains("灯") || lower.contains("补光")) {
            plan.setAdvice("好的，帮您准备开灯操作了。");
            AgentAction led = new AgentAction();
            led.setId("led_on");
            led.setType("led_on");
            led.setTitle("开启补光灯");
            led.setDescription("补光灯开启以提升光照");
            led.setRiskLevel("low");
            actions.add(led);
        }
        // 智能匹配页面跳转
        else if (lower.contains("果蔬") || lower.contains("双检") || lower.contains("检测") || lower.contains("识别")) {
            plan.setAdvice("好的，为您打开果蔬双检分析界面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_fruit_detect");
            nav.setType("navigate");
            nav.setTitle("跳转到果蔬双检分析");
            nav.setDescription("进入果蔬双检分析界面");
            nav.setRoute("/fruit-detect");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("3d") || lower.contains("沙盘") || lower.contains("三维")) {
            plan.setAdvice("好的，为您打开3D沙盘界面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_3d");
            nav.setType("navigate");
            nav.setTitle("跳转到3D沙盘");
            nav.setDescription("进入农场3D沙盘界面");
            nav.setRoute("/farmmap3d");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("aether") || lower.contains("传感") || lower.contains("实时数据")) {
            plan.setAdvice("好的，为您打开传感器数据监控界面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_aether");
            nav.setType("navigate");
            nav.setTitle("跳转到Aether数据监控");
            nav.setDescription("进入Aether传感器数据监控");
            nav.setRoute("/aether-monitor");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("采购") || lower.contains("物资")) {
            plan.setAdvice("好的，为您打开物资采购管理页面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_purchase");
            nav.setType("navigate");
            nav.setTitle("跳转到物资采购");
            nav.setDescription("进入物资采购管理页面");
            nav.setRoute("/purchase");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("收入") || lower.contains("销售") || lower.contains("账单") || lower.contains("出售")) {
            plan.setAdvice("好的，为您打开销售账单页面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_sales");
            nav.setType("navigate");
            nav.setTitle("跳转到出售账单");
            nav.setDescription("进入出售账单查看收入");
            nav.setRoute("/sales");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("经营") || lower.contains("分析") || lower.contains("效益")) {
            plan.setAdvice("好的，为您打开经营分析页面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_analysis");
            nav.setType("navigate");
            nav.setTitle("跳转到经营分析");
            nav.setDescription("进入经营效益分析页面");
            nav.setRoute("/business-analysis");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("库存") || lower.contains("仓库")) {
            plan.setAdvice("好的，为您打开库存管理页面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_inventory");
            nav.setType("navigate");
            nav.setTitle("跳转到物资库存");
            nav.setDescription("进入物资库存管理页面");
            nav.setRoute("/inventory");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("大屏") || lower.contains("可视化")) {
            plan.setAdvice("好的，为您打开可视化大屏。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_bigscreen");
            nav.setType("navigate");
            nav.setTitle("跳转到可视化大屏");
            nav.setDescription("进入全局可视化大屏");
            nav.setRoute("/bigscreen");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("监测") || lower.contains("环境") || lower.contains("温度") || lower.contains("湿度")) {
            plan.setAdvice("好的，为您打开环境监测大屏。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_dashbord");
            nav.setType("navigate");
            nav.setTitle("跳转到环境监测");
            nav.setDescription("进入环境监测大屏");
            nav.setRoute("/dashbordnew");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("农田") || lower.contains("统计")) {
            plan.setAdvice("好的，为您打开农田信息统计页面。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_statistic");
            nav.setType("navigate");
            nav.setTitle("跳转到农田信息");
            nav.setDescription("进入农田信息统计页面");
            nav.setRoute("/statistic");
            nav.setRiskLevel("low");
            actions.add(nav);
        } else if (lower.contains("地图") || lower.contains("高德")) {
            plan.setAdvice("好的，为您打开地理地图。");
            AgentAction nav = new AgentAction();
            nav.setId("navigate_map");
            nav.setType("navigate");
            nav.setTitle("跳转到地理地图");
            nav.setDescription("进入农场地理地图");
            nav.setRoute("/farm-map-gaode");
            nav.setRiskLevel("low");
            actions.add(nav);
        }
        if (actions.isEmpty()) {
            plan.setAdvice("不好意思，我没太明白您的意思。您可以问我：\n• 哪块地需要浇水\n• 今年赚了多少钱\n• 帮我给X号田浇水\n• 打开XX页面");
        }

        plan.setActions(actions);
        return plan;
    }
    
    // ==================== 新增工具实现：全系统数据读取和深度分析 ====================
    
    /**
     * 工具10: 获取用户统计数据
     */
    private String getUserStatistics() {
        JSONObject result = new JSONObject();
        try {
            if (userService == null) {
                return "{\"error\": \"用户服务不可用\"}";
            }
            
            List<?> users = userService.list();
            int totalUsers = users.size();
            
            // 角色分布统计
            Map<String, Long> roleDistribution = new HashMap<>();
            if (roleService != null) {
                List<?> roles = roleService.list();
                for (Object role : roles) {
                    try {
                        java.lang.reflect.Method getRoleName = role.getClass().getMethod("getRole");
                        String roleName = (String) getRoleName.invoke(role);
                        roleDistribution.put(roleName, roleDistribution.getOrDefault(roleName, 0L) + 1);
                    } catch (Exception ignored) {}
                }
            }
            
            result.put("total_users", totalUsers);
            result.put("role_distribution", roleDistribution);
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 工具11: 获取系统整体概览
     */
    private String getSystemOverview() {
        JSONObject result = new JSONObject();
        try {
            // 农田统计
            int totalFarms = 0;
            if (statisticService != null) {
                totalFarms = statisticService.list().size();
            }
            
            // 用户统计
            int totalUsers = 0;
            if (userService != null) {
                totalUsers = userService.list().size();
            }
            
            // 销售统计
            int totalSales = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                totalSales = salesList.size();
                for (Sales sale : salesList) {
                    if (sale.getPrice() != null && sale.getNumber() != null) {
                        totalRevenue = totalRevenue.add(sale.getPrice().multiply(new BigDecimal(sale.getNumber())));
                    }
                }
            }
            
            // 库存统计
            int totalInventoryItems = 0;
            int lowStockItems = 0;
            if (inventoryService != null) {
                List<Inventory> inventoryList = inventoryService.list();
                totalInventoryItems = inventoryList.size();
                for (Inventory inv : inventoryList) {
                    if (inv.getNumber() != null && inv.getNumber() < 10) {
                        lowStockItems++;
                    }
                }
            }
            
            // 公告统计
            int totalNotices = 0;
            if (noticeService != null) {
                totalNotices = noticeService.list().size();
            }
            
            result.put("total_farms", totalFarms);
            result.put("total_users", totalUsers);
            result.put("total_sales_orders", totalSales);
            result.put("total_revenue", totalRevenue.setScale(2, BigDecimal.ROUND_HALF_UP));
            result.put("total_inventory_items", totalInventoryItems);
            result.put("low_stock_items", lowStockItems);
            result.put("total_notices", totalNotices);
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取系统概览失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 工具12: 获取在线销售数据
     */
    private String getOnlineSaleData() {
        JSONObject result = new JSONObject();
        try {
            if (onlineSaleService == null) {
                return "{\"error\": \"在线销售服务不可用\"}";
            }
            
            List<?> onlineSales = onlineSaleService.list();
            int totalOrders = onlineSales.size();
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            JSONArray salesDetails = new JSONArray();
            for (Object sale : onlineSales) {
                try {
                    JSONObject detail = new JSONObject();
                    java.lang.reflect.Method getProduct = sale.getClass().getMethod("getProduct");
                    java.lang.reflect.Method getPrice = sale.getClass().getMethod("getPrice");
                    java.lang.reflect.Method getNumber = sale.getClass().getMethod("getNumber");
                    
                    String product = (String) getProduct.invoke(sale);
                    BigDecimal price = (BigDecimal) getPrice.invoke(sale);
                    Integer number = (Integer) getNumber.invoke(sale);
                    
                    if (price != null && number != null) {
                        BigDecimal amount = price.multiply(new BigDecimal(number));
                        totalAmount = totalAmount.add(amount);
                        detail.put("product", product);
                        detail.put("price", price);
                        detail.put("number", number);
                        detail.put("amount", amount);
                        salesDetails.add(detail);
                    }
                } catch (Exception ignored) {}
            }
            
            result.put("total_orders", totalOrders);
            result.put("total_amount", totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
            result.put("sales_details", salesDetails);
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取在线销售数据失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 工具13: 经营健康度评分
     */
    private String getBusinessHealthScore() {
        JSONObject result = new JSONObject();
        try {
            int totalScore = 0;
            JSONArray diagnostics = new JSONArray();
            
            // 1. 利润率评分 (30分)
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                for (Sales sale : salesList) {
                    if (sale.getPrice() != null && sale.getNumber() != null) {
                        totalIncome = totalIncome.add(sale.getPrice().multiply(new BigDecimal(sale.getNumber())));
                    }
                }
            }
            
            if (purchaseService != null) {
                List<Purchase> purchaseList = purchaseService.list();
                for (Purchase purchase : purchaseList) {
                    if (purchase.getPrice() != null && purchase.getNumber() != null) {
                        totalCost = totalCost.add(purchase.getPrice().multiply(new BigDecimal(purchase.getNumber())));
                    }
                }
            }
            
            BigDecimal profitRate = BigDecimal.ZERO;
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = totalIncome.subtract(totalCost).divide(totalIncome, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
            }
            
            int profitScore = 0;
            String profitDiagnosis = "";
            if (profitRate.compareTo(new BigDecimal(50)) >= 0) {
                profitScore = 30;
                profitDiagnosis = "利润率" + profitRate.setScale(2, BigDecimal.ROUND_HALF_UP) + "%，非常健康";
            } else if (profitRate.compareTo(new BigDecimal(30)) >= 0) {
                profitScore = 20;
                profitDiagnosis = "利润率" + profitRate.setScale(2, BigDecimal.ROUND_HALF_UP) + "%，良好";
            } else if (profitRate.compareTo(BigDecimal.ZERO) > 0) {
                profitScore = 10;
                profitDiagnosis = "利润率" + profitRate.setScale(2, BigDecimal.ROUND_HALF_UP) + "%，偏低，建议优化成本";
            } else {
                profitScore = 0;
                profitDiagnosis = "当前亏损，需要紧急改善经营策略";
            }
            totalScore += profitScore;
            diagnostics.add("【利润率】" + profitDiagnosis + " (" + profitScore + "/30分)");
            
            // 2. 库存管理评分 (25分)
            int inventoryScore = 25;
            int lowStockCount = 0;
            int totalInventory = 0;
            
            if (inventoryService != null) {
                List<Inventory> inventoryList = inventoryService.list();
                totalInventory = inventoryList.size();
                for (Inventory inv : inventoryList) {
                    if (inv.getNumber() != null && inv.getNumber() < 10) {
                        lowStockCount++;
                    }
                }
                
                if (totalInventory > 0) {
                    double lowStockRate = (double) lowStockCount / totalInventory;
                    if (lowStockRate > 0.3) {
                        inventoryScore = 10;
                        diagnostics.add("【库存管理】低库存物品占比" + String.format("%.1f%%", lowStockRate * 100) + "，需要补货 (" + inventoryScore + "/25分)");
                    } else if (lowStockRate > 0.1) {
                        inventoryScore = 18;
                        diagnostics.add("【库存管理】部分物品库存偏低，建议关注 (" + inventoryScore + "/25分)");
                    } else {
                        diagnostics.add("【库存管理】库存充足，管理良好 (" + inventoryScore + "/25分)");
                    }
                } else {
                    diagnostics.add("【库存管理】暂无库存数据 (" + inventoryScore + "/25分)");
                }
            }
            totalScore += inventoryScore;
            
            // 3. 资金流动性评分 (25分)
            int cashFlowScore = 0;
            if (totalIncome.compareTo(totalCost) > 0) {
                cashFlowScore = 25;
                diagnostics.add("【资金流】收入大于支出，资金流健康 (" + cashFlowScore + "/25分)");
            } else if (totalIncome.compareTo(totalCost) == 0) {
                cashFlowScore = 15;
                diagnostics.add("【资金流】收支平衡，建议提升盈利能力 (" + cashFlowScore + "/25分)");
            } else {
                cashFlowScore = 5;
                diagnostics.add("【资金流】支出大于收入，需要控制成本或增加收入 (" + cashFlowScore + "/25分)");
            }
            totalScore += cashFlowScore;
            
            // 4. 农田环境评分 (20分)
            int envScore = 20;
            int abnormalFarms = 0;
            int totalFarms = 0;
            
            if (statisticService != null) {
                List<Statistic> farms = statisticService.list();
                totalFarms = farms.size();
                for (Statistic farm : farms) {
                    if (farm.getSoilhumidity() != null && farm.getSoilhumidity() < 30) abnormalFarms++;
                    if (farm.getTemperature() != null && 
                        (farm.getTemperature().compareTo(new BigDecimal(10)) < 0 || 
                         farm.getTemperature().compareTo(new BigDecimal(35)) > 0)) abnormalFarms++;
                }
                
                if (totalFarms > 0) {
                    double abnormalRate = (double) abnormalFarms / totalFarms;
                    if (abnormalRate > 0.3) {
                        envScore = 8;
                        diagnostics.add("【农田环境】多块农田环境异常，需要及时处理 (" + envScore + "/20分)");
                    } else if (abnormalRate > 0) {
                        envScore = 15;
                        diagnostics.add("【农田环境】个别农田需要关注 (" + envScore + "/20分)");
                    } else {
                        diagnostics.add("【农田环境】所有农田环境良好 (" + envScore + "/20分)");
                    }
                } else {
                    diagnostics.add("【农田环境】暂无农田数据 (" + envScore + "/20分)");
                }
            }
            totalScore += envScore;
            
            // 综合评价
            String overallRating = "";
            String suggestion = "";
            if (totalScore >= 85) {
                overallRating = "优秀";
                suggestion = "农场经营状况非常好，保持当前策略，继续优化细节。";
            } else if (totalScore >= 70) {
                overallRating = "良好";
                suggestion = "农场经营整体健康，部分方面有提升空间。";
            } else if (totalScore >= 50) {
                overallRating = "中等";
                suggestion = "农场经营存在一些问题，建议重点优化利润率和库存管理。";
            } else {
                overallRating = "需改善";
                suggestion = "农场经营面临较大挑战，建议尽快制定改善计划，优先解决资金流和利润问题。";
            }
            
            result.put("total_score", totalScore);
            result.put("max_score", 100);
            result.put("rating", overallRating);
            result.put("diagnostics", diagnostics);
            result.put("suggestion", suggestion);
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("计算经营健康度失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 工具14: 趋势分析
     */
    private String getTrendAnalysis() {
        JSONObject result = new JSONObject();
        try {
            JSONArray insights = new JSONArray();
            
            // 销售趋势分析
            if (salesService != null) {
                List<Sales> salesList = salesService.list();
                if (salesList.size() > 3) {
                    insights.add("销售记录共" + salesList.size() + "条，业务活跃度较好");
                    
                    // 简单的趋势判断：最近的销售额
                    BigDecimal recentSales = BigDecimal.ZERO;
                    int recentCount = Math.min(3, salesList.size());
                    for (int i = salesList.size() - recentCount; i < salesList.size(); i++) {
                        Sales sale = salesList.get(i);
                        if (sale.getPrice() != null && sale.getNumber() != null) {
                            recentSales = recentSales.add(sale.getPrice().multiply(new BigDecimal(sale.getNumber())));
                        }
                    }
                    insights.add("近期销售额约¥" + recentSales.setScale(2, BigDecimal.ROUND_HALF_UP) + "，建议继续扩大销售渠道");
                } else {
                    insights.add("销售记录较少，建议加强产品销售和市场推广");
                }
            }
            
            // 库存趋势分析
            if (inventoryService != null) {
                List<Inventory> inventoryList = inventoryService.list();
                int lowStock = 0;
                for (Inventory inv : inventoryList) {
                    if (inv.getNumber() != null && inv.getNumber() < 10) lowStock++;
                }
                
                if (lowStock > 0) {
                    insights.add("当前有" + lowStock + "项物资库存不足，建议及时补充避免影响生产");
                } else {
                    insights.add("库存充足，物资管理良好");
                }
            }
            
            // 采购成本趋势
            if (purchaseService != null) {
                List<Purchase> purchaseList = purchaseService.list();
                BigDecimal totalPurchase = BigDecimal.ZERO;
                for (Purchase p : purchaseList) {
                    if (p.getPrice() != null && p.getNumber() != null) {
                        totalPurchase = totalPurchase.add(p.getPrice().multiply(new BigDecimal(p.getNumber())));
                    }
                }
                insights.add("累计采购支出¥" + totalPurchase.setScale(2, BigDecimal.ROUND_HALF_UP) + "，建议对比市场价格优化采购渠道");
            }
            
            // 预测建议
            insights.add("【预测建议】根据当前数据，建议：1) 保持稳定的销售节奏 2) 提前规划物资采购避免缺货 3) 关注农田环境变化及时调整");
            
            result.put("insights", insights);
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("趋势分析失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 工具15: 综合分析报告
     */
    private String getComprehensiveReport() {
        JSONObject result = new JSONObject();
        try {
            // 整合所有维度的数据
            JSONObject financialData = JSONUtil.parseObj(getProfitAnalysis());
            JSONObject systemData = JSONUtil.parseObj(getSystemOverview());
            JSONObject healthData = JSONUtil.parseObj(getBusinessHealthScore());
            JSONObject trendData = JSONUtil.parseObj(getTrendAnalysis());
            
            result.put("financial_analysis", financialData);
            result.put("system_overview", systemData);
            result.put("health_score", healthData);
            result.put("trend_analysis", trendData);
            
            // 生成综合结论
            JSONArray conclusions = new JSONArray();
            conclusions.add("【财务状况】" + financialData.getStr("advice", "数据获取中"));
            conclusions.add("【运营概况】系统管理" + systemData.getInt("total_farms", 0) + "块农田，" + 
                          systemData.getInt("total_users", 0) + "个用户，总销售额¥" + 
                          systemData.get("total_revenue"));
            conclusions.add("【健康评分】经营健康度" + healthData.getInt("total_score", 0) + "分，评级：" + 
                          healthData.getStr("rating", "未知"));
            conclusions.add("【发展建议】" + healthData.getStr("suggestion", "持续优化经营策略"));
            
            result.put("comprehensive_conclusion", conclusions);
            result.put("report_time", new Date().toString());
            result.put("status", "success");
            
            return result.toString();
        } catch (Exception e) {
            log.error("生成综合报告失败", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    // ==================== 农田名称解析辅助方法 ====================
    
    /**
     * 从用户输入中提取农田名称/编号
     * 支持格式：二号田、2号田、田地2、农田二、1号、一号 等
     */
    private String extractFarmName(String input) {
        if (input == null) return null;
        
        // 中文数字映射
        Map<String, String> chineseNumbers = new LinkedHashMap<>();
        chineseNumbers.put("一", "1");
        chineseNumbers.put("二", "2");
        chineseNumbers.put("三", "3");
        chineseNumbers.put("四", "4");
        chineseNumbers.put("五", "5");
        chineseNumbers.put("六", "6");
        chineseNumbers.put("七", "7");
        chineseNumbers.put("八", "8");
        chineseNumbers.put("九", "9");
        chineseNumbers.put("十", "10");
        
        // 正则匹配多种格式
        java.util.regex.Pattern[] patterns = {
            // 匹配 "X号田" "X号田地" "X号农田"
            java.util.regex.Pattern.compile("([一二三四五六七八九十\\d]+)\\s*号\\s*(田|田地|农田)?"),
            // 匹配 "田地X" "农田X"
            java.util.regex.Pattern.compile("(田地|农田|田)\\s*([一二三四五六七八九十\\d]+)"),
            // 匹配 "第X块"
            java.util.regex.Pattern.compile("第\\s*([一二三四五六七八九十\\d]+)\\s*(块|号)?"),
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String number = matcher.group(1);
                if (number == null && matcher.groupCount() > 1) {
                    number = matcher.group(2);
                }
                if (number != null) {
                    // 转换中文数字为阿拉伯数字
                    for (Map.Entry<String, String> entry : chineseNumbers.entrySet()) {
                        number = number.replace(entry.getKey(), entry.getValue());
                    }
                    return number;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从用户输入中提取新农田名称
     */
    private String extractNewFarmName(String input) {
        if (input == null) return "新农田";
        
        // 尝试匹配 "叫XX" "名字叫XX" "命名为XX"
        java.util.regex.Pattern[] patterns = {
            java.util.regex.Pattern.compile("(?:叫|名字叫|命名为|名为|取名)\\s*[\"'「]?([\\u4e00-\\u9fa5\\w]+)[\"'」]?"),
            java.util.regex.Pattern.compile("[\"'「]([\\u4e00-\\u9fa5\\w]+)[\"'」]\\s*(?:农田|土地|田地)?"),
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String name = matcher.group(1);
                if (name != null && name.length() >= 2 && name.length() <= 20) {
                    return name;
                }
            }
        }
        
        // 生成默认名称
        List<Statistic> farms = statisticService.list();
        int nextNum = farms.size() + 1;
        return nextNum + "号田";
    }
    
    /**
     * 从用户输入中提取面积
     */
    private String extractArea(String input) {
        if (input == null) return "100";
        
        // 匹配数字+亩
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:亩|mu|平方米|㎡)?");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "100";
    }
    
    /**
     * 从用户输入中提取作物名称
     */
    private String extractCropName(String input) {
        if (input == null) return "作物";
        
        // 常见作物关键词
        String[] crops = {"草莓", "番茄", "西红柿", "黄瓜", "辣椒", "茄子", "白菜", "生菜", 
                          "玉米", "水稻", "小麦", "土豆", "红薯", "萝卜", "胡萝卜", "西瓜",
                          "葡萄", "苹果", "梨", "桃子", "橙子", "柑橘", "香蕉", "芒果",
                          "蓝莓", "樱桃", "荔枝", "龙眼", "柚子", "猕猴桃", "火龙果"};
        
        for (String crop : crops) {
            if (input.contains(crop)) {
                return crop;
            }
        }
        return "作物";
    }
    
    /**
     * 根据地址匹配预设的地理坐标（用于3D地图和GIS地图显示区块）
     */
    private Map<String, Object> getPresetGeoLocation(String address) {
        if (address == null || address.isEmpty()) return null;
        
        // 预设地点坐标数据库（可根据需要扩展）
        Map<String, Map<String, Object>> locationDb = new LinkedHashMap<>();
        
        // 张家界七十二奇楼（位于永定区）
        Map<String, Object> qilou = new LinkedHashMap<>();
        qilou.put("centerLng", "110.455388");
        qilou.put("centerLat", "29.145479");
        qilou.put("coordinates", "[{\"lng\":110.454834,\"lat\":29.145675},{\"lng\":110.45501,\"lat\":29.145181},{\"lng\":110.455942,\"lat\":29.145325},{\"lng\":110.45577,\"lat\":29.145777}]");
        qilou.put("district", "永定区");
        qilou.put("suggestedName", "72奇楼");  // 建议的农田名称
        locationDb.put("七十二奇楼", qilou);
        locationDb.put("72奇楼", qilou);
        locationDb.put("奇楼", qilou);
        
        // 张家界天门山（示例扩展）
        Map<String, Object> tianmen = new LinkedHashMap<>();
        tianmen.put("centerLng", "110.478889");
        tianmen.put("centerLat", "29.053333");
        tianmen.put("coordinates", "[{\"lng\":110.477,\"lat\":29.054},{\"lng\":110.478,\"lat\":29.052},{\"lng\":110.480,\"lat\":29.053},{\"lng\":110.479,\"lat\":29.055}]");
        tianmen.put("district", "张家界市");
        locationDb.put("天门山", tianmen);
        
        // 张家界武陵源（示例扩展）
        Map<String, Object> wulingyuan = new LinkedHashMap<>();
        wulingyuan.put("centerLng", "110.550000");
        wulingyuan.put("centerLat", "29.345000");
        wulingyuan.put("coordinates", "[{\"lng\":110.548,\"lat\":29.346},{\"lng\":110.549,\"lat\":29.343},{\"lng\":110.552,\"lat\":29.344},{\"lng\":110.551,\"lat\":29.347}]");
        wulingyuan.put("district", "武陵源区");
        locationDb.put("武陵源", wulingyuan);
        
        // 遍历匹配
        for (Map.Entry<String, Map<String, Object>> entry : locationDb.entrySet()) {
            if (address.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 从地址中提取区县信息（用于GIS地图匹配）
     */
    private String extractDistrictFromAddress(String address) {
        if (address == null || address.isEmpty()) return null;
        
        // 先检查预设地点是否有区县信息
        Map<String, Object> geoData = getPresetGeoLocation(address);
        if (geoData != null && geoData.get("district") != null) {
            return geoData.get("district").toString();
        }
        
        // 常见城市/区县关键词匹配
        java.util.regex.Pattern[] patterns = {
            // 匹配 XX市、XX区、XX县
            java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,}(?:市|区|县|州))"),
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(address);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // 如果没有明确的市/区/县后缀，尝试提取前几个字作为地区
        if (address.length() >= 2) {
            // 检查是否以常见城市名开头
            String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "武汉", "成都", 
                               "重庆", "天津", "苏州", "西安", "长沙", "青岛", "郑州", "东莞",
                               "张家界", "桂林", "三亚", "厦门", "昆明", "丽江", "大理"};
            for (String city : cities) {
                if (address.startsWith(city)) {
                    return city + "市";
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从用户输入中提取地址/位置信息
     */
    private String extractAddress(String input) {
        if (input == null) return null;
        
        // 匹配 "位置在XX" "地址是XX" "在XX"
        java.util.regex.Pattern[] patterns = {
            java.util.regex.Pattern.compile("(?:位置在|地址是|地址在|位于|在)\\s*([\\u4e00-\\u9fa5\\w]+(?:市|区|县|镇|村|路|街|号|楼|栋)[\\u4e00-\\u9fa5\\w]*)"),
            java.util.regex.Pattern.compile("(?:位置|地址)[：:]\\s*([\\u4e00-\\u9fa5\\w]+)"),
        };
        
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String address = matcher.group(1);
                if (address != null && address.length() >= 2) {
                    return address;
                }
            }
        }
        return null;
    }
    
    /**
     * 根据作物和环境数据给出种植建议
     */
    private String getCropGrowthAdvice(String crop, double temp, double humidity) {
        StringBuilder advice = new StringBuilder();
        
        // 作物生长条件数据库
        Map<String, int[]> cropConditions = new LinkedHashMap<>();
        // 格式：{最低温度, 最高温度, 最适温度, 最低湿度, 最高湿度}
        cropConditions.put("草莓", new int[]{5, 30, 20, 60, 80});
        cropConditions.put("番茄", new int[]{10, 35, 25, 50, 70});
        cropConditions.put("西红柿", new int[]{10, 35, 25, 50, 70});
        cropConditions.put("黄瓜", new int[]{15, 35, 28, 60, 80});
        cropConditions.put("辣椒", new int[]{15, 35, 25, 50, 70});
        cropConditions.put("茄子", new int[]{15, 35, 26, 50, 70});
        cropConditions.put("白菜", new int[]{5, 25, 18, 60, 80});
        cropConditions.put("生菜", new int[]{5, 25, 18, 60, 80});
        cropConditions.put("玉米", new int[]{10, 35, 25, 50, 70});
        cropConditions.put("水稻", new int[]{15, 35, 28, 70, 90});
        cropConditions.put("西瓜", new int[]{18, 38, 28, 50, 70});
        cropConditions.put("葡萄", new int[]{10, 35, 25, 50, 70});
        
        int[] conditions = cropConditions.getOrDefault(crop, new int[]{10, 30, 22, 50, 70});
        int minTemp = conditions[0];
        int maxTemp = conditions[1];
        int optTemp = conditions[2];
        int minHum = conditions[3];
        int maxHum = conditions[4];
        
        // 温度评估
        boolean tempOk = temp >= minTemp && temp <= maxTemp;
        boolean tempOptimal = Math.abs(temp - optTemp) <= 5;
        
        // 湿度评估
        boolean humOk = humidity >= minHum && humidity <= maxHum;
        
        if (tempOptimal && humOk) {
            advice.append("✅ 【非常适合】当前环境非常适合种植").append(crop).append("！\n\n");
            advice.append("• ").append(crop).append("最适生长温度：").append(optTemp).append("°C左右\n");
            advice.append("• 当前温度").append(String.format("%.1f", temp)).append("°C，处于最佳范围\n");
            advice.append("• 土壤湿度适中，利于根系发育\n\n");
            advice.append("💡 建议：现在是种植").append(crop).append("的好时机，可以开始播种或移栽。");
        } else if (tempOk && humOk) {
            advice.append("👍 【适合种植】当前环境适合种植").append(crop).append("。\n\n");
            advice.append("• ").append(crop).append("适宜温度范围：").append(minTemp).append("-").append(maxTemp).append("°C\n");
            advice.append("• 当前温度").append(String.format("%.1f", temp)).append("°C，在可接受范围内\n\n");
            if (temp < optTemp - 3) {
                advice.append("💡 提示：温度略低于最佳值，生长速度可能稍慢，建议适当保温。");
            } else if (temp > optTemp + 3) {
                advice.append("💡 提示：温度略高，注意通风降温和补充水分。");
            }
        } else {
            advice.append("⚠️ 【暂不建议】当前环境不太适合种植").append(crop).append("。\n\n");
            if (!tempOk) {
                if (temp < minTemp) {
                    advice.append("• 温度过低（当前").append(String.format("%.1f", temp)).append("°C，需要≥").append(minTemp).append("°C）\n");
                    advice.append("💡 建议：等气温回升或使用大棚保温后再种植。\n");
                } else {
                    advice.append("• 温度过高（当前").append(String.format("%.1f", temp)).append("°C，需要≤").append(maxTemp).append("°C）\n");
                    advice.append("💡 建议：加强通风降温，或等天气凉爽后再种植。\n");
                }
            }
            if (!humOk) {
                if (humidity < minHum) {
                    advice.append("• 土壤偏干（当前").append(String.format("%.0f", humidity)).append("%，建议").append(minHum).append("-").append(maxHum).append("%）\n");
                    advice.append("💡 建议：增加灌溉频率。\n");
                } else {
                    advice.append("• 土壤过湿（当前").append(String.format("%.0f", humidity)).append("%）\n");
                    advice.append("💡 建议：改善排水，避免根部腐烂。\n");
                }
            }
        }
        
        return advice.toString();
    }
    
    /**
     * 匹配农田编号（支持中文和阿拉伯数字）
     * 例如：农田名称"1号田" 匹配目标"1" 或 "一"
     */
    private boolean matchFarmNumber(String farmName, String target) {
        if (farmName == null || target == null) return false;
        
        // 中文数字映射
        Map<String, String> chineseToArabic = new LinkedHashMap<>();
        chineseToArabic.put("一", "1");
        chineseToArabic.put("二", "2");
        chineseToArabic.put("三", "3");
        chineseToArabic.put("四", "4");
        chineseToArabic.put("五", "5");
        chineseToArabic.put("六", "6");
        chineseToArabic.put("七", "7");
        chineseToArabic.put("八", "8");
        chineseToArabic.put("九", "9");
        chineseToArabic.put("十", "10");
        
        // 将目标转换为阿拉伯数字
        String normalizedTarget = target;
        for (Map.Entry<String, String> entry : chineseToArabic.entrySet()) {
            normalizedTarget = normalizedTarget.replace(entry.getKey(), entry.getValue());
        }
        
        // 从农田名称中提取数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([一二三四五六七八九十\\d]+)");
        java.util.regex.Matcher matcher = pattern.matcher(farmName);
        
        while (matcher.find()) {
            String farmNumber = matcher.group(1);
            // 转换中文数字
            for (Map.Entry<String, String> entry : chineseToArabic.entrySet()) {
                farmNumber = farmNumber.replace(entry.getKey(), entry.getValue());
            }
            if (farmNumber.equals(normalizedTarget)) {
                return true;
            }
        }
        
        return false;
    }
}
