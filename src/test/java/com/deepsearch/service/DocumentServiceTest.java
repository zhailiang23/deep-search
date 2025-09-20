package com.deepsearch.service;

import com.deepsearch.dto.DocumentCreateDto;
import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.entity.Document;
import com.deepsearch.entity.User;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文档服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private Document testDocument;
    private DocumentCreateDto createDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(User.Role.USER);
        testUser.setCreatedAt(LocalDateTime.now());

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setTitle("Test Document");
        testDocument.setContent("This is test content");
        testDocument.setFilePath("/test/path");
        testDocument.setFileType("txt");
        testDocument.setFileSize(1024L);
        testDocument.setStatus(Document.Status.PENDING);
        testDocument.setUserId(testUser.getId());
        testDocument.setUser(testUser);
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setUpdatedAt(LocalDateTime.now());

        createDto = new DocumentCreateDto();
        createDto.setTitle("Test Document");
        createDto.setContent("This is test content");
        createDto.setFilePath("/test/path");
        createDto.setFileType("txt");
        createDto.setFileSize(1024L);
    }

    private void mockSecurityContext(String username, String... roles) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        when(authentication.getAuthorities()).thenReturn((List) authorities);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createDocument_Success() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        // When
        DocumentResponseDto result = documentService.createDocument(createDto);

        // Then
        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        assertEquals(testDocument.getTitle(), result.getTitle());
        assertEquals(testDocument.getContent(), result.getContent());
        assertEquals(Document.Status.PENDING, result.getStatus());

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findById(testUser.getId());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void getDocumentById_Success_Owner() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        DocumentResponseDto result = documentService.getDocumentById(testDocument.getId());

        // Then
        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        assertEquals(testDocument.getTitle(), result.getTitle());

        verify(documentRepository).findById(testDocument.getId());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getDocumentById_Success_Admin() {
        // Given
        mockSecurityContext("admin", "ADMIN");
        User adminUser = new User();
        adminUser.setId(999L);
        adminUser.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        DocumentResponseDto result = documentService.getDocumentById(testDocument.getId());

        // Then
        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());

        verify(documentRepository).findById(testDocument.getId());
        verify(userRepository).findByUsername("admin");
    }

    @Test
    void getDocumentById_NotFound_ThrowsResourceNotFoundException() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> documentService.getDocumentById(999L));

        assertTrue(exception.getMessage().contains("文档"));
        verify(documentRepository).findById(999L);
    }

    @Test
    void getDocumentById_AccessDenied_ThrowsBadRequestException() {
        // Given
        mockSecurityContext("otheruser", "USER");
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> documentService.getDocumentById(testDocument.getId()));

        assertTrue(exception.getMessage().contains("无权访问"));
        verify(documentRepository).findById(testDocument.getId());
    }

    @Test
    void updateDocument_Success() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentCreateDto updateDto = new DocumentCreateDto();
        updateDto.setTitle("Updated Title");
        updateDto.setContent("Updated content");

        // When
        DocumentResponseDto result = documentService.updateDocument(testDocument.getId(), updateDto);

        // Then
        assertNotNull(result);
        verify(documentRepository).findById(testDocument.getId());
        verify(documentRepository).save(testDocument);
    }

    @Test
    void updateDocument_NotOwner_ThrowsBadRequestException() {
        // Given
        mockSecurityContext("otheruser", "USER");
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> documentService.updateDocument(testDocument.getId(), createDto));

        assertTrue(exception.getMessage().contains("无权修改"));
        verify(documentRepository).findById(testDocument.getId());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void deleteDocument_Success_Owner() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        documentService.deleteDocument(testDocument.getId());

        // Then
        verify(documentRepository).findById(testDocument.getId());
        verify(documentRepository).delete(testDocument);
    }

    @Test
    void getCurrentUserDocuments_Success() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        List<Document> documents = Arrays.asList(testDocument);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, 1);

        when(documentRepository.findByUserId(testUser.getId(), pageable)).thenReturn(documentPage);

        // When
        Page<DocumentResponseDto> result = documentService.getCurrentUserDocuments(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testDocument.getTitle(), result.getContent().get(0).getTitle());

        verify(userRepository).findByUsername("testuser");
        verify(documentRepository).findByUserId(testUser.getId(), pageable);
    }

    @Test
    void searchDocumentsByTitle_Success_User() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByUserIdAndTitleContainingIgnoreCase(testUser.getId(), "Test"))
                .thenReturn(documents);

        // When
        List<DocumentResponseDto> result = documentService.searchDocumentsByTitle("Test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getTitle(), result.get(0).getTitle());

        verify(userRepository).findByUsername("testuser");
        verify(documentRepository).findByUserIdAndTitleContainingIgnoreCase(testUser.getId(), "Test");
    }

    @Test
    void searchDocumentsByTitle_Success_Admin() {
        // Given
        mockSecurityContext("admin", "ADMIN");
        User adminUser = new User();
        adminUser.setId(999L);
        adminUser.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByTitleContainingIgnoreCase("Test")).thenReturn(documents);

        // When
        List<DocumentResponseDto> result = documentService.searchDocumentsByTitle("Test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getTitle(), result.get(0).getTitle());

        verify(userRepository).findByUsername("admin");
        verify(documentRepository).findByTitleContainingIgnoreCase("Test");
    }

    @Test
    void updateDocumentStatus_Success_Admin() {
        // Given
        mockSecurityContext("admin", "ADMIN");
        User adminUser = new User();
        adminUser.setId(999L);
        adminUser.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        // When
        DocumentResponseDto result = documentService.updateDocumentStatus(testDocument.getId(), Document.Status.INDEXED);

        // Then
        assertNotNull(result);
        verify(documentRepository).findById(testDocument.getId());
        verify(documentRepository).save(testDocument);
    }

    @Test
    void updateDocumentStatus_NotAdmin_ThrowsBadRequestException() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> documentService.updateDocumentStatus(testDocument.getId(), Document.Status.INDEXED));

        assertTrue(exception.getMessage().contains("无权更新"));
        verify(documentRepository, never()).findById(any());
    }

    @Test
    void getUserDocumentCount_Success() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.countByUserId(testUser.getId())).thenReturn(5L);

        // When
        Long result = documentService.getUserDocumentCount(testUser.getId());

        // Then
        assertEquals(5L, result);
        verify(documentRepository).countByUserId(testUser.getId());
    }

    @Test
    void getDocumentsByStatus_Success_Admin() {
        // Given
        mockSecurityContext("admin", "ADMIN");
        User adminUser = new User();
        adminUser.setId(999L);
        adminUser.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByStatus(Document.Status.INDEXED)).thenReturn(documents);

        // When
        List<DocumentResponseDto> result = documentService.getDocumentsByStatus(Document.Status.INDEXED);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(documentRepository).findByStatus(Document.Status.INDEXED);
    }

    @Test
    void getDocumentsByStatus_NotAdmin_ThrowsBadRequestException() {
        // Given
        mockSecurityContext("testuser", "USER");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> documentService.getDocumentsByStatus(Document.Status.INDEXED));

        assertTrue(exception.getMessage().contains("无权访问"));
        verify(documentRepository, never()).findByStatus(any());
    }
}