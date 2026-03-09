package com.pvpnight;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent data for the PvP cycle.
 *
 * Cycle logic:
 *   - CYCLE_LENGTH = 7 in-game days
 *   - The 7th day of each cycle (index 6) is the PvP day
 *   - After PvP day ends, the next cycle starts immediately
 *
 * Data is saved to world/data/pvpnight_cycle.dat and survives server restarts.
 */
public class PvpCycleData extends SavedData {

    public static final String DATA_NAME = "pvpnight_cycle";

    /** Number of in-game days in one full cycle (including the PvP day). */
    public static final int CYCLE_LENGTH = 7;

    /**
     * The first game-day of the current cycle.
     * PvP is active on day (cycleStartDay + CYCLE_LENGTH - 1).
     */
    private long cycleStartDay;

    /** Whether PvP is currently active (i.e., we are on the PvP day). */
    private boolean pvpActive;

    // -------------------------------------------------
    // Construction
    // -------------------------------------------------

    public PvpCycleData() {
        this.cycleStartDay = 0;
        this.pvpActive = false;
    }

    // -------------------------------------------------
    // Serialization
    // -------------------------------------------------

    public static PvpCycleData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        PvpCycleData data = new PvpCycleData();
        data.cycleStartDay = tag.getLong("cycleStartDay");
        data.pvpActive = tag.getBoolean("pvpActive");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.putLong("cycleStartDay", cycleStartDay);
        tag.putBoolean("pvpActive", pvpActive);
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

    public long getCycleStartDay() {
        return cycleStartDay;
    }

    /** Day on which PvP becomes active (0-indexed, inclusive). */
    public long getPvpDay() {
        return cycleStartDay + CYCLE_LENGTH - 1;
    }

    /** Day on which the current PvP period ends (exclusive). */
    public long getPvpEndDay() {
        return cycleStartDay + CYCLE_LENGTH;
    }

    public boolean isPvpActive() {
        return pvpActive;
    }

    /**
     * How many game-days remain until the next PvP day starts.
     * Returns 0 if PvP is currently active.
     */
    public long daysUntilPvp(long currentDay) {
        if (pvpActive) return 0;
        long pvpDay = getPvpDay();
        return Math.max(0, pvpDay - currentDay);
    }

    // -------------------------------------------------
    // State transitions
    // -------------------------------------------------

    public void activatePvp() {
        this.pvpActive = true;
        setDirty();
    }

    /**
     * Deactivate PvP and start the next 7-day cycle beginning at {@code currentDay}.
     */
    public void endPvpAndStartNewCycle(long currentDay) {
        this.pvpActive = false;
        this.cycleStartDay = currentDay;
        setDirty();
    }

    /**
     * Schedule the next PvP day to occur {@code daysFromNow} in-game days from now.
     * <p>
     * {@code daysFromNow = 0} activates PvP immediately (the tick handler picks it up
     * within one second); {@code daysFromNow = 1} means "next in-game day", etc.
     *
     * @param currentDay  current in-game day ({@code level.getDayTime() / 24000})
     * @param daysFromNow days until PvP starts; must be &gt;= 0
     */
    public void setNextPvpInDays(long currentDay, long daysFromNow) {
        this.pvpActive = false;
        // pvpDay = cycleStartDay + CYCLE_LENGTH - 1
        // We want pvpDay = currentDay + daysFromNow
        // => cycleStartDay = currentDay + daysFromNow - CYCLE_LENGTH + 1
        this.cycleStartDay = currentDay + daysFromNow - CYCLE_LENGTH + 1;
        setDirty();
    }
}
