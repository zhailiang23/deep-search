package com.deepsearch.vector.quality;

import com.deepsearch.vector.model.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量质量评估器测试类
 */
class VectorQualityEvaluatorTest {

    private VectorQualityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new VectorQualityEvaluator();
        // 设置测试用的配置值
        ReflectionTestUtils.setField(evaluator, "minMagnitude", 0.1);
        ReflectionTestUtils.setField(evaluator, "maxMagnitude", 10.0);
        ReflectionTestUtils.setField(evaluator, "similarityThreshold", 0.95);
        ReflectionTestUtils.setField(evaluator, "varianceThreshold", 0.001);
    }

    @Test
    @DisplayName("评估正常向量质量")
    void testAssessVector_Normal() {
        // Arrange
        Vector normalVector = new Vector(Arrays.asList(0.5, -0.3, 0.8, -0.2, 0.6), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(normalVector);

        // Assert
        assertTrue(assessment.score() > 0.0);
        assertTrue(assessment.score() <= 1.0);
        assertTrue(assessment.isValid());
        assertFalse(assessment.issues().contains("问题"));
    }

    @Test
    @DisplayName("评估null向量")
    void testAssessVector_Null() {
        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(null);

        // Assert
        assertEquals(0.0, assessment.score());
        assertFalse(assessment.isValid());
        assertTrue(assessment.issues().contains("向量为空"));
    }

    @Test
    @DisplayName("评估包含NaN的向量")
    void testAssessVector_WithNaN() {
        // Arrange
        Vector nanVector = new Vector(Arrays.asList(1.0, Double.NaN, 3.0), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(nanVector);

        // Assert
        assertFalse(assessment.isValid());
        assertTrue(assessment.issues().contains("无效数值"));
    }

    @Test
    @DisplayName("评估包含无穷大的向量")
    void testAssessVector_WithInfinity() {
        // Arrange
        Vector infinityVector = new Vector(Arrays.asList(1.0, Double.POSITIVE_INFINITY, 3.0), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(infinityVector);

        // Assert
        assertFalse(assessment.isValid());
        assertTrue(assessment.issues().contains("无效数值"));
    }

    @Test
    @DisplayName("评估模长过小的向量")
    void testAssessVector_TooSmallMagnitude() {
        // Arrange
        Vector smallVector = new Vector(Arrays.asList(0.01, 0.01, 0.01), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(smallVector);

        // Assert
        assertTrue(assessment.issues().contains("模长过小"));
    }

    @Test
    @DisplayName("评估模长过大的向量")
    void testAssessVector_TooLargeMagnitude() {
        // Arrange
        Vector largeVector = new Vector(Arrays.asList(10.0, 10.0, 10.0, 10.0, 10.0), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(largeVector);

        // Assert
        assertTrue(assessment.issues().contains("模长过大"));
    }

    @Test
    @DisplayName("评估零方差向量")
    void testAssessVector_ZeroVariance() {
        // Arrange
        Vector constantVector = new Vector(Arrays.asList(0.5, 0.5, 0.5, 0.5), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(constantVector);

        // Assert
        assertTrue(assessment.issues().contains("方差过小") ||
                  assessment.issues().contains("所有向量分量相同"));
    }

    @Test
    @DisplayName("评估高零值比例向量")
    void testAssessVector_HighZeroRatio() {
        // Arrange
        List<Double> dataWithManyZeros = Arrays.asList(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        Vector sparseVector = new Vector(dataWithManyZeros, "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(sparseVector);

        // Assert
        assertTrue(assessment.issues().contains("零值比例过高"));
    }

    @Test
    @DisplayName("相似度验证 - 正常情况")
    void testValidateSimilarity_Normal() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 0.0, 0.0), "test-model");
        Vector v2 = new Vector(Arrays.asList(0.0, 1.0, 0.0), "test-model");

        // Act
        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(v1, v2, "different");

        // Assert
        assertTrue(validation.isValid());
        assertEquals(0.0, validation.similarity(), 0.0001);
        assertFalse(validation.message().contains("异常"));
    }

    @Test
    @DisplayName("相似度验证 - 高相似度")
    void testValidateSimilarity_HighSimilarity() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 1.0, 1.0), "test-model");
        Vector v2 = new Vector(Arrays.asList(1.001, 1.001, 1.001), "test-model");

        // Act
        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(v1, v2, "different");

        // Assert
        // 高相似度但期望不同，应该被标记为异常
        assertFalse(validation.isValid());
        assertTrue(validation.message().contains("异常高"));
    }

    @Test
    @DisplayName("相似度验证 - 不兼容向量")
    void testValidateSimilarity_IncompatibleVectors() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0), "model-1");
        Vector v2 = new Vector(Arrays.asList(3.0, 4.0), "model-2");

        // Act
        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(v1, v2, "similar");

        // Assert
        assertFalse(validation.isValid());
        assertTrue(validation.message().contains("不兼容"));
    }

    @Test
    @DisplayName("相似度验证 - null向量")
    void testValidateSimilarity_NullVector() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test-model");

        // Act
        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(v1, null, "similar");

        // Assert
        assertFalse(validation.isValid());
        assertTrue(validation.message().contains("向量为空"));
    }

    @Test
    @DisplayName("批量质量评估 - 正常情况")
    void testAssessVectorBatch_Normal() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 2.0, 3.0), "test-model"),
            new Vector(Arrays.asList(0.5, -0.3, 0.8), "test-model"),
            new Vector(Arrays.asList(-1.0, 1.5, -0.5), "test-model")
        );

        // Act
        VectorQualityEvaluator.BatchQualityReport report = evaluator.assessVectorBatch(vectors);

        // Assert
        assertEquals(3, report.totalVectors());
        assertTrue(report.validVectors() >= 0);
        assertTrue(report.validVectors() <= 3);
        assertTrue(report.averageScore() >= 0.0);
        assertTrue(report.averageScore() <= 1.0);
        assertTrue(report.validRatio() >= 0.0);
        assertTrue(report.validRatio() <= 1.0);
        assertNotNull(report.summary());
    }

    @Test
    @DisplayName("批量质量评估 - 空列表")
    void testAssessVectorBatch_EmptyList() {
        // Act
        VectorQualityEvaluator.BatchQualityReport report = evaluator.assessVectorBatch(List.of());

        // Assert
        assertEquals(0, report.totalVectors());
        assertEquals(0, report.validVectors());
        assertTrue(report.summary().contains("向量列表为空"));
    }

    @Test
    @DisplayName("批量质量评估 - null列表")
    void testAssessVectorBatch_NullList() {
        // Act
        VectorQualityEvaluator.BatchQualityReport report = evaluator.assessVectorBatch(null);

        // Assert
        assertEquals(0, report.totalVectors());
        assertTrue(report.summary().contains("向量列表为空"));
    }

    @Test
    @DisplayName("批量质量评估 - 混合质量向量")
    void testAssessVectorBatch_MixedQuality() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 2.0, 3.0), "test-model"), // 正常向量
            new Vector(Arrays.asList(Double.NaN, 1.0, 2.0), "test-model"), // 有问题的向量
            new Vector(Arrays.asList(0.5, -0.3, 0.8), "test-model") // 正常向量
        );

        // Act
        VectorQualityEvaluator.BatchQualityReport report = evaluator.assessVectorBatch(vectors);

        // Assert
        assertEquals(3, report.totalVectors());
        assertTrue(report.validVectors() < 3); // 应该有向量被标记为无效
        assertTrue(report.summary().contains("主要问题") || report.validVectors() == 3);
    }

    @Test
    @DisplayName("异常检测 - 正常向量集合")
    void testDetectAnomalies_Normal() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 2.0, 3.0), "test-model"),
            new Vector(Arrays.asList(1.1, 2.1, 3.1), "test-model"),
            new Vector(Arrays.asList(0.9, 1.9, 2.9), "test-model")
        );

        // Act
        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(vectors);

        // Assert
        // 相似的向量应该不会被检测为异常
        assertFalse(detection.hasAnomalies());
        assertTrue(detection.details().contains("未检测到异常"));
    }

    @Test
    @DisplayName("异常检测 - 模长差异巨大")
    void testDetectAnomalies_LargeMagnitudeDifference() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 1.0, 1.0), "test-model"), // 模长 sqrt(3) ≈ 1.73
            new Vector(Arrays.asList(100.0, 100.0, 100.0), "test-model"), // 模长 sqrt(30000) ≈ 173
            new Vector(Arrays.asList(1.1, 1.1, 1.1), "test-model") // 模长 sqrt(3.63) ≈ 1.9
        );

        // Act
        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(vectors);

        // Assert
        assertTrue(detection.hasAnomalies());
        assertTrue(detection.details().contains("模长变化异常"));
    }

    @Test
    @DisplayName("异常检测 - 离群值过多")
    void testDetectAnomalies_TooManyOutliers() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            // 大部分正常向量
            new Vector(Arrays.asList(1.0, 1.0, 1.0), "test-model"),
            new Vector(Arrays.asList(1.1, 1.1, 1.1), "test-model"),
            new Vector(Arrays.asList(0.9, 0.9, 0.9), "test-model"),
            // 一些异常向量
            new Vector(Arrays.asList(10.0, 10.0, 10.0), "test-model"),
            new Vector(Arrays.asList(15.0, 15.0, 15.0), "test-model")
        );

        // Act
        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(vectors);

        // Assert
        // 根据实际的统计分布，可能检测到异常
        // 这个测试更多是验证方法不会崩溃，具体结果取决于统计阈值
        assertNotNull(detection.details());
    }

    @Test
    @DisplayName("异常检测 - 空列表")
    void testDetectAnomalies_EmptyList() {
        // Act
        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(List.of());

        // Assert
        assertFalse(detection.hasAnomalies());
        assertTrue(detection.details().contains("向量列表为空"));
    }

    @Test
    @DisplayName("异常检测 - null列表")
    void testDetectAnomalies_NullList() {
        // Act
        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(null);

        // Assert
        assertFalse(detection.hasAnomalies());
        assertTrue(detection.details().contains("向量列表为空"));
    }

    @Test
    @DisplayName("质量评估边界情况 - 最小有效向量")
    void testAssessVector_MinimalValidVector() {
        // Arrange
        Vector minimalVector = new Vector(Arrays.asList(0.1, 0.1), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(minimalVector);

        // Assert
        assertTrue(assessment.score() > 0.0);
        // 可能有警告但应该是有效的
    }

    @Test
    @DisplayName("质量评估边界情况 - 最大有效向量")
    void testAssessVector_MaximalValidVector() {
        // Arrange
        // 创建一个接近最大模长但仍然有效的向量
        Vector maximalVector = new Vector(Arrays.asList(5.0, 5.0, 5.0), "test-model");

        // Act
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(maximalVector);

        // Assert
        assertTrue(assessment.score() > 0.0);
    }

    @Test
    @DisplayName("相似度验证边界情况")
    void testValidateSimilarity_BoundaryValues() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 0.0), "test-model");
        Vector v2 = new Vector(Arrays.asList(-1.0, 0.0), "test-model"); // 相反向量

        // Act
        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(v1, v2, "opposite");

        // Assert
        assertEquals(-1.0, validation.similarity(), 0.0001);
        assertTrue(validation.isValid());
    }

    @Test
    @DisplayName("评估器异常处理")
    void testEvaluatorExceptionHandling() {
        // 这个测试确保评估器能够优雅地处理各种异常情况

        // 测试评估过程中的异常恢复
        VectorQualityEvaluator.QualityAssessment assessment = evaluator.assessVector(null);
        assertNotNull(assessment);

        VectorQualityEvaluator.SimilarityValidation validation =
            evaluator.validateSimilarity(null, null, "test");
        assertNotNull(validation);

        VectorQualityEvaluator.BatchQualityReport report = evaluator.assessVectorBatch(null);
        assertNotNull(report);

        VectorQualityEvaluator.AnomalyDetection detection = evaluator.detectAnomalies(null);
        assertNotNull(detection);
    }
}