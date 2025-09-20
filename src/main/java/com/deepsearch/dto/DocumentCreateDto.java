package com.deepsearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档创建DTO
 */
@Data
public class DocumentCreateDto {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "文档标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    @Size(max = 500, message = "文件路径长度不能超过500个字符")
    private String filePath;

    @Size(max = 50, message = "文件类型长度不能超过50个字符")
    private String fileType;

    private Long fileSize;
}