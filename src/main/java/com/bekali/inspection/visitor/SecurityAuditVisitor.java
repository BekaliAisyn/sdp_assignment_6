package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;
import java.util.ArrayList;
import java.util.List;
import static com.bekali.inspection.visitor.Finding.Severity.*;

public final class SecurityAuditVisitor extends RecursiveProjectVisitor {
    private final List<Finding> findings = new ArrayList<>();
    private void credentials(String name, boolean exposed) {
        if (exposed) findings.add(new Finding(CRITICAL, name, "SEC-CREDENTIALS", "Exposed credentials"));
    }
    @Override public void visit(SourceCodeFile file) {
        credentials(file.name(), file.hardcodedCredentials());
        if (file.highRisk()) findings.add(new Finding(WARNING, file.name(), "SEC-HIGH-RISK", "High-risk source requires review"));
    }
    @Override public void visit(TestFile file) { credentials(file.name(), file.hardcodedCredentials()); }
    @Override public void visit(ConfigurationFile file) {
        credentials(file.name(), file.exposedCredentials());
        if (file.insecureConfiguration()) findings.add(new Finding(CRITICAL, file.name(), "SEC-CONFIG", "Insecure configuration"));
    }
    @Override public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            if (dependency.vulnerable()) findings.add(new Finding(CRITICAL, file.name(), "SEC-DEPENDENCY", "Vulnerable dependency: " + dependency.name()));
        }
    }
    public List<Finding> findings() { return List.copyOf(findings); }
    public long criticalIssues() { return findings.stream().filter(f -> f.severity() == CRITICAL).count(); }
    public int score() {
        long penalty = criticalIssues() * 25 + (findings.size() - criticalIssues()) * 10;
        return (int) Math.max(0, 100 - penalty);
    }
}
