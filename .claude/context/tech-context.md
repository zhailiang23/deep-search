---
created: 2025-09-20T02:51:13Z
last_updated: 2025-09-20T08:17:33Z
version: 1.1
author: Claude Code PM System
---

# Technology Context

## Technology Stack Overview

### Backend Technology Stack
- **Framework**: Spring Boot 3.2.5 (Monolithic Architecture)
- **Language**: Java 17 (LTS)
- **Database**: MySQL 8.0 (Primary data storage via Docker)
- **Cache**: Redis 7 (Session management, vector caching via Docker)
- **Search Engine**: Elasticsearch 8.11.0 (Vector search with IK Chinese analyzer via Docker)
- **Security**: Spring Security + JWT authentication
- **Build Tool**: Maven (switched from Gradle)
- **Container**: Docker + Docker Compose
- **ORM**: Hibernate/JPA
- **Migration**: Flyway database migrations

### Frontend Technology Stack
- **Framework**: Vue 3.x with Composition API (Planned)
- **Language**: TypeScript (Planned)
- **UI Library**: shadcn/ui components (Planned)
- **State Management**: Pinia (Planned)
- **Routing**: Vue Router 4.x (Planned)
- **Build Tool**: Vite (Planned)
- **Styling**: Tailwind CSS (Planned)
- **HTTP Client**: Axios (Planned)

### Search and AI Technology
- **Vector Processing**:
  - **Offline Mode**: sentence-transformers (local models) - Implemented
  - **Online Mode**: OpenAI API integration - Implemented
  - **Intelligent Mode Switching**: Automatic cost/performance optimization - Implemented
- **Text Processing**: IK Analyzer for Chinese text segmentation - Implemented
- **Vector Storage**: Elasticsearch dense_vector fields (1536 dimensions) - Implemented
- **Vector Caching**: Redis-based caching with TTL - Implemented
- **Quality Evaluation**: Vector quality assessment system - Implemented

### Development Tools and Environment
- **Version Control**: Git
- **Repository**: GitHub (zhailiang23/deep-search)
- **Project Management**: Claude Code PM system + Serena MCP tools
- **Development OS**: macOS (Darwin 25.0.0)
- **Package Manager**:
  - **Java**: Maven wrapper (mvnw)
  - **macOS**: Homebrew (for local tools)
- **IDE Support**: VS Code with various MCP extensions

## Current Dependencies and Versions

### Backend Dependencies (Implemented)
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Elasticsearch with manual dependency management -->
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>8.11.0</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>8.11.0</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>8.11.0</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- HTTP Client for external APIs -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.3</version>
</dependency>

<!-- OpenAPI/Swagger Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-bom</artifactId>
    <version>1.19.6</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### Docker Infrastructure (Implemented)
```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: deep_search
      MYSQL_USER: deep_search_user
      MYSQL_PASSWORD: deep_search_pass
    ports:
      - "3306:3306"
    
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --requirepass redispass
    ports:
      - "6379:6379"
      
  elasticsearch:
    build:
      context: .
      dockerfile: docker/elasticsearch/Dockerfile
    # Custom ES image with IK analyzer
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
```

### Frontend Dependencies (Planned)
```json
{
  "dependencies": {
    "vue": "^3.3.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "@vueuse/core": "^10.0.0",
    "axios": "^1.4.0",
    "tailwindcss": "^3.3.0",
    "@radix-ui/colors": "^1.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.2.0",
    "vite": "^4.3.0",
    "typescript": "^5.0.0",
    "@types/node": "^18.0.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  }
}
```

## Development Environment Setup

### Required System Dependencies
- **Java 17**: Currently using OpenJDK 17.0.16
- **Docker**: Docker Desktop for Mac (required for all middleware)
- **Maven**: Using Maven wrapper (mvnw) included in project
- **Git**: Version control and GitHub integration

### Current Environment Status
- ✅ **MySQL**: Running via Docker on port 3306
- ✅ **Redis**: Running via Docker on port 6379 (password: redispass)
- ✅ **Elasticsearch**: Running via Docker on port 9200 with IK analyzer
- ✅ **Spring Boot**: Running on port 8080
- ⚠️ **Redis Authentication**: Minor configuration issue being resolved

### Development Tools in Use
- **MCP Servers**: context7, serena, playwright, filesystem-with-morph
- **Claude Code**: AI-powered development assistant
- **Docker Compose**: Multi-service orchestration
- **Swagger UI**: API documentation at /swagger-ui.html

## Architecture Patterns and Design Decisions

### Backend Architecture
- **Monolithic Design**: Single Spring Boot application (not microservices)
- **Layered Architecture**: Controller → Service → Repository pattern
- **Dependency Injection**: Spring IoC container
- **Configuration Management**: YAML-based configuration with environment variables
- **Database Access**: JPA/Hibernate for MySQL, Elasticsearch Java Client
- **Vector Processing**: Dual-mode engine with intelligent switching
- **Caching Strategy**: Multi-level caching (Redis + application-level)

### Data Management
- **Primary Storage**: MySQL for structured data
- **Search Storage**: Elasticsearch for documents and vectors
- **Cache Storage**: Redis for sessions, vector cache, and performance optimization
- **Vector Dimensions**: 1536-dimensional embeddings (OpenAI compatible)
- **Text Analysis**: IK Chinese analyzer for enhanced Chinese text processing

### Integration Patterns
- **API Communication**: RESTful APIs with JSON payloads
- **Authentication**: JWT tokens with Redis-based session management
- **Vector Processing**: Asynchronous task queue with batch processing
- **Mode Switching**: Cost and performance-based intelligent switching
- **Error Handling**: Global exception handling with custom error responses

## Performance and Scalability Considerations

### Performance Targets (Implemented Features)
- **Vector Caching**: 24-hour TTL with 10,000 item capacity
- **Connection Pooling**: HikariCP with optimized settings
- **Task Queue**: Batched processing with concurrent execution
- **Elasticsearch**: Optimized index mappings with vector similarity search
- **Redis**: Optimized connection pooling and timeout settings

### Current Performance Optimizations
- **Smart Mode Switching**: Automatic selection between online/offline processing
- **Vector Quality Assessment**: Quality-based processing optimization
- **Text Preprocessing**: Optimized text cleaning and tokenization
- **Database Indexing**: Proper indexes for search and analytics queries
- **Connection Management**: Pooled connections for all external services

### Scalability Approach
- **Horizontal Scaling**: Stateless application design ready for multiple instances
- **Database Scaling**: Docker-based services ready for clustering
- **Caching Strategy**: Redis-based distributed caching
- **Queue Management**: Task queue system for background processing

## Development Workflow Technology

### Version Control
- **Git Flow**: Feature branches, commit management
- **GitHub Integration**: Issues tracking, automated workflows
- **Claude Code PM**: Automated project management and task tracking

### Current Testing Strategy
- **Backend Testing**: JUnit 5, Spring Boot Test, Testcontainers
- **Integration Testing**: Full stack testing with real Docker services
- **Vector Testing**: Quality evaluation and performance testing
- **API Testing**: Controller and service layer testing

### Deployment Technology
- **Containerization**: Docker containers for all services
- **Orchestration**: Docker Compose for development environment
- **Configuration**: Environment-based configuration management
- **Monitoring**: Spring Boot Actuator endpoints for health and metrics

## Update History
- 2025-09-20T08:17:33Z: Updated with current implemented technology stack, actual dependencies, Docker infrastructure, and performance optimizations