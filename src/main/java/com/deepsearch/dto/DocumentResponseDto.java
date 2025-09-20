package com.deepsearch.dto;

import com.deepsearch.entity.Document;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档响应DTO
 */
@Data
public class DocumentResponseDto {

    private Long id;
    private String title;
    private String content;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Document.Status status;
    private Long userId;
    private String username; // 用户名，用于显示
    private String summary; // 文档摘要
    private String category; // 文档分类
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponseDto(Document document) {
        this.id = document.getId();
        this.title = document.getTitle();
        this.content = document.getContent();
        this.filePath = document.getFilePath();
        this.fileType = document.getFileType();
        this.fileSize = document.getFileSize();
        this.status = document.getStatus();
        this.userId = document.getUserId();
        this.username = document.getUser() != null ? document.getUser().getUsername() : null;
        this.summary = extractSummary(document.getContent()); // 从内容中提取摘要
        this.category = "default"; // 默认分类，实际应该从document获取
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();
    }

    // 从内容中提取摘要的辅助方法
    private String extractSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 简单的摘要提取：取前200个字符
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}