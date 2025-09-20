package com.deepsearch.service;

import com.deepsearch.service.SceneClassifier.BankingScene;
import com.deepsearch.service.SceneClassifier.QueryIntent;
import com.deepsearch.service.SceneClassifier.SearchScene;
import com.deepsearch.service.UserBehaviorService.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SceneClassifier测试类
 */
@ExtendWith(MockitoExtension.class)
class SceneClassifierTest {

    @InjectMocks
    private SceneClassifier sceneClassifier;

    private UserProfile testUserProfile;

    @BeforeEach
    void setUp() {
        // 设置测试用户画像
        testUserProfile = new UserProfile();
        testUserProfile.setUserId(100L);
        testUserProfile.setPersonalizationStrength(0.7f);
        testUserProfile.setPreferenceType("BALANCED");
        testUserProfile.setTotalSearches(50L);

        // 设置场景偏好
        Map<String, Double> scenePreferences = new HashMap<>();
        scenePreferences.put("mobile_banking", 0.8);
        scenePreferences.put("internet_banking", 0.6);
        scenePreferences.put("counter_service", 0.3);
        testUserProfile.setScenePreferences(scenePreferences);

        // 设置分类偏好
        Map<String, Double> categoryPreferences = new HashMap<>();
        categoryPreferences.put("产品", 0.7);
        categoryPreferences.put("服务", 0.5);
        testUserProfile.setPreferences(categoryPreferences);
    }

    @Test
    void testClassifyMobileBankingScene() {
        // Given
        String query = "手机银行如何转账";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertNotNull(scene);
        assertEquals(BankingScene.MOBILE_BANKING, scene.getPrimaryScene());
        assertEquals(QueryIntent.FEATURE_USAGE, scene.getQueryIntent());
        assertTrue(scene.getConfidence() > 0.5);
        assertTrue(scene.getRecommendedChannels().contains("mobile_app"));
        assertTrue(scene.getTags().contains("mobile_banking"));
    }

