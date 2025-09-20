---
created: 2025-09-20T02:51:13Z
last_updated: 2025-09-20T08:17:33Z
version: 1.1
author: Claude Code PM System
---

# Project Structure

## Root Directory Organization

```
deep-search/
├── .claude/                     # Claude Code PM system files
│   ├── CLAUDE.md               # Project-specific development rules
│   ├── context/                # Project context documentation
│   ├── epics/                  # Epic and task management
│   │   └── 创建智能搜索平台/     # Current epic workspace
│   ├── prds/                   # Product Requirements Documents
│   └── scripts/                # PM automation scripts
├── .git/                       # Git repository data
├── .gitignore                  # Git ignore patterns
├── .serena/                    # Serena MCP tools configuration
├── AGENTS.md                   # Claude Code agent descriptions
├── CLAUDE.md                   # Main development guidance
├── COMMANDS.md                 # Available command reference
├── LICENSE                     # MIT license
├── README.md                   # Project documentation
├── screenshot.webp             # Project screenshot
├── mvnw                        # Maven wrapper script
├── mvnw.cmd                    # Maven wrapper for Windows
├── pom.xml                     # Maven project configuration
├── Dockerfile                  # Container build configuration
├── docker-compose.yml          # Multi-service orchestration
└── docker/                     # Docker configuration files
    └── elasticsearch/
        └── Dockerfile          # Custom Elasticsearch with IK analyzer
```

## Current Implementation Structure

### Backend Implementation (Spring Boot)
```
src/main/java/com/deepsearch/
├── DeepSearchApplication.java          # Main Spring Boot application
├── config/                            # Configuration classes
│   └── RedisConfig.java              # Redis caching configuration
├── controller/                        # REST API endpoints
│   ├── SearchController.java         # Search APIs
│   ├── DocumentController.java       # Document management APIs
│   └── UserController.java           # User management APIs
├── service/                           # Business logic layer
│   ├── SearchService.java            # Core search functionality
│   ├── DocumentService.java          # Document management
│   └── UserService.java              # User management
├── entity/                            # JPA entities
│   ├── Document.java                 # Main document entity
│   ├── User.java                     # User entity
│   ├── SearchLog.java                # Search analytics
│   ├── DocumentVector.java           # Vector embeddings
│   ├── VectorProcessingMetrics.java  # Processing metrics
│   └── VectorProcessingTask.java     # Task management
├── repository/                        # Data access layer
│   ├── DocumentRepository.java       # Document data access
│   ├── UserRepository.java           # User data access
│   ├── SearchLogRepository.java      # Search logs
│   ├── DocumentVectorRepository.java # Vector data
│   ├── VectorMetricsRepository.java  # Metrics data
│   └── VectorTaskRepository.java     # Task data
├── dto/                               # Data Transfer Objects
│   ├── ApiResponse.java              # Standard API response
│   ├── DocumentCreateDto.java        # Document creation
│   ├── DocumentResponseDto.java      # Document response
│   ├── UserLoginDto.java             # User login
│   ├── UserRegistrationDto.java      # User registration
│   └── UserResponseDto.java          # User response
├── exception/                         # Exception handling
│   ├── GlobalExceptionHandler.java   # Global error handler
│   ├── ResourceNotFoundException.java # 404 errors
│   ├── BadRequestException.java      # 400 errors
│   └── ConflictException.java        # 409 errors
├── vector/                            # Vector processing engine
│   ├── config/
│   │   └── VectorProcessingConfig.java # Vector configuration
│   ├── enums/                         # Processing enums
│   │   ├── ProcessingMode.java        # ONLINE/OFFLINE modes
│   │   ├── TaskStatus.java            # Task status tracking
│   │   └── TaskType.java              # Task type definitions
│   ├── model/                         # Vector models
│   │   ├── Vector.java                # Vector data model
│   │   ├── VectorTask.java            # Task model
│   │   ├── ProcessingContext.java     # Processing context
│   │   └── ProcessingMetrics.java     # Metrics model
│   ├── cache/                         # Vector caching
│   │   ├── VectorCache.java           # Cache interface
│   │   └── RedisVectorCache.java      # Redis implementation
│   ├── optimization/                  # Performance optimization
│   │   └── TextPreprocessor.java      # Text preprocessing
│   ├── quality/                       # Quality assurance
│   │   └── VectorQualityEvaluator.java # Quality evaluation
│   ├── util/                          # Utility classes
│   │   └── VectorUtils.java           # Vector utilities
│   ├── monitoring/                    # Monitoring and metrics
│   │   └── VectorMetricsCollector.java # Metrics collection
│   ├── queue/                         # Task queue management
│   │   └── VectorTaskQueueService.java # Queue service
│   ├── scheduler/                     # Task scheduling
│   │   └── VectorProcessingScheduler.java # Processing scheduler
│   └── strategy/                      # Processing strategies
│       └── ModeSwitchStrategy.java    # Mode switching logic
└── elasticsearch/                     # Elasticsearch integration
    ├── config/
    │   ├── ElasticsearchConfig.java   # ES client configuration
    │   └── ElasticsearchIndexConfig.java # Index management
    ├── service/
    │   └── ElasticsearchSearchService.java # Search implementation
    └── analyzer/
        └── SlowQueryAnalyzer.java     # Query optimization
```

