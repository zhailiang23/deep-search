package com.deepsearch.vector.quality;

import com.deepsearch.vector.model.Vector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * 向量质量评估器
 * 评估向量的质量、相似度验证和异常检测
 */
@Component
@Slf4j
public class VectorQualityEvaluator {

    @Value("${vector.quality.min-magnitude:0.1}")
    private double minMagnitude;

    @Value("${vector.quality.max-magnitude:10.0}")
    private double maxMagnitude;

    @Value("${vector.quality.similarity-threshold:0.95}")
    private double similarityThreshold;

    @Value("${vector.quality.variance-threshold:0.001}")
    private double varianceThreshold;

    /**
     * 评估单个向量的质量
     *
     * @param vector 待评估的向量
     * @return 质量评估结果
     */
    public QualityAssessment assessVector(Vector vector) {
        if (vector == null) {
            return new QualityAssessment(0.0, false, "向量为空");
        }

        try {
            double score = 0.0;
            StringBuilder issues = new StringBuilder();

            // 1. 检查向量维度
            if (vector.getDimension() <= 0) {
                issues.append("无效的向量维度; ");
                return new QualityAssessment(0.0, false, issues.toString());
            }

            // 2. 检查向量模长
            double magnitudeScore = assessMagnitude(vector.getMagnitude(), issues);
            score += magnitudeScore * 0.3;

            // 3. 检查向量分布
            double distributionScore = assessDistribution(vector, issues);
            score += distributionScore * 0.4;

            // 4. 检查数值稳定性
            double stabilityScore = assessStability(vector, issues);
            score += stabilityScore * 0.3;

            boolean isValid = score >= 0.7 && issues.length() == 0;
            String issueText = issues.length() > 0 ? issues.toString() : "无问题";

            log.debug("向量质量评估完成: score={}, valid={}, dimension={}, model={}",
                score, isValid, vector.getDimension(), vector.getModelName());

            return new QualityAssessment(score, isValid, issueText);

        } catch (Exception e) {
            log.error("向量质量评估失败", e);
            return new QualityAssessment(0.0, false, "评估过程出错: " + e.getMessage());
        }
    }

    /**
     * 评估向量模长
     */
    private double assessMagnitude(double magnitude, StringBuilder issues) {
        if (Double.isNaN(magnitude) || Double.isInfinite(magnitude)) {
            issues.append("向量模长无效; ");
            return 0.0;
        }

        if (magnitude < minMagnitude) {
            issues.append(String.format("向量模长过小(%.4f < %.4f); ", magnitude, minMagnitude));
            return Math.max(0.0, magnitude / minMagnitude);
        }

        if (magnitude > maxMagnitude) {
            issues.append(String.format("向量模长过大(%.4f > %.4f); ", magnitude, maxMagnitude));
            return Math.max(0.0, 1.0 - (magnitude - maxMagnitude) / maxMagnitude);
        }

        return 1.0;
    }

    /**
     * 评估向量分布特性
     */
    private double assessDistribution(Vector vector, StringBuilder issues) {
        List<Double> data = vector.getData();

        // 计算统计量
        DoubleSummaryStatistics stats = data.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        double mean = stats.getAverage();
        double min = stats.getMin();
        double max = stats.getMax();

        // 1. 检查是否有NaN或无穷大
        boolean hasInvalidValues = data.stream()
            .anyMatch(val -> Double.isNaN(val) || Double.isInfinite(val));

        if (hasInvalidValues) {
            issues.append("包含无效数值(NaN/Infinity); ");
            return 0.0;
        }

        // 2. 检查值的范围
        double score = 1.0;

        // 检查是否所有值都相同（零方差）
        double variance = calculateVariance(data, mean);
        if (variance < varianceThreshold) {
            issues.append(String.format("向量方差过小(%.6f); ", variance));
            score *= 0.5;
        }

        // 检查极值比例
        double range = max - min;
        if (range == 0.0) {
            issues.append("所有向量分量相同; ");
            score *= 0.3;
        }

        // 检查零值比例
        long zeroCount = data.stream()
            .mapToLong(val -> Math.abs(val) < 1e-10 ? 1 : 0)
            .sum();

        double zeroRatio = (double) zeroCount / data.size();
        if (zeroRatio > 0.9) {
            issues.append(String.format("零值比例过高(%.2f%%); ", zeroRatio * 100));
            score *= 0.4;
        }

        return score;
    }

    /**
     * 评估向量数值稳定性
     */
    private double assessStability(Vector vector, StringBuilder issues) {
        List<Double> data = vector.getData();
        double score = 1.0;

        // 检查数值精度问题
        double sumSquares = data.stream()
            .mapToDouble(val -> val * val)
            .sum();

        if (sumSquares < 1e-20) {
            issues.append("向量数值过小可能导致精度问题; ");
            score *= 0.6;
        }

        if (sumSquares > 1e20) {
            issues.append("向量数值过大可能导致溢出; ");
            score *= 0.6;
        }

        return score;
    }

