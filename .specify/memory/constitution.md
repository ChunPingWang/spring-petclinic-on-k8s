<!--
  SYNC IMPACT REPORT
  ==================
  Version Change: 1.1.0 → 1.1.1

  Modified Principles: None

  Added Sections:
  - Git Commit Message Policy (under Development Workflow)

  Removed Sections: None

  Templates Requiring Updates:
  - .specify/templates/plan-template.md ✅ (no changes needed)
  - .specify/templates/spec-template.md ✅ (no changes needed)
  - .specify/templates/tasks-template.md ✅ (no changes needed)

  Follow-up TODOs: None
-->

# Spring PetClinic Microservices Constitution

## Brownfield Project Principle — 棕地專案原則

**This is a BROWNFIELD PROJECT. The following rules MUST be applied to all development activities.**

### 0. 棕地專案原則 (Brownfield Project Rules) — NON-NEGOTIABLE

This project operates on an existing codebase with established patterns, data, and users. All changes MUST respect and preserve existing functionality.

Core Rules:
- **Preserve Existing Behavior**: All existing functionality MUST continue to work unchanged unless explicitly modified
- **No Breaking Changes**: Changes MUST NOT break existing APIs, data structures, or user workflows without migration plan
- **Incremental Improvement**: Improvements MUST be made incrementally, not through wholesale rewrites
- **Test Before Modify**: Existing code MUST have characterization tests before any modification
- **Backward Compatibility**: New features MUST maintain backward compatibility with existing integrations
- **Strangler Fig Pattern**: Large refactoring SHOULD use the Strangler Fig pattern to gradually replace old code

Change Management Rules:
- All changes MUST be reversible or have a rollback plan
- Database migrations MUST be backward compatible (expand-contract pattern)
- API changes MUST version endpoints or maintain compatibility
- Configuration changes MUST not break existing deployments

Risk Mitigation:
- Feature flags SHOULD be used for significant changes
- Canary deployments SHOULD be used for production releases
- Monitoring MUST be in place before and after changes
- Changes MUST be small and frequent, not large and infrequent

**Rationale**: Brownfield projects carry the risk of breaking existing functionality that users depend on. By enforcing strict preservation of existing behavior and incremental change, we minimize risk while still enabling improvement. This principle takes precedence over all other principles when conflicts arise.

---

## Core Principles

### I. 程式碼品質 (Code Quality)

All code MUST adhere to clean code standards. Quality is non-negotiable.

- Code MUST be readable, maintainable, and self-documenting
- Functions MUST be small, focused, and do one thing well
- Names MUST be meaningful and reveal intent (classes, methods, variables)
- Code duplication MUST be eliminated through proper abstraction
- Comments SHOULD explain "why", not "what" — code itself explains "what"
- Magic numbers and strings MUST be replaced with named constants
- All code MUST pass static analysis and linting checks before merge
- Technical debt MUST be tracked and addressed systematically

**Rationale**: High-quality code reduces maintenance costs, decreases bug introduction rate, and enables sustainable development velocity in a microservices environment.

### II. 測試驅動開發 (Test-Driven Development) — NON-NEGOTIABLE

All features MUST follow the TDD Red-Green-Refactor cycle. No production code without failing tests first.

- **RED**: Write a failing test that defines desired behavior
- **GREEN**: Write minimal code to make the test pass
- **REFACTOR**: Clean up code while keeping tests green

Mandatory coverage requirements:
- Unit tests MUST cover business logic in domain layer (≥80% coverage)
- Integration tests MUST verify adapter implementations
- Contract tests MUST validate API boundaries between microservices
- Tests MUST be fast, isolated, and repeatable

**Brownfield Addition**: Before modifying existing code, characterization tests MUST be written to capture current behavior.

**Rationale**: TDD ensures correctness by design, creates living documentation, and enables confident refactoring. In a microservices architecture, comprehensive testing is critical for system reliability.

### III. 行為驅動開發 (Behavior-Driven Development)

Features MUST be specified using BDD acceptance criteria before implementation.

- User stories MUST follow Given-When-Then format
- Acceptance scenarios MUST be written in collaboration with stakeholders
- Scenarios MUST be executable as automated acceptance tests
- Feature files MUST use domain language, not technical jargon
- Each scenario MUST be independently testable

**Rationale**: BDD bridges communication between technical and non-technical stakeholders, ensures features meet actual user needs, and creates executable specifications.

