# 1️⃣ High-Level Architecture Diagram (HLD)

### Purpose

Shows **system boundaries, responsibilities, and data flow**
→ This is your **system design interview diagram**

```mermaid
flowchart TB
    LLM[MCP Client / LLM / CLI]

    subgraph MCP_Server["Local MCP Server (Ktor)"]
        TR[Tool Router]
        QP[Query Planner]
        QE[Query Engine]
    end

    subgraph Intelligence["Code Intelligence Engine"]
        CI[Codebase Scanner]
        AST[AST Indexer]
        PSI[Kotlin PSI Resolver]
        GE[Graph Engine]
        KA[Kafka Analyzer]
        GM[Git Metadata Analyzer]
    end

    subgraph Storage["In-Memory Indexes"]
        SI[Symbol Index]
        RG[Reference Graph]
        CG[Call Graph]
        DG[Dependency Graph]
        TG[Topic Graph]
        GMETA[Git Metadata Store]
    end

    Repo[Local Git Repository]
    LLM -->|MCP Tool Calls| TR
    TR --> QP
    QP --> QE
    QE --> CI
    CI --> AST
    AST --> SI
    AST --> RG
    QE --> PSI
    PSI --> SI
    PSI --> RG
    QE --> GE
    GE --> CG
    GE --> DG
    QE --> KA
    KA --> TG
    QE --> GM
    GM --> GMETA
    CI --> Repo

```

### Key Interview Callouts

* **Tree-sitter first**, PSI only for precision
* **Graphs are first-class citizens**
* Entire system is **offline + deterministic**

---

# 2️⃣ Component / Class Diagram

### Purpose

Shows **internal structure & responsibilities**
→ This answers *“how would you implement it?”*

```mermaid
classDiagram
    direction LR

    class McpServer {
        +start()
        +registerTools()
    }

    class ToolRouter {
        +route(toolName, args)
    }

    class QueryPlanner {
        +plan(tool, args)
    }

    class QueryEngine {
        +execute(plan)
    }

    class CodebaseScanner {
        +scanRepo()
    }

    class AstIndexer {
        +indexFiles()
    }

    class PsiResolver {
        +resolveSymbol()
        +findUsages()
    }

    class GraphEngine {
        +buildCallGraph()
        +buildDependencyGraph()
    }

    class KafkaAnalyzer {
        +findProducers()
        +findConsumers()
    }

    class GitAnalyzer {
        +collectMetadata()
    }

    class SymbolIndex
    class ReferenceGraph
    class CallGraph
    class DependencyGraph
    class TopicGraph
    class GitMetadataStore

    McpServer --> ToolRouter
    ToolRouter --> QueryPlanner
    QueryPlanner --> QueryEngine
    QueryEngine --> CodebaseScanner
    QueryEngine --> AstIndexer
    QueryEngine --> PsiResolver
    QueryEngine --> GraphEngine
    QueryEngine --> KafkaAnalyzer
    QueryEngine --> GitAnalyzer
    AstIndexer --> SymbolIndex
    AstIndexer --> ReferenceGraph
    PsiResolver --> SymbolIndex
    PsiResolver --> ReferenceGraph
    GraphEngine --> CallGraph
    GraphEngine --> DependencyGraph
    KafkaAnalyzer --> TopicGraph
    GitAnalyzer --> GitMetadataStore
```

### Design Patterns Used

* **Facade** → `QueryEngine`
* **Command** → MCP tool execution
* **Builder** → Graph construction
* **Strategy** → Tree-sitter vs PSI resolution

---

# 3️⃣ Core Data Model (ER-Style)

### Purpose

Shows **what you store & why**
→ Great for *“how do you model this?”*

```mermaid
erDiagram
    FILE ||--o{ CLASS: contains
    CLASS ||--o{ METHOD: defines
    METHOD ||--o{ METHOD: calls
    CLASS ||--o{ CLASS: depends_on
    MODULE ||--o{ CLASS: owns
    METHOD }o--o{ SYMBOL: references
    CLASS }o--o{ SYMBOL: references
    KAFKA_TOPIC ||--o{ KAFKA_CONSUMER: consumed_by
    KAFKA_TOPIC ||--o{ KAFKA_PRODUCER: produced_by
    FILE ||--|| GIT_METADATA: has

    FILE {
        string path
        string module
    }

    CLASS {
        string name
        string package
    }

    METHOD {
        string name
        string signature
    }

    KAFKA_TOPIC {
        string name
    }

    GIT_METADATA {
        string lastAuthor
        int commitCount
    }
```

### Important Scope Note

⚠️ All relationships are **static**
No runtime / reflection resolution (intentionally out of scope)

---

# 4️⃣ Sequence Diagrams (Critical MCP Tools)

---

## 4.1 `/search_code`

```mermaid
sequenceDiagram
    participant LLM
    participant MCP as MCP Server
    participant QE as Query Engine
    participant AST as AST Index
    participant SI as Symbol Index
    LLM ->> MCP: /search_code(query)
    MCP ->> QE: executeSearch(query)
    QE ->> AST: scanText(query)
    AST ->> SI: lookupSymbols()
    SI -->> QE: matchingSymbols
    QE -->> MCP: results
    MCP -->> LLM: structured response
```

---

## 4.2 `/find_usages`

```mermaid
sequenceDiagram
    participant LLM
    participant MCP
    participant QE
    participant PSI
    participant RG as Reference Graph
    LLM ->> MCP: /find_usages(symbol)
    MCP ->> QE: planFindUsages(symbol)
    QE ->> PSI: resolveSymbol(symbol)
    PSI ->> RG: findInboundRefs()
    RG -->> QE: usageList
    QE -->> MCP: result
    MCP -->> LLM: usage graph
```

---

## 4.3 `/dependency_graph`

```mermaid
sequenceDiagram
    participant LLM
    participant MCP
    participant QE
    participant GE as Graph Engine
    participant DG as Dependency Graph
LLM ->> MCP: /dependency_graph(target)
MCP ->> QE: 
QE ->> GE: buildOrQueryGraph(target)
GE ->> DG: traverse()
DG -->> QE: dependencies
QE -->> MCP:
MCP -->> LLM:
```

---

## 4.4 `/impact_analysis` (Most Important)

```mermaid
sequenceDiagram
    participant LLM
    participant MCP
    participant QE
    participant PSI
    participant CG as Call Graph
    participant TG as Topic Graph
    participant GM as Git Metadata
    LLM ->> MCP: /impact_analysis(symbol)
MCP ->> QE: 

QE ->> PSI: resolveSymbol(symbol)
PSI ->> CG: findCallers()
PSI ->> TG: findKafkaImpact()
QE ->> GM: fetchRiskSignals()

CG -->> QE: callerSet
TG -->> QE: topicImpact
GM -->> QE: churnData

QE -->> MCP: impactSummary
MCP -->> LLM: structured impact report
```