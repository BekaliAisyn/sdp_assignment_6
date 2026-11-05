package com.bekali.inspection.visitor;

import com.bekali.inspection.model.*;

public final class ProjectStatisticsVisitor extends RecursiveProjectVisitor {
    private long files, lines, sources, testFiles, tests, dependencies, complexity;
    @Override public void visit(SourceCodeFile file) {
        files++; sources++; lines += file.linesOfCode(); complexity += file.complexity();
    }
    @Override public void visit(TestFile file) {
        files++; testFiles++; lines += file.linesOfCode(); tests += file.testCount();
    }
    @Override public void visit(ConfigurationFile file) { files++; lines += file.linesOfCode(); }
    @Override public void visit(DependencyFile file) {
        files++; lines += file.linesOfCode(); dependencies += file.dependencies().size();
    }
    public Statistics statistics() {
        return new Statistics(files, lines, sources, testFiles, tests, dependencies,
                sources == 0 ? 0.0 : (double) complexity / sources,
                sources == 0 ? 0.0 : (double) testFiles / sources);
    }
    public record Statistics(long totalFiles, long linesOfCode, long sourceFiles, long testFiles,
                             long numberOfTests, long dependencies, double averageComplexity,
                             double testSourceRatio) { }
}
