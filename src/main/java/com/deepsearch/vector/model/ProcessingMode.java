package com.deepsearch.vector.model;

/**
 * 向量处理模式枚举
 *
 * @author DeepSearch Vector Team
 */
public enum ProcessingMode {
    /**
     * 离线批量处理模式
     * 适用于大文件和批量文档处理，优先考虑吞吐量
     */
    OFFLINE_BATCH("离线批量处理"),

    /**
     * 在线实时处理模式
     * 适用于查询响应，优先考虑延迟
     */
    ONLINE_REALTIME("在线实时处理"),

    /**
     * 自动切换模式
     * 根据负载、成本、响应时间等因素自动选择最优模式
     */
    AUTO_SWITCH("自动切换");

    private final String description;

    ProcessingMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为实时处理模式
     */
    public boolean isRealtime() {
        return this == ONLINE_REALTIME;
    }

    /**
     * 判断是否为批量处理模式
     */
    public boolean isBatch() {
        return this == OFFLINE_BATCH;
    }

    /**
     * 判断是否为自动模式
     */
    public boolean isAuto() {
        return this == AUTO_SWITCH;
    }
}