    @Test
    void testClassifyInternetBankingScene() {
        // Given
        String query = "网银U盾怎么使用";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.INTERNET_BANKING, scene.getPrimaryScene());
        assertEquals(QueryIntent.FEATURE_USAGE, scene.getQueryIntent());
        assertTrue(scene.getRecommendedChannels().contains("web"));
    }

    @Test
    void testClassifyCounterServiceScene() {
        // Given
        String query = "银行网点营业时间查询";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.COUNTER_SERVICE, scene.getPrimaryScene());
        assertEquals(QueryIntent.GENERAL_HELP, scene.getQueryIntent());
        assertTrue(scene.getRecommendedChannels().contains("counter"));
    }

    @Test
    void testClassifyAtmServiceScene() {
        // Given
        String query = "ATM取款限额是多少";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.ATM_SERVICE, scene.getPrimaryScene());
        assertEquals(QueryIntent.GENERAL_HELP, scene.getQueryIntent());
        assertTrue(scene.getRecommendedChannels().contains("atm"));
    }

    @Test
    void testClassifyWealthManagementScene() {
        // Given
        String query = "理财产品收益率查询";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.WEALTH_MANAGEMENT, scene.getPrimaryScene());
        assertEquals(QueryIntent.RATE_INQUIRY, scene.getQueryIntent());
        assertTrue(scene.getRecommendedChannels().contains("web"));
    }

    @Test
    void testClassifyLoanServiceScene() {
        // Given
        String query = "房贷申请流程";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.LOAN_SERVICE, scene.getPrimaryScene());
        assertEquals(QueryIntent.SERVICE_PROCESS, scene.getQueryIntent());
        assertTrue(scene.getRecommendedChannels().contains("web"));
    }

    @Test
    void testClassifyCreditCardScene() {
        // Given
        String query = "信用卡积分兑换";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.CREDIT_CARD, scene.getPrimaryScene());
        assertEquals(QueryIntent.FEATURE_USAGE, scene.getQueryIntent());
    }

    @Test
    void testClassifyProductInquiryIntent() {
        // Given
        String query = "银行有什么理财产品";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.PRODUCT_INQUIRY, scene.getQueryIntent());
    }

    @Test
    void testClassifyServiceProcessIntent() {
        // Given
        String query = "开户需要什么材料";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.SERVICE_PROCESS, scene.getQueryIntent());
    }

    @Test
    void testClassifyProblemSolvingIntent() {
        // Given
        String query = "转账失败是什么原因";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.PROBLEM_SOLVING, scene.getQueryIntent());
    }

    @Test
    void testClassifyAccountManagementIntent() {
        // Given
        String query = "修改手机号码";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.ACCOUNT_MANAGEMENT, scene.getQueryIntent());
    }

    @Test
    void testClassifyTransactionInquiryIntent() {
        // Given
        String query = "查询转账记录";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.TRANSACTION_INQUIRY, scene.getQueryIntent());
    }

    @Test
    void testClassifyPromotionInfoIntent() {
        // Given
        String query = "银行最新活动优惠";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(QueryIntent.PROMOTION_INFO, scene.getQueryIntent());
    }

    @Test
    void testUserHistoryAdjustment() {
        // Given
        String query = "手机银行功能";

        // 用户有手机银行使用历史
        testUserProfile.getScenePreferences().put("mobile_banking", 0.9);

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.MOBILE_BANKING, scene.getPrimaryScene());
        assertTrue(scene.getUserPreferenceWeight() > 0.5);
    }

    @Test
    void testTimeContextAdjustment() {
        // Given
        String query = "银行营业时间";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertNotNull(scene.getTimeContext());
        assertNotNull(scene.getTimeContext().getDayPeriod());
        assertNotNull(scene.getTimeContext().getBusinessHours());
    }

    @Test
    void testSceneConfidenceCalculation() {
        // Given
        String query = "手机银行扫码转账功能";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertTrue(scene.getConfidence() >= 0.0 && scene.getConfidence() <= 1.0);
        assertTrue(scene.getConfidence() > 0.3); // 应该有较高置信度
    }

    @Test
    void testSceneTagGeneration() {
        // Given
        String query = "紧急处理银行卡丢失";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        List<String> tags = scene.getTags();
        assertNotNull(tags);
        assertTrue(tags.contains("urgent"));
        assertTrue(tags.size() > 1);
    }

    @Test
    void testBeginnerUserTagGeneration() {
        // Given
        String query = "新手如何使用网银";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertTrue(scene.getTags().contains("beginner"));
    }

    @Test
    void testAdvancedUserTagGeneration() {
        // Given
        String query = "高级理财策略咨询";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertTrue(scene.getTags().contains("advanced"));
    }

    @Test
    void testRecommendedChannelsForMobileBanking() {
        // Given
        String query = "手机银行转账";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        List<String> channels = scene.getRecommendedChannels();
        assertTrue(channels.contains("mobile_app"));
        assertTrue(channels.contains("mobile_web"));
        assertTrue(channels.contains("sms"));
    }

    @Test
    void testRecommendedChannelsForCounterService() {
        // Given
        String query = "开户业务办理";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        List<String> channels = scene.getRecommendedChannels();
        assertTrue(channels.contains("counter"));
        assertTrue(channels.contains("appointment"));
    }

    @Test
    void testGeneralInquiryFallback() {
        // Given
        String query = "随机不相关的查询内容xyz123";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        assertEquals(BankingScene.GENERAL_INQUIRY, scene.getPrimaryScene());
        assertEquals(QueryIntent.GENERAL_HELP, scene.getQueryIntent());
        assertTrue(scene.getConfidence() < 0.5); // 置信度应该较低
    }

    @Test
    void testSceneToString() {
        // Given
        String query = "手机银行转账";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);
        String sceneString = scene.toString();

        // Then
        assertNotNull(sceneString);
        assertTrue(sceneString.contains("MOBILE_BANKING"));
        assertTrue(sceneString.contains("mobile_app"));
    }

    @Test
    void testMultipleKeywordsSceneClassification() {
        // Given
        String query = "手机银行网银转账功能比较";

        // When
        SearchScene scene = sceneClassifier.classifyScene(query, testUserProfile);

        // Then
        // 应该识别为手机银行场景（权重更高）
        assertTrue(scene.getPrimaryScene() == BankingScene.MOBILE_BANKING ||
                  scene.getPrimaryScene() == BankingScene.INTERNET_BANKING);
        assertTrue(scene.getConfidence() > 0.4);
    }
}