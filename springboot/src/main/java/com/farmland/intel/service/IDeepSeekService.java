package com.farmland.intel.service;

/**
 * DeepSeek AI 服务接口
 */
public interface IDeepSeekService {
    // 定义一个方法：向 AI 提问
    String askAgriExpert(String userQuestion, double indoorTemp, double indoorHumidity, double outdoorTemp);
}