### IV. 領域驅動設計 (Domain-Driven Design)

Business logic MUST be modeled using DDD tactical and strategic patterns.

Strategic patterns:
- Each microservice MUST represent a bounded context
- Context maps MUST document relationships between services
- Ubiquitous language MUST be used consistently within each bounded context

Tactical patterns:
- Entities MUST have identity and lifecycle
- Value Objects MUST be immutable and compared by attributes
- Aggregates MUST enforce consistency boundaries
- Domain Events MUST communicate state changes across boundaries
- Repositories MUST provide collection-like interfaces for aggregates
- Domain Services MUST contain logic that doesn't belong to entities

**Rationale**: DDD aligns software design with business domains, making the system easier to understand, modify, and extend as business requirements evolve.

### V. SOLID 原則 (SOLID Principles) — NON-NEGOTIABLE

All object-oriented code MUST comply with SOLID principles.

- **S**ingle Responsibility: Each class MUST have only one reason to change
- **O**pen/Closed: Classes MUST be open for extension, closed for modification
- **L**iskov Substitution: Subtypes MUST be substitutable for their base types
- **I**nterface Segregation: Clients MUST NOT depend on interfaces they don't use
- **D**ependency Inversion: High-level modules MUST NOT depend on low-level modules; both MUST depend on abstractions

**Rationale**: SOLID principles create flexible, maintainable, and testable code. They are essential for managing complexity in large-scale microservices systems.

### VI. 六角形架構 (Hexagonal Architecture)

All microservices MUST follow the Ports and Adapters (Hexagonal) architecture pattern.

Architecture layers (inside → outside):
- **Domain Layer (Core)**: Business logic, entities, value objects, domain services
- **Application Layer**: Use cases, application services, port interfaces
- **Infrastructure Layer (Adapters)**: Frameworks, databases, external services, UI

Rules:
- Domain layer MUST have zero dependencies on infrastructure
- Dependencies MUST point inward only (Infrastructure → Application → Domain)
- Ports (interfaces) MUST be defined in application layer
- Adapters MUST implement ports and live in infrastructure layer
- Domain logic MUST be framework-agnostic and testable in isolation

**Brownfield Note**: Existing code may not follow this structure. New code MUST follow this pattern; existing code SHOULD be migrated incrementally using the Strangler Fig pattern.

**Rationale**: Hexagonal architecture decouples business logic from technical concerns, enabling technology changes without affecting core domain, and making the system highly testable.

### VII. 依賴反轉與基礎設施層 (Dependency Inversion & Infrastructure Layer)

Frameworks and external dependencies MUST reside in the infrastructure layer only. Inner layers access outer layers through port interfaces.

Infrastructure layer includes:
- Spring Framework components (Controllers, Configuration, Spring Data repositories)
- Database implementations (JPA, JDBC, MySQL, HSQLDB)
- External service clients (REST clients, messaging)
- Web/API adapters (REST controllers, GraphQL resolvers)
- Persistence adapters (JPA/Hibernate implementations)

Port interface rules:
- Application layer MUST define port interfaces (inbound and outbound)
- Infrastructure adapters MUST implement these port interfaces
- Dependency injection MUST wire adapters to ports at runtime
- No `@Autowired` or Spring annotations in domain layer
- Domain layer MUST use plain Java/Kotlin objects only

**Rationale**: This separation ensures the core business logic remains independent of technology choices, enabling easier testing, technology upgrades, and long-term maintainability.

## Architecture Constraints

### Layer Dependency Rules

```
┌─────────────────────────────────────────────┐
│           Infrastructure Layer              │
│  (Spring, JPA, REST, Messaging, External)   │
├─────────────────────────────────────────────┤
│           Application Layer                 │
│  (Use Cases, Ports, Application Services)   │
├─────────────────────────────────────────────┤
│             Domain Layer                    │
│  (Entities, Value Objects, Domain Services) │
└─────────────────────────────────────────────┘
         ↑ Dependencies point inward only
```

### Package Structure Per Microservice

Each microservice MUST follow this package structure:

