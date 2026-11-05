package com.bekali.inspection.visitor;

public record Finding(Severity severity, String location, String rule, String message) {
    public enum Severity { CRITICAL, WARNING }
    @Override public String toString() {
        return severity + " [" + rule + "] " + location + ": " + message;
    }
}
