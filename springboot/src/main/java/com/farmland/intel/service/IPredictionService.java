package com.farmland.intel.service;

import com.farmland.intel.dto.YieldPredictionRequest;

import java.util.List;
import java.util.Map;

public interface IPredictionService {

    /**
     * 预测作物产量
     * @param request 预测请求
     * @return 预测结果
     */
    Map<String, Object> predictYield(YieldPredictionRequest request);

    /**
     * 获取历史预测数据
     * @param cropType 作物类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 历史数据
     */
    List<Map<String, Object>> getPredictionHistory(String cropType, String startDate, String endDate);

    /**
     * 获取模型信息
     * @return 模型信息
     */
    Map<String, Object> getModelInfo();

    /**
     * 训练预测模型
     */
    void trainModel();
}