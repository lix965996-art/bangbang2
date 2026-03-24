package com.farmland.intel.service;

import com.farmland.intel.dto.AISuggestionRequest;

public interface IAIService {

    /**
     * 分析果蔬成熟度
     * @param imageBytes 图像数据
     * @param cropType 作物类型
     * @return 分析结果
     */
    String analyzeRipeness(byte[] imageBytes, String cropType);

    /**
     * 分析果蔬病虫害
     * @param imageBytes 图像数据
     * @param cropType 作物类型
     * @return 分析结果
     */
    String analyzeDisease(byte[] imageBytes, String cropType);

    /**
     * 双检一体化分析
     * @param imageBytes 图像数据
     * @param cropType 作物类型
     * @return 分析结果
     */
    String analyzeDual(byte[] imageBytes, String cropType);

    /**
     * 获取AI智能建议
     * @param request 请求参数
     * @return 智能建议
     */
    String getAISuggestion(AISuggestionRequest request);
}