package com.deepsearch.service;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.entity.User;
import com.deepsearch.service.SceneClassifier.SearchScene;
import com.deepsearch.service.UserBehaviorService.PersonalizationWeights;
import com.deepsearch.service.UserBehaviorService.SearchIntent;
import com.deepsearch.service.UserBehaviorService.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文感知搜索服务
 * 整合用户行为分析、场景识别、个性化推荐等功能
 * 提供智能的、个性化的搜索体验
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextAwareSearchService {

    private final UserBehaviorService userBehaviorService;
    private final SceneClassifier sceneClassifier;
    private final HybridSearchService hybridSearchService;

    /**
     * 执行上下文感知的智能搜索
     */
    public SearchResult contextAwareSearch(SearchRequest searchRequest, User user, HttpServletRequest request) {
        log.info("开始上下文感知搜索: query={}, userId={}", searchRequest.getQuery(), user.getId());

        try {
            // 1. 构建搜索上下文
            SearchContext context = buildSearchContext(searchRequest.getQuery(), user, request);

            // 2. 基于上下文优化搜索请求
            SearchRequest optimizedRequest = optimizeSearchRequest(searchRequest, context);

            // 3. 执行混合搜索
            SearchResult result = hybridSearchService.hybridSearch(optimizedRequest);

            // 4. 应用上下文过滤和重排序
            result = applyContextualFiltering(result, context);

            // 5. 记录搜索行为
            recordSearchBehavior(user.getId(), searchRequest, result, context);

            // 6. 添加上下文信息到结果
            enrichResultWithContext(result, context);

            log.info("上下文感知搜索完成: userId={}, results={}, scene={}",
                user.getId(), result.getTotalHits(), context.getScene().getPrimaryScene());

            return result;

        } catch (Exception e) {
            log.error("上下文感知搜索失败: query={}, userId={}", searchRequest.getQuery(), user.getId(), e);

            // 降级到普通搜索
            return hybridSearchService.hybridSearch(searchRequest);
        }
    }

    /**
     * 构建搜索上下文
     */
    public SearchContext buildSearchContext(String query, User user, HttpServletRequest request) {
        log.debug("构建搜索上下文: query={}, userId={}", query, user.getId());

        SearchContext context = new SearchContext();
        context.setQuery(query);
        context.setUserId(user.getId());
        context.setTimestamp(LocalDateTime.now());

        // 1. 获取用户画像
        UserProfile userProfile = userBehaviorService.getUserProfile(user.getId());
        context.setUserProfile(userProfile);

        // 2. 分析搜索意图
        SearchIntent intent = userBehaviorService.analyzeSearchIntent(user.getId(), query);
        context.setSearchIntent(intent);

        // 3. 识别业务场景
        SearchScene scene = sceneClassifier.classifyScene(query, userProfile);
        context.setScene(scene);

        // 4. 构建时间上下文
        TimeContext timeContext = buildTimeContext();
        context.setTimeContext(timeContext);

        // 5. 解析地理位置上下文（如果可用）
        LocationContext locationContext = parseLocationContext(request);
        context.setLocationContext(locationContext);

        // 6. 获取个性化权重
        PersonalizationWeights weights = userBehaviorService.getPersonalizationWeights(user.getId());
        context.setPersonalizationWeights(weights);

        // 7. 构建设备上下文
        DeviceContext deviceContext = buildDeviceContext(request);
        context.setDeviceContext(deviceContext);

        log.debug("搜索上下文构建完成: {}", context);
        return context;
    }

    /**
     * 基于上下文优化搜索请求
     */
    private SearchRequest optimizeSearchRequest(SearchRequest originalRequest, SearchContext context) {
        SearchRequest optimizedRequest = cloneSearchRequest(originalRequest);

        // 1. 应用个性化权重
        PersonalizationWeights weights = context.getPersonalizationWeights();
        if (weights != null) {
            optimizedRequest.setKeywordWeight(weights.getKeywordWeight());
            optimizedRequest.setVectorWeight(weights.getSemanticWeight());
            optimizedRequest.setFreshnessWeight(weights.getFreshnessWeight());
            optimizedRequest.setPopularityWeight(weights.getPopularityWeight());
        }

        // 2. 根据场景调整搜索类型
        SearchScene scene = context.getScene();
        if (scene != null) {
            String preferredType = determinePreferredSearchType(scene, context.getUserProfile());
            optimizedRequest.setSearchType(preferredType);
        }

        // 3. 应用渠道过滤
        List<String> recommendedChannels = scene.getRecommendedChannels();
        if (recommendedChannels != null && !recommendedChannels.isEmpty()) {
            optimizedRequest.setChannels(recommendedChannels);
        }

        // 4. 调整相关性权重
        adjustRelevanceWeights(optimizedRequest, context);

        // 5. 设置用户上下文
        Map<String, Object> userContext = buildUserContextMap(context);
        optimizedRequest.setUserContext(userContext);

        log.debug("搜索请求优化完成: {}", optimizedRequest);
        return optimizedRequest;
    }

    /**
     * 应用上下文过滤和重排序
     */
    private SearchResult applyContextualFiltering(SearchResult result, SearchContext context) {
        if (result.getDocuments() == null || result.getDocuments().isEmpty()) {
            return result;
        }

        List<DocumentIndex> documents = result.getDocuments();

        // 1. 应用场景相关性过滤
        documents = applySceneRelevanceFiltering(documents, context.getScene());

        // 2. 应用个性化重排序
        documents = applyPersonalizedRanking(documents, context);

        // 3. 应用时间相关性调整
        documents = applyTimeRelevanceAdjustment(documents, context.getTimeContext());

        // 4. 应用多样性优化
        documents = applyDiversityOptimization(documents, context);

        // 更新结果
        result.setDocuments(documents);
        result.setTotalHits(documents.size());

        return result;
    }

    /**
     * 记录搜索行为
     */
    private void recordSearchBehavior(Long userId, SearchRequest request, SearchResult result, SearchContext context) {
        try {
            // 确定搜索类型
            String searchType = request.getSearchType();
            com.deepsearch.entity.SearchLog.SearchType logType;

            switch (searchType.toLowerCase()) {
                case "semantic":
                    logType = com.deepsearch.entity.SearchLog.SearchType.SEMANTIC;
                    break;
                case "keyword":
                    logType = com.deepsearch.entity.SearchLog.SearchType.KEYWORD;
                    break;
                default:
                    logType = com.deepsearch.entity.SearchLog.SearchType.HYBRID;
            }

            userBehaviorService.recordSearchBehavior(
                userId,
                request.getQuery(),
                logType,
                result.getTotalHits(),
                (int) result.getResponseTime()
            );

        } catch (Exception e) {
            log.warn("记录搜索行为失败: userId={}", userId, e);
        }
    }

    /**
     * 丰富结果的上下文信息
     */
    private void enrichResultWithContext(SearchResult result, SearchContext context) {
        // 添加场景信息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scene", context.getScene().getPrimaryScene().name());
        metadata.put("intent", context.getSearchIntent().getPrimaryIntent());
        metadata.put("confidence", context.getScene().getConfidence());
        metadata.put("personalizationStrength", context.getPersonalizationWeights().getPersonalizationStrength());
        metadata.put("recommendedChannels", context.getScene().getRecommendedChannels());
        metadata.put("timeContext", context.getTimeContext().getDayPeriod());

        result.setMetadata(metadata);

        // 添加搜索建议
        List<String> suggestions = generateContextualSuggestions(context);
        result.setSuggestions(suggestions);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建时间上下文
     */
    private TimeContext buildTimeContext() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();

        TimeContext context = new TimeContext();
        context.setTimestamp(now);
        context.setHour(time.getHour());
        context.setDayOfWeek(now.getDayOfWeek().toString());
        context.setBusinessHours(isBusinessHours(time.getHour()));
        context.setDayPeriod(getDayPeriod(time.getHour()));

        return context;
    }

    /**
     * 解析地理位置上下文
     */
    private LocationContext parseLocationContext(HttpServletRequest request) {
        LocationContext context = new LocationContext();

        String locationHeader = request.getHeader("X-User-Location");
        if (locationHeader != null) {
            try {
                // 解析位置信息（格式：lat,lng或city_code）
                if (locationHeader.contains(",")) {
                    String[] parts = locationHeader.split(",");
                    context.setLatitude(Double.parseDouble(parts[0]));
                    context.setLongitude(Double.parseDouble(parts[1]));
                } else {
                    context.setCityCode(locationHeader);
                }
            } catch (Exception e) {
                log.warn("解析位置信息失败: {}", locationHeader, e);
            }
        }

        // 从IP地址推断位置（如果没有明确的位置信息）
        if (context.getCityCode() == null) {
            String clientIp = getClientIpAddress(request);
            context.setCityCode(inferCityFromIp(clientIp));
        }

        return context;
    }

    /**
     * 构建设备上下文
     */
    private DeviceContext buildDeviceContext(HttpServletRequest request) {
        DeviceContext context = new DeviceContext();

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            context.setUserAgent(userAgent);
            context.setDeviceType(inferDeviceType(userAgent));
            context.setBrowser(inferBrowser(userAgent));
            context.setMobile(isMobileDevice(userAgent));
        }

        return context;
    }

    /**
     * 克隆搜索请求
     */
    private SearchRequest cloneSearchRequest(SearchRequest original) {
        SearchRequest clone = new SearchRequest();
        clone.setQuery(original.getQuery());
        clone.setSpaceId(original.getSpaceId());
        clone.setChannels(original.getChannels());
        clone.setFrom(original.getFrom());
        clone.setSize(original.getSize());
        clone.setKeywordWeight(original.getKeywordWeight());
        clone.setVectorWeight(original.getVectorWeight());
        clone.setFreshnessWeight(original.getFreshnessWeight());
        clone.setQualityWeight(original.getQualityWeight());
        clone.setPopularityWeight(original.getPopularityWeight());
        clone.setSearchType(original.getSearchType());
        clone.setHighlight(original.isHighlight());
        clone.setMinScore(original.getMinScore());
        // 复制其他需要的字段...

        return clone;
    }

    /**
     * 确定首选搜索类型
     */
    private String determinePreferredSearchType(SearchScene scene, UserProfile profile) {
        String preferenceType = profile.getPreferenceType();

        // 根据用户偏好确定搜索类型
        switch (preferenceType) {
            case "SEMANTIC_ORIENTED":
                return "semantic";
            case "KEYWORD_ORIENTED":
                return "keyword";
            default:
                // 根据场景确定最适合的搜索类型
                switch (scene.getPrimaryScene()) {
                    case MOBILE_BANKING:
                    case INTERNET_BANKING:
                        return "hybrid"; // 银行应用场景适合混合搜索
                    case COUNTER_SERVICE:
                        return "keyword"; // 柜台服务更适合精确匹配
                    default:
                        return "hybrid";
                }
        }
    }

    /**
     * 调整相关性权重
     */
    private void adjustRelevanceWeights(SearchRequest request, SearchContext context) {
        // 根据时间上下文调整新鲜度权重
        if (!context.getTimeContext().getBusinessHours()) {
            // 非营业时间，提高自助服务相关内容的权重
            request.setFreshnessWeight(request.getFreshnessWeight() * 1.2f);
        }

        // 根据设备类型调整权重
        if (context.getDeviceContext().getMobile()) {
            // 移动设备，提高移动端相关内容权重
            request.setPopularityWeight(request.getPopularityWeight() * 1.1f);
        }
    }

    /**
     * 构建用户上下文映射
     */
    private Map<String, Object> buildUserContextMap(SearchContext context) {
        Map<String, Object> userContext = new HashMap<>();

        userContext.put("userId", context.getUserId());
        userContext.put("scene", context.getScene().getPrimaryScene().name());
        userContext.put("intent", context.getSearchIntent().getPrimaryIntent());
        userContext.put("deviceType", context.getDeviceContext().getDeviceType());
        userContext.put("isMobile", context.getDeviceContext().getMobile());
        userContext.put("businessHours", context.getTimeContext().getBusinessHours());
        userContext.put("preferenceType", context.getUserProfile().getPreferenceType());

        return userContext;
    }

    /**
     * 应用场景相关性过滤
     */
    private List<DocumentIndex> applySceneRelevanceFiltering(List<DocumentIndex> documents, SearchScene scene) {
        return documents.stream()
            .filter(doc -> isRelevantToScene(doc, scene))
            .collect(Collectors.toList());
    }

    /**
     * 应用个性化重排序
     */
    private List<DocumentIndex> applyPersonalizedRanking(List<DocumentIndex> documents, SearchContext context) {
        UserProfile profile = context.getUserProfile();
        Map<String, Double> categoryPreferences = profile.getPreferences();

        return documents.stream()
            .map(doc -> {
                // 基于用户偏好调整文档评分
                String category = extractDocumentCategory(doc);
                if (category != null && categoryPreferences.containsKey(category)) {
                    double preference = categoryPreferences.get(category);
                    float adjustedScore = doc.getScore() * (1.0f + (float) preference * 0.2f);
                    doc.setScore(adjustedScore);
                }
                return doc;
            })
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());
    }

    /**
     * 应用时间相关性调整
     */
    private List<DocumentIndex> applyTimeRelevanceAdjustment(List<DocumentIndex> documents, TimeContext timeContext) {
        return documents.stream()
            .map(doc -> {
                // 根据文档的时效性和当前时间调整评分
                if (isTimeRelevant(doc, timeContext)) {
                    doc.setScore(doc.getScore() * 1.1f);
                }
                return doc;
            })
            .collect(Collectors.toList());
    }

    /**
     * 应用多样性优化
     */
    private List<DocumentIndex> applyDiversityOptimization(List<DocumentIndex> documents, SearchContext context) {
        // 实现多样性优化算法，避免结果过于单一
        List<DocumentIndex> diversifiedResults = new ArrayList<>();
        Set<String> seenCategories = new HashSet<>();

        for (DocumentIndex doc : documents) {
            String category = extractDocumentCategory(doc);

            // 优先选择不同类别的文档
            if (category == null || !seenCategories.contains(category) || diversifiedResults.size() < 3) {
                diversifiedResults.add(doc);
                if (category != null) {
                    seenCategories.add(category);
                }
            } else if (diversifiedResults.size() >= 10) {
                break; // 保持合理的结果数量
            }
        }

        // 如果多样化后结果太少，补充原始结果
        if (diversifiedResults.size() < Math.min(documents.size(), 10)) {
            for (DocumentIndex doc : documents) {
                if (!diversifiedResults.contains(doc) && diversifiedResults.size() < 10) {
                    diversifiedResults.add(doc);
                }
            }
        }

        return diversifiedResults;
    }

    /**
     * 生成上下文相关的搜索建议
     */
    private List<String> generateContextualSuggestions(SearchContext context) {
        List<String> suggestions = new ArrayList<>();

        SearchScene scene = context.getScene();
        switch (scene.getPrimaryScene()) {
            case MOBILE_BANKING:
                suggestions.addAll(Arrays.asList("手机银行登录", "转账流程", "余额查询", "账单查看"));
                break;
            case INTERNET_BANKING:
                suggestions.addAll(Arrays.asList("网银安全", "U盾使用", "网上转账", "理财产品"));
                break;
            case COUNTER_SERVICE:
                suggestions.addAll(Arrays.asList("营业时间", "网点地址", "预约服务", "业务流程"));
                break;
            case WEALTH_MANAGEMENT:
                suggestions.addAll(Arrays.asList("理财收益", "风险评估", "投资建议", "基金推荐"));
                break;
            default:
                suggestions.addAll(Arrays.asList("常见问题", "服务指南", "产品介绍", "联系客服"));
        }

        return suggestions.stream().limit(5).collect(Collectors.toList());
    }

    // ==================== 工具方法 ====================

    private boolean isBusinessHours(int hour) {
        return hour >= 9 && hour <= 17;
    }

    private String getDayPeriod(int hour) {
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 18) return "afternoon";
        if (hour >= 18 && hour < 22) return "evening";
        return "night";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String inferCityFromIp(String ip) {
        // 简化的IP城市推断，实际应该使用GeoIP库
        return "unknown";
    }

    private String inferDeviceType(String userAgent) {
        if (userAgent.toLowerCase().contains("mobile")) return "mobile";
        if (userAgent.toLowerCase().contains("tablet")) return "tablet";
        return "desktop";
    }

    private String inferBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome")) return "chrome";
        if (ua.contains("firefox")) return "firefox";
        if (ua.contains("safari")) return "safari";
        if (ua.contains("edge")) return "edge";
        return "unknown";
    }

    private boolean isMobileDevice(String userAgent) {
        return userAgent.toLowerCase().contains("mobile");
    }

    private boolean isRelevantToScene(DocumentIndex doc, SearchScene scene) {
        // 简化的场景相关性检查
        return true; // 实际应该基于文档内容和场景标签进行匹配
    }

    private String extractDocumentCategory(DocumentIndex doc) {
        // 从文档中提取分类信息
        return doc.getCategory(); // 假设DocumentIndex有category字段
    }

    private boolean isTimeRelevant(DocumentIndex doc, TimeContext timeContext) {
        // 检查文档是否与当前时间上下文相关
        return true; // 简化实现
    }

    // ==================== 内部数据类 ====================

    /**
     * 搜索上下文
     */
    public static class SearchContext {
        private String query;
        private Long userId;
        private LocalDateTime timestamp;
        private UserProfile userProfile;
        private SearchIntent searchIntent;
        private SearchScene scene;
        private TimeContext timeContext;
        private LocationContext locationContext;
        private PersonalizationWeights personalizationWeights;
        private DeviceContext deviceContext;

        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public UserProfile getUserProfile() { return userProfile; }
        public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

        public SearchIntent getSearchIntent() { return searchIntent; }
        public void setSearchIntent(SearchIntent searchIntent) { this.searchIntent = searchIntent; }

        public SearchScene getScene() { return scene; }
        public void setScene(SearchScene scene) { this.scene = scene; }

        public TimeContext getTimeContext() { return timeContext; }
        public void setTimeContext(TimeContext timeContext) { this.timeContext = timeContext; }

        public LocationContext getLocationContext() { return locationContext; }
        public void setLocationContext(LocationContext locationContext) { this.locationContext = locationContext; }

        public PersonalizationWeights getPersonalizationWeights() { return personalizationWeights; }
        public void setPersonalizationWeights(PersonalizationWeights personalizationWeights) { this.personalizationWeights = personalizationWeights; }

        public DeviceContext getDeviceContext() { return deviceContext; }
        public void setDeviceContext(DeviceContext deviceContext) { this.deviceContext = deviceContext; }

        @Override
        public String toString() {
            return String.format("SearchContext{userId=%d, scene=%s, intent=%s, device=%s}",
                userId, scene != null ? scene.getPrimaryScene() : null,
                searchIntent != null ? searchIntent.getPrimaryIntent() : null,
                deviceContext != null ? deviceContext.getDeviceType() : null);
        }
    }

    /**
     * 时间上下文
     */
    public static class TimeContext {
        private LocalDateTime timestamp;
        private int hour;
        private String dayOfWeek;
        private Boolean businessHours;
        private String dayPeriod;

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getHour() { return hour; }
        public void setHour(int hour) { this.hour = hour; }

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public Boolean getBusinessHours() { return businessHours; }
        public void setBusinessHours(Boolean businessHours) { this.businessHours = businessHours; }

        public String getDayPeriod() { return dayPeriod; }
        public void setDayPeriod(String dayPeriod) { this.dayPeriod = dayPeriod; }
    }

    /**
     * 地理位置上下文
     */
    public static class LocationContext {
        private Double latitude;
        private Double longitude;
        private String cityCode;
        private String region;

        // Getters and Setters
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public String getCityCode() { return cityCode; }
        public void setCityCode(String cityCode) { this.cityCode = cityCode; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    /**
     * 设备上下文
     */
    public static class DeviceContext {
        private String userAgent;
        private String deviceType;
        private String browser;
        private Boolean mobile;

        // Getters and Setters
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }

        public Boolean getMobile() { return mobile; }
        public void setMobile(Boolean mobile) { this.mobile = mobile; }
    }
}