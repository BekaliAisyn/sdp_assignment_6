package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;

public final class QualityGateVisitor extends RecursiveProjectVisitor {
    private final ProjectStatisticsVisitor statistics = new ProjectStatisticsVisitor();
    private final SecurityAuditVisitor security = new SecurityAuditVisitor();
    @Override public void visit(SourceCodeFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(TestFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(ConfigurationFile file) { statistics.visit(file); security.visit(file); }
    @Override public void visit(DependencyFile file) { statistics.visit(file); security.visit(file); }
    public Result result() {
        var snapshot = statistics.statistics();
        return new Result(security.criticalIssues() == 0, snapshot.averageComplexity() <= 10.0,
                snapshot.sourceFiles() > 0 && snapshot.testSourceRatio() >= 0.40);
    }
    public record Result(boolean securityPass, boolean complexityPass, boolean testsPass) {
        public boolean passed() { return securityPass && complexityPass && testsPass; }
        private String label(boolean pass) { return pass ? "PASS" : "FAIL"; }
        @Override public String toString() {
            return "Security: " + label(securityPass) + "\nComplexity: " + label(complexityPass)
                    + "\nTests: " + label(testsPass) + "\nFINAL RESULT: " + (passed() ? "PASSED" : "FAILED");
        }
    }
}
