package sos.agent.telemetry;

import java.util.Map;

/**
 * Immutable snapshot of settlement state at a point in time.
 *
 * This is the data contract between the Java mod and the Python supervisor.
 * The Python side deserialises this JSON structure — so field names here
 * become the keys in the JSON the LLM reads. Keep them readable.
 *
 * Fields are intentionally plain types (long, double, Map<String,Integer>)
 * so Jackson serialises them without any annotations needed.
 */
public final class TelemetrySnapshot {

    /** Wall-clock timestamp of this snapshot (Unix epoch ms) */
    public final long timestampMs;

    /** In-game year, season, and day if available — null if not yet readable */
    public final String gameDate;

    /** Total population across all races */
    public final int totalPopulation;

    /**
     * All settlement stats keyed by stat name.
     * Format mirrors more-options convention: "STAT_NAME:TOTAL" for aggregate,
     * "STAT_NAME:RaceName" for per-race values.
     * Examples: "HAPPINESS:TOTAL", "HAPPINESS:Cretor", "FOOD:TOTAL"
     */
    public final Map<String, Integer> stats;

    /**
     * Game-wide event counters (births, deaths, attacks, etc.)
     * Keyed by the counter's internal key string.
     */
    public final Map<String, Object> counters;

    public TelemetrySnapshot(
            long timestampMs,
            String gameDate,
            int totalPopulation,
            Map<String, Integer> stats,
            Map<String, Object> counters) {
        this.timestampMs = timestampMs;
        this.gameDate = gameDate;
        this.totalPopulation = totalPopulation;
        this.stats = stats;
        this.counters = counters;
    }
}
