package com.deepsearch.vector.queue;

import com.deepsearch.vector.config.VectorProcessingConfig;
import com.deepsearch.vector.enums.TaskStatus;
import com.deepsearch.vector.enums.TaskType;
import com.deepsearch.vector.model.VectorTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 向量任务队列服务
 * 支持优先级队列、重试机制、并发控制
 */
@Service
public class VectorTaskQueueService {

    private static final Logger logger = LoggerFactory.getLogger(VectorTaskQueueService.class);

    @Autowired
    private VectorProcessingConfig config;

    // 使用优先级队列，按优先级和创建时间排序
    private final PriorityQueue<VectorTask> taskQueue = new PriorityQueue<>(
        Comparator.comparing(VectorTask::getPriority)
                  .thenComparing(VectorTask::getCreatedAt)
    );

    // 失败任务重试队列
    private final DelayQueue<DelayedTask> retryQueue = new DelayQueue<>();

    // 当前处理中的任务
    private final Map<Long, VectorTask> processingTasks = new ConcurrentHashMap<>();

    // 队列操作锁
    private final ReentrantLock queueLock = new ReentrantLock();

    // 线程池
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    // 统计信息
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        int maxConcurrentTasks = config.getTaskQueue().getMaxConcurrentTasks();
        executorService = Executors.newFixedThreadPool(maxConcurrentTasks);
        scheduledExecutorService = Executors.newScheduledThreadPool(2);

        // 启动重试任务处理器
        startRetryTaskProcessor();

