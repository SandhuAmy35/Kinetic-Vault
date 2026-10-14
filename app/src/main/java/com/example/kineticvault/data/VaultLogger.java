package com.example.kineticvault.data;

/** Phase 3 will supply storage; these hooks deliberately accept no sensitive material. */
public final class VaultLogger {
    private VaultLogger() { }
    public static void logHide() { }
    public static void logReveal() { }
    public static void logExport() { }
}
