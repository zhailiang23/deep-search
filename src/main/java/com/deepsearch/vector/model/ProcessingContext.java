package com.deepsearch.vector.model;

import com.deepsearch.vector.enums.ProcessingMode;

/**
 * 向量处理上下文
 */
public class ProcessingContext {
    private final Long documentId;
    private final String content;
    private final ProcessingMode requestedMode;
    private final Integer priority;
    private final boolean requiresHighQuality;
    private final Long maxLatencyMs;
    private final Integer maxCostCents;

    public ProcessingContext(Long documentId, String content, ProcessingMode requestedMode,
                           Integer priority, boolean requiresHighQuality,
                           Long maxLatencyMs, Integer maxCostCents) {
        this.documentId = documentId;
        this.content = content;
        this.requestedMode = requestedMode;
        this.priority = priority;
        this.requiresHighQuality = requiresHighQuality;
        this.maxLatencyMs = maxLatencyMs;
        this.maxCostCents = maxCostCents;
    }

    // Getters
    public Long getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public ProcessingMode getRequestedMode() { return requestedMode; }
    public Integer getPriority() { return priority; }
    public boolean isRequiresHighQuality() { return requiresHighQuality; }
    public Long getMaxLatencyMs() { return maxLatencyMs; }
    public Integer getMaxCostCents() { return maxCostCents; }
}