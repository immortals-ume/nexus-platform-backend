# Product Requirements Document (PRD)

## Local Codebase Intelligence MCP

---

## 1. Problem Statement

Modern codebases are large, multi-module, and distributed across services. Developers frequently need to answer
questions such as:

* Where is a specific behavior implemented?
* Who depends on this service or method?
* What will break if I refactor this code?
* Which Kafka consumers/producers are connected?
* Is this code still used at all?

Today, developers rely on:

* `grep` / ripgrep
* IDE navigation
* Tribal knowledge
* Manual architecture diagrams (often outdated)

These approaches are:

* Slow
* Error-prone
* Non-holistic
* Not machine-queryable

**There is no local, deterministic, LLM-friendly system that deeply understands a codebase and exposes that
understanding via structured tools.**

---

## 2. Product Vision

> **A local MCP server that builds semantic intelligence over a codebase and answers high-level engineering questions
instantly, safely, and deterministically.**

The system acts as a **source-of-truth code intelligence layer** that can be queried by:

* LLMs (via MCP)
* CLI tools
* IDE integrations (future)

---

## 3. Goals & Non-Goals

### 🎯 Goals

* Provide **accurate, explainable answers** about code structure and dependencies
* Enable **safe refactoring** via impact analysis
* Reduce onboarding time for new developers
* Replace repetitive IDE hopping and manual searching
* Work **fully offline** on a local machine

### 🚫 Non-Goals (Scope Lock)

❌ Not a code generator
❌ Not an LLM training system
❌ Not a cloud service
❌ Not a real-time runtime tracer
❌ Not an IDE plugin (for v1)
❌ Not multi-language (Kotlin only for v1)
❌ Not a security scanner or linter

---

## 4. Target Users

### Primary

* Backend engineers (Kotlin / JVM)
* Senior engineers doing refactors
* Tech leads onboarding into large codebases

### Secondary

* Architects documenting systems
* SREs understanding service dependencies

---

## 5. Core User Stories

### US-1: Code Discovery

> As a developer, I want to find where a feature or behavior is implemented without manually searching files.

### US-2: Dependency Understanding

> As a developer, I want to know which services, modules, or components depend on a given service or method.

### US-3: Refactor Safety

> As a developer, I want to understand the blast radius of a change before making it.

### US-4: Event-Driven Visibility

> As a developer, I want to see all Kafka producers and consumers for a given topic.

### US-5: Code Cleanup

> As a developer, I want to identify unused classes, APIs, or consumers.

---

## 6. Functional Requirements

### FR-1: Codebase Scanning

* Recursively scan a given repository
* Support multi-module Gradle projects
* Index:

    * Files
    * Packages
    * Classes
    * Methods
    * Imports

---

### FR-2: AST & Semantic Indexing

* Parse Kotlin source using:

    * Tree-sitter (fast structural parsing)
    * Kotlin PSI (semantic resolution when needed)
* Build:

    * Symbol table
    * Reference map
    * Inheritance map

---

### FR-3: MCP Tool Interface

The server **must expose the following MCP tools**:

#### `/search_code`

* Text + symbol search
* Scoped by module/package

#### `/find_usages`

* Find all references to a symbol
* Includes cross-module references

#### `/explain_module`

* High-level explanation of a module:

    * Responsibilities
    * Key classes
    * External dependencies

#### `/dependency_graph`

* Return dependency relationships:

    * Module → module
    * Service → service
    * Class → class

#### `/impact_analysis`

* Analyze impact of changing:

    * Method
    * Class
    * API
* Include:

    * Callers
    * Tests
    * Kafka consumers/producers
    * API endpoints

---

### FR-4: Kafka Intelligence (Static)

* Detect Kafka:

    * Producers
    * Consumers
    * Topics
* Build topic → producer/consumer mapping
* No runtime tracing (static only)

---

### FR-5: Git Metadata Integration

* Read:

    * Last modified time
    * Commit frequency (churn)
    * Author ownership
* Use for:

    * Confidence scoring
    * Risk indication in impact analysis

---

### FR-6: Dead Code Detection (Heuristic)

* Identify:

    * Classes with zero inbound references
    * Public APIs never called internally
    * Kafka consumers with no producers
* Clearly label results as **heuristic**, not guaranteed

---

## 7. Non-Functional Requirements

### NFR-1: Performance

* Initial indexing ≤ 60 seconds for ~500k LOC
* Incremental re-indexing on file change

### NFR-2: Accuracy

* Prefer correctness over completeness
* PSI resolution only when required

### NFR-3: Determinism

* Same query → same result
* No probabilistic outputs

### NFR-4: Local-Only

* No external network calls
* No cloud dependencies

### NFR-5: Extensibility

* New tools can be added without refactoring core engine

---

## 8. Architecture Overview (Textual)

### Major Components

1. **Codebase Scanner**

    * File discovery
    * Language filtering

2. **Indexing Engine**

    * AST index
    * Symbol resolution
    * Reference graph

3. **Graph Engine**

    * Call graph
    * Dependency graph
    * Topic graph

4. **Query Engine**

    * Tool-specific planners
    * Result aggregation

5. **MCP Server**

    * Tool registration
    * Request/response handling

---

## 9. Data Models (High Level)

### Core Entities

* `FileNode`
* `ClassNode`
* `MethodNode`
* `SymbolRef`
* `ModuleNode`
* `KafkaTopicNode`
* `GitMetadata`

Graphs are stored as:

* Directed Acyclic Graphs (where applicable)
* Adjacency lists

---

## 10. Out-of-Scope (Explicit Lock)

The following are **intentionally excluded from v1**:

❌ Runtime profiling
❌ Bytecode analysis
❌ Reflection resolution
❌ Spring context graph resolution
❌ SQL / schema analysis
❌ Frontend (JS/TS) analysis
❌ Security vulnerability detection
❌ Auto-generated documentation

---

## 11. Success Metrics

* Can answer **all listed example questions correctly**
* Indexing completes within defined performance bounds
* MCP tools usable by an LLM without hallucination
* Clear, explainable outputs suitable for human review

---

## 12. Risks & Mitigations

| Risk                         | Mitigation                             |
|------------------------------|----------------------------------------|
| PSI is slow                  | Use Tree-sitter first, PSI selectively |
| False positives in dead code | Label as heuristic                     |
| Large repo memory usage      | Streamed indexing + pruning            |
| Kotlin complexity            | Limit to idiomatic Kotlin (v1)         |

---

## 13. Milestone Breakdown

### Milestone 1

* Scanner + AST index
* `/search_code`

### Milestone 2

* Symbol resolution
* `/find_usages`, `/explain_module`

### Milestone 3

* Graph engine
* `/dependency_graph`

### Milestone 4

* Impact analysis + Kafka graph
* `/impact_analysis`

---

## 14. Final Scope Lock Summary

✅ Kotlin only
✅ Static analysis only
✅ MCP tool interface only
✅ Local execution only
❌ No IDE plugin
❌ No runtime data
❌ No AI inference inside server
