package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;
import java.util.*;
import static com.bekali.inspection.visitor.Finding.Severity.CRITICAL;

public final class ComplianceVisitor extends RecursiveProjectVisitor {
    private final Set<String> prohibited;
    private final Set<String> productionModules = new TreeSet<>();
    private final Set<String> testedModules = new HashSet<>();
    private final List<Finding> violations = new ArrayList<>();
    public ComplianceVisitor() { this(Set.of("legacy-crypto", "unsafe-deserializer")); }
    public ComplianceVisitor(Set<String> prohibited) { this.prohibited = Set.copyOf(prohibited); }
    private void add(String location, String rule, String message) {
        violations.add(new Finding(CRITICAL, location, rule, message));
    }
    private void credentials(String location, boolean exposed) {
        if (exposed) add(location, "CMP-CREDENTIALS", "Credentials must not be exposed");
    }
    @Override public void visit(SourceCodeFile file) {
        productionModules.add(file.module());
        credentials(file.name(), file.hardcodedCredentials());
    }
    @Override public void visit(TestFile file) {
        if (file.testCount() > 0) testedModules.add(file.module());
        credentials(file.name(), file.hardcodedCredentials());
    }
    @Override public void visit(ConfigurationFile file) { credentials(file.name(), file.exposedCredentials()); }
    @Override public void visit(DependencyFile file) {
        for (Dependency dependency : file.dependencies()) {
            if (dependency.version().isBlank()) add(file.name(), "CMP-VERSION", "Missing version: " + dependency.name());
            if (prohibited.contains(dependency.name())) add(file.name(), "CMP-PROHIBITED", "Prohibited dependency: " + dependency.name());
        }
    }
    public List<Finding> violations() {
        List<Finding> result = new ArrayList<>(violations);
        for (String module : productionModules) {
            if (!testedModules.contains(module)) result.add(new Finding(CRITICAL, module, "CMP-TESTS", "Production module has no tests"));
        }
        return List.copyOf(result);
    }
    public boolean compliant() { return violations().isEmpty(); }
}
