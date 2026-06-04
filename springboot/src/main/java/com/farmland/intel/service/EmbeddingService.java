package com.farmland.intel.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedding 服务：使用 Spring AI EmbeddingModel 替代手动 HTTP 调用。
 * 通过当前登录用户在个人中心保存的 Key 生成 text-embedding-v3 向量。
 */
@Service
@Slf4j
public class EmbeddingService {

    @Autowired(required = false)
    private com.farmland.intel.service.IAiConfigService aiConfigService;

    private static final String EMBEDDING_MODEL = "text-embedding-v3";

    private final Map<String, EmbeddingModel> modelCache = new ConcurrentHashMap<>();

    public void evictCache(Integer userId) {
        if (userId == null) {
            return;
        }
        modelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(userId + ":"));
    }

    /**
     * 生成单条文本的 embedding
     */
    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) {
            return new float[0];
        }

        try {
            EmbeddingModel model = getEmbeddingModel();
            if (model == null) {
                log.warn("EmbeddingModel 未初始化：当前用户尚未配置个人 AI API Key");
                return new float[0];
            }

            float[] embedding = model.embed(text);
            return embedding != null ? embedding : new float[0];
        } catch (Exception e) {
            log.error("调用 Embedding API 异常", e);
            return new float[0];
        }
    }

    /**
     * 批量生成 embedding
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
            // 简单限流，避免 API 过载
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 获取或创建 EmbeddingModel 实例（懒加载 + 缓存）
     */
    @SuppressWarnings("null")
    private EmbeddingModel getEmbeddingModel() {
        com.farmland.intel.entity.User user = com.farmland.intel.utils.TokenUtils.getCurrentUser();
        if (user == null || aiConfigService == null) {
            return null;
        }

        com.farmland.intel.entity.AiConfig cfg = aiConfigService.getByUserId(user.getId());
        if (cfg == null || !StringUtils.hasText(cfg.getApiKey())) {
            return null;
        }
        if (!StringUtils.hasText(cfg.getBaseUrl())) {
            return null;
        }

        String baseUrl = cfg.getBaseUrl().replaceAll("/+$", "");
        if (!isDashScopeEmbeddingConfig(cfg, baseUrl)) {
            log.debug("当前模型配置不支持 text-embedding-v3，跳过向量生成: provider={}, baseUrl={}",
                    cfg.getProvider(), baseUrl);
            return null;
        }

        String cacheKey = user.getId() + ":" + cfg.getProvider() + ":" + baseUrl + ":"
                + EMBEDDING_MODEL + ":" + cfg.getApiKey().hashCode();
        return modelCache.computeIfAbsent(cacheKey, key -> {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(cfg.getApiKey())
                    .build();

            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(EMBEDDING_MODEL)
                    .build();

            log.info("EmbeddingModel 已初始化: userId={}, model={}, url={}", user.getId(), EMBEDDING_MODEL, baseUrl);
            return new OpenAiEmbeddingModel(api, org.springframework.ai.document.MetadataMode.EMBED, options);
        });
    }

    private boolean isDashScopeEmbeddingConfig(com.farmland.intel.entity.AiConfig cfg, String baseUrl) {
        String provider = cfg.getProvider() == null ? "" : cfg.getProvider().trim().toLowerCase();
        return "qwen".equals(provider) || baseUrl.contains("dashscope.aliyuncs.com");
    }
}
