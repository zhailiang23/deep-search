package com.deepsearch.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class ElasticsearchIndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexConfig.class);

    private static final String DOCUMENT_INDEX_NAME = "deep-search-documents";
    private static final String MAPPING_FILE_PATH = "elasticsearch/mappings/document-index.json";

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        try {
            createDocumentIndexIfNotExists();
            logger.info("Elasticsearch indices initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Elasticsearch indices", e);
            throw new RuntimeException("Elasticsearch indices initialization failed", e);
        }
    }

    private void createDocumentIndexIfNotExists() throws IOException {
        ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(DOCUMENT_INDEX_NAME));

        boolean indexExists = elasticsearchClient.indices().exists(existsRequest).value();

        if (!indexExists) {
            logger.info("Creating index: {}", DOCUMENT_INDEX_NAME);

            // Load mapping from JSON file
            Map<String, Object> indexMapping = loadMappingFromFile();

            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                .index(DOCUMENT_INDEX_NAME)
                .settings(s -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> settings = (Map<String, Object>) indexMapping.get("settings");
                    return s.withJson(createJsonFromMap(settings));
                })
                .mappings(m -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mappings = (Map<String, Object>) indexMapping.get("mappings");
                    return m.withJson(createJsonFromMap(mappings));
                })
            );

            elasticsearchClient.indices().create(createRequest);
            logger.info("Successfully created index: {}", DOCUMENT_INDEX_NAME);
        } else {
            logger.info("Index already exists: {}", DOCUMENT_INDEX_NAME);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadMappingFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource(MAPPING_FILE_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, Map.class);
        }
    }

    private InputStream createJsonFromMap(Map<String, Object> map) {
        try {
            String json = objectMapper.writeValueAsString(map);
            return new java.io.ByteArrayInputStream(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }

    public String getDocumentIndexName() {
        return DOCUMENT_INDEX_NAME;
    }
}