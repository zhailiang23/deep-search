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
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();
    }
}