package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;

public record ConfigurationFile(String name, int linesOfCode, boolean exposedCredentials,
                                boolean insecureConfiguration) implements ProjectElement {
    public ConfigurationFile {
        name = Checks.text(name, "name");
        Checks.nonNegative(linesOfCode, "linesOfCode");
    }
    @Override public void accept(ProjectVisitor visitor) { visitor.visit(this); }
}
