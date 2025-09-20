---
created: 2025-09-20T02:51:13Z
last_updated: 2025-09-20T02:51:13Z
version: 1.0
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
├── AGENTS.md                   # Claude Code agent descriptions
├── CLAUDE.md                   # Main development guidance
├── COMMANDS.md                 # Available command reference
├── LICENSE                     # MIT license
├── README.md                   # Project documentation
└── screenshot.webp             # Project screenshot
```

## Claude Code PM Structure

### Epic Management (`.claude/epics/`)
- **Epic Directory**: `创建智能搜索平台/`
  - `epic.md` - Technical implementation plan
  - `10.md` through `17.md` - Individual task files (GitHub issue numbers)
  - `github-mapping.md` - Issue URL mappings

### Product Requirements (`.claude/prds/`)
- `创建智能搜索平台.md` - Comprehensive PRD with user stories, requirements, and success criteria

### Context Documentation (`.claude/context/`)
- Context files for project state, architecture, and guidelines
- Automatically maintained by Claude Code PM system

## Planned Implementation Structure

Based on the epic, the project will implement the following structure:

### Backend (Spring Boot Monolith)
```
src/main/java/
├── com/bank/search/
│   ├── SearchApplication.java          # Main Spring Boot application
│   ├── config/                         # Configuration classes
│   │   ├── ElasticsearchConfig.java   # Elasticsearch setup
│   │   ├── SecurityConfig.java        # JWT and security
│   │   └── RedisConfig.java           # Redis caching
│   ├── controller/                     # REST API endpoints
│   │   ├── SearchController.java      # Search APIs
│   │   ├── DataController.java        # Data upload APIs
│   │   └── AnalyticsController.java   # Statistics APIs
│   ├── service/                        # Business logic
│   │   ├── SearchService.java         # Core search functionality
│   │   ├── VectorService.java         # Vector processing
│   │   ├── PermissionService.java     # Three-tier permissions
│   │   └── DataService.java           # JSON data management
│   ├── model/                          # Data models and entities
│   │   ├── SearchResource.java        # Main search entity
│   │   ├── User.java                  # User management
│   │   └── Permission.java            # Permission entities
│   └── repository/                     # Data access layer
│       ├── SearchRepository.java      # MySQL repositories
│       └── VectorRepository.java      # Elasticsearch repositories
```

### Frontend (Vue.js Application)
```
src/
├── components/                         # Reusable Vue components
│   ├── search/                        # Search-specific components
│   │   ├── SearchBox.vue              # Main search interface
│   │   ├── SearchResults.vue          # Results display
│   │   └── SearchFilters.vue          # Filter controls
│   └── admin/                         # Admin interface components
│       ├── DataUpload.vue             # JSON upload interface
│       ├── PermissionMatrix.vue       # Permission configuration
│       └── Analytics.vue              # Statistics dashboard
├── views/                             # Page-level components
│   ├── Search.vue                     # Main search page
│   ├── Admin.vue                      # Admin dashboard
│   └── Analytics.vue                  # Analytics page
├── store/                             # Pinia state management
│   ├── search.js                      # Search state
│   ├── auth.js                        # Authentication state
│   └── admin.js                       # Admin state
└── router/                            # Vue Router configuration
    └── index.js                       # Route definitions
```

### Database Structure
```
database/
├── schema/
│   ├── search_resources.sql           # Main search data table
│   ├── users.sql                      # User management
│   ├── permissions.sql                # Permission tables
│   └── analytics.sql                  # Search statistics
└── migrations/
    └── [timestamp]_initial_schema.sql # Database migrations
```

### Configuration Files
```
config/
├── application.properties             # Spring Boot configuration
├── application-dev.properties         # Development settings
├── application-prod.properties        # Production settings
└── docker-compose.yml                 # Container orchestration
```

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

### Parallel Development Support
- Issues #14 and #16 can be developed in parallel
- Each task has clear dependency relationships
- Git worktree available for isolated development at `../epic-创建智能搜索平台`

### Code Organization Principles
- Monolithic Spring Boot backend (not microservices)
- Clear separation between search, permission, data, and analytics modules
- Vue.js frontend with component-based architecture
- Configuration-driven rather than code-driven customization