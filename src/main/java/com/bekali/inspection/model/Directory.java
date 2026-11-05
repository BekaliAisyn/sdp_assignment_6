package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;
import java.util.List;

public record Directory(String name, List<ProjectElement> children) implements ProjectElement {
    public Directory {
        name = Checks.text(name, "name");
        children = List.copyOf(children);
    }
    @Override public void accept(ProjectVisitor visitor) { visitor.visit(this); }
}
