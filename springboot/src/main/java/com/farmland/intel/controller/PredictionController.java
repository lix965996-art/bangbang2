package com.farmland.intel.controller;

import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.dto.YieldPredictionRequest;
import com.farmland.intel.service.IPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    @Autowired
    private IPredictionService predictionService;

    /**
     * 获取作物产量预测
     */
    @PostMapping("/yield")
    public Result predictYield(@RequestBody YieldPredictionRequest request) {
        try {
            Map<String, Object> result = predictionService.predictYield(request);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(Constants.CODE_500, "预测失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史预测数据
     */
    @GetMapping("/history")
    public Result getPredictionHistory(
            @RequestParam(value = "cropType", required = false) String cropType,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            List<Map<String, Object>> history = predictionService.getPredictionHistory(cropType, startDate, endDate);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(Constants.CODE_500, "获取历史数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取预测模型信息
     */
    @GetMapping("/model-info")
    public Result getModelInfo() {
        try {
            Map<String, Object> modelInfo = predictionService.getModelInfo();
            return Result.success(modelInfo);
        } catch (Exception e) {
            return Result.error(Constants.CODE_500, "获取模型信息失败: " + e.getMessage());
        }
    }

    /**
     * 训练预测模型
     */
    @PostMapping("/train")
    public Result trainModel() {
        try {
            predictionService.trainModel();
            return Result.success("模型训练成功");
        } catch (Exception e) {
            return Result.error(Constants.CODE_500, "模型训练失败: " + e.getMessage());
        }
    }
}