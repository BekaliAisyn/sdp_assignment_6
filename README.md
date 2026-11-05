# Smart Code Inspection & Compliance Engine

Assignment 6 — Visitor Pattern. Java 17+, Maven, JUnit 5.

The engine runs security, statistics, quality, compliance, and license checks on
one project tree. Project elements hold data; visitors implement the operations.

## Run

Install JDK 17 or newer and Maven 3.9 or newer, then run:

```bash
mvn clean test
java -cp target/classes com.bekali.inspection.demo.Main
```

No database, server, API keys, or application environment variables are required.
The application has no external runtime dependencies. JUnit 5 is used for tests.
Test reports are written to `target/surefire-reports/`.

## Demo project

`DemoProject.create()` builds this in-memory project structure:

```text
fintech-project/
├── src/
│   ├── PaymentService.java
│   └── UserService.java
├── tests/
│   └── PaymentServiceTest.java
├── config/
│   └── application.properties
└── pom.xml
```

All five visitors process the same project object. The fixture deliberately
contains exposed credentials, insecure configuration, a vulnerable prohibited
dependency without a version, an untested production module, and a license that
requires review. Credentials are represented by flags; no real secrets are used.

| Visitor | Expected result |
|---|---|
| SecurityAuditVisitor | 3 critical issues, 1 warning, score 15/100 |
| ProjectStatisticsVisitor | 5 files, 450 LOC, 2 source files, 1 test file, 8 tests, 2 dependencies |
| ProjectStatisticsVisitor | Average complexity 10.00; test/source ratio 0.50 |
| QualityGateVisitor | Security: FAIL; Complexity: PASS; Tests: PASS; FINAL RESULT: FAILED |
| ComplianceVisitor | 4 violations: credentials, missing version, prohibited dependency, missing module tests |
| LicenseAuditVisitor | 1 license warning for GPL-3.0 |

The failed quality gate is the expected analysis result for this fixture. Tests
also cover a safe project that passes the gate.

The engine analyzes supplied metadata. It does not parse source files or query a
live vulnerability database.

## Structure

```text
src/main/java/com/bekali/inspection/
├── model/       ProjectElement, five elements, Dependency, Checks
├── visitor/     ProjectVisitor, RecursiveProjectVisitor, five visitors, Finding
└── demo/        DemoProject, Main
src/test/java/com/bekali/inspection/InspectionTest.java
docs/uml.md
```

The five elements are `SourceCodeFile`, `TestFile`, `ConfigurationFile`,
`DependencyFile`, and `Directory`. Each implements `ProjectElement` and calls
`visitor.visit(this)` inside `accept`.

`ProjectVisitor` declares one `visit` overload per element type.
`RecursiveProjectVisitor.visit(Directory)` visits each child through
`child.accept(this)`. Concrete visitors inherit directory traversal and implement
the four file overloads. There are no type checks using `instanceof` or `switch`.

`Directory` holds other `ProjectElement` objects, including nested directories.
Elements are immutable records, with defensive copies of their lists. Ordinary
construction cannot create a cycle. Repeated references are processed once per
entry rather than deduplicated.

`QualityGateVisitor` delegates file processing to its own security and statistics
visitors. It traverses directories once. Reports return immutable snapshots.
Create a new visitor for each independent inspection: reusing one accumulates
results. Visitors are not thread-safe.

See [UML diagrams](docs/uml.md) for the class and sequence diagrams.

## Rules

| Metric or check | Definition |
|---|---|
| Total files | All source, test, configuration, and dependency files; directories excluded |
| Lines of code | LOC summed across all four file types |
| Number of tests | Sum of `TestFile.testCount`, separate from the test-file count |
| Dependencies | Number of dependency declarations, without deduplication |
| Average complexity | Sum of source-file complexities divided by source-file count |
| Test/source ratio | Test-file count divided by source-file count; not code coverage |
| No source files | Average complexity and ratio are 0.0; the tests gate fails |
| Quality gate | No critical issues, average complexity ≤ 10, ratio ≥ 0.40, and at least one source file |
| Security score | max(0, 100 − 25 × critical issues − 10 × warnings) |

Exposed credentials in source, test, or configuration files, vulnerable dependencies,
and insecure configurations are critical security issues. A high-risk source flag
produces a warning.

Compliance requires credentials to remain unexposed, dependency versions to be
specified, prohibited dependencies to be absent, and each production module to
have at least one test. A test file with `testCount == 0` does not satisfy the
module rule. Files are matched by `module`. Module violations are calculated when
the report is read, so traversal order does not affect the result or duplicate
findings. Prohibited dependency names can be supplied through the constructor.

