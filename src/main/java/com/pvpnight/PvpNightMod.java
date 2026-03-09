package com.pvpnight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PvP Night Mod — PvP is disabled by default.
 * Every 7 in-game days one day becomes a PvP day.
 * State survives server restarts and player reconnects via SavedData.
 *
 * Config: config/pvpnight-common.toml
 * Command: /setpvpdelay <days>  (requires OP level 2)
 */
@Mod(PvpNightMod.MOD_ID)
public class PvpNightMod {

    public static final String MOD_ID = "pvpnight";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public PvpNightMod(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PvpNightConfig.SPEC);
        NeoForge.EVENT_BUS.register(new PvpEventHandler());
        LOGGER.info("PvP Night Mod initialized. Config: config/pvpnight-common.toml");
    }
}
