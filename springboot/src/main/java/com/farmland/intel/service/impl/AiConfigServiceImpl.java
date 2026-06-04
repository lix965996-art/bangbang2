package com.farmland.intel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.farmland.intel.entity.AiConfig;
import com.farmland.intel.mapper.AiConfigMapper;
import com.farmland.intel.service.IAiConfigService;
import com.farmland.intel.utils.CryptoUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements IAiConfigService {

    public static String defaultBaseUrl(String provider) {
        if (provider == null) return "";
        switch (provider) {
            case "qwen":     return "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "deepseek": return "https://api.deepseek.com/v1";
            case "glm":      return "https://open.bigmodel.cn/api/paas/v4";
            case "minimax":  return "https://api.minimax.chat/v1";
            case "openai":   return "https://api.openai.com/v1";
            default:         return "";
        }
    }

    public static String defaultModel(String provider) {
        if (provider == null) return "qwen-max";
        switch (provider) {
            case "qwen":     return "qwen-max";
            case "deepseek": return "deepseek-chat";
            case "glm":      return "glm-4";
            case "minimax":  return "abab6.5s-chat";
            case "openai":   return "gpt-4o-mini";
            default:         return "gpt-3.5-turbo";
        }
    }

    @Override
    public AiConfig getByUserId(Integer userId) {
        if (userId == null) {
            return buildDefault();
        }

        AiConfig cfg = getOne(new QueryWrapper<AiConfig>().eq("user_id", userId));
        if (cfg == null) {
            return buildDefault();
        }

        cfg.setApiKey(CryptoUtils.decrypt(cfg.getApiKey()));
        cfg.setChatApiKey(CryptoUtils.decrypt(cfg.getChatApiKey()));
        return cfg;
    }

    @Override
    public AiConfig saveOrUpdateByUserId(AiConfig config) {
        if (config == null || config.getUserId() == null) {
            throw new IllegalArgumentException("userId cannot be empty");
        }

        normalizeDefaults(config);

        AiConfig saved = copy(config);
        saved.setApiKey(CryptoUtils.encrypt(saved.getApiKey()));
        saved.setChatApiKey(CryptoUtils.encrypt(saved.getChatApiKey()));

        AiConfig existing = getOne(new QueryWrapper<AiConfig>().eq("user_id", config.getUserId()));
        if (existing != null) {
            saved.setId(existing.getId());
        }

        saveOrUpdate(saved);
        config.setId(saved.getId());
        return config;
    }

    private void normalizeDefaults(AiConfig config) {
        if (config.getProvider() == null || config.getProvider().isBlank()) {
            config.setProvider("qwen");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            config.setBaseUrl(defaultBaseUrl(config.getProvider()));
        }
        if (config.getModelName() == null || config.getModelName().isBlank()) {
            config.setModelName(defaultModel(config.getProvider()));
        }
        if (config.getChatModelName() == null) {
            config.setChatModelName("");
        }
        if (config.getChatBaseUrl() == null) {
            config.setChatBaseUrl("");
        }
        if (config.getChatApiKey() == null) {
            config.setChatApiKey("");
        }
        if (config.getTemperature() == null) {
            config.setTemperature(new BigDecimal("0.42"));
        }
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
    }

    private AiConfig copy(AiConfig source) {
        AiConfig target = new AiConfig();
        target.setId(source.getId());
        target.setUserId(source.getUserId());
        target.setProvider(source.getProvider());
        target.setBaseUrl(source.getBaseUrl());
        target.setApiKey(source.getApiKey());
        target.setModelName(source.getModelName());
        target.setChatModelName(source.getChatModelName());
        target.setChatBaseUrl(source.getChatBaseUrl());
        target.setChatApiKey(source.getChatApiKey());
        target.setTemperature(source.getTemperature());
        target.setEnabled(source.getEnabled());
        target.setUpdateTime(source.getUpdateTime());
        return target;
    }

    private AiConfig buildDefault() {
        AiConfig cfg = new AiConfig();
        cfg.setProvider("qwen");
        cfg.setBaseUrl(defaultBaseUrl("qwen"));
        cfg.setApiKey("");
        cfg.setModelName("qwen-max");
        cfg.setChatModelName("");
        cfg.setChatBaseUrl("");
        cfg.setChatApiKey("");
        cfg.setTemperature(new BigDecimal("0.42"));
        cfg.setEnabled(1);
        return cfg;
    }
}
