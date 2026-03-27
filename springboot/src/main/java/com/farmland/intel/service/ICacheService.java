package com.farmland.intel.service;

public interface ICacheService {

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param timeout 过期时间（秒）
     */
    void setCache(String key, Object value, long timeout);

    /**
     * 设置缓存（默认24小时过期）
     * @param key 键
     * @param value 值
     */
    void setCache(String key, Object value);

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    Object getCache(String key);

    /**
     * 删除缓存
     * @param key 键
     */
    void deleteCache(String key);

    /**
     * 检查缓存是否存在
     * @param key 键
     * @return 是否存在
     */
    boolean hasCache(String key);

    /**
     * 设置Hash缓存
     * @param key 键
     * @param field 字段
     * @param value 值
     */
    void setHashCache(String key, String field, Object value);

    /**
     * 获取Hash缓存
     * @param key 键
     * @param field 字段
     * @return 值
     */
    Object getHashCache(String key, String field);

    /**
     * 删除Hash缓存
     * @param key 键
     * @param field 字段
     */
    void deleteHashCache(String key, String field);

    /**
     * 设置List缓存
     * @param key 键
     * @param value 值
     */
    void setListCache(String key, Object value);

    /**
     * 获取List缓存
     * @param key 键
     * @param index 索引
     * @return 值
     */
    Object getListCache(String key, long index);

    /**
     * 设置Set缓存
     * @param key 键
     * @param value 值
     */
    void setSetCache(String key, Object value);

    /**
     * 获取Set缓存
     * @param key 键
     * @return 值
     */
    Object getSetCache(String key);
}