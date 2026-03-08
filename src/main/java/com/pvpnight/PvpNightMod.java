package com.pvpnight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PvP Night Mod — PvP is disabled by default.
 * Every 7 in-game days one day becomes a PvP day.
 * State survives server restarts and player reconnects via SavedData.
 */
@Mod(PvpNightMod.MOD_ID)
public class PvpNightMod {

    public static final String MOD_ID = "pvpnight";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public PvpNightMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new PvpEventHandler());
        LOGGER.info("PvP Night Mod initialized. PvP is disabled until day 7 of each cycle.");
    }
}

