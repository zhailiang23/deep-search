package com.deepsearch.vector.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 向量处理配置类
 *
 * @author DeepSearch Vector Team
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(VectorProperties.class)
public class VectorConfig {

    private final VectorProperties vectorProperties;

    public VectorConfig(VectorProperties vectorProperties) {
        this.vectorProperties = vectorProperties;
    }

    /**
     * 向量处理任务执行器
     */
    @Bean("vectorTaskExecutor")
    public Executor vectorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 基于配置设置线程池参数
        int maxConcurrentTasks = vectorProperties.getTaskQueue().getMaxConcurrentTasks();

        executor.setCorePoolSize(maxConcurrentTasks);
        executor.setMaxPoolSize(maxConcurrentTasks * 2);
        executor.setQueueCapacity(vectorProperties.getTaskQueue().getMaxQueueSize());
        executor.setThreadNamePrefix("vector-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * 批量处理任务执行器
     */
    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 批量处理使用更少的线程但更大的队列
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(vectorProperties.getTaskQueue().getMaxQueueSize() * 2);
        executor.setThreadNamePrefix("vector-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * 实时处理任务执行器
     */
    @Bean("realtimeTaskExecutor")
    public Executor realtimeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 实时处理使用更多线程，更小队列，优先响应速度
        int maxConcurrentTasks = vectorProperties.getTaskQueue().getMaxConcurrentTasks();

        executor.setCorePoolSize(maxConcurrentTasks);
        executor.setMaxPoolSize(maxConcurrentTasks * 3);
        executor.setQueueCapacity(50); // 较小的队列确保快速响应
        executor.setThreadNamePrefix("vector-realtime-");
        executor.setWaitForTasksToCompleteOnShutdown(false); // 实时任务不等待完成
        executor.setAwaitTerminationSeconds(5);

        // 拒绝策略：抛异常，快速失败
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * 获取向量处理属性
     */
    @Bean
    public VectorProperties vectorProperties() {
        return vectorProperties;
    }
}