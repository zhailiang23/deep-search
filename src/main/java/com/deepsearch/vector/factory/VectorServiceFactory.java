package com.deepsearch.vector.factory;

import com.deepsearch.vector.VectorService;
import com.deepsearch.vector.VectorService.VectorProcessingException;
import com.deepsearch.vector.local.LocalBertVectorService;
import com.deepsearch.vector.openai.OpenAIVectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量服务工厂
 * 管理和协调多个向量服务实现
 *
 * @author DeepSearch Vector Team
 */
@Component
public class VectorServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(VectorServiceFactory.class);

    private final Map<String, VectorService> services = new ConcurrentHashMap<>();
    private final Map<String, String> modelToServiceMapping = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private OpenAIVectorService openAIVectorService;

    @Autowired(required = false)
    private LocalBertVectorService localBertVectorService;

    @PostConstruct
    public void initialize() {
        logger.info("初始化向量服务工厂");

        // 注册OpenAI服务
        if (openAIVectorService != null) {
            registerService(openAIVectorService);
        }

        // 注册本地BERT服务
        if (localBertVectorService != null) {
            registerService(localBertVectorService);
        }

        logger.info("向量服务工厂初始化完成，注册服务数量: {}", services.size());
    }

    /**
     * 注册向量服务
     */
    private void registerService(VectorService service) {
        String serviceType = service.getServiceType();
        services.put(serviceType, service);

        // 建立模型到服务的映射
        for (String model : service.getSupportedModels()) {
            modelToServiceMapping.put(model, serviceType);
            logger.debug("注册模型映射: {} -> {}", model, serviceType);
        }

        logger.info("注册向量服务: {} ({})", service.getServiceName(), serviceType);
    }

    /**
     * 根据服务类型获取向量服务
     */
    public VectorService getService(String serviceType) {
        VectorService service = services.get(serviceType);
        if (service == null) {
            throw VectorProcessingException.invalidInput("不支持的服务类型: " + serviceType);
        }
        return service;
    }

    /**
     * 根据模型名称获取向量服务
     */
    public VectorService getServiceByModel(String modelName) {
        String serviceType = modelToServiceMapping.get(modelName);
        if (serviceType == null) {
            throw VectorProcessingException.invalidInput("不支持的模型: " + modelName);
        }
        return getService(serviceType);
    }

    /**
     * 获取OpenAI服务
     */
    public VectorService getOpenAIService() {
        return getService("openai");
    }

    /**
     * 获取本地BERT服务
     */
    public VectorService getLocalBertService() {
        return getService("local-bert");
    }

    /**
     * 获取默认服务（优先使用本地服务）
     */
    public VectorService getDefaultService() {
        // 优先使用本地服务
        if (services.containsKey("local-bert")) {
            return services.get("local-bert");
        }
        // 其次使用OpenAI服务
        if (services.containsKey("openai")) {
            return services.get("openai");
        }
        throw VectorProcessingException.internalError("没有可用的向量服务", null);
    }

    /**
     * 获取所有注册的服务
     */
    public Collection<VectorService> getAllServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    /**
     * 获取所有支持的服务类型
     */
    public Set<String> getSupportedServiceTypes() {
        return Collections.unmodifiableSet(services.keySet());
    }

    /**
     * 获取所有支持的模型
     */
    public Set<String> getAllSupportedModels() {
        return Collections.unmodifiableSet(modelToServiceMapping.keySet());
    }

    /**
     * 检查服务是否可用
     */
    public boolean isServiceAvailable(String serviceType) {
        VectorService service = services.get(serviceType);
        if (service == null) {
            return false;
        }

        try {
            VectorService.ServiceHealth health = service.checkHealth().get();
            return health.isAvailable();
        } catch (Exception e) {
            logger.warn("检查服务健康状态失败: {}", serviceType, e);
            return false;
        }
    }

    /**
     * 检查模型是否可用
     */
    public boolean isModelAvailable(String modelName) {
        String serviceType = modelToServiceMapping.get(modelName);
        return serviceType != null && isServiceAvailable(serviceType);
    }

    /**
     * 获取可用的服务
     */
    public List<VectorService> getAvailableServices() {
        return services.values().stream()
                .filter(service -> {
                    try {
                        VectorService.ServiceHealth health = service.checkHealth().get();
                        return health.isAvailable();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 获取可用的模型
     */
    public Set<String> getAvailableModels() {
        return modelToServiceMapping.entrySet().stream()
                .filter(entry -> isServiceAvailable(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    /**
     * 选择最佳服务
     * 基于可用性、性能和成本考虑
     */
    public VectorService selectBestService(ServiceSelectionCriteria criteria) {
        List<VectorService> availableServices = getAvailableServices();

        if (availableServices.isEmpty()) {
            throw VectorProcessingException.modelUnavailable("没有可用的向量服务");
        }

        // 如果指定了模型，直接返回对应服务
        if (criteria.preferredModel != null) {
            String serviceType = modelToServiceMapping.get(criteria.preferredModel);
            if (serviceType != null && isServiceAvailable(serviceType)) {
                return getService(serviceType);
            }
        }

        // 如果指定了服务类型，检查是否可用
        if (criteria.preferredServiceType != null && isServiceAvailable(criteria.preferredServiceType)) {
            return getService(criteria.preferredServiceType);
        }

        // 根据选择策略选择最佳服务
        switch (criteria.strategy) {
            case COST_OPTIMIZED:
                return selectCostOptimizedService(availableServices, criteria);
            case PERFORMANCE_OPTIMIZED:
                return selectPerformanceOptimizedService(availableServices, criteria);
            case BALANCED:
            default:
                return selectBalancedService(availableServices, criteria);
        }
    }

    private VectorService selectCostOptimizedService(List<VectorService> services, ServiceSelectionCriteria criteria) {
        // 优先选择成本最低的服务（本地服务成本为0）
        return services.stream()
                .min((s1, s2) -> {
                    int cost1 = s1.estimateCost(criteria.sampleText, s1.getDefaultModel());
                    int cost2 = s2.estimateCost(criteria.sampleText, s2.getDefaultModel());
                    return Integer.compare(cost1, cost2);
                })
                .orElse(services.get(0));
    }

    private VectorService selectPerformanceOptimizedService(List<VectorService> services, ServiceSelectionCriteria criteria) {
        // 优先选择处理时间最短的服务
        return services.stream()
                .min((s1, s2) -> {
                    long time1 = s1.estimateProcessingTime(criteria.sampleText, s1.getDefaultModel());
                    long time2 = s2.estimateProcessingTime(criteria.sampleText, s2.getDefaultModel());
                    return Long.compare(time1, time2);
                })
                .orElse(services.get(0));
    }

    private VectorService selectBalancedService(List<VectorService> services, ServiceSelectionCriteria criteria) {
        // 平衡考虑成本和性能
        return services.stream()
                .min((s1, s2) -> {
                    double score1 = calculateBalancedScore(s1, criteria);
                    double score2 = calculateBalancedScore(s2, criteria);
                    return Double.compare(score1, score2);
                })
                .orElse(services.get(0));
    }

    private double calculateBalancedScore(VectorService service, ServiceSelectionCriteria criteria) {
        int cost = service.estimateCost(criteria.sampleText, service.getDefaultModel());
        long time = service.estimateProcessingTime(criteria.sampleText, service.getDefaultModel());

        // 归一化并计算综合得分（成本和时间权重各50%）
        double normalizedCost = Math.min(1.0, cost / 100.0); // 假设100分为高成本阈值
        double normalizedTime = Math.min(1.0, time / 5000.0); // 假设5秒为高延迟阈值

        return normalizedCost * 0.5 + normalizedTime * 0.5;
    }

    /**
     * 检查所有服务健康状态
     */
    public CompletableFuture<Map<String, VectorService.ServiceHealth>> checkAllServicesHealth() {
        Map<String, CompletableFuture<VectorService.ServiceHealth>> healthFutures = new HashMap<>();

        for (Map.Entry<String, VectorService> entry : services.entrySet()) {
            healthFutures.put(entry.getKey(), entry.getValue().checkHealth());
        }

        return CompletableFuture.allOf(healthFutures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, VectorService.ServiceHealth> healthMap = new HashMap<>();
                    for (Map.Entry<String, CompletableFuture<VectorService.ServiceHealth>> entry : healthFutures.entrySet()) {
                        try {
                            healthMap.put(entry.getKey(), entry.getValue().get());
                        } catch (Exception e) {
                            healthMap.put(entry.getKey(), VectorService.ServiceHealth.UNKNOWN);
                        }
                    }
                    return healthMap;
                });
    }

    /**
     * 预热所有服务
     */
    public CompletableFuture<Void> warmupAllServices() {
        CompletableFuture<Void>[] warmupFutures = services.values().stream()
                .map(VectorService::warmup)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(warmupFutures);
    }

    /**
     * 关闭所有服务
     */
    public CompletableFuture<Void> shutdownAllServices() {
        CompletableFuture<Void>[] shutdownFutures = services.values().stream()
                .map(VectorService::shutdown)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(shutdownFutures);
    }

    /**
     * 服务选择标准
     */
    public static class ServiceSelectionCriteria {
        private String preferredServiceType;
        private String preferredModel;
        private String sampleText = "sample text for estimation";
        private SelectionStrategy strategy = SelectionStrategy.BALANCED;

        public ServiceSelectionCriteria preferredServiceType(String serviceType) {
            this.preferredServiceType = serviceType;
            return this;
        }

        public ServiceSelectionCriteria preferredModel(String model) {
            this.preferredModel = model;
            return this;
        }

        public ServiceSelectionCriteria sampleText(String text) {
            this.sampleText = text;
            return this;
        }

        public ServiceSelectionCriteria strategy(SelectionStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public static ServiceSelectionCriteria create() {
            return new ServiceSelectionCriteria();
        }
    }

    /**
     * 服务选择策略
     */
    public enum SelectionStrategy {
        COST_OPTIMIZED,      // 成本优化
        PERFORMANCE_OPTIMIZED, // 性能优化
        BALANCED             // 平衡
    }
}