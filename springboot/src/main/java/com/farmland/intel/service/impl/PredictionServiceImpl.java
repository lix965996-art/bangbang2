package com.farmland.intel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.farmland.intel.dto.YieldPredictionRequest;
import com.farmland.intel.service.IPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionServiceImpl implements IPredictionService {

    @Autowired
    private RestTemplate restTemplate;

    private static final Map<String, String> CROP_MODELS = new HashMap<>();
    static {
        CROP_MODELS.put("tomato", "tomato_yield_model");
        CROP_MODELS.put("corn", "corn_yield_model");
        CROP_MODELS.put("rice", "rice_yield_model");
        CROP_MODELS.put("strawberry", "strawberry_yield_model");
        CROP_MODELS.put("cucumber", "cucumber_yield_model");
        CROP_MODELS.put("pepper", "pepper_yield_model");
    }

    @Override
    public Map<String, Object> predictYield(YieldPredictionRequest request) {
        String cropType = request.getCropType().toLowerCase();
        String model = CROP_MODELS.getOrDefault(cropType, "default_yield_model");

        String url = "http://ai-service:5000/api/predict/yield";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cropType", cropType);
        requestBody.put("plantingArea", request.getPlantingArea());
        requestBody.put("averageYield", request.getAverageYield());
        requestBody.put("fertilizerUsage", request.getFertilizerUsage());
        requestBody.put("irrigationAmount", request.getIrrigationAmount());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("humidity", request.getHumidity());
        requestBody.put("growthStage", request.getGrowthStage());

        Map<String, Object> result = restTemplate.postForObject(url, requestBody, Map.class);
        return result != null ? result : new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getPredictionHistory(String cropType, String startDate, String endDate) {
        String url = "http://ai-service:5000/api/predict/history";

        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(cropType)) {
            params.put("cropType", cropType);
        }
        if (StrUtil.isNotBlank(startDate)) {
            params.put("startDate", startDate);
        }
        if (StrUtil.isNotBlank(endDate)) {
            params.put("endDate", endDate);
        }

        List<Map<String, Object>> history = restTemplate.getForObject(url, List.class, params);
        return history != null ? history : new ArrayList<>();
    }

    @Override
    public Map<String, Object> getModelInfo() {
        String url = "http://ai-service:5000/api/predict/model-info";
        Map<String, Object> modelInfo = restTemplate.getForObject(url, Map.class);
        return modelInfo != null ? modelInfo : new HashMap<>();
    }

    @Override
    public void trainModel() {
        String url = "http://ai-service:5000/api/predict/train";
        restTemplate.postForObject(url, null, String.class);
    }
}