`LicenseAuditVisitor` allows `Apache-2.0`, `MIT`, and `BSD-3-Clause` by default.
Missing licenses and licenses outside that list require review. The constructor
accepts a custom allowlist.

Negative metrics and blank required names are rejected. Null or blank dependency
versions and licenses are treated as missing so visitors can report them.

## Visitor pattern questions

### What problem does Visitor solve here?

The same project tree needs several independent operations. Visitor keeps their
rules outside the element classes, so adding an operation does not require adding
methods such as `checkSecurity()` to every element.

### Where are Element and Visitor?

`ProjectElement` and its five implementations are in `model`. `ProjectVisitor`
and its implementations are in `visitor`.

### What is double dispatch?

The first dispatch selects `accept` using the element's runtime type. Inside
`SourceCodeFile.accept`, the static type of `this` is `SourceCodeFile`, so the
compiler selects `visit(SourceCodeFile)`. The second dispatch selects the
implementation of that overload using the visitor's runtime type, such as
`SecurityAuditVisitor`. The operation therefore depends on both types.

### Why not put all operations directly inside Element classes?

That would mix project data with security, statistics, and business policies.
Every new operation would require changes across all element classes. Separate
visitors allow these policies to evolve independently.

### Is it easier to add a new Visitor or a new Element?

A new visitor implements the existing overloads and can immediately process the
existing tree. `LicenseAuditVisitor` was added in its own commit without changing
`ProjectElement`, concrete elements, or `ProjectVisitor`. Adding an element requires
changing the visitor interface and all concrete visitors.

### What changes if DatabaseMigrationFile is added?

1. Add `DatabaseMigrationFile implements ProjectElement`, its data, and an
   `accept` method that calls `visitor.visit(this)`.
2. Add `void visit(DatabaseMigrationFile file)` to `ProjectVisitor`.
3. Implement that method in all five concrete visitors. Otherwise they fail to
   compile: the abstract `RecursiveProjectVisitor` does not provide this overload.
   The quality visitor must also delegate the new file type to its internal visitors.
4. Update test implementations of `ProjectVisitor`, add behavior tests, and update
   the demo and UML diagrams.

Existing element classes and `ProjectElement` do not need changes. `Directory`
already accepts any `ProjectElement`. Visitor works best when operations change
frequently but the element hierarchy remains stable.

### When would Visitor be a bad choice?

It is a poor fit when element types change frequently, there are few operations,
or visitors would need access to too many private implementation details.
Ordinary polymorphism may then be simpler.

### How is Visitor different from Strategy?

Strategy selects an interchangeable algorithm for a context through a common
contract. Visitor defines an operation across several element types using
overloads and double dispatch. Strategy alone does not provide that interaction
between two runtime types or traversal of a heterogeneous tree.

### Which SOLID principles apply?

- **SRP:** element data, traversal, and individual inspection policies are separate.
- **OCP:** new operations are added as visitors. Adding element types exposes the
  limitation of this design because existing visitors must change.
- **DIP:** traversal and callers work through `ProjectElement` and `ProjectVisitor`.
- **LSP:** every element can be placed in a directory and accept the same visitor.
- **ISP:** there is a tradeoff: visitors know every element type, so license audit
  has empty overloads for elements without dependencies.

## Tests

The 27 JUnit 5 tests cover:

- Dispatch to the correct overload for all five element types.
- Every security rule, severity, score deductions, and score clamping.
- Exact statistics, fractional averages, and file ratios.
- Quality gate PASS and FAIL, including exact thresholds and each failing criterion.
- Credentials, unpinned and prohibited dependencies, and untested modules.
- Compliance traversal order and repeated report reads.
- Missing, allowed, and disallowed licenses, including a custom policy.
- Nested directories, 100 levels of nesting, and empty directories.
- No source files, empty tests, invalid metrics, defensive copies, and immutable reports.
- Exact results for the fintech demo.

## Live modification

Use **Predict → Explain → Modify → Run → Test**.

For a new visitor, extend `RecursiveProjectVisitor`, implement the four file
methods, add its result, invoke `accept`, and test it. Existing elements and the
visitor interface remain unchanged.

For a new security rule, change the relevant security overload and test both a
violating and a safe case. Explain its effect on the score and quality gate.

Removing `accept` breaks the `ProjectElement` contract and directory traversal.
Overloaded `visit` methods alone cannot dispatch from a variable of type
`ProjectElement` to a concrete element overload.

Traversal uses recursion. The tests cover 100 levels; extreme depths may exhaust
the Java stack and would require iterative traversal.

## Git history

```bash
git log --oneline --reverse
```

The history includes separate implementation stages and a dedicated commit for
`LicenseAuditVisitor`. Cloning the repository preserves the history; downloading
a GitHub ZIP includes the current files only.
