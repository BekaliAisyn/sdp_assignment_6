# UML — Smart Code Inspection & Compliance Engine

GitHub renders the Mermaid diagrams below. Names and relationships match the Java
implementation. Records are shown as classes.

## Class diagram

```mermaid
classDiagram
    class ProjectElement {
        <<interface>>
        +accept(ProjectVisitor visitor) void
    }
    class SourceCodeFile {
        <<record>>
        +String name
        +String module
        +int linesOfCode
        +int complexity
        +boolean hardcodedCredentials
        +boolean highRisk
        +accept(ProjectVisitor visitor) void
    }
    class TestFile {
        <<record>>
        +String name
        +String module
        +int linesOfCode
        +int testCount
        +boolean hardcodedCredentials
        +accept(ProjectVisitor visitor) void
    }
    class ConfigurationFile {
        <<record>>
        +String name
        +int linesOfCode
        +boolean exposedCredentials
        +boolean insecureConfiguration
        +accept(ProjectVisitor visitor) void
    }
    class DependencyFile {
        <<record>>
        +String name
        +int linesOfCode
        +List~Dependency~ dependencies
        +accept(ProjectVisitor visitor) void
    }
    class Dependency {
        <<record>>
        +String name
        +String version
        +boolean vulnerable
        +String license
    }
    class Directory {
        <<record>>
        +String name
        +List~ProjectElement~ children
        +accept(ProjectVisitor visitor) void
    }
    class ProjectVisitor {
        <<interface>>
        +visit(SourceCodeFile file) void
        +visit(TestFile file) void
        +visit(ConfigurationFile file) void
        +visit(DependencyFile file) void
        +visit(Directory directory) void
    }
    class RecursiveProjectVisitor {
        <<abstract>>
        +visit(Directory directory) void
    }
    class SecurityAuditVisitor {
        +findings() List~Finding~
        +criticalIssues() long
        +score() int
    }
    class ProjectStatisticsVisitor {
        +statistics() Statistics
    }
    class QualityGateVisitor {
        +result() Result
    }
    class ComplianceVisitor {
        +violations() List~Finding~
        +compliant() boolean
    }
    class LicenseAuditVisitor {
        +findings() List~Finding~
        +passed() boolean
    }
    class Finding {
        <<record>>
        +Severity severity
        +String location
        +String rule
        +String message
    }
    class Statistics {
        <<record>>
        +long totalFiles
        +long linesOfCode
        +long sourceFiles
        +long testFiles
        +long numberOfTests
        +long dependencies
        +double averageComplexity
        +double testSourceRatio
    }
    class Result {
        <<record>>
        +boolean securityPass
        +boolean complexityPass
        +boolean testsPass
        +passed() boolean
    }
    ProjectElement <|.. SourceCodeFile
    ProjectElement <|.. TestFile
    ProjectElement <|.. ConfigurationFile
    ProjectElement <|.. DependencyFile
    ProjectElement <|.. Directory
    Directory o-- "0..*" ProjectElement : children
    DependencyFile o-- "0..*" Dependency : dependencies
    ProjectElement ..> ProjectVisitor : accept(visitor)
    ProjectVisitor ..> SourceCodeFile : visit(file)
    ProjectVisitor ..> TestFile : visit(file)
    ProjectVisitor ..> ConfigurationFile : visit(file)
    ProjectVisitor ..> DependencyFile : visit(file)
    ProjectVisitor ..> Directory : visit(directory)
    ProjectVisitor <|.. RecursiveProjectVisitor
    RecursiveProjectVisitor <|-- SecurityAuditVisitor
    RecursiveProjectVisitor <|-- ProjectStatisticsVisitor
    RecursiveProjectVisitor <|-- QualityGateVisitor
    RecursiveProjectVisitor <|-- ComplianceVisitor
    RecursiveProjectVisitor <|-- LicenseAuditVisitor
    QualityGateVisitor *-- SecurityAuditVisitor : delegates security
    QualityGateVisitor *-- ProjectStatisticsVisitor : delegates statistics
    SecurityAuditVisitor ..> Finding
    ComplianceVisitor ..> Finding
    LicenseAuditVisitor ..> Finding
    ProjectStatisticsVisitor ..> Statistics : creates snapshot
    QualityGateVisitor ..> Result : creates snapshot
```

`Statistics` is nested in `ProjectStatisticsVisitor`, `Result` in
`QualityGateVisitor`, and the `Severity` enum in `Finding`. Each concrete visitor
implements the four file overloads and inherits `visit(Directory)` from
`RecursiveProjectVisitor`. `Dependency` is supporting data, not a sixth element.

Directory children use aggregation because the model does not enforce exclusive
ownership. The same object can appear more than once and is processed per entry.

## Sequence diagram

Example: `SecurityAuditVisitor` processes the root, the `src` directory, and `PaymentService.java`.

```mermaid
sequenceDiagram
    participant Main
    participant Root as root:Directory
    participant Audit as audit:SecurityAuditVisitor
    participant Src as src:Directory
    participant Payment as payment:SourceCodeFile
    Main->>Root: accept(audit)
    Root->>Audit: visit(this: Directory)
    Note over Audit: inherited RecursiveProjectVisitor.visit(Directory)
    Audit->>Src: child.accept(this)
    Src->>Audit: visit(this: Directory)
    Audit->>Payment: child.accept(this)
    Note over Payment: First dispatch selects SourceCodeFile.accept
    Payment->>Audit: visit(this: SourceCodeFile)
    Note over Payment,Audit: Overload selected using this:SourceCodeFile; second dispatch selects SecurityAuditVisitor.visit
    Audit->>Audit: credentials(name, true)
    Audit->>Audit: record CRITICAL and high-risk WARNING
    Audit-->>Payment: return
    Payment-->>Audit: return
    Audit-->>Src: return
    Src-->>Audit: return
    Note over Audit: Continue remaining children using the same visitor
    Audit-->>Root: return
    Root-->>Main: return
    Main->>Audit: findings(), criticalIssues(), score()
    Audit-->>Main: immutable snapshot and calculated results
```

Other visitors use the same `accept/visit` protocol. `QualityGateVisitor`
delegates file processing to its internal visitors without traversing directories
a second time.
