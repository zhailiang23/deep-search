---
issue: 11
title: 双模式向量处理引擎
analyzed: 2025-09-20T06:13:37Z
estimated_hours: 8
parallelization_factor: 2.0
---

# Parallel Work Analysis: Issue #11

## Overview

Issue #11 需要对现有的向量处理引擎进行配置化模式切换的修改。大部分功能已经实现，当前只需要简化智能切换逻辑，改为基于配置文件的模式选择。这是一个相对小规模的重构任务，主要涉及移除复杂的自动决策逻辑，替换为简单的配置驱动方式。

## Parallel Streams

### Stream A: 配置系统重构
**Scope**: 简化模式切换配置，移除复杂的自动决策逻辑
**Files**:
- `src/main/java/com/deepsearch/vector/config/VectorProcessingConfig.java`
- `src/main/java/com/deepsearch/vector/VectorProcessingEngineImpl.java`
- `src/main/java/com/deepsearch/vector/ProcessingMode.java`
- `src/main/resources/application.yml`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 4
**Dependencies**: none

### Stream B: 清理旧代码和测试更新
**Scope**: 移除旧的智能切换策略代码，更新相关测试
**Files**:
- `src/main/java/com/deepsearch/vector/strategy/ModeSwithcStrategy.java` (删除)
- `src/test/java/com/deepsearch/vector/VectorProcessingEngineTest.java`
- `src/test/java/com/deepsearch/vector/integration/ConfigBasedModeSelectionTest.java` (新增)
- `src/test/resources/application-test.yml`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 4
**Dependencies**: none

## Coordination Points

### Shared Files
以下文件需要多个流协调修改：
- `VectorProcessingEngineImpl.java` - Stream A 修改注入，Stream B 更新测试
- `application.yml` - Stream A 简化配置，Stream B 添加测试配置

### Sequential Requirements
无严格的顺序要求，两个流可以完全并行执行：
1. Stream A 和 Stream B 可以同时进行
2. 最后进行集成测试验证

## Conflict Risk Assessment
- **Low Risk**: 两个流处理不同的代码区域
- **文件协调**: VectorProcessingEngineImpl.java 需要简单协调，但冲突风险很低
- **测试隔离**: 测试文件相互独立，不存在冲突

## Parallelization Strategy

**Recommended Approach**: parallel

同时启动 Stream A 和 Stream B，它们处理不同的代码区域：
- Stream A 专注于配置系统和核心逻辑修改
- Stream B 专注于清理工作和测试更新

两个流可以完全独立工作，最后合并时只需要简单的协调。

## Expected Timeline

With parallel execution:
- Wall time: 4 hours (最长流的时间)
- Total work: 8 hours
- Efficiency gain: 50%

Without parallel execution:
- Wall time: 8 hours

## Detailed Work Breakdown

### Stream A Tasks:
1. **修改 ProcessingMode 枚举** (0.5h)
   - 移除 AUTO_SWITCH 值
   - 保留 OFFLINE_BATCH 和 ONLINE_REALTIME

2. **创建 VectorProcessingConfig 配置类** (1h)
   - 使用 @ConfigurationProperties
   - 设置默认模式为 ONLINE_REALTIME
   - 添加模式验证逻辑

3. **重构 VectorProcessingEngineImpl** (2h)
   - 移除 ModeSwithcStrategy 依赖
   - 注入 VectorProcessingConfig
   - 简化模式选择逻辑

4. **更新 application.yml 配置** (0.5h)
   - 简化向量处理配置
   - 移除复杂的模式切换参数
   - 添加清晰的配置注释

### Stream B Tasks:
1. **删除旧的策略类** (0.5h)
   - 删除 ModeSwithcStrategy.java
   - 删除相关的系统指标类

2. **更新现有测试** (2h)
   - 修改 VectorProcessingEngineTest.java
   - 移除自动切换相关测试
   - 添加配置化模式测试

3. **创建新的集成测试** (1h)
   - ConfigBasedModeSelectionTest.java
   - 测试配置文件驱动的模式选择

4. **更新测试配置** (0.5h)
   - application-test.yml 配置
   - 添加不同模式的测试场景

## Success Criteria

完成后需要验证：
1. ✅ 配置文件可以正确控制向量处理模式
2. ✅ 移除了所有自动决策相关代码
3. ✅ 现有功能保持正常工作
4. ✅ 所有测试通过
5. ✅ 配置文档清晰易懂

## Notes

- 这是一个简化重构任务，风险较低
- 主要目标是降低系统复杂度，提高可维护性
- 保持向后兼容性，确保现有向量处理功能正常
- 优先考虑代码清晰度和配置简单性