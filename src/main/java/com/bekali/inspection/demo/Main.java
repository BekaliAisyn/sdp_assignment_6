package com.bekali.inspection.demo;

import com.bekali.inspection.visitor.*;
import java.util.Locale;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        var project = DemoProject.create();
        var security = new SecurityAuditVisitor();
        var statistics = new ProjectStatisticsVisitor();
        var quality = new QualityGateVisitor();
        var compliance = new ComplianceVisitor();
        var licenses = new LicenseAuditVisitor();
        project.accept(security);
        project.accept(statistics);
        project.accept(quality);
        project.accept(compliance);
        project.accept(licenses);
        System.out.println("Smart Code Inspection & Compliance Engine: fintech-project");
        System.out.println("\n=== SECURITY AUDIT ===");
        System.out.println("Score: " + security.score() + "/100; critical issues: " + security.criticalIssues());
        security.findings().forEach(System.out::println);
        var s = statistics.statistics();
        System.out.println("\n=== PROJECT STATISTICS ===");
        System.out.printf(Locale.ROOT,
                "Files: %d; source files: %d; test files: %d; LOC: %d; tests: %d; dependencies: %d%nAverage complexity: %.2f; test/source ratio: %.2f%n",
                s.totalFiles(), s.sourceFiles(), s.testFiles(), s.linesOfCode(), s.numberOfTests(),
                s.dependencies(), s.averageComplexity(), s.testSourceRatio());
        System.out.println("\n=== QUALITY GATE ===\n" + quality.result());
        System.out.println("\n=== COMPLIANCE ===");
        System.out.println(compliance.compliant() ? "COMPLIANT" : "NON-COMPLIANT");
        compliance.violations().forEach(System.out::println);
        System.out.println("\n=== LICENSE AUDIT ===");
        System.out.println(licenses.passed() ? "PASS" : "REVIEW REQUIRED");
        licenses.findings().forEach(System.out::println);
    }
}
