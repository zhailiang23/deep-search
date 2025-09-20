# Deep Search Platform - 任务完成检查清单

## 开发任务完成后必须执行的步骤

### 1. 代码质量检查
```bash
# Maven编译检查
./mvnw compile

# 运行所有测试
./mvnw test

# 检查测试覆盖率
./mvnw test jacoco:report
```

### 2. 代码格式化和静态检查
```bash
# Maven代码格式检查 (如果配置了)
./mvnw spotless:check

# 应用代码格式化
./mvnw spotless:apply
```

### 3. 应用启动测试
```bash
# 确保Docker服务运行
docker compose up -d mysql redis elasticsearch

# 启动应用并验证
DB_USERNAME=deep_search_user DB_PASSWORD=deep_search_pass SPRING_REDIS_HOST=localhost SPRING_REDIS_PASSWORD=redispass ELASTICSEARCH_HOST=localhost ./mvnw spring-boot:run -Dmaven.test.skip=true

# 检查健康端点
curl http://localhost:8080/actuator/health
```

### 4. API功能验证
```bash
# 检查Swagger UI
open http://localhost:8080/swagger-ui.html

# 验证关键API端点
curl -X GET "http://localhost:8080/api/users"
curl -X GET "http://localhost:8080/api/search/suggest?q=test"
```

### 5. 数据库检查
```bash
# 检查数据库连接
mysql -h localhost -P 3306 -u deep_search_user -p -e "SHOW TABLES;"

# 验证Flyway迁移
./mvnw flyway:info
```

### 6. 搜索功能验证
```bash
# 检查Elasticsearch健康状态
curl -X GET "localhost:9200/_cluster/health"

# 验证索引是否正确创建
curl -X GET "localhost:9200/_cat/indices"
```

### 7. 缓存功能验证
```bash
# 检查Redis连接
redis-cli -h localhost -p 6379 -a redispass ping

# 验证缓存功能
redis-cli -h localhost -p 6379 -a redispass keys "*"
```

## 提交前检查清单

### Git提交规范
- [ ] 代码编译通过
- [ ] 所有测试通过
- [ ] 无明显的代码质量问题
- [ ] 提交信息清晰描述变更内容
- [ ] 没有敏感信息（密码、API密钥等）

### 文档更新
- [ ] 如有新API，更新API文档
- [ ] 如有配置变更，更新配置说明
- [ ] 如有新依赖，更新README

### 性能考虑
- [ ] 新代码不会导致明显性能问题
- [ ] 数据库查询已优化
- [ ] 缓存策略合理
- [ ] 内存使用控制在合理范围

## 部署前检查清单

### 环境配置
- [ ] 生产环境配置文件正确
- [ ] 数据库迁移脚本准备就绪
- [ ] 环境变量配置完整
- [ ] SSL证书配置正确

### 监控和日志
- [ ] 关键业务流程有适当日志
- [ ] 错误处理和异常捕获完整
- [ ] 监控指标配置正确
- [ ] 健康检查端点正常

### 安全检查
- [ ] 敏感数据加密存储
- [ ] API访问权限控制正确
- [ ] SQL注入防护到位
- [ ] XSS攻击防护到位