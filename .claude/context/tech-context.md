---
created: 2025-09-20T02:51:13Z
last_updated: 2025-09-20T02:51:13Z
version: 1.0
author: Claude Code PM System
---

# Technology Context

## Technology Stack Overview

### Backend Technology Stack
- **Framework**: Spring Boot 3.x (Monolithic Architecture)
- **Language**: Java 17+ (LTS)
- **Database**: MySQL 8.0+ (Primary data storage)
- **Cache**: Redis 6.0+ (Session management, permission caching)
- **Search Engine**: Elasticsearch 8.0+ (Vector search capabilities)
- **Security**: Spring Security + JWT authentication
- **Build Tool**: Maven or Gradle
- **Container**: Docker + Docker Compose

### Frontend Technology Stack
- **Framework**: Vue 3.x with Composition API
- **Language**: TypeScript
- **UI Library**: shadcn/ui components
- **State Management**: Pinia
- **Routing**: Vue Router 4.x
- **Build Tool**: Vite
- **Styling**: Tailwind CSS (for shadcn components)
- **HTTP Client**: Axios

### Search and AI Technology
- **Vector Processing**:
  - **Offline Mode**: sentence-transformers (local models)
  - **Online Mode**: External vector API services (OpenAI, etc.)
- **Text Processing**: IK Analyzer for Chinese text segmentation
- **Semantic Models**: Chinese pre-trained language models
- **Vector Storage**: Elasticsearch dense_vector fields

### Development Tools and Environment
- **Version Control**: Git
- **Repository**: GitHub (zhailiang23/deep-search)
- **Project Management**: Claude Code PM system
- **Development OS**: macOS (Darwin)
- **Package Manager**:
  - **Java**: Maven/Gradle
  - **Node.js**: npm/yarn
  - **macOS**: Homebrew
- **IDE Support**: VS Code, IntelliJ IDEA

## Current Dependencies and Versions

### Backend Dependencies (Planned)
```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.x</version>
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

<!-- Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
</dependency>

<!-- Vector Processing -->
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
</dependency>
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
- **Java 17+**: `brew install openjdk@17`
- **Node.js 18+**: `brew install node` or use `.nvmrc`
- **Docker**: Docker Desktop for Mac
- **MySQL**: `brew install mysql` or Docker container
- **Redis**: `brew install redis` or Docker container
- **Elasticsearch**: Docker container or Elastic Cloud

### Development Tools
- **GitHub CLI**: `brew install gh` (already installed)
- **Maven**: `brew install maven`
- **Git**: Already available on macOS
- **curl/wget**: For API testing

## Architecture Patterns and Design Decisions

### Backend Architecture
- **Monolithic Design**: Single Spring Boot application (not microservices)
- **Layered Architecture**: Controller → Service → Repository pattern
- **Dependency Injection**: Spring IoC container
- **Configuration Management**: application.properties files (no Spring Cloud Config)
- **Database Access**: JPA/Hibernate for MySQL, Spring Data Elasticsearch

### Frontend Architecture
- **Component-Based**: Vue 3 Composition API
- **State Management**: Pinia stores for global state
- **Reactive Design**: Vue's reactivity system
- **Type Safety**: TypeScript for enhanced development experience
- **UI Consistency**: shadcn/ui component library

### Integration Patterns
- **API Communication**: RESTful APIs with JSON payloads
- **Authentication**: JWT tokens with Redis-based session management
- **File Upload**: Multipart form data for JSON file uploads
- **Real-time Updates**: WebSocket for live analytics (optional)

## Performance and Scalability Considerations

### Performance Targets
- **Search Response Time**: < 500ms (P95)
- **Concurrent Users**: 200+ simultaneous users
- **Data Volume**: Support for tens of thousands of search resources
- **Vector Processing**: Efficient batch and real-time processing

### Scalability Approach
- **Horizontal Scaling**: Support for multiple application instances
- **Database Scaling**: MySQL read replicas, Elasticsearch clustering
- **Caching Strategy**: Multi-level caching (application, Redis, Elasticsearch)
- **Load Balancing**: Nginx for request distribution

## Development Workflow Technology

### Version Control
- **Git Flow**: Feature branches, pull requests, code reviews
- **GitHub Integration**: Issues, project boards, automated workflows
- **Claude Code PM**: Automated project management and task tracking

### Testing Strategy
- **Backend Testing**: JUnit 5, Spring Boot Test, Testcontainers
- **Frontend Testing**: Vitest, Vue Test Utils, Cypress
- **Integration Testing**: API testing with real databases
- **Performance Testing**: Load testing with Apache JMeter

### Deployment Technology
- **Containerization**: Docker containers for all services
- **Orchestration**: Docker Compose for development
- **CI/CD**: GitHub Actions for automated testing and deployment
- **Monitoring**: Spring Boot Actuator, Prometheus metrics