package com.deepsearch.vector.scheduler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.deepsearch.vector.config.VectorProcessingConfig;
import com.deepsearch.vector.enums.ProcessingMode;
import com.deepsearch.vector.model.ProcessingContext;
import com.deepsearch.vector.model.ProcessingMetrics;
import com.deepsearch.vector.model.VectorTask;
import com.deepsearch.vector.monitoring.VectorMetricsCollector;
import com.deepsearch.vector.queue.VectorTaskQueueService;
import com.deepsearch.vector.strategy.ModeSwitchStrategy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 向量处理批量调度器
 * 负责批量文档处理的调度和执行
 */
@Service
public class VectorProcessingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VectorProcessingScheduler.class);

    @Autowired
    private VectorProcessingConfig config;

    @Autowired
    private VectorTaskQueueService queueService;

    @Autowired
    private ModeSwitchStrategy modeSwitchStrategy;

    @Autowired
    private VectorMetricsCollector metricsCollector;

    private ExecutorService batchExecutorService;
    private ScheduledExecutorService scheduledExecutorService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    // 当前处理模式
    private volatile ProcessingMode currentMode = ProcessingMode.AUTO_SWITCH;

    @PostConstruct
    public void init() {
        int maxConcurrentTasks = config.getTaskQueue().getMaxConcurrentTasks();
        batchExecutorService = Executors.newFixedThreadPool(maxConcurrentTasks);
        scheduledExecutorService = Executors.newScheduledThreadPool(3);

        // 启动批处理调度器
        startBatchScheduler();

        // 启动模式监控器
        startModeMonitor();

        logger.info("向量处理调度器已启动，最大并发任务数: {}", maxConcurrentTasks);
    }

    @PreDestroy
    public void destroy() {
        stop();
        if (batchExecutorService != null) {
            batchExecutorService.shutdown();
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        logger.info("向量处理调度器已停止");
    }

    /**
     * 启动调度器
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            isPaused.set(false);
            logger.info("调度器已启动");
        }
    }

    /**
     * 停止调度器
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            isPaused.set(false);
            logger.info("调度器已停止");
        }
    }

    /**
     * 暂停调度器
     */
    public void pause() {
        if (isRunning.get()) {
            isPaused.set(true);
            logger.info("调度器已暂停");
        }
    }

    /**
     * 恢复调度器
     */
    public void resume() {
        if (isRunning.get()) {
            isPaused.set(false);
            logger.info("调度器已恢复");
        }
    }

    /**
     * 手动切换处理模式
     */
    public void switchMode(ProcessingMode mode) {
        ProcessingMode oldMode = this.currentMode;
        this.currentMode = mode;
        logger.info("处理模式已切换: {} -> {}", oldMode, mode);
    }

    /**
     * 启动批处理调度器
     */
    private void startBatchScheduler() {
        scheduledExecutorService.scheduleWithFixedDelay(this::processBatch,
                                                        5, 2, TimeUnit.SECONDS);
        isRunning.set(true);
    }

    /**
     * 启动模式监控器
     */
    private void startModeMonitor() {
        scheduledExecutorService.scheduleWithFixedDelay(this::monitorAndSwitchMode,
                                                        10, 30, TimeUnit.SECONDS);
    }

    /**
     * 批处理执行逻辑
     */
    private void processBatch() {
        if (!isRunning.get() || isPaused.get()) {
            return;
        }

        try {
            // 获取当前处理模式
            ProcessingMode effectiveMode = determineEffectiveMode();

            if (effectiveMode == ProcessingMode.OFFLINE_BATCH) {
                processBatchMode();
            } else if (effectiveMode == ProcessingMode.ONLINE_REALTIME) {
                processOnlineMode();
            }
        } catch (Exception e) {
            logger.error("批处理调度异常", e);
        }
    }

    /**
     * 离线批处理模式
     */
    private void processBatchMode() {
        List<VectorTask> batch = queueService.getNextBatch();
        if (batch.isEmpty()) {
            return;
        }

        logger.debug("开始批处理: 任务数量={}", batch.size());

        // 将批次分组并并行处理
        List<List<VectorTask>> chunks = partitionTasks(batch, getOptimalChunkSize());

        List<CompletableFuture<Void>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.runAsync(() -> processTaskChunk(chunk), batchExecutorService))
                .toList();

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("批处理任务执行异常", throwable);
                    } else {
                        logger.debug("批处理完成: 任务数量={}", batch.size());
                    }
                });
    }

    /**
     * 在线实时处理模式
     */
    private void processOnlineMode() {
        // 在线模式主要处理高优先级任务
        VectorTask task = queueService.getNextTask();
        if (task != null && (task.getPriority() <= 3)) { // 高优先级任务
            CompletableFuture.runAsync(() -> processTask(task), batchExecutorService);
        }
    }

    /**
     * 处理单个任务
     */
    private void processTask(VectorTask task) {
        long startTime = System.currentTimeMillis();
        try {
            logger.debug("开始处理任务: documentId={}, type={}",
                        task.getDocumentId(), task.getTaskType());

            // 模拟向量处理
            simulateVectorProcessing(task);

            queueService.markTaskCompleted(task.getId());

            long processingTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordTaskCompletion(processingTime, currentMode);

        } catch (Exception e) {
            logger.error("任务处理失败: documentId={}, error={}",
                        task.getDocumentId(), e.getMessage());
            queueService.markTaskFailed(task.getId(), e.getMessage());
            metricsCollector.recordTaskFailure(currentMode);
        }
    }

    /**
     * 处理任务块
     */
    private void processTaskChunk(List<VectorTask> tasks) {
        for (VectorTask task : tasks) {
            try {
                processTask(task);

                // 添加小延迟避免过度并发
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("任务处理被中断");
                break;
            }
        }
    }

    /**
     * 模拟向量处理（实际实现中应该调用真实的向量处理服务）
     */
    private void simulateVectorProcessing(VectorTask task) throws Exception {
        // 模拟不同类型任务的处理时间
        int processingTime = switch (task.getTaskType()) {
            case INITIAL -> 1000 + (int) (Math.random() * 2000); // 1-3秒
            case REPROCESS -> 800 + (int) (Math.random() * 1200); // 0.8-2秒
            case QUALITY_CHECK -> 500 + (int) (Math.random() * 1000); // 0.5-1.5秒
        };

        Thread.sleep(processingTime);

        // 模拟偶发的处理失败
        if (Math.random() < 0.05) { // 5%失败率
            throw new RuntimeException("模拟的处理失败");
        }
    }

    /**
     * 将任务列表分割为更小的块
     */
    private List<List<VectorTask>> partitionTasks(List<VectorTask> tasks, int chunkSize) {
        List<List<VectorTask>> chunks = new CopyOnWriteArrayList<>();
        for (int i = 0; i < tasks.size(); i += chunkSize) {
            chunks.add(tasks.subList(i, Math.min(i + chunkSize, tasks.size())));
        }
        return chunks;
    }

    /**
     * 获取最优的块大小
     */
    private int getOptimalChunkSize() {
        ProcessingMetrics metrics = metricsCollector.getCurrentMetrics();

        // 根据系统负载动态调整块大小
        if (metrics.getSystemLoadAverage() > 0.8) {
            return 5; // 高负载时使用较小的块
        } else if (metrics.getSystemLoadAverage() > 0.5) {
            return 10; // 中等负载
        } else {
            return 20; // 低负载时使用较大的块
        }
    }

    /**
     * 确定有效处理模式
     */
    private ProcessingMode determineEffectiveMode() {
        if (currentMode != ProcessingMode.AUTO_SWITCH) {
            return currentMode;
        }

        // 创建虚拟处理上下文用于模式决策
        ProcessingContext context = new ProcessingContext(
            null, null, ProcessingMode.AUTO_SWITCH, 5, false, null, null
        );

        ProcessingMetrics metrics = metricsCollector.getCurrentMetrics();
        return modeSwitchStrategy.determineOptimalMode(context, metrics);
    }

    /**
     * 监控并自动切换模式
     */
    private void monitorAndSwitchMode() {
        if (currentMode != ProcessingMode.AUTO_SWITCH) {
            return; // 手动模式不进行自动切换
        }

        try {
            ProcessingMetrics metrics = metricsCollector.getCurrentMetrics();
            ProcessingContext context = new ProcessingContext(
                null, null, ProcessingMode.AUTO_SWITCH, 5, false, null, null
            );

            ProcessingMode optimalMode = modeSwitchStrategy.determineOptimalMode(context, metrics);
            ProcessingMode actualCurrentMode = determineEffectiveMode();

            if (optimalMode != actualCurrentMode) {
                logger.info("系统建议切换处理模式: {} -> {}", actualCurrentMode, optimalMode);
                // 注意：这里不直接切换，而是记录建议，实际切换可能需要更复杂的逻辑
            }
        } catch (Exception e) {
            logger.error("模式监控异常", e);
        }
    }

    /**
     * 定时健康检查
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void healthCheck() {
        try {
            VectorTaskQueueService.QueueStatus status = queueService.getQueueStatus();

            logger.info("调度器健康检查 - 队列状态: 待处理={}, 处理中={}, 成功率={:.2f}%, 当前模式={}",
                       status.getPendingTasks(),
                       status.getProcessingTasks(),
                       status.getSuccessRate() * 100,
                       currentMode);

            // 检查异常情况
            if (status.getPendingTasks() > config.getModeSwitching().getQueueSizeThreshold() * 2) {
                logger.warn("队列积压严重: 待处理任务数={}", status.getPendingTasks());
            }

            if (status.getSuccessRate() < 0.9) {
                logger.warn("任务成功率较低: {:.2f}%", status.getSuccessRate() * 100);
            }

        } catch (Exception e) {
            logger.error("健康检查异常", e);
        }
    }

    /**
     * 获取调度器状态
     */
    public SchedulerStatus getStatus() {
        return new SchedulerStatus(
            isRunning.get(),
            isPaused.get(),
            currentMode,
            queueService.getQueueStatus(),
            metricsCollector.getCurrentMetrics()
        );
    }

    /**
     * 调度器状态信息
     */
    public static class SchedulerStatus {
        private final boolean running;
        private final boolean paused;
        private final ProcessingMode currentMode;
        private final VectorTaskQueueService.QueueStatus queueStatus;
        private final ProcessingMetrics metrics;

        public SchedulerStatus(boolean running, boolean paused, ProcessingMode currentMode,
                             VectorTaskQueueService.QueueStatus queueStatus, ProcessingMetrics metrics) {
            this.running = running;
            this.paused = paused;
            this.currentMode = currentMode;
            this.queueStatus = queueStatus;
            this.metrics = metrics;
        }

        // Getters
        public boolean isRunning() { return running; }
        public boolean isPaused() { return paused; }
        public ProcessingMode getCurrentMode() { return currentMode; }
        public VectorTaskQueueService.QueueStatus getQueueStatus() { return queueStatus; }
        public ProcessingMetrics getMetrics() { return metrics; }
    }
}