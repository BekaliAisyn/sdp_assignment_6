package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;

public record TestFile(String name, String module, int linesOfCode, int testCount,
                       boolean hardcodedCredentials) implements ProjectElement {
    public TestFile {
        name = Checks.text(name, "name");
        module = Checks.text(module, "module");
        Checks.nonNegative(linesOfCode, "linesOfCode");
        Checks.nonNegative(testCount, "testCount");
    }
    @Override public void accept(ProjectVisitor visitor) { visitor.visit(this); }
}
