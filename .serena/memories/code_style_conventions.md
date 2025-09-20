# Deep Search Platform - 代码风格和约定

## 项目结构约定

### 包结构
```
com.deepsearch/
├── controller/     # REST API控制器
├── service/        # 业务逻辑服务
├── repository/     # 数据访问层
├── entity/         # JPA实体类
├── dto/           # 数据传输对象
├── config/        # 配置类
├── security/      # 安全相关
├── exception/     # 异常处理
├── elasticsearch/ # ES相关功能
└── vector/        # 向量处理
```

### 命名约定

#### Java类命名
- **Controller类**: `*Controller` (如: `UserController`, `SearchController`)
- **Service类**: `*Service` (如: `UserService`, `SearchService`)  
- **Repository类**: `*Repository` (如: `UserRepository`)
- **Entity类**: 实体名称 (如: `User`, `SearchLog`)
- **DTO类**: `*Dto` (如: `UserResponseDto`, `SearchRequestDto`)
- **Config类**: `*Config` (如: `CacheConfig`, `SecurityConfig`)

#### 方法命名
- REST端点: HTTP动词风格 (`getUsers`, `createUser`, `updateUser`, `deleteUser`)
- Service方法: 业务逻辑描述 (`findUserByEmail`, `performSemanticSearch`)
- Repository方法: JPA标准 (`findBy*`, `save`, `delete`)

## 代码风格

### 注解使用
- **Lombok**: 广泛使用 `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`
- **Spring**: `@RestController`, `@Service`, `@Repository`, `@Entity`
- **Validation**: `@Valid`, `@NotNull`, `@NotBlank` 用于数据验证
- **缓存**: `@Cacheable`, `@CacheEvict` 用于缓存管理

### 异常处理
- 统一异常处理器模式
- 自定义异常类继承适当的Spring异常
- 返回标准化的错误响应格式

### 数据传输
- Controller层使用DTO对象
- Service层内部可以直接使用Entity
- 明确区分请求DTO和响应DTO

### 测试约定
- 测试类命名: `*Test` (如: `UserServiceTest`)
- 集成测试: `*IntegrationTest`
- 使用`@SpringBootTest`进行集成测试
- 使用`@MockBean`进行单元测试Mock
- Testcontainers用于数据库测试

## 配置约定

### application.yml结构
```yaml
spring:
  datasource: # 数据库配置
  jpa: # JPA配置  
  redis: # Redis配置
  elasticsearch: # ES配置
  security: # 安全配置

app:
  vector: # 向量处理配置
  search: # 搜索配置
  cache: # 缓存配置
```

### 环境变量
- 数据库: `DB_USERNAME`, `DB_PASSWORD`
- Redis: `SPRING_REDIS_HOST`, `SPRING_REDIS_PASSWORD`
- Elasticsearch: `ELASTICSEARCH_HOST`, `ELASTICSEARCH_PORT`
- JWT: `JWT_SECRET`

## 安全约定

### JWT处理
- 使用`io.jsonwebtoken`库
- Token存储在Redis中进行管理
- 统一的认证过滤器

### 权限管理
- 基于角色的访问控制(RBAC)
- 三层权限结构: Space → Channel → Role
- 使用Spring Security的`@PreAuthorize`注解

## 数据库约定

### 表命名
- 使用下划线分隔的小写字母
- 实体映射使用`@Table(name = "table_name")`

### 字段约定
- ID字段统一使用`@GeneratedValue`
- 时间字段使用`@CreationTimestamp`, `@UpdateTimestamp`
- 外键关系使用JPA注解明确定义

## 日志约定

### 日志级别
- **ERROR**: 系统错误，需要立即关注
- **WARN**: 警告信息，可能影响功能
- **INFO**: 关键业务流程信息
- **DEBUG**: 详细调试信息

### 日志内容
- 包含关键业务参数
- 搜索相关操作记录查询条件和结果
- 异常处理包含堆栈信息