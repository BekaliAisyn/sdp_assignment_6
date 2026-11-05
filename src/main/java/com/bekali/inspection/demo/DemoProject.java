package com.bekali.inspection.demo;

import com.bekali.inspection.model.*;
import java.util.List;

public final class DemoProject {
    private DemoProject() { }
    public static Directory create() {
        return new Directory("fintech-project", List.of(
            new Directory("src", List.of(
                new SourceCodeFile("PaymentService.java", "payments", 180, 14, true, true),
                new SourceCodeFile("UserService.java", "users", 120, 6, false, false))),
            new Directory("tests", List.of(
                new TestFile("PaymentServiceTest.java", "payments", 90, 8, false))),
            new Directory("config", List.of(
                new ConfigurationFile("application.properties", 20, false, true))),
            new DependencyFile("pom.xml", 40, List.of(
                new Dependency("spring-core", "6.2.0", false, "Apache-2.0"),
                new Dependency("legacy-crypto", "", true, "GPL-3.0")))));
    }
}
