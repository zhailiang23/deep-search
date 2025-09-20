package com.deepsearch.repository;

import com.deepsearch.entity.Document;
import com.deepsearch.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentRepository 单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class DocumentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentRepository documentRepository;

    private User testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashed_password");
        testUser.setRole(User.Role.USER);
        testUser = entityManager.persistAndFlush(testUser);

        // 创建测试文档
        testDocument = new Document();
        testDocument.setTitle("Test Document");
        testDocument.setContent("This is a test document content");
        testDocument.setFilePath("/path/to/test.pdf");
        testDocument.setFileType("pdf");
        testDocument.setFileSize(1024L);
        testDocument.setStatus(Document.Status.PENDING);
        testDocument.setUserId(testUser.getId());
    }

    @Test
    void testSaveDocument() {
        // When
        Document savedDocument = documentRepository.save(testDocument);

        // Then
        assertThat(savedDocument).isNotNull();
        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedDocument.getTitle()).isEqualTo("Test Document");
        assertThat(savedDocument.getContent()).isEqualTo("This is a test document content");
        assertThat(savedDocument.getFilePath()).isEqualTo("/path/to/test.pdf");
        assertThat(savedDocument.getFileType()).isEqualTo("pdf");
        assertThat(savedDocument.getFileSize()).isEqualTo(1024L);
        assertThat(savedDocument.getStatus()).isEqualTo(Document.Status.PENDING);
        assertThat(savedDocument.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedDocument.getCreatedAt()).isNotNull();
        assertThat(savedDocument.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByUserId() {
        // Given
        Document document2 = new Document();
        document2.setTitle("Second Document");
        document2.setContent("Another test document");
        document2.setStatus(Document.Status.INDEXED);
        document2.setUserId(testUser.getId());

        entityManager.persistAndFlush(testDocument);
        entityManager.persistAndFlush(document2);

        // When
        List<Document> userDocuments = documentRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userDocuments).hasSize(2);
        assertThat(userDocuments).extracting(Document::getTitle)
                .containsExactlyInAnyOrder("Test Document", "Second Document");
    }

    @Test
    void testFindByUserIdPaginated() {
        // Given
        for (int i = 0; i < 5; i++) {
            Document doc = new Document();
            doc.setTitle("Document " + i);
            doc.setContent("Content " + i);
            doc.setStatus(Document.Status.PENDING);
            doc.setUserId(testUser.getId());
            entityManager.persistAndFlush(doc);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // When
        Page<Document> userDocuments = documentRepository.findByUserId(testUser.getId(), pageable);

        // Then
        assertThat(userDocuments.getContent()).hasSize(3);
        assertThat(userDocuments.getTotalElements()).isEqualTo(5);
        assertThat(userDocuments.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByStatus() {
        // Given
        Document pendingDoc = new Document();
        pendingDoc.setTitle("Pending Document");
        pendingDoc.setContent("Pending content");
        pendingDoc.setStatus(Document.Status.PENDING);
        pendingDoc.setUserId(testUser.getId());

        Document indexedDoc = new Document();
        indexedDoc.setTitle("Indexed Document");
        indexedDoc.setContent("Indexed content");
        indexedDoc.setStatus(Document.Status.INDEXED);
        indexedDoc.setUserId(testUser.getId());

        entityManager.persistAndFlush(pendingDoc);
        entityManager.persistAndFlush(indexedDoc);

        // When
        List<Document> pendingDocuments = documentRepository.findByStatus(Document.Status.PENDING);
        List<Document> indexedDocuments = documentRepository.findByStatus(Document.Status.INDEXED);

        // Then
        assertThat(pendingDocuments).hasSize(1);
        assertThat(pendingDocuments.get(0).getTitle()).isEqualTo("Pending Document");
        assertThat(indexedDocuments).hasSize(1);
        assertThat(indexedDocuments.get(0).getTitle()).isEqualTo("Indexed Document");
    }

    @Test
    void testFindByUserIdAndStatus() {
        // Given
        Document pendingDoc = new Document();
        pendingDoc.setTitle("User Pending Document");
        pendingDoc.setContent("User pending content");
        pendingDoc.setStatus(Document.Status.PENDING);
        pendingDoc.setUserId(testUser.getId());

        entityManager.persistAndFlush(testDocument); // PENDING status
        entityManager.persistAndFlush(pendingDoc); // Also PENDING status

        // When
        List<Document> userPendingDocs = documentRepository.findByUserIdAndStatus(
                testUser.getId(), Document.Status.PENDING);

        // Then
        assertThat(userPendingDocs).hasSize(2);
    }

    @Test
    void testFindByFileType() {
        // Given
        Document pdfDoc = new Document();
        pdfDoc.setTitle("PDF Document");
        pdfDoc.setContent("PDF content");
        pdfDoc.setFileType("pdf");
        pdfDoc.setStatus(Document.Status.PENDING);
        pdfDoc.setUserId(testUser.getId());

        Document txtDoc = new Document();
        txtDoc.setTitle("Text Document");
        txtDoc.setContent("Text content");
        txtDoc.setFileType("txt");
        txtDoc.setStatus(Document.Status.PENDING);
        txtDoc.setUserId(testUser.getId());

        entityManager.persistAndFlush(pdfDoc);
        entityManager.persistAndFlush(txtDoc);

        // When
        List<Document> pdfDocuments = documentRepository.findByFileType("pdf");

        // Then
        assertThat(pdfDocuments).hasSize(1);
        assertThat(pdfDocuments.get(0).getTitle()).isEqualTo("PDF Document");
    }

    @Test
    void testFindByTitleContainingIgnoreCase() {
        // Given
        Document doc1 = new Document();
        doc1.setTitle("Important Report");
        doc1.setContent("Report content");
        doc1.setStatus(Document.Status.PENDING);
        doc1.setUserId(testUser.getId());

        Document doc2 = new Document();
        doc2.setTitle("research paper");
        doc2.setContent("Research content");
        doc2.setStatus(Document.Status.PENDING);
        doc2.setUserId(testUser.getId());

        entityManager.persistAndFlush(doc1);
        entityManager.persistAndFlush(doc2);

        // When
        List<Document> reportDocs = documentRepository.findByTitleContainingIgnoreCase("report");

        // Then
        assertThat(reportDocs).hasSize(1);
        assertThat(reportDocs.get(0).getTitle()).isEqualTo("Important Report");
    }

    @Test
    void testFindByContentContainingIgnoreCase() {
        // Given
        entityManager.persistAndFlush(testDocument);

        // When
        List<Document> foundDocs = documentRepository.findByContentContainingIgnoreCase("TEST");

        // Then
        assertThat(foundDocs).hasSize(1);
        assertThat(foundDocs.get(0).getContent()).contains("test document content");
    }

    @Test
    void testCountByUserId() {
        // Given
        Document doc2 = new Document();
        doc2.setTitle("Second Document");
        doc2.setContent("Second content");
        doc2.setStatus(Document.Status.PENDING);
        doc2.setUserId(testUser.getId());

        entityManager.persistAndFlush(testDocument);
        entityManager.persistAndFlush(doc2);

        // When
        Long count = documentRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testCountByStatus() {
        // Given
        Document pendingDoc = new Document();
        pendingDoc.setTitle("Pending Document");
        pendingDoc.setContent("Pending content");
        pendingDoc.setStatus(Document.Status.PENDING);
        pendingDoc.setUserId(testUser.getId());

        Document indexedDoc = new Document();
        indexedDoc.setTitle("Indexed Document");
        indexedDoc.setContent("Indexed content");
        indexedDoc.setStatus(Document.Status.INDEXED);
        indexedDoc.setUserId(testUser.getId());

        entityManager.persistAndFlush(pendingDoc);
        entityManager.persistAndFlush(indexedDoc);

        // When
        Long pendingCount = documentRepository.countByStatus(Document.Status.PENDING);
        Long indexedCount = documentRepository.countByStatus(Document.Status.INDEXED);

        // Then
        assertThat(pendingCount).isEqualTo(1L);
        assertThat(indexedCount).isEqualTo(1L);
    }

    @Test
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        entityManager.persistAndFlush(testDocument);

        // When
        List<Document> foundDocs = documentRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(foundDocs).hasSize(1);
        assertThat(foundDocs.get(0).getTitle()).isEqualTo("Test Document");
    }

    @Test
    void testFindRecentDocumentsByUserId() {
        // Given
        for (int i = 0; i < 3; i++) {
            Document doc = new Document();
            doc.setTitle("Recent Document " + i);
            doc.setContent("Recent content " + i);
            doc.setStatus(Document.Status.PENDING);
            doc.setUserId(testUser.getId());
            entityManager.persistAndFlush(doc);
        }

        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<Document> recentDocs = documentRepository.findRecentDocumentsByUserId(testUser.getId(), pageable);

        // Then
        assertThat(recentDocs).hasSize(2);
        // 验证结果是按创建时间降序排列的
        LocalDateTime firstCreatedAt = recentDocs.get(0).getCreatedAt();
        LocalDateTime secondCreatedAt = recentDocs.get(1).getCreatedAt();
        assertThat(firstCreatedAt).isAfterOrEqualTo(secondCreatedAt);
    }

    @Test
    void testFindByFileSizeBetween() {
        // Given
        Document smallDoc = new Document();
        smallDoc.setTitle("Small Document");
        smallDoc.setContent("Small content");
        smallDoc.setFileSize(500L);
        smallDoc.setStatus(Document.Status.PENDING);
        smallDoc.setUserId(testUser.getId());

        Document largeDoc = new Document();
        largeDoc.setTitle("Large Document");
        largeDoc.setContent("Large content");
        largeDoc.setFileSize(2000L);
        largeDoc.setStatus(Document.Status.PENDING);
        largeDoc.setUserId(testUser.getId());

        entityManager.persistAndFlush(testDocument); // Size: 1024L
        entityManager.persistAndFlush(smallDoc);
        entityManager.persistAndFlush(largeDoc);

        // When
        List<Document> mediumSizeDocs = documentRepository.findByFileSizeBetween(1000L, 1500L);

        // Then
        assertThat(mediumSizeDocs).hasSize(1);
        assertThat(mediumSizeDocs.get(0).getTitle()).isEqualTo("Test Document");
    }

    @Test
    void testExistsByUserIdAndTitle() {
        // Given
        entityManager.persistAndFlush(testDocument);

        // When
        Boolean exists = documentRepository.existsByUserIdAndTitle(testUser.getId(), "Test Document");
        Boolean notExists = documentRepository.existsByUserIdAndTitle(testUser.getId(), "Nonexistent Document");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testDeleteDocument() {
        // Given
        Document persistedDoc = entityManager.persistAndFlush(testDocument);
        Long docId = persistedDoc.getId();

        // When
        documentRepository.deleteById(docId);
        entityManager.flush();

        // Then
        Optional<Document> deletedDoc = documentRepository.findById(docId);
        assertThat(deletedDoc).isEmpty();
    }

    @Test
    void testUpdateDocument() {
        // Given
        Document persistedDoc = entityManager.persistAndFlush(testDocument);

        // When
        persistedDoc.setTitle("Updated Title");
        persistedDoc.setStatus(Document.Status.INDEXED);
        Document updatedDoc = documentRepository.save(persistedDoc);
        entityManager.flush();

        // Then
        assertThat(updatedDoc.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedDoc.getStatus()).isEqualTo(Document.Status.INDEXED);
        assertThat(updatedDoc.getUpdatedAt()).isAfter(updatedDoc.getCreatedAt());
    }
}