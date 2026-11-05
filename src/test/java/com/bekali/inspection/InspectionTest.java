package com.bekali.inspection;

import com.bekali.inspection.model.*;
import com.bekali.inspection.visitor.*;
import com.bekali.inspection.demo.DemoProject;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InspectionTest {
    private SourceCodeFile source(int complexity) {
        return new SourceCodeFile("Service.java", "payments", 100, complexity, false, false);
    }
    private TestFile tests(int count) { return new TestFile("ServiceTest.java", "payments", 50, count, false); }
    private Directory tree(ProjectElement... children) { return new Directory("project", List.of(children)); }
    private DependencyFile deps(Dependency... dependencies) { return new DependencyFile("pom.xml", 20, List.of(dependencies)); }
    private QualityGateVisitor.Result gate(ProjectElement element) {
        var visitor = new QualityGateVisitor(); element.accept(visitor); return visitor.result();
    }
    private List<Finding> violations(ProjectElement element) {
        var visitor = new ComplianceVisitor(); element.accept(visitor); return visitor.violations();
    }

    @Test void eachElementSelectsItsOwnOverload() {
        List<String> calls = new ArrayList<>();
        ProjectVisitor visitor = new ProjectVisitor() {
            public void visit(SourceCodeFile file) { calls.add("source"); }
            public void visit(TestFile file) { calls.add("test"); }
            public void visit(ConfigurationFile file) { calls.add("config"); }
            public void visit(DependencyFile file) { calls.add("dependencies"); }
            public void visit(Directory directory) { calls.add("directory"); }
        };
        List<ProjectElement> elements = List.of(source(1), tests(1),
                new ConfigurationFile("app.properties", 1, false, false), deps(), tree());
        elements.forEach(e -> e.accept(visitor));
        assertEquals(List.of("source", "test", "config", "dependencies", "directory"), calls);
    }

    @Test void securityDetectsEveryRequiredRisk() {
        var visitor = new SecurityAuditVisitor();
        tree(new SourceCodeFile("Risk.java", "m", 10, 3, true, true),
             new ConfigurationFile("app.properties", 2, true, true),
             deps(new Dependency("bad", "1", true, "MIT"))).accept(visitor);
        assertEquals(4, visitor.criticalIssues());
        assertEquals(5, visitor.findings().size());
        assertEquals(Set.of("SEC-CREDENTIALS", "SEC-HIGH-RISK", "SEC-CONFIG", "SEC-DEPENDENCY"),
                new HashSet<>(visitor.findings().stream().map(Finding::rule).toList()));
        assertEquals(0, visitor.score());
    }

    @Test void credentialsInTestFilesAreAlsoCritical() {
        var visitor = new SecurityAuditVisitor();
        new TestFile("UnsafeTest.java", "m", 5, 1, true).accept(visitor);
        assertEquals(1, visitor.criticalIssues());
        assertEquals(75, visitor.score());
    }

    @Test void highRiskWarningDoesNotBecomeACriticalIssue() {
        var visitor = new SecurityAuditVisitor();
        new SourceCodeFile("Risk.java", "m", 10, 1, false, true).accept(visitor);
        assertEquals(0, visitor.criticalIssues());
        assertEquals(90, visitor.score());
        assertEquals(Finding.Severity.WARNING, visitor.findings().get(0).severity());
    }

    @Test void secureProjectHasPerfectScoreAndNoFindings() {
        var visitor = new SecurityAuditVisitor();
        tree(source(4), tests(2), new ConfigurationFile("app", 2, false, false),
                deps(new Dependency("safe", "1", false, "MIT"))).accept(visitor);
        assertTrue(visitor.findings().isEmpty());
        assertEquals(100, visitor.score());
    }

    @Test void statisticsCountAllFileTypesAndUseSourceComplexityOnly() {
        var visitor = new ProjectStatisticsVisitor();
        tree(source(6), source(9), tests(7), new ConfigurationFile("app", 5, false, false),
                deps(new Dependency("a", "1", false, "MIT"), new Dependency("b", "2", false, "MIT"))).accept(visitor);
        var s = visitor.statistics();
        assertEquals(5, s.totalFiles()); assertEquals(275, s.linesOfCode());
        assertEquals(2, s.sourceFiles()); assertEquals(1, s.testFiles());
        assertEquals(7, s.numberOfTests()); assertEquals(2, s.dependencies());
        assertEquals(7.5, s.averageComplexity()); assertEquals(0.5, s.testSourceRatio());
    }

    @Test void nestedDirectoriesAreTraversedExactlyOnce() {
        var visitor = new ProjectStatisticsVisitor();
        tree(tree(tree(source(8))), tree(tests(3))).accept(visitor);
        assertEquals(2, visitor.statistics().totalFiles());
        assertEquals(150, visitor.statistics().linesOfCode());
        assertEquals(3, visitor.statistics().numberOfTests());
    }

    @Test void deeplyNestedProjectCanBeInspected() {
        ProjectElement project = source(1);
        for (int i = 0; i < 100; i++) project = tree(project);
        var visitor = new ProjectStatisticsVisitor(); project.accept(visitor);
        assertEquals(1, visitor.statistics().totalFiles());
        assertEquals(100, visitor.statistics().linesOfCode());
    }

    @Test void emptyDirectoryHasFiniteZeroStatisticsAndCannotShip() {
        var visitor = new ProjectStatisticsVisitor(); tree().accept(visitor);
        var s = visitor.statistics();
        assertEquals(0, s.totalFiles()); assertEquals(0, s.linesOfCode());
        assertEquals(0.0, s.averageComplexity()); assertEquals(0.0, s.testSourceRatio());
        assertFalse(gate(tree()).passed());
    }

    @Test void testOnlyProjectDoesNotDivideByZeroOrPassReleaseGate() {
        var visitor = new ProjectStatisticsVisitor(); tree(tests(3)).accept(visitor);
        assertEquals(0.0, visitor.statistics().testSourceRatio());
        assertFalse(gate(tree(tests(3))).testsPass());
    }

    @Test void qualityPassesAtExactComplexityAndRatioThresholds() {
        var result = gate(tree(source(10), source(10), source(10), source(10), source(10), tests(1), tests(1)));
        assertTrue(result.securityPass()); assertTrue(result.complexityPass());
        assertTrue(result.testsPass()); assertTrue(result.passed());
        assertTrue(result.toString().contains("FINAL RESULT: PASSED"));
    }

    @Test void qualityFailsForHighComplexity() {
        var result = gate(tree(source(11), tests(1)));
        assertFalse(result.complexityPass()); assertTrue(result.securityPass());
        assertTrue(result.testsPass()); assertFalse(result.passed());
    }

    @Test void qualityFailsBelowTestRatioThreshold() {
        var result = gate(tree(source(1), source(1), source(1), tests(1)));
        assertFalse(result.testsPass()); assertTrue(result.complexityPass());
        assertFalse(result.passed());
    }

    @Test void qualityFailsWhenCriticalSecurityIssueExists() {
        var result = gate(tree(source(1), tests(1), new ConfigurationFile("app", 1, false, true)));
        assertFalse(result.securityPass()); assertFalse(result.passed());
        assertTrue(result.toString().contains("Security: FAIL"));
    }

    @Test void complianceDetectsUnpinnedAndProhibitedDependency() {
        var result = violations(deps(new Dependency("legacy-crypto", "  ", false, "MIT")));
        assertEquals(List.of("CMP-VERSION", "CMP-PROHIBITED"), result.stream().map(Finding::rule).toList());
    }

    @Test void complianceDetectsCredentialsInEveryApplicableElement() {
        var result = violations(tree(new SourceCodeFile("S", "m", 1, 1, true, false),
                new TestFile("T", "m", 1, 1, true), new ConfigurationFile("C", 1, true, false)));
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(f -> f.rule().equals("CMP-CREDENTIALS")));
    }

    @Test void complianceFindsUntestedModule() {
        var result = violations(tree(source(1), new TestFile("OtherTest", "other", 2, 1, false)));
        assertEquals(1, result.size());
        assertEquals("payments", result.get(0).location());
        assertEquals("CMP-TESTS", result.get(0).rule());
    }

    @Test void emptyTestFileDoesNotSatisfyModuleCompliance() {
        assertEquals("CMP-TESTS", violations(tree(source(1), tests(0))).get(0).rule());
    }

    @Test void complianceIsIndependentOfDirectoryOrderAndReportIsIdempotent() {
        var visitor = new ComplianceVisitor();
        tree(tree(tests(1)), tree(source(1))).accept(visitor);
        assertTrue(visitor.compliant()); assertEquals(visitor.violations(), visitor.violations());
        assertEquals(violations(tree(source(1), tests(1))), visitor.violations());
    }

    @Test void complianceAllowsCustomProhibitedDependencyPolicy() {
        var visitor = new ComplianceVisitor(Set.of("custom"));
        deps(new Dependency("custom", "1", false, "MIT")).accept(visitor);
        assertEquals("CMP-PROHIBITED", visitor.violations().get(0).rule());
    }

    @Test void licenseAuditFlagsMissingAndDisallowedLicensesInNestedDirectory() {
        var visitor = new LicenseAuditVisitor();
        tree(source(1), tests(1), new ConfigurationFile("app", 1, false, false),
                tree(deps(new Dependency("a", "1", false, null),
                          new Dependency("b", "2", false, "GPL-3.0")))).accept(visitor);
        assertFalse(visitor.passed());
        assertEquals(List.of("LIC-MISSING", "LIC-REVIEW"), visitor.findings().stream().map(Finding::rule).toList());
    }

    @Test void licenseAuditAcceptsAllowedLicensesAndCustomPolicy() {
        var visitor = new LicenseAuditVisitor();
        deps(new Dependency("a", "1", false, "MIT"), new Dependency("b", "1", false, "Apache-2.0")).accept(visitor);
        assertTrue(visitor.passed());
        var custom = new LicenseAuditVisitor(Set.of("Proprietary"));
        deps(new Dependency("internal", "1", false, "Proprietary")).accept(custom);
        assertTrue(custom.passed());
    }

    @Test void invalidMetricsAndNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> source(-1));
        assertThrows(IllegalArgumentException.class, () -> tests(-1));
        assertThrows(IllegalArgumentException.class, () -> new SourceCodeFile("S", "m", -1, 1, false, false));
        assertThrows(IllegalArgumentException.class, () -> new TestFile("T", "m", -1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFile("C", -1, false, false));
        assertThrows(IllegalArgumentException.class, () -> new DependencyFile("D", -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Directory(" ", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Dependency("", "1", false, "MIT"));
        assertThrows(IllegalArgumentException.class, () -> new SourceCodeFile("S", "", 1, 1, false, false));
    }

    @Test void modelListsAreDefensivelyCopiedAndRejectNullChildren() {
        var children = new ArrayList<ProjectElement>(); children.add(source(1));
        var directory = new Directory("src", children); children.clear();
        assertEquals(1, directory.children().size());
        assertThrows(UnsupportedOperationException.class, () -> directory.children().clear());
        var dependencies = new ArrayList<Dependency>(); dependencies.add(new Dependency("a", "1", false, "MIT"));
        var manifest = new DependencyFile("pom", 1, dependencies); dependencies.clear();
        assertEquals(1, manifest.dependencies().size());
        assertThrows(UnsupportedOperationException.class, () -> manifest.dependencies().clear());
        assertThrows(NullPointerException.class, () -> new Directory("src", Arrays.asList((ProjectElement) null)));
    }

    @Test void reportSnapshotsDoNotChangeWhenVisitorAccumulatesMoreElements() {
        var visitor = new SecurityAuditVisitor(); source(1).accept(visitor);
        var before = visitor.findings();
        new ConfigurationFile("bad", 1, true, false).accept(visitor);
        assertTrue(before.isEmpty()); assertEquals(1, visitor.findings().size());
        assertThrows(UnsupportedOperationException.class, () -> visitor.findings().clear());
        var compliance = new ComplianceVisitor(); source(1).accept(compliance);
        assertEquals(1, compliance.violations().size());
        assertEquals(1, compliance.violations().size());
    }

    @Test void emptyDirectoryIsSafeForEveryAudit() {
        var security = new SecurityAuditVisitor();
        var compliance = new ComplianceVisitor();
        var licenses = new LicenseAuditVisitor();
        var empty = tree();
        empty.accept(security); empty.accept(compliance); empty.accept(licenses);
        assertEquals(100, security.score()); assertTrue(security.findings().isEmpty());
        assertTrue(compliance.compliant()); assertTrue(licenses.passed());
    }

    @Test void realisticDemoProducesExpectedStatisticsAndFailures() {
        var project = DemoProject.create();
        var statistics = new ProjectStatisticsVisitor(); project.accept(statistics);
        assertEquals(new ProjectStatisticsVisitor.Statistics(5, 450, 2, 1, 8, 2, 10.0, 0.5), statistics.statistics());
        var security = new SecurityAuditVisitor(); project.accept(security);
        assertEquals(3, security.criticalIssues()); assertEquals(15, security.score());
        var quality = gate(project);
        assertFalse(quality.securityPass()); assertTrue(quality.testsPass()); assertTrue(quality.complexityPass());
        assertEquals(Set.of("CMP-CREDENTIALS", "CMP-VERSION", "CMP-PROHIBITED", "CMP-TESTS"),
                new HashSet<>(violations(project).stream().map(Finding::rule).toList()));
        var licenses = new LicenseAuditVisitor(); project.accept(licenses);
        assertEquals(1, licenses.findings().size());
    }
}
