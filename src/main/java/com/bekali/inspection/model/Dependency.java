package com.bekali.inspection.model;

public record Dependency(String name, String version, boolean vulnerable, String license) {
    public Dependency {
        name = Checks.text(name, "name");
        version = version == null ? "" : version.trim();
        license = license == null ? "" : license.trim();
    }
}
