package com.pvpnight;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Handles all game events for the PvP Night mod:
 *
 *  - {@link LevelTickEvent.Post} — checks whether a PvP day has started or ended.
 *  - {@link LivingAttackEvent}   — cancels player-vs-player damage outside PvP days.
 *  - {@link PlayerEvent.PlayerLoggedInEvent} — informs joining players of the PvP state.
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

        // Only run in the Overworld on the server side
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;

        MinecraftServer server = serverLevel.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        long currentDay = serverLevel.getDayTime() / 24000L;
        PvpCycleData data = PvpCycleData.get(serverLevel);

        if (!data.isPvpActive()) {
            // Check if the PvP day has arrived
            if (currentDay >= data.getPvpDay()) {
                data.activatePvp();
                PvpNightMod.LOGGER.info("PvP day started! Current day: {}", currentDay);
                broadcastPvpStart(server, data, currentDay);
            }
        } else {
            // Check if the PvP day has ended
            if (currentDay >= data.getPvpEndDay()) {
                data.endPvpAndStartNewCycle(currentDay);
                PvpNightMod.LOGGER.info("PvP day ended. Next cycle starts at day {}", currentDay);
                broadcastPvpEnd(server, data, currentDay);
            }
        }
    }

    // ------------------------------------------------------------------
    // Damage event — block PvP outside of PvP days
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        // We only care about player-vs-player damage
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        ServerLevel level = (ServerLevel) victim.level();
        PvpCycleData data = PvpCycleData.get(level);

        if (!data.isPvpActive()) {
            event.setCanceled(true);

            long days = data.daysUntilPvp(level.getDayTime() / 24000L);
            String daysText = days == 1 ? "1 день" : (days + " дней");

            // Show message in the action bar (above hotbar) so it's not spammy
            attacker.displayClientMessage(
                    Component.literal("\u26d4 PvP отключён! Следующий день PvP через ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(daysText).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(".").withStyle(ChatFormatting.RED)),
                    true // action bar
            );
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

        if (data.isPvpActive()) {
            player.sendSystemMessage(
                    Component.literal("[PvP Night] ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal("Сегодня день PvP! Осторожно!")
                                    .withStyle(ChatFormatting.YELLOW))
            );
        } else {
            long days = data.daysUntilPvp(overworld.getDayTime() / 24000L);
            String daysText = formatDays(days);
            player.sendSystemMessage(
                    Component.literal("[PvP Night] ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal("PvP отключён. Следующий день PvP через ")
                                    .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(daysText).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(".").withStyle(ChatFormatting.GRAY))
            );
        }
    }

    // ------------------------------------------------------------------
    // Broadcast helpers
    // ------------------------------------------------------------------

    private static void broadcastPvpStart(MinecraftServer server, PvpCycleData data, long currentDay) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("\u2694 [PvP Night] ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("ДЕНЬ PVP НАЧАЛСЯ! Сражайтесь! Осталось до конца: 1 день.")
                                .withStyle(ChatFormatting.YELLOW)),
                false
        );
    }

    private static void broadcastPvpEnd(MinecraftServer server, PvpCycleData data, long currentDay) {
        long daysUntilNext = data.daysUntilPvp(currentDay);
        String daysText = formatDays(daysUntilNext);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("\u2705 [PvP Night] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("День PvP закончился. Следующий через ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(daysText).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(".").withStyle(ChatFormatting.GRAY)),
                false
        );
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    /**
     * Returns a Russian-friendly day string: "1 день", "2 дня", "5 дней", etc.
     */
    private static String formatDays(long days) {
        if (days == 1) return "1 день";
        if (days >= 2 && days <= 4) return days + " дня";
        return days + " дней";
    }
}
