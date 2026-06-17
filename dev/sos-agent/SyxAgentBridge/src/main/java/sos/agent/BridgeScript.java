package sos.agent;

import script.SCRIPT;
import util.info.INFO;

/**
 * Entry point for the SyxAgentBridge mod.
 *
 * The game discovers this class by scanning the mod jar for implementations
 * of SCRIPT via reflection — no registration required. The no-args constructor
 * is mandatory; Lombok's @NoArgsConstructor would also work but we keep it
 * explicit here for clarity.
 *
 * Lifecycle order:
 *   1. initBeforeGameCreated()  — fired before any game data loads
 *   2. initBeforeGameInited()   — fired after game data loads, before simulation starts
 *   3. createInstance()         — fired when a save is loaded or a new game starts;
 *                                 returns the BridgeInstance that runs the tick loop
 */
@SuppressWarnings("unused") // loaded by the game via reflection
public final class BridgeScript implements SCRIPT {

    private static final String MOD_NAME = "SyxAgentBridge";
    private static final String MOD_DESC = "Writes live settlement telemetry to JSON for LLM governor";

    private final INFO info = new INFO(MOD_NAME, MOD_DESC);

    public BridgeScript() {
        // no-args constructor required by the game's reflection loader
    }

    @Override
    public CharSequence name() {
        return info.name;
    }

    @Override
    public CharSequence desc() {
        return info.desc;
    }

    /**
     * Called before game data is created.
     * Safe place to log startup; game state is not available yet.
     */
    @Override
    public void initBeforeGameCreated() {
        System.out.println("[SyxAgentBridge] initBeforeGameCreated");
    }

    /**
     * Called after game data loads but before the simulation starts.
     * Safe place to read static game data (races, stat definitions, etc.)
     * if we need to build a whitelist or index at startup.
     */
    @Override
    public void initBeforeGameInited() {
        System.out.println("[SyxAgentBridge] initBeforeGameInited");
    }

    /**
     * Whether the mod appears as a selectable script when starting a new game.
     * false = always active, loads into any save automatically via forceInit().
     */
    @Override
    public boolean isSelectable() {
        return false;
    }

    /**
     * true = mod loads itself into existing saves that didn't originally use it.
     * This is what we want — the bridge should work with any existing colony.
     */
    @Override
    public boolean forceInit() {
        return true;
    }

    /**
     * Creates the live session instance. Called once per save load or new game.
     * All tick-loop logic lives in BridgeInstance.
     */
    @Override
    public SCRIPT_INSTANCE createInstance() {
        System.out.println("[SyxAgentBridge] createInstance — starting telemetry session");
        return new BridgeInstance();
    }
}
