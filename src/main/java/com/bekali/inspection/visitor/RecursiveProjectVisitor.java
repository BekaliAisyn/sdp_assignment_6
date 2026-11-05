package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;

public abstract class RecursiveProjectVisitor implements ProjectVisitor {
    @Override public void visit(Directory directory) {
        for (ProjectElement child : directory.children()) child.accept(this);
    }
}
