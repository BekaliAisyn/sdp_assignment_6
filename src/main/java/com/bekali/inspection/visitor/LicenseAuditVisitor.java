package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;
import java.util.*;
import static com.bekali.inspection.visitor.Finding.Severity.WARNING;

public final class LicenseAuditVisitor extends RecursiveProjectVisitor {
    private final Set<String> allowed;
    private final List<Finding> findings = new ArrayList<>();
    public LicenseAuditVisitor() { this(Set.of("Apache-2.0", "MIT", "BSD-3-Clause")); }
    public LicenseAuditVisitor(Set<String> allowed) { this.allowed = Set.copyOf(allowed); }
    @Override public void visit(SourceCodeFile file) { }
    @Override public void visit(TestFile file) { }
    @Override public void visit(ConfigurationFile file) { }
    @Override public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            if (dependency.license().isBlank()) {
                findings.add(new Finding(WARNING, file.name(), "LIC-MISSING", "Missing license: " + dependency.name()));
            } else if (!allowed.contains(dependency.license())) {
                findings.add(new Finding(WARNING, file.name(), "LIC-REVIEW", "License requires review: " + dependency.name() + " (" + dependency.license() + ")"));
            }
        }
    }
    public List<Finding> findings() { return List.copyOf(findings); }
    public boolean passed() { return findings.isEmpty(); }
}
