package sos.agent;

import script.SCRIPT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import sos.agent.telemetry.TelemetryCollector;
import sos.agent.telemetry.TelemetryWriter;

/**
 * The live session instance of the bridge mod.
 *
 * Created once per save load (or new game) by BridgeScript.createInstance().
 * The game calls update() many times per second — we throttle actual telemetry
 * writes to every SNAPSHOT_INTERVAL_SECONDS of real wall-clock time.
 *
 * Thread safety: update() is called on the game thread. TelemetryWriter uses
 * an atomic file swap (write to .tmp, then rename) so the Python supervisor
 * always reads a complete JSON file, never a partial write.
 */
@SuppressWarnings("unused") // used by game via BridgeScript.createInstance()
final class BridgeInstance implements SCRIPT.SCRIPT_INSTANCE {

    /** How often (real seconds) we write a telemetry snapshot. Adjust as needed. */
    private static final double SNAPSHOT_INTERVAL_SECONDS = 30.0;

    private final TelemetryCollector collector = new TelemetryCollector();
    private final TelemetryWriter writer = new TelemetryWriter();

    /** Accumulates real time between snapshots */
    private double secondsAccumulator = 0.0;

    /**
     * Called by the game many times per second.
     *
     * @param deltaSeconds real-world seconds elapsed since the last call
     *                     (not in-game time — this is wall-clock delta)
     */
    @Override
    public void update(double deltaSeconds) {
        secondsAccumulator += deltaSeconds;

        if (secondsAccumulator >= SNAPSHOT_INTERVAL_SECONDS) {
            secondsAccumulator = 0.0;
            takeSnapshot();
        }
    }

    private void takeSnapshot() {
        try {
            writer.write(collector.collect());
        } catch (Exception e) {
            // Never let telemetry errors crash the game
            System.err.println("[SyxAgentBridge] Snapshot failed: " + e.getMessage());
        }
    }

    /**
     * Called when the game saves. We persist the accumulator so the interval
     * survives a save/load cycle without immediately re-firing.
     */
    @Override
    public void save(FilePutter file) {
        file.d(secondsAccumulator);
    }

    /**
     * Called when the game loads a save.
     */
    @Override
    public void load(FileGetter file) throws java.io.IOException {
        secondsAccumulator = file.d();
    }

    /**
     * If our saved state is somehow corrupt (e.g. after a mod update),
     * return true to recover gracefully rather than crashing the load.
     */
    @Override
    public boolean handleBrokenSavedState() {
        secondsAccumulator = 0.0;
        return true;
    }
}
