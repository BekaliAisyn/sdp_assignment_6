package com.bekali.inspection.model;

import com.bekali.inspection.visitor.ProjectVisitor;

public interface ProjectElement {
    void accept(ProjectVisitor visitor);
}