        logger.info("向量任务队列服务已启动，最大并发任务数: {}", maxConcurrentTasks);
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        logger.info("向量任务队列服务已停止");
    }

    /**
     * 提交新任务
     */
    public boolean submitTask(Long documentId, TaskType taskType, Integer priority) {
        if (documentId == null || taskType == null) {
            logger.warn("无效的任务参数: documentId={}, taskType={}", documentId, taskType);
            return false;
        }

        VectorTask task = new VectorTask(documentId, taskType, priority != null ? priority : 5);
        task.setMaxRetries(config.getTaskQueue().getMaxRetries());

        queueLock.lock();
        try {
            taskQueue.offer(task);
            totalTasks.incrementAndGet();
            logger.debug("任务已加入队列: documentId={}, type={}, priority={}",
                        documentId, taskType, task.getPriority());
            return true;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 批量提交任务
     */
    public int submitBatchTasks(List<VectorTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        int submitted = 0;
        queueLock.lock();
        try {
            for (VectorTask task : tasks) {
                if (task.getDocumentId() != null && task.getTaskType() != null) {
                    if (task.getMaxRetries() == null) {
                        task.setMaxRetries(config.getTaskQueue().getMaxRetries());
                    }
                    taskQueue.offer(task);
                    submitted++;
                }
            }
            totalTasks.addAndGet(submitted);
            logger.info("批量提交任务完成: 提交数量={}", submitted);
        } finally {
            queueLock.unlock();
        }

        return submitted;
    }

    /**
     * 获取下一个待处理任务
     */
    public VectorTask getNextTask() {
        queueLock.lock();
        try {
            VectorTask task = taskQueue.poll();
            if (task != null) {
                task.setStatus(TaskStatus.PROCESSING);
                task.setStartedAt(LocalDateTime.now());
                processingTasks.put(task.getId(), task);
                logger.debug("任务开始处理: documentId={}, type={}",
                           task.getDocumentId(), task.getTaskType());
            }
            return task;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 批量获取任务
     */
    public List<VectorTask> getNextBatch() {
        return getNextBatch(config.getTaskQueue().getBatchSize());
    }

    /**
     * 批量获取指定数量的任务
     */
    public List<VectorTask> getNextBatch(int batchSize) {
        List<VectorTask> batch = new ArrayList<>();

        queueLock.lock();
        try {
            for (int i = 0; i < batchSize && !taskQueue.isEmpty(); i++) {
                VectorTask task = taskQueue.poll();
                if (task != null) {
                    task.setStatus(TaskStatus.PROCESSING);
                    task.setStartedAt(LocalDateTime.now());
                    processingTasks.put(task.getId(), task);
                    batch.add(task);
                }
            }

            if (!batch.isEmpty()) {
                logger.debug("批量获取任务: 数量={}", batch.size());
            }
        } finally {
            queueLock.unlock();
        }

        return batch;
    }

    /**
     * 标记任务完成
     */
    public void markTaskCompleted(Long taskId) {
        VectorTask task = processingTasks.remove(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            completedTasks.incrementAndGet();
            logger.debug("任务已完成: documentId={}, 处理时间={}ms",
                        task.getDocumentId(),
                        task.getStartedAt() != null && task.getCompletedAt() != null
                            ? java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis()
                            : 0);
        }
    }

    /**
     * 标记任务失败并安排重试
     */
    public void markTaskFailed(Long taskId, String errorMessage) {
        VectorTask task = processingTasks.remove(taskId);
        if (task != null) {
            task.setErrorMessage(errorMessage);
            task.incrementRetryCount();

            if (task.canRetry()) {
                // 安排重试
                long delayMs = config.getTaskQueue().getRetryDelaySeconds().toMillis()
                             * (long) Math.pow(2, task.getRetryCount() - 1); // 指数退避
                DelayedTask delayedTask = new DelayedTask(task, delayMs);
                retryQueue.offer(delayedTask);

                logger.warn("任务失败，安排重试: documentId={}, 重试次数={}/{}, 延迟={}ms, 错误={}",
                           task.getDocumentId(), task.getRetryCount(), task.getMaxRetries(),
                           delayMs, errorMessage);
            } else {
                // 重试次数耗尽，标记为最终失败
                task.setStatus(TaskStatus.FAILED);
                task.setCompletedAt(LocalDateTime.now());
                failedTasks.incrementAndGet();

                logger.error("任务最终失败: documentId={}, 重试次数={}, 错误={}",
                            task.getDocumentId(), task.getRetryCount(), errorMessage);
            }
        }
    }

    /**
     * 获取队列状态
     */
    public QueueStatus getQueueStatus() {
        queueLock.lock();
        try {
            return new QueueStatus(
                taskQueue.size(),
                processingTasks.size(),
                retryQueue.size(),
                totalTasks.get(),
                completedTasks.get(),
                failedTasks.get()
            );
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 清空队列
     */
    public void clearQueue() {
        queueLock.lock();
        try {
            taskQueue.clear();
            retryQueue.clear();
            logger.info("队列已清空");
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 启动重试任务处理器
     */
    private void startRetryTaskProcessor() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                DelayedTask delayedTask = retryQueue.poll();
                if (delayedTask != null) {
                    VectorTask task = delayedTask.getTask();
                    task.setStatus(TaskStatus.PENDING);

                    queueLock.lock();
                    try {
                        taskQueue.offer(task);
                        logger.debug("重试任务已重新加入队列: documentId={}, 重试次数={}",
                                   task.getDocumentId(), task.getRetryCount());
                    } finally {
                        queueLock.unlock();
                    }
                }
            } catch (Exception e) {
                logger.error("重试任务处理器异常", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 延迟任务包装类
     */
    private static class DelayedTask implements Delayed {
        private final VectorTask task;
        private final long executeTime;

        public DelayedTask(VectorTask task, long delayMs) {
            this.task = task;
            this.executeTime = System.currentTimeMillis() + delayMs;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = executeTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.executeTime, ((DelayedTask) o).executeTime);
        }

        public VectorTask getTask() {
            return task;
        }
    }

    /**
     * 队列状态信息
     */
    public static class QueueStatus {
        private final int pendingTasks;
        private final int processingTasks;
        private final int retryTasks;
        private final int totalTasks;
        private final int completedTasks;
        private final int failedTasks;

        public QueueStatus(int pendingTasks, int processingTasks, int retryTasks,
                          int totalTasks, int completedTasks, int failedTasks) {
            this.pendingTasks = pendingTasks;
            this.processingTasks = processingTasks;
            this.retryTasks = retryTasks;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.failedTasks = failedTasks;
        }

        // Getters
        public int getPendingTasks() { return pendingTasks; }
        public int getProcessingTasks() { return processingTasks; }
        public int getRetryTasks() { return retryTasks; }
        public int getTotalTasks() { return totalTasks; }
        public int getCompletedTasks() { return completedTasks; }
        public int getFailedTasks() { return failedTasks; }

        public double getSuccessRate() {
            return totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;
        }
    }
}