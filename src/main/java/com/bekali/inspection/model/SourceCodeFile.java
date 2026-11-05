package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;

public record SourceCodeFile(String name, String module, int linesOfCode, int complexity,
                             boolean hardcodedCredentials, boolean highRisk) implements ProjectElement {
    public SourceCodeFile {
        name = Checks.text(name, "name");
        module = Checks.text(module, "module");
        Checks.nonNegative(linesOfCode, "linesOfCode");
        Checks.nonNegative(complexity, "complexity");
    }
    @Override public void accept(ProjectVisitor visitor) { visitor.visit(this); }
}
