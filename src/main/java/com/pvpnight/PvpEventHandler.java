package com.pvpnight;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Handles all game events for the PvP Night mod:
 *
 *  - {@link LevelTickEvent.Post}             — drives the PvP day cycle.
 *  - {@link LivingIncomingDamageEvent}       — blocks PvP damage outside PvP days.
 *  - {@link PlayerEvent.PlayerLoggedInEvent} — informs joining players of the state.
 *  - {@link RegisterCommandsEvent}           — registers /setpvpdelay.
 */
public class PvpEventHandler {

    /** Check PvP state once per second (20 ticks) to reduce overhead. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    // ------------------------------------------------------------------
    // Level tick — drives the PvP day cycle
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;

        MinecraftServer server = serverLevel.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        long currentDay = serverLevel.getDayTime() / 24000L;
        PvpCycleData data = PvpCycleData.get(serverLevel);

        if (!data.isPvpActive()) {
            if (currentDay >= data.getPvpDay()) {
                data.activatePvp();
                PvpNightMod.LOGGER.info("PvP day started! Current day: {}", currentDay);
                server.getPlayerList().broadcastSystemMessage(
                        PvpMessageFormatter.parse(PvpNightConfig.MSG_PVP_START.get()), false);
            }
        } else {
            if (currentDay >= data.getPvpEndDay()) {
                data.endPvpAndStartNewCycle(currentDay);
                PvpNightMod.LOGGER.info("PvP day ended. Next cycle starts at day {}", currentDay);
                long daysUntilNext = data.daysUntilPvp(currentDay);
                server.getPlayerList().broadcastSystemMessage(
                        PvpMessageFormatter.parse(PvpNightConfig.MSG_PVP_END.get(), daysUntilNext), false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Damage event — block PvP outside of PvP days
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        ServerLevel level = (ServerLevel) victim.level();
        PvpCycleData data = PvpCycleData.get(level);

        if (!data.isPvpActive()) {
            event.setCanceled(true);
            long days = data.daysUntilPvp(level.getDayTime() / 24000L);
            attacker.displayClientMessage(
                    PvpMessageFormatter.parse(PvpNightConfig.MSG_PVP_BLOCKED.get(), days),
                    true /* action bar */);
        }
    }

    // ------------------------------------------------------------------
    // Player login — inform player of current PvP state
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel overworld = player.getServer().overworld();
        PvpCycleData data = PvpCycleData.get(overworld);

        Component msg;
        if (data.isPvpActive()) {
            msg = PvpMessageFormatter.parse(PvpNightConfig.MSG_LOGIN_PVP_ACTIVE.get());
        } else {
            long days = data.daysUntilPvp(overworld.getDayTime() / 24000L);
            msg = PvpMessageFormatter.parse(PvpNightConfig.MSG_LOGIN_PVP_INACTIVE.get(), days);
        }
        player.sendSystemMessage(msg);
    }

    // ------------------------------------------------------------------
    // Command — /setpvpdelay <days>
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("setpvpdelay")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("days", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int days = IntegerArgumentType.getInteger(context, "days");
                                    MinecraftServer server = context.getSource().getServer();
                                    ServerLevel overworld = server.overworld();
                                    long currentDay = overworld.getDayTime() / 24000L;

                                    PvpCycleData data = PvpCycleData.get(overworld);
                                    data.setNextPvpInDays(currentDay, days);

                                    PvpNightMod.LOGGER.info("PvP delay set to {} days by {}",
                                            days, context.getSource().getTextName());

                                    context.getSource().sendSuccess(
                                            () -> PvpMessageFormatter.parse(
                                                    PvpNightConfig.MSG_SET_DELAY_SUCCESS.get(), days),
                                            true);
                                    return days;
                                }))
        );
    }
}
