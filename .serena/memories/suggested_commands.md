# Deep Search Platform - 建议命令

## 开发环境运行命令

### Docker环境启动
```bash
# 启动所有中间件服务（MySQL, Redis, Elasticsearch）
docker compose up -d

# 检查服务健康状态
docker compose ps

# 查看日志
docker logs deep-search-mysql
docker logs deep-search-redis  
docker logs deep-search-elasticsearch
```

### Spring Boot应用运行
```bash
# 使用Maven运行（开发环境）
DB_USERNAME=deep_search_user DB_PASSWORD=deep_search_pass ./mvnw spring-boot:run -Dmaven.test.skip=true

# 使用Docker运行完整应用
docker compose up app

# 带Redis和Elasticsearch的完整运行
DB_USERNAME=deep_search_user DB_PASSWORD=deep_search_pass SPRING_REDIS_HOST=localhost SPRING_REDIS_PASSWORD=redispass ELASTICSEARCH_HOST=localhost ./mvnw spring-boot:run -Dmaven.test.skip=true
```

### 构建和测试命令
```bash
# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包应用
./mvnw package -Dmaven.test.skip=true

# 清理项目
./mvnw clean
```

### 数据库相关
```bash
# 连接MySQL
mysql -h localhost -P 3306 -u deep_search_user -p

# 连接Redis
redis-cli -h localhost -p 6379 -a redispass

# 检查Elasticsearch
curl -X GET "localhost:9200/_cluster/health"
```

## 项目管理命令

### Claude Code PM系统
```bash
# 初始化PM系统
/pm:init

# 查看项目状态
/pm:status

# 获取下一个优先任务
/pm:next

# 开始处理特定issue
/pm:issue-start <issue_number>

# 更新上下文
/context:update

# 创建新的PRD
/pm:prd-new <feature-name>
```

## 系统工具命令 (macOS/Darwin)

### 基本文件操作
```bash
# 列出文件
ls -la

# 搜索文件
find . -name "*.java" -type f

# 搜索代码内容  
grep -r "pattern" src/

# 目录导航
cd path/to/directory

# 复制文件
cp source destination

# 移动/重命名文件
mv source destination
```

### Git操作
```bash
# 查看状态
git status

# 查看提交历史
git log --oneline -10

# 查看差异
git diff

# 添加文件
git add .

# 提交更改
git commit -m "commit message"
```

### 进程管理
```bash
# 查看Java进程
jps

# 查看端口占用
lsof -i :8080

# 终止进程
pkill -f "spring-boot"

# 查看系统资源
top
```