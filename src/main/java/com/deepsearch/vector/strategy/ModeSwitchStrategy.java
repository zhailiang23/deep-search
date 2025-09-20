package com.deepsearch.vector.strategy;

import com.deepsearch.vector.config.VectorProcessingConfig;
import com.deepsearch.vector.enums.ProcessingMode;
import com.deepsearch.vector.model.ProcessingContext;
import com.deepsearch.vector.model.ProcessingMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 智能模式切换策略
 * 根据负载、成本、延迟等因素自动选择最优的处理模式
 */
@Component
public class ModeSwitchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ModeSwitchStrategy.class);

    @Autowired
    private VectorProcessingConfig config;

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * 确定最优处理模式
     *
     * @param context 处理上下文
     * @param metrics 当前系统指标
     * @return 最优处理模式
     */
    public ProcessingMode determineOptimalMode(ProcessingContext context, ProcessingMetrics metrics) {
        // 如果明确指定了非AUTO_SWITCH模式，直接返回
        if (context.getRequestedMode() != ProcessingMode.AUTO_SWITCH) {
            logger.debug("使用明确指定的处理模式: {}", context.getRequestedMode());
            return context.getRequestedMode();
        }

        // 如果禁用了自动切换，使用默认模式
        if (!config.getModeSwitching().isAutoSwitchEnabled()) {
            ProcessingMode defaultMode = config.getDefaultMode();
            if (defaultMode == ProcessingMode.AUTO_SWITCH) {
                defaultMode = ProcessingMode.ONLINE_REALTIME; // 默认使用在线模式
            }
            logger.debug("自动切换已禁用，使用默认模式: {}", defaultMode);
            return defaultMode;
        }

        // 计算各种决策因子的权重
        double costWeight = calculateCostWeight(metrics);
        double latencyWeight = calculateLatencyWeight(metrics, context);
        double loadWeight = calculateLoadWeight(metrics);
        double queueWeight = calculateQueueWeight(metrics);

        // 综合评分决策
        double offlineScore = calculateOfflineScore(costWeight, latencyWeight, loadWeight, queueWeight);
        double onlineScore = calculateOnlineScore(costWeight, latencyWeight, loadWeight, queueWeight);

        ProcessingMode selectedMode = offlineScore > onlineScore
            ? ProcessingMode.OFFLINE_BATCH
            : ProcessingMode.ONLINE_REALTIME;

        logger.info("模式切换决策 - 离线评分: {}, 在线评分: {}, 选择模式: {}",
                   offlineScore, onlineScore, selectedMode);

        return selectedMode;
    }

    /**
     * 计算成本权重
     */
    private double calculateCostWeight(ProcessingMetrics metrics) {
        Integer costThreshold = config.getModeSwitching().getCostThresholdCents();
        if (costThreshold == null || costThreshold <= 0) {
            return 0.0;
        }

        double costRatio = (double) metrics.getTotalCostCents() / costThreshold;

        // 成本越高，越倾向于使用离线模式
        if (costRatio > 1.0) {
            return 1.0; // 超过阈值，强烈倾向离线
        } else if (costRatio > 0.8) {
            return 0.7; // 接近阈值，较强倾向离线
        } else if (costRatio > 0.5) {
            return 0.3; // 适中，轻微倾向离线
        } else {
            return 0.0; // 成本较低，不影响决策
        }
    }

    /**
     * 计算延迟权重
     */
    private double calculateLatencyWeight(ProcessingMetrics metrics, ProcessingContext context) {
        Long latencyThreshold = config.getModeSwitching().getLatencyThresholdMs();
        if (latencyThreshold == null || latencyThreshold <= 0) {
            return 0.0;
        }

        // 如果用户有明确的延迟要求
        if (context.getMaxLatencyMs() != null && context.getMaxLatencyMs() < latencyThreshold) {
            return -1.0; // 强烈倾向在线模式
        }

        double avgLatency = metrics.getAverageProcessingTime();
        double latencyRatio = avgLatency / latencyThreshold;

        // 延迟越高，越倾向于使用离线模式
        if (latencyRatio > 1.5) {
            return 0.8; // 延迟严重，强烈倾向离线
        } else if (latencyRatio > 1.0) {
            return 0.5; // 延迟超标，倾向离线
        } else if (latencyRatio > 0.7) {
            return 0.2; // 延迟接近阈值，轻微倾向离线
        } else {
            return -0.3; // 延迟较低，倾向在线
        }
    }

    /**
     * 计算系统负载权重
     */
    private double calculateLoadWeight(ProcessingMetrics metrics) {
        Double loadThreshold = config.getModeSwitching().getLoadThreshold();
        if (loadThreshold == null || loadThreshold <= 0) {
            return 0.0;
        }

        double systemLoad = metrics.getSystemLoadAverage();
        double loadRatio = systemLoad / loadThreshold;

        // 系统负载越高，越倾向于使用离线模式
        if (loadRatio > 1.2) {
            return 1.0; // 负载严重，强烈倾向离线
        } else if (loadRatio > 1.0) {
            return 0.7; // 负载超标，倾向离线
        } else if (loadRatio > 0.8) {
            return 0.3; // 负载较高，轻微倾向离线
        } else {
            return -0.1; // 负载正常，轻微倾向在线
        }
    }

    /**
     * 计算队列权重
     */
    private double calculateQueueWeight(ProcessingMetrics metrics) {
        Integer queueThreshold = config.getModeSwitching().getQueueSizeThreshold();
        if (queueThreshold == null || queueThreshold <= 0) {
            return 0.0;
        }

        int queueSize = metrics.getCurrentQueueSize();
        double queueRatio = (double) queueSize / queueThreshold;

        // 队列越长，越倾向于使用离线批处理模式
        if (queueRatio > 1.5) {
            return 1.0; // 队列严重积压，强烈倾向离线
        } else if (queueRatio > 1.0) {
            return 0.8; // 队列超标，倾向离线
        } else if (queueRatio > 0.7) {
            return 0.4; // 队列较长，轻微倾向离线
        } else {
            return -0.2; // 队列较短，轻微倾向在线
        }
    }

    /**
     * 计算离线模式评分
     */
    private double calculateOfflineScore(double costWeight, double latencyWeight,
                                       double loadWeight, double queueWeight) {
        // 离线模式的基础分数
        double baseScore = 0.5;

        // 权重配置
        double costFactor = 0.3;
        double latencyFactor = 0.2;
        double loadFactor = 0.3;
        double queueFactor = 0.2;

        return baseScore +
               costWeight * costFactor +
               Math.max(0, latencyWeight) * latencyFactor +
               loadWeight * loadFactor +
               queueWeight * queueFactor;
    }

    /**
     * 计算在线模式评分
     */
    private double calculateOnlineScore(double costWeight, double latencyWeight,
                                      double loadWeight, double queueWeight) {
        // 在线模式的基础分数
        double baseScore = 0.5;

        // 权重配置
        double costFactor = 0.3;
        double latencyFactor = 0.4; // 在线模式更关注延迟
        double loadFactor = 0.2;
        double queueFactor = 0.1;

        return baseScore -
               costWeight * costFactor +
               Math.max(0, -latencyWeight) * latencyFactor -
               loadWeight * loadFactor -
               queueWeight * queueFactor;
    }

    /**
     * 评估当前模式是否需要切换
     */
    public boolean shouldSwitchMode(ProcessingMode currentMode, ProcessingContext context,
                                  ProcessingMetrics metrics) {
        ProcessingMode optimalMode = determineOptimalMode(context, metrics);

        if (currentMode != optimalMode) {
            logger.info("建议从模式 {} 切换到模式 {}", currentMode, optimalMode);
            return true;
        }

        return false;
    }

    /**
     * 获取当前系统负载
     */
    public double getCurrentSystemLoad() {
        return osBean.getSystemLoadAverage();
    }
}