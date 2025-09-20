package com.deepsearch.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.deepsearch.elasticsearch.config.ElasticsearchIndexConfig;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.vector.service.VectorProcessingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentIndexService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexService.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchIndexConfig indexConfig;

    @Autowired
    private VectorProcessingEngine vectorProcessingEngine;

    public String indexDocument(DocumentIndex document) throws IOException {
        try {
            // Generate vectors if not provided
            if (document.getContentVector() == null && document.getContent() != null) {
                List<Float> contentVector = vectorProcessingEngine.generateEmbedding(document.getContent());
                document.setContentVector(contentVector);
            }

            if (document.getTitleVector() == null && document.getTitle() != null) {
                List<Float> titleVector = vectorProcessingEngine.generateEmbedding(document.getTitle());
                document.setTitleVector(titleVector);
            }

            // Update timestamp
            document.setUpdatedAt(LocalDateTime.now());

            IndexRequest<DocumentIndex> indexRequest = IndexRequest.of(i -> i
                .index(indexConfig.getDocumentIndexName())
                .id(document.getId())
                .document(document)
            );

            IndexResponse response = elasticsearchClient.index(indexRequest);

            logger.info("Successfully indexed document: {} with result: {}",
                document.getId(), response.result().jsonValue());

            return response.id();
        } catch (Exception e) {
            logger.error("Failed to index document: {}", document.getId(), e);
            throw new IOException("Failed to index document", e);
        }
    }

    public BulkResponse indexDocumentsBatch(List<DocumentIndex> documents) throws IOException {
        try {
            List<BulkOperation> bulkOperations = new ArrayList<>();

            for (DocumentIndex document : documents) {
                // Generate vectors if not provided
                if (document.getContentVector() == null && document.getContent() != null) {
                    List<Float> contentVector = vectorProcessingEngine.generateEmbedding(document.getContent());
                    document.setContentVector(contentVector);
                }

                if (document.getTitleVector() == null && document.getTitle() != null) {
                    List<Float> titleVector = vectorProcessingEngine.generateEmbedding(document.getTitle());
                    document.setTitleVector(titleVector);
                }

                // Update timestamp
                document.setUpdatedAt(LocalDateTime.now());

                BulkOperation operation = BulkOperation.of(o -> o
                    .index(idx -> idx
                        .index(indexConfig.getDocumentIndexName())
                        .id(document.getId())
                        .document(document)
                    )
                );

                bulkOperations.add(operation);
            }

            BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
            );

            BulkResponse response = elasticsearchClient.bulk(bulkRequest);

            if (response.errors()) {
                logger.warn("Bulk indexing completed with some errors. Total items: {}, Errors: {}",
                    response.items().size(),
                    response.items().stream().filter(item -> item.error() != null).count());
            } else {
                logger.info("Successfully bulk indexed {} documents", documents.size());
            }

            return response;
        } catch (Exception e) {
            logger.error("Failed to bulk index documents", e);
            throw new IOException("Failed to bulk index documents", e);
        }
    }

    public Optional<DocumentIndex> getDocument(String documentId) throws IOException {
        try {
            GetRequest getRequest = GetRequest.of(g -> g
                .index(indexConfig.getDocumentIndexName())
                .id(documentId)
            );

            GetResponse<DocumentIndex> response = elasticsearchClient.get(getRequest, DocumentIndex.class);

            if (response.found()) {
                logger.debug("Successfully retrieved document: {}", documentId);
                return Optional.of(response.source());
            } else {
                logger.debug("Document not found: {}", documentId);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Failed to get document: {}", documentId, e);
            throw new IOException("Failed to get document", e);
        }
    }

    public String updateDocument(DocumentIndex document) throws IOException {
        try {
            // Generate vectors if content changed
            if (document.getContentVector() == null && document.getContent() != null) {
                List<Float> contentVector = vectorProcessingEngine.generateEmbedding(document.getContent());
                document.setContentVector(contentVector);
            }

            if (document.getTitleVector() == null && document.getTitle() != null) {
                List<Float> titleVector = vectorProcessingEngine.generateEmbedding(document.getTitle());
                document.setTitleVector(titleVector);
            }

            // Update timestamp
            document.setUpdatedAt(LocalDateTime.now());

            UpdateRequest<DocumentIndex, DocumentIndex> updateRequest = UpdateRequest.of(u -> u
                .index(indexConfig.getDocumentIndexName())
                .id(document.getId())
                .doc(document)
                .docAsUpsert(true)
            );

            UpdateResponse<DocumentIndex> response = elasticsearchClient.update(updateRequest, DocumentIndex.class);

            logger.info("Successfully updated document: {} with result: {}",
                document.getId(), response.result().jsonValue());

            return response.id();
        } catch (Exception e) {
            logger.error("Failed to update document: {}", document.getId(), e);
            throw new IOException("Failed to update document", e);
        }
    }

    public boolean deleteDocument(String documentId) throws IOException {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                .index(indexConfig.getDocumentIndexName())
                .id(documentId)
            );

            DeleteResponse response = elasticsearchClient.delete(deleteRequest);

            boolean deleted = response.result().jsonValue().equals("deleted");
            if (deleted) {
                logger.info("Successfully deleted document: {}", documentId);
            } else {
                logger.warn("Document not found for deletion: {}", documentId);
            }

            return deleted;
        } catch (Exception e) {
            logger.error("Failed to delete document: {}", documentId, e);
            throw new IOException("Failed to delete document", e);
        }
    }
}