package com.pvpnight;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent data for the PvP cycle.
 *
 * Cycle logic:
 *   - By default: 6 non-PvP days + 1 PvP day = 7-day cycle.
 *   - /setpvpdelay N changes the delay to N days for ALL future cycles.
 *   - After each PvP day the next cycle begins immediately using the saved delay.
 *
 * Data is saved to world/data/pvpnight_cycle.dat and survives server restarts.
 */
public class PvpCycleData extends SavedData {

    public static final String DATA_NAME = "pvpnight_cycle";

    /** Default number of non-PvP days before each PvP day. */
    public static final int DEFAULT_DELAY = 6;

    /** First game-day of the current cycle. PvP starts on (cycleStartDay + customDelay). */
    private long cycleStartDay;

    /** Whether PvP is currently active. */
    private boolean pvpActive;

    /**
     * How many non-PvP days precede each PvP day.
     * Saved so that /setpvpdelay affects all future cycles, not just the current one.
     */
    private long customDelay;

    // -------------------------------------------------
    // Construction
    // -------------------------------------------------

    public PvpCycleData() {
        this.cycleStartDay = 0;
        this.pvpActive = false;
        this.customDelay = DEFAULT_DELAY;
    }

    // -------------------------------------------------
    // Serialization
    // -------------------------------------------------

    public static PvpCycleData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        PvpCycleData data = new PvpCycleData();
        data.cycleStartDay = tag.getLong("cycleStartDay");
        data.pvpActive = tag.getBoolean("pvpActive");
        // Fall back to DEFAULT_DELAY for worlds created before this field existed
        data.customDelay = tag.contains("customDelay") ? tag.getLong("customDelay") : DEFAULT_DELAY;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.putLong("cycleStartDay", cycleStartDay);
        tag.putBoolean("pvpActive", pvpActive);
        tag.putLong("customDelay", customDelay);
        return tag;
    }

    // -------------------------------------------------
    // Access from SavedData storage
    // -------------------------------------------------

    public static PvpCycleData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PvpCycleData::new, PvpCycleData::load, null),
                DATA_NAME
        );
    }

    // -------------------------------------------------
    // Getters
    // -------------------------------------------------

    /** Day on which PvP becomes active (inclusive). */
    public long getPvpDay() {
        return cycleStartDay + customDelay;
    }

    /** Day on which the current PvP period ends (exclusive). */
    public long getPvpEndDay() {
        return cycleStartDay + customDelay + 1;
    }

    public boolean isPvpActive() {
        return pvpActive;
    }

    public long getCustomDelay() {
        return customDelay;
    }

    /**
     * How many game-days remain until the next PvP day starts.
     * Returns 0 if PvP is currently active.
     */
    public long daysUntilPvp(long currentDay) {
        if (pvpActive) return 0;
        return Math.max(0, getPvpDay() - currentDay);
    }

    // -------------------------------------------------
    // State transitions
    // -------------------------------------------------

    public void activatePvp() {
        this.pvpActive = true;
        setDirty();
    }

    /**
     * Deactivate PvP and start the next cycle from {@code currentDay}.
     * Uses the saved {@code customDelay}, so the interval set by /setpvpdelay persists.
     */
    public void endPvpAndStartNewCycle(long currentDay) {
        this.pvpActive = false;
        this.cycleStartDay = currentDay;
        // customDelay is intentionally NOT reset here — it stays as configured
        setDirty();
    }

    /**
     * Change the delay between PvP days and reschedule accordingly.
     * The new delay is saved and will be used for ALL future cycles automatically.
     *
     * @param currentDay  current in-game day ({@code level.getDayTime() / 24000})
     * @param daysFromNow days until PvP starts; must be &gt;= 0
     */
    public void setNextPvpInDays(long currentDay, long daysFromNow) {
        this.pvpActive = false;
        this.customDelay = daysFromNow;   // saved — used by endPvpAndStartNewCycle too
        this.cycleStartDay = currentDay;   // pvpDay = currentDay + customDelay
        setDirty();
    }
}
