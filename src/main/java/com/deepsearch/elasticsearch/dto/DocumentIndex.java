package com.deepsearch.elasticsearch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DocumentIndex {

    private String id;
    private String title;
    private String content;
    private String summary;
    private List<String> tags;
    private String category;
    private String source;
    private String channel;

    @JsonProperty("space_id")
    private String spaceId;

    @JsonProperty("content_vector")
    private List<Float> contentVector;

    @JsonProperty("title_vector")
    private List<Float> titleVector;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("popularity_score")
    private Float popularityScore;

    @JsonProperty("relevance_boost")
    private Float relevanceBoost = 1.0f;

    @JsonProperty("score")
    private Float score;

    private Map<String, Object> metadata;

    // Default constructor
    public DocumentIndex() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public DocumentIndex(String id, String title, String content) {
        this();
        this.id = id;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public List<Float> getContentVector() {
        return contentVector;
    }

    public void setContentVector(List<Float> contentVector) {
        this.contentVector = contentVector;
    }

    public List<Float> getTitleVector() {
        return titleVector;
    }

    public void setTitleVector(List<Float> titleVector) {
        this.titleVector = titleVector;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Float getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Float popularityScore) {
        this.popularityScore = popularityScore;
    }

    public Float getRelevanceBoost() {
        return relevanceBoost;
    }

    public void setRelevanceBoost(Float relevanceBoost) {
        this.relevanceBoost = relevanceBoost;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "DocumentIndex{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", source='" + source + '\'' +
                ", channel='" + channel + '\'' +
                ", spaceId='" + spaceId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}