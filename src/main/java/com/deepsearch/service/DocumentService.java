package com.deepsearch.service;

import com.deepsearch.dto.DocumentCreateDto;
import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.entity.Document;
import com.deepsearch.entity.User;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档业务服务
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * 创建文档
     */
    public DocumentResponseDto createDocument(DocumentCreateDto createDto) {
        Long currentUserId = getCurrentUserId();

        // 验证用户是否存在
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", currentUserId));

        // 创建文档实体
        Document document = new Document();
        document.setTitle(createDto.getTitle());
        document.setContent(createDto.getContent());
        document.setFilePath(createDto.getFilePath());
        document.setFileType(createDto.getFileType());
        document.setFileSize(createDto.getFileSize());
        document.setStatus(Document.Status.PENDING);
        document.setUserId(currentUserId);

        Document savedDocument = documentRepository.save(document);
        log.info("文档创建成功: {} by user {}", savedDocument.getTitle(), user.getUsername());

        return new DocumentResponseDto(savedDocument);
    }

    /**
     * 根据ID获取文档
     */
    @Transactional(readOnly = true)
    public DocumentResponseDto getDocumentById(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档", "id", documentId));

        // 检查权限：只有文档所有者或管理员可以访问
        checkDocumentAccess(document);

        return new DocumentResponseDto(document);
    }

    /**
     * 更新文档
     */
    public DocumentResponseDto updateDocument(Long documentId, DocumentCreateDto updateDto) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档", "id", documentId));

        // 检查权限：只有文档所有者可以更新
        checkDocumentOwnership(document);

        // 更新文档信息
        document.setTitle(updateDto.getTitle());
        document.setContent(updateDto.getContent());
        document.setFilePath(updateDto.getFilePath());
        document.setFileType(updateDto.getFileType());
        document.setFileSize(updateDto.getFileSize());

        Document updatedDocument = documentRepository.save(document);
        log.info("文档更新成功: {}", updatedDocument.getTitle());

        return new DocumentResponseDto(updatedDocument);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档", "id", documentId));

        // 检查权限：只有文档所有者或管理员可以删除
        checkDocumentAccess(document);

        documentRepository.delete(document);
        log.info("文档删除成功: {}", document.getTitle());
    }

    /**
     * 获取用户的所有文档
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getUserDocuments(Long userId, Pageable pageable) {
        // 如果不是管理员，只能查看自己的文档
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && !currentUserId.equals(userId)) {
            throw new BadRequestException("无权访问其他用户的文档");
        }

        Page<Document> documents = documentRepository.findByUserId(userId, pageable);
        List<DocumentResponseDto> documentDtos = documents.getContent().stream()
                .map(DocumentResponseDto::new)
                .collect(Collectors.toList());

        return new PageImpl<>(documentDtos, pageable, documents.getTotalElements());
    }

    /**
     * 获取当前用户的文档
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getCurrentUserDocuments(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        return getUserDocuments(currentUserId, pageable);
    }

    /**
     * 根据状态获取文档列表
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsByStatus(Document.Status status) {
        // 只有管理员可以查看所有文档的状态
        if (!isAdmin()) {
            throw new BadRequestException("无权访问");
        }

        return documentRepository.findByStatus(status).stream()
                .map(DocumentResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 搜索文档（按标题）
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> searchDocumentsByTitle(String title) {
        Long currentUserId = getCurrentUserId();

        if (isAdmin()) {
            // 管理员可以搜索所有文档
            return documentRepository.findByTitleContainingIgnoreCase(title).stream()
                    .map(DocumentResponseDto::new)
                    .collect(Collectors.toList());
        } else {
            // 普通用户只能搜索自己的文档
            return documentRepository.findByUserIdAndTitleContainingIgnoreCase(currentUserId, title).stream()
                    .map(DocumentResponseDto::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 更新文档状态
     */
    public DocumentResponseDto updateDocumentStatus(Long documentId, Document.Status status) {
        // 只有管理员或系统可以更新文档状态
        if (!isAdmin()) {
            throw new BadRequestException("无权更新文档状态");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档", "id", documentId));

        document.setStatus(status);
        Document updatedDocument = documentRepository.save(document);
        log.info("文档状态更新成功: {} -> {}", document.getTitle(), status);

        return new DocumentResponseDto(updatedDocument);
    }

    /**
     * 获取用户文档统计
     */
    @Transactional(readOnly = true)
    public Long getUserDocumentCount(Long userId) {
        // 如果不是管理员，只能查看自己的统计
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && !currentUserId.equals(userId)) {
            throw new BadRequestException("无权访问其他用户的统计信息");
        }

        return documentRepository.countByUserId(userId);
    }

    /**
     * 获取文档状态统计
     */
    @Transactional(readOnly = true)
    public Long getDocumentCountByStatus(Document.Status status) {
        // 只有管理员可以查看全局统计
        if (!isAdmin()) {
            throw new BadRequestException("无权访问");
        }

        return documentRepository.countByStatus(status);
    }

    /**
     * 获取指定时间范围内的文档
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // 只有管理员可以查看全局统计
        if (!isAdmin()) {
            throw new BadRequestException("无权访问");
        }

        return documentRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(DocumentResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("用户未认证");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));

        return user.getId();
    }

    /**
     * 检查当前用户是否为管理员
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * 检查文档访问权限（所有者或管理员）
     */
    private void checkDocumentAccess(Document document) {
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && !document.getUserId().equals(currentUserId)) {
            throw new BadRequestException("无权访问此文档");
        }
    }

    /**
     * 检查文档所有权（只有所有者）
     */
    private void checkDocumentOwnership(Document document) {
        Long currentUserId = getCurrentUserId();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BadRequestException("无权修改此文档");
        }
    }
}