# Deep Search Platform - 项目概览

## 项目目的
Deep Search Platform是一个专为银行业务场景设计的**智能搜索平台**，提供语义搜索功能，支持银行产品、服务、活动和信息的高级搜索，并包含权限管理和分析功能。

## 技术栈
- **后端**: Spring Boot 3.2.5 (单体架构)
- **Java版本**: Java 17
- **数据库**: MySQL 8.0+ (主数据库) + Redis 7.0+ (缓存)
- **搜索引擎**: Elasticsearch 8.11.0 (带向量搜索功能)
- **语义处理**: sentence-transformers模型或外部API
- **部署**: Docker容器化
- **构建工具**: Maven
- **安全**: Spring Security + JWT
- **缓存**: Redis + Caffeine
- **API文档**: SpringDoc OpenAPI 2.3.0

## 核心功能模块
1. **搜索引擎模块**: 使用Elasticsearch进行语义搜索和向量处理
2. **权限管理**: 三层架构 (Space → Channel → Role)
3. **数据管理**: JSON上传和动态字段映射
4. **分析模块**: 搜索行为跟踪和统计

## 架构特点
- 单体Spring Boot应用
- 双模式向量处理 (离线/在线)
- 三层权限系统
- 基于JSON的数据导入与动态字段映射
- 全面的搜索分析和报表功能

## 项目状态
当前项目正在积极开发中，最近完成了Issue #13的所有功能：
- 同义词与查询扩展系统
- 自动补全与搜索建议功能
- 语义搜索模块
- 测试编译错误修复