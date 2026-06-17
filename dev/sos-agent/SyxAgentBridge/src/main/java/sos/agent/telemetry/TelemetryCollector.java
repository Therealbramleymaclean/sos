package sos.agent.telemetry;

import game.GAME;
import game.time.TIME;
import game.values.GCOUNTS;
import init.race.RACES;
import init.race.Race;
import settlement.stats.STATS;
import settlement.stats.stat.STAT;
import snake2d.util.sets.LIST;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads live game state and builds a TelemetrySnapshot.
 *
 * All game API calls happen here and nowhere else — keeping the rest of the
 * mod isolated from the game's internals. If a V72 update renames a class,
 * this is the only file that needs updating.
 *
 * The stat reading pattern is taken directly from the more-options mod's
 * MetricCollector (V69), verified against the V71 jar structure.
 *
 * Convention for stat keys (matches more-options for Python-side compatibility):
 *   "STAT_NAME:TOTAL"     — aggregate value across all races
 *   "STAT_NAME:RaceKey"   — value for a specific race
 */
public final class TelemetryCollector {

    /**
     * Stat keys known to throw exceptions in some game states.
     * Silently skip these rather than letting one bad stat abort the snapshot.
     */
    private static final java.util.Set<String> STAT_BLACKLIST = java.util.Set.of(
            "HOME_FURNITURE"
    );

    public TelemetrySnapshot collect() {
        long timestampMs = System.currentTimeMillis();
        String gameDate = readGameDate();
        Map<String, Integer> stats = readAllStats();
        Map<String, Object> counters = readCounters();
        int totalPop = readTotalPopulation(stats);

        return new TelemetrySnapshot(timestampMs, gameDate, totalPop, stats, counters);
    }

    // -------------------------------------------------------------------------
    // Game date
    // -------------------------------------------------------------------------

    private String readGameDate() {
        try {
            int year = TIME.years().bitCurrent();
            String day = TIME.days().bitNameCurrent();
            String season = TIME.season().name;
            return "Year " + year + ", " + season + ", " + day;
        } catch (Exception e) {
            return "unknown";
        }
    }

    // -------------------------------------------------------------------------
    // Settlement stats  (happiness, food, health, morale, etc.)
    // -------------------------------------------------------------------------

    private Map<String, Integer> readAllStats() {
        Map<String, Integer> stats = new HashMap<>();

        LIST<STAT> allStats = STATS.all();
        int size = allStats.size();

        for (int i = 0; i < size; i++) {
            STAT stat = allStats.get(i);
            String statKey = stat.info().name.toString();

            if (STAT_BLACKLIST.contains(statKey)) {
                continue;
            }

            // Total (all races combined) — pass null to get aggregate
            try {
                int total = stat.data().get(null);
                stats.put(statKey + ":TOTAL", total);
            } catch (Exception e) {
                // stat not available in current game state, skip
            }

            // Per-race breakdown
            if (stat.info().indu()) { // indu() = "individual" = has per-race data
                LIST<Race> races = RACES.all();
                for (int r = 0; r < races.size(); r++) {
                    Race race = races.get(r);
                    try {
                        int value = stat.data().get(race);
                        stats.put(statKey + ":" + race.key, value);
                    } catch (Exception e) {
                        // race doesn't have this stat, skip
                    }
                }
            }
        }

        // Population is a special case in the more-options SDK
        try {
            int pop = STATS.POP().POP.data().get(null);
            stats.put("POP:TOTAL", pop);
        } catch (Exception e) {
            // not available yet
        }

        return stats;
    }

    // -------------------------------------------------------------------------
    // Game-wide counters (deaths, births, attacks, etc.)
    // -------------------------------------------------------------------------

    private Map<String, Object> readCounters() {
        Map<String, Object> counters = new HashMap<>();
        try {
            LIST<GCOUNTS.SAccumilator> all = GAME.count().ALL;
            for (int i = 0; i < all.size(); i++) {
                GCOUNTS.SAccumilator acc = all.get(i);
                counters.put(acc.key, acc.current());
            }
        } catch (Exception e) {
            System.err.println("[SyxAgentBridge] Could not read game counters: " + e.getMessage());
        }
        return counters;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int readTotalPopulation(Map<String, Integer> stats) {
        return stats.getOrDefault("POP:TOTAL", 0);
    }
}
