package com.farmland.intel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.farmland.intel.dto.AISuggestionRequest;
import com.farmland.intel.service.IAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIServiceImpl implements IAIService {

    @Autowired
    private RestTemplate restTemplate;

    private static final Map<String, String> CROP_MODELS = new HashMap<>();
    static {
        CROP_MODELS.put("tomato", "tomato_model");
        CROP_MODELS.put("corn", "corn_model");
        CROP_MODELS.put("rice", "rice_model");
        CROP_MODELS.put("strawberry", "strawberry_model");
        CROP_MODELS.put("cucumber", "cucumber_model");
        CROP_MODELS.put("pepper", "pepper_model");
    }

    @Override
    public String analyzeRipeness(byte[] imageBytes, String cropType) {
        String model = CROP_MODELS.getOrDefault(cropType.toLowerCase(), "default_model");
        String url = "http://ai-service:5000/api/analyze/ripeness";

        Map<String, Object> request = new HashMap<>();
        request.put("image", imageBytes);
        request.put("model", model);

        return restTemplate.postForObject(url, request, String.class);
    }

    @Override
    public String analyzeDisease(byte[] imageBytes, String cropType) {
        String model = CROP_MODELS.getOrDefault(cropType.toLowerCase(), "default_model");
        String url = "http://ai-service:5000/api/analyze/disease";

        Map<String, Object> request = new HashMap<>();
        request.put("image", imageBytes);
        request.put("model", model);

        return restTemplate.postForObject(url, request, String.class);
    }

    @Override
    public String analyzeDual(byte[] imageBytes, String cropType) {
        String model = CROP_MODELS.getOrDefault(cropType.toLowerCase(), "default_model");
        String url = "http://ai-service:5000/api/analyze/dual";

        Map<String, Object> request = new HashMap<>();
        request.put("image", imageBytes);
        request.put("model", model);

        return restTemplate.postForObject(url, request, String.class);
    }

    @Override
    public String getAISuggestion(AISuggestionRequest request) {
        String url = "http://ai-service:5000/api/suggest";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cropType", request.getCropType());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("humidity", request.getHumidity());
        requestBody.put("ph", request.getPh());
        requestBody.put("growthStage", request.getGrowthStage());
        requestBody.put("problemDescription", request.getProblemDescription());

        return restTemplate.postForObject(url, requestBody, String.class);
    }
}