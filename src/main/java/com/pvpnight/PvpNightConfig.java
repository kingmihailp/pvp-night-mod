package com.pvpnight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for PvP Night Mod.
 * Stored in config/pvpnight-common.toml
 *
 * Message format:
 *   &X            — legacy Minecraft formatting code (e.g. &c = red, &l = bold)
 *   &#RRGGBB      — hex color (e.g. &#FF4444 = bright red)
 *   %days%        — replaced with the number of days (where applicable)
 */
public class PvpNightConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ------------------------------------------------------------------
    // Message settings
    // ------------------------------------------------------------------

    public static final ModConfigSpec.ConfigValue<String> MSG_PVP_START;
    public static final ModConfigSpec.ConfigValue<String> MSG_PVP_END;
    public static final ModConfigSpec.ConfigValue<String> MSG_PVP_BLOCKED;
    public static final ModConfigSpec.ConfigValue<String> MSG_LOGIN_PVP_ACTIVE;
    public static final ModConfigSpec.ConfigValue<String> MSG_LOGIN_PVP_INACTIVE;
    public static final ModConfigSpec.ConfigValue<String> MSG_SET_DELAY_SUCCESS;

    static {
        BUILDER.comment(
                "PvP Night Mod — Message Configuration",
                "Use & for Minecraft formatting codes:  &c=red  &a=green  &e=yellow  &7=gray  &l=bold  &r=reset",
                "Use &#RRGGBB for hex colors:  &#FF4444=bright red  &#44FF44=bright green",
                "Use %days% as a placeholder for the day count in supported messages."
        );

        BUILDER.push("messages");

        MSG_PVP_START = BUILDER
                .comment("Broadcast to all players when the PvP day starts.")
                .define("pvp_start",
                        "&#FF4444\u2694 [PvP Night] &eДЕНЬ PVP \u041D\u0410\u0427\u0410\u041B\u0421\u042F! \u0421\u0440\u0430\u0436\u0430\u0439\u0442\u0435\u0441\u044C!");

        MSG_PVP_END = BUILDER
                .comment("Broadcast to all players when the PvP day ends. %days% = days until next PvP day.")
                .define("pvp_end",
                        "&#44FF44\u2705 [PvP Night] &7\u0414\u0435\u043D\u044C PvP \u0437\u0430\u043A\u043E\u043D\u0447\u0438\u043B\u0441\u044F. \u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0438\u0439 \u0447\u0435\u0440\u0435\u0437 &e%days%&7.");

        MSG_PVP_BLOCKED = BUILDER
                .comment("Shown in the action bar when a player tries to attack outside a PvP day. %days% = days until next PvP day.")
                .define("pvp_blocked",
                        "&#FF4444\u26D4 PvP \u043E\u0442\u043A\u043B\u044E\u0447\u0451\u043D! \u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0438\u0439 \u0434\u0435\u043D\u044C PvP \u0447\u0435\u0440\u0435\u0437 &e%days%&c.");

        MSG_LOGIN_PVP_ACTIVE = BUILDER
                .comment("Shown to a player on login when a PvP day is currently active.")
                .define("login_pvp_active",
                        "&#FF4444[PvP Night] &e\u0421\u0435\u0433\u043E\u0434\u043D\u044F \u0434\u0435\u043D\u044C PvP! \u041E\u0441\u0442\u043E\u0440\u043E\u0436\u043D\u043E!");

        MSG_LOGIN_PVP_INACTIVE = BUILDER
                .comment("Shown to a player on login when PvP is not active. %days% = days until next PvP day.")
                .define("login_pvp_inactive",
                        "&#44FF44[PvP Night] &7PvP \u043E\u0442\u043A\u043B\u044E\u0447\u0451\u043D. \u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0438\u0439 \u0447\u0435\u0440\u0435\u0437 &e%days%&7.");

        MSG_SET_DELAY_SUCCESS = BUILDER
                .comment("Shown to the operator after running /setpvpdelay. %days% = the value they entered.")
                .define("set_delay_success",
                        "&#44FF44[PvP Night] &7\u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0438\u0439 \u0434\u0435\u043D\u044C PvP \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D \u0447\u0435\u0440\u0435\u0437 &e%days%&7.");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
