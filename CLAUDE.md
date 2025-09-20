# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an **intelligent search platform** (z�"s�) designed specifically for banking scenarios. The platform provides semantic search capabilities for bank products, services, activities, and information with advanced permission management and analytics.

### Key Technologies
- **Backend**: Spring Boot 3.x (monolithic architecture)
- **Frontend**: Vue 3.x with TypeScript and shadcn UI components
- **Search**: Elasticsearch 8.0+ with vector search capabilities
- **Database**: MySQL 8.0+ (primary) + Redis 6.0+ (caching)
- **Semantic Processing**: sentence-transformers models or external APIs
- **Deployment**: Docker containers

### Architecture
The platform uses a **monolithic Spring Boot application** with separate Vue.js frontend, implementing:
- Dual-mode vector processing (offline/online)
- Three-tier permission system (Space � Channel � Role)
- JSON-based data import with dynamic field mapping
- Comprehensive search analytics and reporting

## Development Commands

### Project Management (Claude Code PM)
```bash
/pm:status              # Show overall project status
/pm:next               # Get next priority task
/pm:issue-start <num>  # Start working on specific GitHub issue
/pm:epic-show <name>   # Show epic details and progress
```

### Common Development Tasks
Since this is a Spring Boot + Vue.js project:
- Backend development happens in Spring Boot modules
- Frontend development in Vue 3.x components
- Elasticsearch configuration for search capabilities
- MySQL schema management for data persistence
- Redis configuration for caching and sessions

## SUB-AGENT USAGE (MANDATORY)

### 1. Always use the file-analyzer sub-agent when reading files
Especially for log files and verbose outputs. Provides concise summaries while preserving critical information.

### 2. Always use the code-analyzer sub-agent for code analysis
Use when searching code, analyzing logic flow, researching bugs, or tracing execution paths.

### 3. Always use the test-runner sub-agent for testing
Ensures full test output capture, clean conversation flow, and proper issue surfacing.

## ABSOLUTE RULES

- **NO PARTIAL IMPLEMENTATION** - Complete all functionality fully
- **NO SIMPLIFICATION** - No placeholder comments like "//This is simplified for now"
- **NO CODE DUPLICATION** - Check existing codebase before writing new functions
- **NO DEAD CODE** - Either use code or delete it completely
- **IMPLEMENT TESTS FOR EVERY FUNCTION** - Comprehensive test coverage required
- **NO CHEATER TESTS** - Tests must be accurate and reveal real flaws
- **CONSISTENT NAMING** - Follow existing codebase patterns
- **NO OVER-ENGINEERING** - Prefer simple working solutions
- **CLEAR SEPARATION OF CONCERNS** - No mixed responsibilities
- **RESOURCE MANAGEMENT** - Clean up connections, timeouts, listeners

## Error Handling Philosophy

- **Fail fast** for critical configuration (missing text model)
- **Log and continue** for optional features (extraction model)
- **Graceful degradation** when external services unavailable
- **User-friendly messages** through resilience layer

## Testing Requirements

- Always use the test-runner agent to execute tests
- Never use mock services - use real implementations
- Complete one test before moving to the next
- Tests must be verbose for debugging
- Tests should reflect real usage scenarios

## Search Platform Specific Guidance

### Core Modules
1. **Search Engine Module**: Semantic search with Elasticsearch and vector processing
2. **Permission Management**: Three-tier architecture (Space/Channel/Role)
3. **Data Management**: JSON upload and dynamic field mapping
4. **Analytics Module**: Search behavior tracking and statistics

### Key Features to Understand
- Semantic search with synonym matching (e.g., "?7" matches "O?7>")
- Contextual search (e.g., "��" relates to "D�Q�")
- Multi-channel access control (mobile banking, internet banking, counter systems)
- Dynamic data source configuration without code changes
- Real-time search analytics and heat maps

### Development Focus Areas
- Vector similarity search implementation
- JWT-based authentication with Redis caching
- JSON data import with field mapping validation
- Search result ranking and relevance algorithms
- Mobile-responsive Vue.js components with shadcn styling

## Communication Style

- Be direct and concise
- Welcome criticism and suggest better approaches
- Ask questions when intent is unclear
- Provide factual, skeptical analysis
- Avoid unnecessary explanations unless requested