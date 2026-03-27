package com.farmland.intel.controller;

import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.dto.AISuggestionRequest;
import com.farmland.intel.service.IAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private IAIService aiService;

    /**
     * 果蔬成熟度检测
     */
    @PostMapping("/ripeness")
    public Result ripenessAnalysis(@RequestParam("image") MultipartFile image,
                               @RequestParam(value = "cropType", defaultValue = "tomato") String cropType) {
        try {
            byte[] imageBytes = image.getBytes();
            String result = aiService.analyzeRipeness(imageBytes, cropType);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(Constants.CODE_500, "图像处理失败: " + e.getMessage());
        }
    }

    /**
     * 果蔬病虫害检测
     */
    @PostMapping("/disease")
    public Result diseaseAnalysis(@RequestParam("image") MultipartFile image,
                              @RequestParam(value = "cropType", defaultValue = "tomato") String cropType) {
        try {
            byte[] imageBytes = image.getBytes();
            String result = aiService.analyzeDisease(imageBytes, cropType);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(Constants.CODE_500, "图像处理失败: " + e.getMessage());
        }
    }

    /**
     * 双检一体化分析
     */
    @PostMapping("/analyze")
    public Result dualAnalysis(@RequestParam("image") MultipartFile image,
                            @RequestParam(value = "cropType", defaultValue = "tomato") String cropType) {
        try {
            byte[] imageBytes = image.getBytes();
            String result = aiService.analyzeDual(imageBytes, cropType);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(Constants.CODE_500, "图像处理失败: " + e.getMessage());
        }
    }

    /**
     * AI智能建议
     */
    @PostMapping("/suggest")
    public Result getAISuggestion(@RequestBody AISuggestionRequest request) {
        String suggestion = aiService.getAISuggestion(request);
        return Result.success(suggestion);
    }
}