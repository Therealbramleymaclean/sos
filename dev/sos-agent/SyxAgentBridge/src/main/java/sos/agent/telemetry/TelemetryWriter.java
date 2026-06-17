package sos.agent.telemetry;

import sos.agent.util.JsonWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Writes TelemetrySnapshot to disk as JSON.
 *
 * Uses an atomic write pattern:
 *   1. Write to telemetry.json.tmp
 *   2. Rename to telemetry.json
 *
 * This ensures the Python supervisor never reads a half-written file.
 * On macOS, Files.move with ATOMIC_MOVE is supported on the same filesystem.
 *
 * Output path:
 *   ~/Library/Application Support/songsofsyx/mods/SyxAgentBridge/telemetry/telemetry.json
 */
public final class TelemetryWriter {

    private static final String MODS_BASE =
            System.getProperty("user.home") +
            "/Library/Application Support/songsofsyx/mods/SyxAgentBridge/telemetry";

    private static final String SNAPSHOT_FILENAME = "telemetry.json";
    private static final String TMP_FILENAME = "telemetry.json.tmp";

    private final Path outputDir;
    private final Path snapshotPath;
    private final Path tmpPath;

    private boolean dirReady = false;

    public TelemetryWriter() {
        this.outputDir = Paths.get(MODS_BASE);
        this.snapshotPath = outputDir.resolve(SNAPSHOT_FILENAME);
        this.tmpPath = outputDir.resolve(TMP_FILENAME);
    }

    /**
     * Serialises the snapshot to JSON and atomically replaces the previous file.
     * Errors are logged but never thrown — telemetry must not crash the game.
     */
    public void write(TelemetrySnapshot snapshot) {
        try {
            ensureDir();
            String json = JsonWriter.toJson(snapshot);
            Files.writeString(tmpPath, json);
            Files.move(tmpPath, snapshotPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[SyxAgentBridge] Write failed: " + e.getMessage());
        }
    }

    private void ensureDir() throws IOException {
        if (!dirReady) {
            Files.createDirectories(outputDir);
            dirReady = true;
        }
    }
}
