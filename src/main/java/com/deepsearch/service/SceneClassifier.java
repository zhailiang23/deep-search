package com.deepsearch.service;

import com.deepsearch.service.UserBehaviorService.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 场景分类器 - 识别银行业务场景和用户上下文
 * 根据查询内容、用户行为、时间等因素分类业务场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SceneClassifier {

    // 银行业务场景枚举
    public enum BankingScene {
        MOBILE_BANKING("手机银行", 0.9),
        INTERNET_BANKING("网上银行", 0.8),
        COUNTER_SERVICE("柜台服务", 0.7),
        ATM_SERVICE("ATM服务", 0.6),
        PHONE_BANKING("电话银行", 0.5),
        WEALTH_MANAGEMENT("理财服务", 0.8),
        LOAN_SERVICE("贷款服务", 0.7),
        CREDIT_CARD("信用卡服务", 0.8),
        GENERAL_INQUIRY("一般咨询", 0.4);

        private final String description;
        private final double priority;

        BankingScene(String description, double priority) {
            this.description = description;
            this.priority = priority;
        }

        public String getDescription() { return description; }
        public double getPriority() { return priority; }
    }

    // 查询意图枚举
    public enum QueryIntent {
        PRODUCT_INQUIRY("产品咨询"),
        SERVICE_PROCESS("业务流程"),
        PROBLEM_SOLVING("问题解决"),
        FEATURE_USAGE("功能使用"),
        ACCOUNT_MANAGEMENT("账户管理"),
        TRANSACTION_INQUIRY("交易查询"),
        RATE_INQUIRY("利率查询"),
        PROMOTION_INFO("活动信息"),
        POLICY_INFO("政策信息"),
        GENERAL_HELP("一般帮助");

        private final String description;

        QueryIntent(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // 关键词模式定义
    private static final Map<BankingScene, List<Pattern>> SCENE_PATTERNS = initializeScenePatterns();
    private static final Map<QueryIntent, List<Pattern>> INTENT_PATTERNS = initializeIntentPatterns();

    /**
     * 分类搜索场景
     */
    public SearchScene classifyScene(String query, UserProfile userProfile) {
        log.debug("开始场景分类: query={}, userId={}", query, userProfile.getUserId());

        SearchScene scene = new SearchScene();
        scene.setQuery(query);
        scene.setUserId(userProfile.getUserId());
        scene.setTimestamp(LocalDateTime.now());

        // 1. 基于关键词分析场景
        BankingScene primaryScene = classifyBankingScene(query);
        scene.setPrimaryScene(primaryScene);

        // 2. 分析查询意图
        QueryIntent intent = classifyQueryIntent(query);
        scene.setQueryIntent(intent);

        // 3. 基于用户行为历史调整场景权重
        adjustSceneByUserHistory(scene, userProfile);

        // 4. 基于时间上下文调整
        adjustSceneByTimeContext(scene);

        // 5. 计算场景置信度
        double confidence = calculateSceneConfidence(scene, query, userProfile);
        scene.setConfidence(confidence);

        // 6. 生成场景标签
        scene.setTags(generateSceneTags(scene, query));

        // 7. 设置推荐渠道
        scene.setRecommendedChannels(getRecommendedChannels(scene));

        log.debug("场景分类完成: {}", scene);
        return scene;
    }

    /**
     * 分类银行业务场景
     */
    private BankingScene classifyBankingScene(String query) {
        String normalizedQuery = query.toLowerCase().trim();

        Map<BankingScene, Double> sceneScores = new HashMap<>();

        // 遍历所有场景模式进行匹配
        for (Map.Entry<BankingScene, List<Pattern>> entry : SCENE_PATTERNS.entrySet()) {
            BankingScene scene = entry.getKey();
            List<Pattern> patterns = entry.getValue();

            double score = 0.0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(normalizedQuery).find()) {
                    score += 1.0;
                }
            }

            if (score > 0) {
                // 考虑场景优先级
                sceneScores.put(scene, score * scene.getPriority());
            }
        }

        // 返回得分最高的场景
        return sceneScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(BankingScene.GENERAL_INQUIRY);
    }

    /**
     * 分类查询意图
     */
    private QueryIntent classifyQueryIntent(String query) {
        String normalizedQuery = query.toLowerCase().trim();

        Map<QueryIntent, Double> intentScores = new HashMap<>();

        for (Map.Entry<QueryIntent, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            QueryIntent intent = entry.getKey();
            List<Pattern> patterns = entry.getValue();

            double score = 0.0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(normalizedQuery).find()) {
                    score += 1.0;
                }
            }

            if (score > 0) {
                intentScores.put(intent, score);
            }
        }

        return intentScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(QueryIntent.GENERAL_HELP);
    }

    /**
     * 基于用户历史行为调整场景
     */
    private void adjustSceneByUserHistory(SearchScene scene, UserProfile userProfile) {
        Map<String, Double> scenePreferences = userProfile.getScenePreferences();

        String sceneKey = scene.getPrimaryScene().name().toLowerCase();
        if (scenePreferences.containsKey(sceneKey)) {
            double userPreference = scenePreferences.get(sceneKey);
            // 用户偏好影响场景权重
            scene.setUserPreferenceWeight(userPreference);
        }

        // 基于用户最近搜索调整
        List<String> recentSearches = userProfile.getRecentSearches();
        if (!recentSearches.isEmpty()) {
            long relatedSearches = recentSearches.stream()
                .mapToLong(search -> countSceneKeywords(search, scene.getPrimaryScene()))
                .sum();

            if (relatedSearches > 0) {
                scene.setHistoryRelevanceScore(Math.min(1.0, relatedSearches / 10.0));
            }
        }
    }

    /**
     * 基于时间上下文调整场景
     */
    private void adjustSceneByTimeContext(SearchScene scene) {
        LocalTime currentTime = LocalTime.now();
        int hour = currentTime.getHour();

        // 根据时间调整场景权重
        TimeContext timeContext = new TimeContext();
        timeContext.setHour(hour);
        timeContext.setDayPeriod(getDayPeriod(hour));
        timeContext.setBusinessHours(isBusinessHours(hour));

        // 调整场景偏好
        adjustSceneByTime(scene, timeContext);
        scene.setTimeContext(timeContext);
    }

    /**
     * 计算场景置信度
     */
    private double calculateSceneConfidence(SearchScene scene, String query, UserProfile userProfile) {
        double confidence = 0.0;

        // 基础关键词匹配得分 (40%)
        confidence += countSceneKeywords(query, scene.getPrimaryScene()) * 0.4;

        // 用户历史偏好得分 (30%)
        confidence += scene.getUserPreferenceWeight() * 0.3;

        // 历史相关性得分 (20%)
        confidence += scene.getHistoryRelevanceScore() * 0.2;

        // 时间上下文得分 (10%)
        confidence += scene.getTimeContext().getBusinessHours() ? 0.1 : 0.05;

        return Math.min(1.0, confidence);
    }

    /**
     * 生成场景标签
     */
    private List<String> generateSceneTags(SearchScene scene, String query) {
        List<String> tags = new ArrayList<>();

        // 添加场景标签
        tags.add(scene.getPrimaryScene().name().toLowerCase());
        tags.add(scene.getQueryIntent().name().toLowerCase());

        // 基于查询内容添加特定标签
        if (query.contains("紧急") || query.contains("急")) {
            tags.add("urgent");
        }
        if (query.contains("新手") || query.contains("第一次")) {
            tags.add("beginner");
        }
        if (query.contains("高级") || query.contains("专业")) {
            tags.add("advanced");
        }

        return tags;
    }

    /**
     * 获取推荐渠道
     */
    private List<String> getRecommendedChannels(SearchScene scene) {
        List<String> channels = new ArrayList<>();

        switch (scene.getPrimaryScene()) {
            case MOBILE_BANKING:
                channels.addAll(Arrays.asList("mobile_app", "mobile_web", "sms"));
                break;
            case INTERNET_BANKING:
                channels.addAll(Arrays.asList("web", "mobile_web"));
                break;
            case COUNTER_SERVICE:
                channels.addAll(Arrays.asList("counter", "appointment", "queue_management"));
                break;
            case ATM_SERVICE:
                channels.addAll(Arrays.asList("atm", "mobile_app", "sms"));
                break;
            case PHONE_BANKING:
                channels.addAll(Arrays.asList("phone", "ivr", "callback"));
                break;
            default:
                channels.addAll(Arrays.asList("web", "mobile_app", "phone"));
        }

        return channels;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统计场景关键词匹配数量
     */
    private long countSceneKeywords(String query, BankingScene scene) {
        List<Pattern> patterns = SCENE_PATTERNS.get(scene);
        if (patterns == null) return 0;

        String normalizedQuery = query.toLowerCase();
        return patterns.stream()
            .mapToLong(pattern -> pattern.matcher(normalizedQuery).find() ? 1 : 0)
            .sum();
    }

    /**
     * 获取一天中的时段
     */
    private String getDayPeriod(int hour) {
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 18) return "afternoon";
        if (hour >= 18 && hour < 22) return "evening";
        return "night";
    }

    /**
     * 判断是否为营业时间
     */
    private boolean isBusinessHours(int hour) {
        return hour >= 9 && hour <= 17; // 9:00-17:00
    }

    /**
     * 根据时间调整场景
     */
    private void adjustSceneByTime(SearchScene scene, TimeContext timeContext) {
        // 营业时间外，降低柜台服务相关场景的权重
        if (!timeContext.getBusinessHours()) {
            if (scene.getPrimaryScene() == BankingScene.COUNTER_SERVICE) {
                scene.setTimeRelevanceScore(0.3);
            } else if (scene.getPrimaryScene() == BankingScene.MOBILE_BANKING ||
                      scene.getPrimaryScene() == BankingScene.INTERNET_BANKING) {
                scene.setTimeRelevanceScore(1.0);
            }
        } else {
            scene.setTimeRelevanceScore(0.8);
        }
    }

    /**
     * 初始化场景模式
     */
    private static Map<BankingScene, List<Pattern>> initializeScenePatterns() {
        Map<BankingScene, List<Pattern>> patterns = new HashMap<>();

        // 手机银行
        patterns.put(BankingScene.MOBILE_BANKING, Arrays.asList(
            Pattern.compile("手机银行|mobile|app|应用|移动端"),
            Pattern.compile("扫码|二维码|指纹|面容|生物识别"),
            Pattern.compile("推送|通知|短信验证")
        ));

        // 网上银行
        patterns.put(BankingScene.INTERNET_BANKING, Arrays.asList(
            Pattern.compile("网银|网上银行|online|网页|浏览器"),
            Pattern.compile("u盾|数字证书|安全控件"),
            Pattern.compile("网站|登录|密码")
        ));

        // 柜台服务
        patterns.put(BankingScene.COUNTER_SERVICE, Arrays.asList(
            Pattern.compile("柜台|营业厅|银行网点|现场办理"),
            Pattern.compile("身份证|户口本|现金|印章"),
            Pattern.compile("排队|叫号|预约")
        ));

        // ATM服务
        patterns.put(BankingScene.ATM_SERVICE, Arrays.asList(
            Pattern.compile("atm|自助|取款机|存取款"),
            Pattern.compile("银行卡|插卡|现金|取现"),
            Pattern.compile("密码|pin码")
        ));

        // 理财服务
        patterns.put(BankingScene.WEALTH_MANAGEMENT, Arrays.asList(
            Pattern.compile("理财|投资|基金|股票|债券"),
            Pattern.compile("收益|风险|资产配置"),
            Pattern.compile("财富管理|私人银行")
        ));

        // 贷款服务
        patterns.put(BankingScene.LOAN_SERVICE, Arrays.asList(
            Pattern.compile("贷款|借款|房贷|车贷|信贷"),
            Pattern.compile("利率|还款|额度|申请"),
            Pattern.compile("抵押|担保|征信")
        ));

        // 信用卡
        patterns.put(BankingScene.CREDIT_CARD, Arrays.asList(
            Pattern.compile("信用卡|credit card|刷卡|pos"),
            Pattern.compile("额度|账单|分期|积分"),
            Pattern.compile("年费|优惠|活动")
        ));

        return patterns;
    }

    /**
     * 初始化意图模式
     */
    private static Map<QueryIntent, List<Pattern>> initializeIntentPatterns() {
        Map<QueryIntent, List<Pattern>> patterns = new HashMap<>();

        // 产品咨询
        patterns.put(QueryIntent.PRODUCT_INQUIRY, Arrays.asList(
            Pattern.compile("什么是|介绍|了解|产品|服务"),
            Pattern.compile("有什么|有哪些|包括|种类"),
            Pattern.compile("功能|特点|优势")
        ));

        // 业务流程
        patterns.put(QueryIntent.SERVICE_PROCESS, Arrays.asList(
            Pattern.compile("怎么办|如何申请|流程|步骤"),
            Pattern.compile("需要什么|准备|材料|证件"),
            Pattern.compile("多长时间|几天|审批")
        ));

        // 问题解决
        patterns.put(QueryIntent.PROBLEM_SOLVING, Arrays.asList(
            Pattern.compile("问题|错误|失败|异常"),
            Pattern.compile("解决|处理|修复"),
            Pattern.compile("为什么|怎么回事")
        ));

        // 功能使用
        patterns.put(QueryIntent.FEATURE_USAGE, Arrays.asList(
            Pattern.compile("怎么用|如何使用|操作"),
            Pattern.compile("在哪里|怎么找|入口"),
            Pattern.compile("设置|配置|开启")
        ));

        // 账户管理
        patterns.put(QueryIntent.ACCOUNT_MANAGEMENT, Arrays.asList(
            Pattern.compile("账户|余额|明细"),
            Pattern.compile("修改|更新|变更"),
            Pattern.compile("密码|手机号|地址")
        ));

        // 交易查询
        patterns.put(QueryIntent.TRANSACTION_INQUIRY, Arrays.asList(
            Pattern.compile("交易|转账|汇款|收款"),
            Pattern.compile("查询|记录|历史"),
            Pattern.compile("状态|进度|到账")
        ));

        // 利率查询
        patterns.put(QueryIntent.RATE_INQUIRY, Arrays.asList(
            Pattern.compile("利率|收益率|回报"),
            Pattern.compile("多少|百分比|年化"),
            Pattern.compile("存款|贷款|理财")
        ));

        // 活动信息
        patterns.put(QueryIntent.PROMOTION_INFO, Arrays.asList(
            Pattern.compile("活动|优惠|促销|折扣"),
            Pattern.compile("礼品|奖励|积分|返现"),
            Pattern.compile("参加|条件|要求")
        ));

        return patterns;
    }

    // ==================== 数据类定义 ====================

    /**
     * 搜索场景
     */
    public static class SearchScene {
        private String query;
        private Long userId;
        private LocalDateTime timestamp;
        private BankingScene primaryScene;
        private QueryIntent queryIntent;
        private double confidence;
        private double userPreferenceWeight = 0.0;
        private double historyRelevanceScore = 0.0;
        private double timeRelevanceScore = 0.0;
        private TimeContext timeContext;
        private List<String> tags = new ArrayList<>();
        private List<String> recommendedChannels = new ArrayList<>();

        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public BankingScene getPrimaryScene() { return primaryScene; }
        public void setPrimaryScene(BankingScene primaryScene) { this.primaryScene = primaryScene; }

        public QueryIntent getQueryIntent() { return queryIntent; }
        public void setQueryIntent(QueryIntent queryIntent) { this.queryIntent = queryIntent; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public double getUserPreferenceWeight() { return userPreferenceWeight; }
        public void setUserPreferenceWeight(double userPreferenceWeight) { this.userPreferenceWeight = userPreferenceWeight; }

        public double getHistoryRelevanceScore() { return historyRelevanceScore; }
        public void setHistoryRelevanceScore(double historyRelevanceScore) { this.historyRelevanceScore = historyRelevanceScore; }

        public double getTimeRelevanceScore() { return timeRelevanceScore; }
        public void setTimeRelevanceScore(double timeRelevanceScore) { this.timeRelevanceScore = timeRelevanceScore; }

        public TimeContext getTimeContext() { return timeContext; }
        public void setTimeContext(TimeContext timeContext) { this.timeContext = timeContext; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public List<String> getRecommendedChannels() { return recommendedChannels; }
        public void setRecommendedChannels(List<String> recommendedChannels) { this.recommendedChannels = recommendedChannels; }

        @Override
        public String toString() {
            return String.format("SearchScene{scene=%s, intent=%s, confidence=%.2f, channels=%s}",
                primaryScene, queryIntent, confidence, recommendedChannels);
        }
    }

    /**
     * 时间上下文
     */
    public static class TimeContext {
        private int hour;
        private String dayPeriod;
        private Boolean businessHours;

        public int getHour() { return hour; }
        public void setHour(int hour) { this.hour = hour; }

        public String getDayPeriod() { return dayPeriod; }
        public void setDayPeriod(String dayPeriod) { this.dayPeriod = dayPeriod; }

        public Boolean getBusinessHours() { return businessHours; }
        public void setBusinessHours(Boolean businessHours) { this.businessHours = businessHours; }
    }
}