```
com.example.{service}/
├── domain/
│   ├── model/          # Entities, Value Objects, Aggregates
│   ├── service/        # Domain Services
│   └── event/          # Domain Events
├── application/
│   ├── port/
│   │   ├── in/         # Inbound ports (use case interfaces)
│   │   └── out/        # Outbound ports (repository, external service interfaces)
│   └── service/        # Application Services (use case implementations)
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── web/    # REST Controllers
    │   └── out/
    │       ├── persistence/  # JPA Repositories, Entity mappings
    │       └── client/       # External service clients
    └── config/         # Spring Configuration
```

**Brownfield Note**: Existing microservices may use different package structures. Migration to this structure SHOULD be done incrementally during feature development.

### Technology Stack Constraints

- Java 17+ for all microservices
- Spring Boot for application framework (infrastructure layer only)
- Spring Cloud for microservices infrastructure
- JUnit 5 + Mockito for unit testing
- Cucumber or JGiven for BDD acceptance tests
- ArchUnit for architecture validation

## Development Workflow

### Git Commit Message Policy — AI 輔助開發記錄規範

Commit messages MUST NOT include AI assistant attribution or generated signatures.

**Prohibited content in commit messages**:
- `🤖 Generated with [Claude Code]` or similar AI tool signatures
- `Co-Authored-By: Claude <noreply@anthropic.com>` or similar AI co-author lines
- Any reference to AI assistants (Claude, ChatGPT, Copilot, etc.) in commit messages
- Auto-generated footers or watermarks from AI tools

**Required commit message format**:
- Follow conventional commits format: `type(scope): description`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- Keep subject line under 72 characters
- Use imperative mood ("Add feature" not "Added feature")
- Include ticket/issue reference when applicable

**Examples**:
```
# Good
feat(customers): add owner creation endpoint validation

# Bad - includes AI attribution
feat(customers): add owner creation endpoint validation

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Rationale**: Git history should reflect human ownership and accountability for code changes. AI tools are development aids, but the human developer is responsible for reviewing, understanding, and committing code. Including AI attribution clutters the git history and obscures human accountability.

### Code Review Requirements

All pull requests MUST:
- Pass all automated tests (unit, integration, contract)
- Pass architecture validation (ArchUnit rules)
- Pass static code analysis (linting, code quality)
- Include tests for new functionality (TDD evidence)
- Have at least one approval from team member
- Include BDD scenarios for user-facing features
- **[Brownfield]** Verify no regression in existing functionality
- **[AI Policy]** NOT include AI assistant attribution in commit messages

### Quality Gates

Before merge, code MUST pass:
1. **Compilation**: Clean build with no warnings
2. **Unit Tests**: All pass, coverage ≥80% for domain layer
3. **Integration Tests**: All adapter tests pass
4. **Contract Tests**: All API contracts validated
5. **Architecture Tests**: ArchUnit rules pass (layer dependencies)
6. **Static Analysis**: No critical/blocker issues
7. **[Brownfield] Regression Tests**: All existing tests pass
8. **[AI Policy] Commit Message Check**: No AI attribution in commit messages

### Continuous Integration Pipeline

```
Build → Unit Tests → Integration Tests → Contract Tests → Architecture Validation → Commit Message Check → Deploy
```

## Governance

### Amendment Process

1. Propose amendment via pull request to this file
2. Document rationale for change
3. Review by team leads
4. Update affected templates and documentation
5. Communicate changes to all team members
6. Migration plan for existing code if breaking changes

### Compliance Verification

- Architecture tests (ArchUnit) MUST run in CI pipeline
- Code reviews MUST verify principle compliance
- Quarterly architecture audits SHOULD be conducted
- Violations MUST be documented and tracked for remediation
- Commit message format SHOULD be validated by git hooks or CI

### Versioning Policy

- **MAJOR**: Breaking changes to principles or architecture rules
- **MINOR**: New principles added or existing ones materially expanded
- **PATCH**: Clarifications, wording improvements, non-semantic refinements

### Reference Documentation

- Domain-Driven Design: Eric Evans, "Domain-Driven Design"
- Hexagonal Architecture: Alistair Cockburn
- Clean Architecture: Robert C. Martin
- SOLID Principles: Robert C. Martin
- TDD: Kent Beck, "Test-Driven Development By Example"
- BDD: Dan North, "Introducing BDD"
- Brownfield Development: Michael Feathers, "Working Effectively with Legacy Code"
- Strangler Fig Pattern: Martin Fowler
- Conventional Commits: https://www.conventionalcommits.org/

**Version**: 1.1.1 | **Ratified**: 2025-12-10 | **Last Amended**: 2025-12-10