    /**
     * 计算方差
     */
    private double calculateVariance(List<Double> data, double mean) {
        return data.stream()
            .mapToDouble(val -> Math.pow(val - mean, 2))
            .average()
            .orElse(0.0);
    }

    /**
     * 验证两个向量的相似度是否合理
     */
    public SimilarityValidation validateSimilarity(Vector vector1, Vector vector2,
                                                  String expectedRelation) {
        if (vector1 == null || vector2 == null) {
            return new SimilarityValidation(false, 0.0, "向量为空");
        }

        if (!vector1.isCompatibleWith(vector2)) {
            return new SimilarityValidation(false, 0.0, "向量不兼容");
        }

        try {
            double similarity = vector1.cosineSimilarity(vector2);
            boolean isValid = true;
            StringBuilder message = new StringBuilder();

            // 检查相似度合理性
            if (similarity > similarityThreshold) {
                if (!"identical".equals(expectedRelation) && !"very_similar".equals(expectedRelation)) {
                    isValid = false;
                    message.append("相似度异常高（可能是重复向量）; ");
                }
            }

            if (similarity < -0.8) {
                message.append("相似度异常低; ");
            }

            if (Double.isNaN(similarity)) {
                isValid = false;
                message.append("相似度计算结果无效; ");
            }

            String resultMessage = message.length() > 0 ? message.toString() : "相似度验证通过";

            log.debug("相似度验证: similarity={}, valid={}, expected={}",
                similarity, isValid, expectedRelation);

            return new SimilarityValidation(isValid, similarity, resultMessage);

        } catch (Exception e) {
            log.error("相似度验证失败", e);
            return new SimilarityValidation(false, 0.0, "计算过程出错: " + e.getMessage());
        }
    }

    /**
     * 批量评估向量质量
     */
    public BatchQualityReport assessVectorBatch(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return new BatchQualityReport(0, 0, 0.0, 0.0, "向量列表为空");
        }

        int totalVectors = vectors.size();
        int validVectors = 0;
        double totalScore = 0.0;
        StringBuilder issues = new StringBuilder();

        for (Vector vector : vectors) {
            QualityAssessment assessment = assessVector(vector);
            totalScore += assessment.score();

            if (assessment.isValid()) {
                validVectors++;
            } else {
                issues.append(String.format("向量质量问题: %s; ", assessment.issues()));
            }
        }

        double averageScore = totalScore / totalVectors;
        double validRatio = (double) validVectors / totalVectors;

        String reportMessage = String.format(
            "批量评估完成: %d/%d个向量有效(%.1f%%), 平均得分: %.3f",
            validVectors, totalVectors, validRatio * 100, averageScore);

        if (issues.length() > 0) {
            reportMessage += "; 主要问题: " + issues.toString();
        }

        return new BatchQualityReport(totalVectors, validVectors, averageScore, validRatio, reportMessage);
    }

    /**
     * 检测向量异常
     */
    public AnomalyDetection detectAnomalies(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return new AnomalyDetection(false, "向量列表为空");
        }

        try {
            // 计算向量的统计特征
            List<Double> magnitudes = vectors.stream()
                .map(Vector::getMagnitude)
                .toList();

            DoubleSummaryStatistics stats = magnitudes.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

            double mean = stats.getAverage();
            double min = stats.getMin();
            double max = stats.getMax();

            // 检测异常
            StringBuilder anomalies = new StringBuilder();
            boolean hasAnomalies = false;

            // 1. 模长异常
            if (max / min > 100) { // 模长差距过大
                hasAnomalies = true;
                anomalies.append(String.format("模长变化异常(最大/最小=%.2f); ", max / min));
            }

            // 2. 计算标准差
            double variance = magnitudes.stream()
                .mapToDouble(mag -> Math.pow(mag - mean, 2))
                .average()
                .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            // 3. 检查离群值
            long outliers = magnitudes.stream()
                .mapToLong(mag -> Math.abs(mag - mean) > 3 * stdDev ? 1 : 0)
                .sum();

            if (outliers > vectors.size() * 0.1) { // 超过10%的离群值
                hasAnomalies = true;
                anomalies.append(String.format("离群值过多(%d个); ", outliers));
            }

            String message = hasAnomalies ? anomalies.toString() : "未检测到异常";

            log.debug("异常检测完成: 检查{}个向量, 发现异常: {}", vectors.size(), hasAnomalies);

            return new AnomalyDetection(hasAnomalies, message);

        } catch (Exception e) {
            log.error("异常检测失败", e);
            return new AnomalyDetection(true, "检测过程出错: " + e.getMessage());
        }
    }

    /**
     * 质量评估结果
     */
    public record QualityAssessment(double score, boolean isValid, String issues) {}

    /**
     * 相似度验证结果
     */
    public record SimilarityValidation(boolean isValid, double similarity, String message) {}

    /**
     * 批量质量报告
     */
    public record BatchQualityReport(int totalVectors, int validVectors,
                                   double averageScore, double validRatio, String summary) {}

    /**
     * 异常检测结果
     */
    public record AnomalyDetection(boolean hasAnomalies, String details) {}
}