### Resources and Configuration
```
src/main/resources/
├── application.yml                    # Main Spring configuration
├── application-vector.yml             # Vector processing config
├── db/migration/                      # Database migrations
│   ├── V1__Create_initial_schema.sql  # Initial schema
│   └── V2__Create_vector_tables.sql   # Vector tables
└── elasticsearch/mappings/            # Elasticsearch mappings
    └── document-index.json            # Document index with IK analyzer
```

### Test Structure
```
src/test/java/com/deepsearch/
├── controller/                        # Controller tests
│   └── UserControllerTest.java       # User API tests
├── service/                           # Service layer tests
│   ├── UserServiceTest.java          # User service tests
│   └── DocumentServiceTest.java      # Document service tests
├── vector/                            # Vector processing tests
│   ├── VectorProcessingIntegrationTest.java # Integration tests
│   ├── cache/
│   │   └── RedisVectorCacheTest.java  # Cache tests
│   ├── model/
│   │   └── VectorTest.java            # Vector model tests
│   ├── optimization/
│   │   └── TextPreprocessorTest.java  # Preprocessing tests
│   ├── quality/
│   │   └── VectorQualityEvaluatorTest.java # Quality tests
│   └── util/
│       └── VectorUtilsTest.java       # Utility tests
└── elasticsearch/                     # Elasticsearch tests
    └── [test files for ES integration]
```

### Docker Infrastructure
```
docker/
└── elasticsearch/
    └── Dockerfile                     # Custom ES with IK analyzer

docker-compose.yml services:
├── mysql                              # MySQL 8.0 database
├── redis                              # Redis 7 cache
├── elasticsearch                      # Elasticsearch 8.11.0 + IK
└── app                                # Spring Boot application
```

## Claude Code PM Structure

### Epic Management (`.claude/epics/`)
- **Epic Directory**: `创建智能搜索平台/`
  - `epic.md` - Technical implementation plan
  - `10.md` through `17.md` - Individual task files (GitHub issue numbers)
  - `github-mapping.md` - Issue URL mappings
  - `updates/` - Implementation progress tracking

### Product Requirements (`.claude/prds/`)
- `创建智能搜索平台.md` - Comprehensive PRD with user stories, requirements, and success criteria

### Context Documentation (`.claude/context/`)
- Context files for project state, architecture, and guidelines
- Automatically maintained by Claude Code PM system

## Implementation Status

### Completed Modules
- ✅ **Core Spring Boot Application**: Main application and configuration
- ✅ **Database Layer**: JPA entities and repositories for all data models
- ✅ **Service Layer**: Business logic for users, documents, and search
- ✅ **API Layer**: REST controllers with comprehensive DTOs
- ✅ **Vector Processing Engine**: Complete dual-mode processing system
- ✅ **Docker Infrastructure**: Full containerized environment
- ✅ **Elasticsearch Integration**: Search engine with IK Chinese analyzer
- ✅ **Redis Caching**: Vector caching and session management
- ✅ **Exception Handling**: Global error handling and custom exceptions

### In Progress
- ⚠️ **Redis Authentication**: Password configuration issue needs resolution
- 🔄 **Search Functionality**: Core search features (Issue #13)

### Planned Modules
- 📋 **Permission System**: Three-tier permission architecture (Issue #14)
- 📋 **Data Management**: JSON upload and field mapping (Issue #15)
- 📋 **Frontend**: Vue.js user interface (Issue #16)
- 📋 **Analytics**: Search statistics and reporting (Issue #17)

## File Naming Patterns

### Task Files
- Format: `{GitHub-issue-number}.md`
- Example: `10.md` corresponds to GitHub Issue #10
- Contains: Task description, acceptance criteria, technical details

### Epic Files
- Format: `epic.md` in named directory
- Directory: `.claude/epics/{epic-name}/`
- Contains: Technical implementation plan and architecture decisions

### Context Files
- Format: `{context-type}.md`
- Location: `.claude/context/`
- Purpose: Maintain project state and knowledge between sessions

## Development Workflow Organization

### Build System
- **Build Tool**: Maven (switched from Gradle)
- **Java Version**: 17
- **Spring Boot**: 3.2.5
- **Container**: Docker with multi-service orchestration

### Database Management
- **Primary**: MySQL 8.0 via Docker
- **Cache**: Redis 7 via Docker
- **Search**: Elasticsearch 8.11.0 with IK analyzer via Docker
- **Migrations**: Flyway for schema management

### Code Organization Principles
- Monolithic Spring Boot backend (not microservices)
- Clear separation between search, permission, data, and analytics modules
- Vector processing engine with intelligent mode switching
- Configuration-driven rather than code-driven customization
- Docker-first deployment strategy

## Update History
- 2025-09-20T08:17:33Z: Updated with current implementation structure, Docker infrastructure, and completed modules based on recent commits