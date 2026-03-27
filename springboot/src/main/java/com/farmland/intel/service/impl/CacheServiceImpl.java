package com.farmland.intel.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.farmland.intel.service.ICacheService;

import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImpl implements ICacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param timeout 过期时间（秒）
     */
    public void setCache(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存（默认24小时过期）
     * @param key 键
     * @param value 值
     */
    public void setCache(String key, Object value) {
        setCache(key, value, 86400); // 24小时
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * @param key 键
     */
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 检查缓存是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean hasCache(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置Hash缓存
     * @param key 键
     * @param field 字段
     * @param value 值
     */
    public void setHashCache(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取Hash缓存
     * @param key 键
     * @param field 字段
     * @return 值
     */
    public Object getHashCache(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 删除Hash缓存
     * @param key 键
     * @param field 字段
     */
    public void deleteHashCache(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    /**
     * 设置List缓存
     * @param key 键
     * @param value 值
     */
    public void setListCache(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 获取List缓存
     * @param key 键
     * @param index 索引
     * @return 值
     */
    public Object getListCache(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 设置Set缓存
     * @param key 键
     * @param value 值
     */
    public void setSetCache(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 获取Set缓存
     * @param key 键
     * @return 值
     */
    public Object getSetCache(String key) {
        return redisTemplate.opsForSet().pop(key);
    }
}
