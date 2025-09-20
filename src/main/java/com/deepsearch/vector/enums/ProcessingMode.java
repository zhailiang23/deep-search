package com.deepsearch.vector.enums;

/**
 * 向量处理模式枚举
 */
public enum ProcessingMode {
    /** 离线批量处理 */
    OFFLINE_BATCH,

    /** 在线实时处理 */
    ONLINE_REALTIME,

    /** 自动切换 */
    AUTO_SWITCH
}