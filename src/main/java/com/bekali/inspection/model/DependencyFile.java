package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;
import java.util.List;

public record DependencyFile(String name, int linesOfCode, List<Dependency> dependencies) implements ProjectElement {
    public DependencyFile {
        name = Checks.text(name, "name");
        Checks.nonNegative(linesOfCode, "linesOfCode");
        dependencies = List.copyOf(dependencies);
    }
    @Override public void accept(ProjectVisitor visitor) { visitor.visit(this); }
}
