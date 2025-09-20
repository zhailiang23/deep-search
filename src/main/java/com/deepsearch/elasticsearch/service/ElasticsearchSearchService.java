package com.deepsearch.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.deepsearch.elasticsearch.config.ElasticsearchIndexConfig;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.vector.service.VectorProcessingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSearchService.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchIndexConfig indexConfig;

    @Autowired
    private VectorProcessingEngine vectorProcessingEngine;

    public List<DocumentIndex> keywordSearch(String query, String spaceId, List<String> channels,
                                           int from, int size) throws IOException {
        try {
            Query searchQuery = buildKeywordQuery(query, spaceId, channels);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexConfig.getDocumentIndexName())
                .query(searchQuery)
                .from(from)
                .size(size)
                .source(src -> src.filter(f -> f.excludes("*_vector")))
            );

            SearchResponse<DocumentIndex> response = elasticsearchClient.search(searchRequest, DocumentIndex.class);

            List<DocumentIndex> results = extractDocumentsFromResponse(response);

            logger.info("Keyword search completed. Query: '{}', Results: {}", query, results.size());
            return results;
        } catch (Exception e) {
            logger.error("Failed to perform keyword search for query: {}", query, e);
            throw new IOException("Keyword search failed", e);
        }
    }

    public List<DocumentIndex> vectorSearch(String query, String spaceId, List<String> channels,
                                          int from, int size) throws IOException {
        try {
            // Generate query vector
            List<Float> queryVector = vectorProcessingEngine.generateEmbedding(query);
            
            if (queryVector == null || queryVector.isEmpty()) {
                logger.warn("Failed to generate embedding vector, falling back to keyword search");
                return keywordSearch(query, spaceId, channels, from, size);
            }

            // Build KNN search request with filters
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(indexConfig.getDocumentIndexName())
                .knn(k -> k
                    .field("content_vector")
                    .queryVector(queryVector)
                    .k(size + from)
                    .numCandidates(Math.max(100, (size + from) * 3))
                )
                .from(from)
                .size(size)
                .source(src -> src.filter(f -> f.excludes("*_vector")));

            // Add filters for space and channels
            if ((spaceId != null && !spaceId.isEmpty()) || (channels != null && !channels.isEmpty())) {
                BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
                
                if (spaceId != null && !spaceId.isEmpty()) {
                    boolBuilder.filter(TermQuery.of(t -> t.field("space_id").value(spaceId))._toQuery());
                }
                
                if (channels != null && !channels.isEmpty()) {
                    boolBuilder.filter(Query.of(q -> q.terms(t -> t
                        .field("channel")
                        .terms(terms -> terms.value(channels.stream()
                            .map(channel -> co.elastic.clients.elasticsearch._types.FieldValue.of(channel))
                            .toList()))
                    )));
                }
                
                searchBuilder.postFilter(boolBuilder.build()._toQuery());
            }

            SearchResponse<DocumentIndex> response = elasticsearchClient.search(searchBuilder.build(), DocumentIndex.class);
            List<DocumentIndex> results = extractDocumentsFromResponse(response);

            logger.info("Vector search completed. Query: '{}', Results: {}", query, results.size());
            return results;
        } catch (Exception e) {
            logger.error("Failed to perform vector search for query: {}", query, e);
            // Fallback to keyword search if vector search fails
            logger.warn("Falling back to keyword search due to vector search failure");
            return keywordSearch(query, spaceId, channels, from, size);
        }
    }

    public List<DocumentIndex> hybridSearch(String query, String spaceId, List<String> channels,
                                          int from, int size, float keywordWeight, float vectorWeight) throws IOException {
        try {
            // Generate query vector
            List<Float> queryVector = vectorProcessingEngine.generateEmbedding(query);
            
            if (queryVector == null || queryVector.isEmpty()) {
                logger.warn("Failed to generate embedding vector, falling back to keyword search");
                return keywordSearch(query, spaceId, channels, from, size);
            }

            // Build keyword query
            Query keywordQuery = buildKeywordQuery(query, spaceId, channels);

            // Create hybrid search with both KNN and traditional query
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexConfig.getDocumentIndexName())
                .query(q -> q.functionScore(fs -> fs
                    .query(keywordQuery)
                    .boost(keywordWeight)
                ))
                .knn(k -> k
                    .field("content_vector")
                    .queryVector(queryVector)
                    .k(size * 2)
                    .numCandidates(Math.max(100, size * 6))
                    .boost(vectorWeight)
                )
                .from(from)
                .size(size)
                .source(src -> src.filter(f -> f.excludes("*_vector")))
            );

            SearchResponse<DocumentIndex> response = elasticsearchClient.search(searchRequest, DocumentIndex.class);
            List<DocumentIndex> results = extractDocumentsFromResponse(response);

            logger.info("Hybrid search completed. Query: '{}', Results: {}, Keyword Weight: {}, Vector Weight: {}",
                query, results.size(), keywordWeight, vectorWeight);
            return results;
        } catch (Exception e) {
            logger.error("Failed to perform hybrid search for query: {}", query, e);
            // Fallback to keyword search if hybrid search fails
            logger.warn("Falling back to keyword search due to hybrid search failure");
            return keywordSearch(query, spaceId, channels, from, size);
        }
    }

    private Query buildKeywordQuery(String query, String spaceId, List<String> channels) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Multi-match query for text fields
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
            .query(query)
            .fields("title^2", "content", "summary^1.5", "tags")
            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
            .fuzziness("AUTO")
        )._toQuery();

        boolBuilder.must(multiMatchQuery);

        // Add space filter
        if (spaceId != null && !spaceId.isEmpty()) {
            boolBuilder.filter(TermQuery.of(t -> t.field("space_id").value(spaceId))._toQuery());
        }

        // Add channel filter
        if (channels != null && !channels.isEmpty()) {
            boolBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("channel")
                .terms(terms -> terms.value(channels.stream()
                    .map(channel -> co.elastic.clients.elasticsearch._types.FieldValue.of(channel))
                    .toList()))
            )));
        }

        return boolBuilder.build()._toQuery();
    }

    private List<DocumentIndex> extractDocumentsFromResponse(SearchResponse<DocumentIndex> response) {
        HitsMetadata<DocumentIndex> hits = response.hits();

        if (hits.hits().isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentIndex> documents = new ArrayList<>();
        for (Hit<DocumentIndex> hit : hits.hits()) {
            DocumentIndex document = hit.source();
            if (document != null) {
                documents.add(document);
            }
        }

        return documents;
    }

    public long getDocumentCount(String spaceId) throws IOException {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            if (spaceId != null && !spaceId.isEmpty()) {
                boolBuilder.filter(TermQuery.of(t -> t.field("space_id").value(spaceId))._toQuery());
            }

            SearchRequest countRequest = SearchRequest.of(s -> s
                .index(indexConfig.getDocumentIndexName())
                .query(boolBuilder.build()._toQuery())
                .size(0)
                .trackTotalHits(th -> th.enabled(true))
            );

            SearchResponse<DocumentIndex> response = elasticsearchClient.search(countRequest, DocumentIndex.class);

            return response.hits().total().value();
        } catch (Exception e) {
            logger.error("Failed to get document count for spaceId: {}", spaceId, e);
            throw new IOException("Failed to get document count", e);
        }
    }
}