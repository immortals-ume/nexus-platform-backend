# Development & Deployment Workflow

This document describes the **standard Git workflow** and 
the **CI/CD pipeline with environments and feedback loops**
used across teams and services.

The goal is to ensure:

* Predictable releases
* Safe deployments
* Fast feedback from **Dev, QA, Staging, and Production**

---

## 🔁 1. Git Workflow (Source Code Lifecycle)

This diagram explains **how code flows through branches**, from feature development to production and hotfixes.

### Branching Model

* `main` → production-ready code
* `develop` → integration branch
* `feature/*` → short-lived feature work
* `release/*` → stabilization before release
* `hotfix/*` → emergency production fixes

### 📊 GitGraph

```mermaid
gitGraph
    commit
    commit
    branch develop
    checkout develop
    commit
    commit
    branch feature/task
    checkout feature/task
    commit
    commit
    checkout develop
    merge feature/task
    branch release/x.y
    checkout release/x.y
    commit
    commit
    checkout main
    merge release/x.y
    commit tag: "vX.Y.0"
    checkout develop
    merge release/x.y
    branch hotfix/issue
    checkout hotfix/issue
    commit
    checkout main
    merge hotfix/issue
    commit tag: "vX.Y.1"
    checkout develop
    merge hotfix/issue
```

### Key Principles

* Features always merge into `develop`
* Releases are cut from `develop`
* Production is updated only from `release` or `hotfix`
* Hotfixes are merged back to `develop` to avoid drift

---

## 2. CI/CD Pipeline with Environments & Feedback

This diagram shows **how the same artifact** flows through environments with **validation and feedback at every stage**.

### Environments Covered

* Dev
* QA
* Staging
* Production

Each environment provides **feedback**, not just production.

### 📊 CI/CD + Environments (Top → Bottom)

```mermaid
flowchart TB

    %% SOURCE
    Dev[Developer]
    Repo[Source Control]

    %% CI
    CI[CI Pipeline]
    Build[Build]
    UnitTest[Unit Tests]
    StaticCheck[Static Analysis]
    SecurityScan[Security Scan]
    Artifact[Artifact Repository]

    %% CONFIG
    Config[Configuration Store]
    Secrets[Secrets Store]

    %% CD
    CD[CD Pipeline]

    %% ENVIRONMENTS
    DevEnv[Dev Environment]
    DevTest[Dev Validation]

    QAEnv[QA Environment]
    QATest[QA Validation]

    StageEnv[Staging Environment]
    StageTest[Staging Validation]

    Approval[Approval Gate]

    ProdEnv[Production Environment]

    %% FEEDBACK
    Monitor[Monitoring]
    Alert[Alerts]
    Feedback[Feedback Loop]
    Rollback[Rollback Process]

    %% FLOW
    Dev --> Repo
    Repo --> CI

    CI --> Build
    Build --> UnitTest
    UnitTest --> StaticCheck
    StaticCheck --> SecurityScan
    SecurityScan --> Artifact

    Artifact --> CD
    Config --> CD
    Secrets --> CD

    CD --> DevEnv
    DevEnv --> DevTest

    DevTest --> QAEnv
    QAEnv --> QATest

    QATest --> StageEnv
    StageEnv --> StageTest

    StageTest --> Approval
    Approval --> ProdEnv
    Approval --> Rollback

    %% FEEDBACK LOOPS
    DevTest --> Feedback
    QATest --> Feedback
    StageTest --> Feedback

    ProdEnv --> Monitor
    Monitor --> Alert
    Alert --> Feedback

    Feedback --> Dev
```

---

## Environment → Feedback Responsibility

| Environment | Feedback Type                         |
|-------------|---------------------------------------|
| Dev         | Logic, early integration              |
| QA          | Functional and regression             |
| Staging     | Performance, config, prod-like issues |
| Production  | Reliability, latency, incidents       |

---

## ✅ Core DevOps Guarantees

* Same artifact promoted across all environments
* No rebuilding between environments
* Fail fast in Dev and QA
* Approval before Production
* Rollback always available
* Feedback loops at **every stage**

