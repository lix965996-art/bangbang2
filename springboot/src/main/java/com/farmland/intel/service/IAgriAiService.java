package com.farmland.intel.service;

/**
 * 农业 AI 问答服务接口。
 */
public interface IAgriAiService {
    // 定义一个方法：向 AI 提问
    String askAgriExpert(String userQuestion, double indoorTemp, double indoorHumidity, double outdoorTemp);
}
