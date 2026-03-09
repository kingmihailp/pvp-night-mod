package com.pvpnight;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses message strings from config into Minecraft {@link Component} objects.
 *
 * Supported syntax:
 *   &#RRGGBB   — 24-bit hex color  (e.g. &#FF4444)
 *   &0–&9, &a–&f — legacy color codes
 *   &k &l &m &n &o — obfuscated, bold, strikethrough, underline, italic
 *   &r           — reset all formatting
 */
public class PvpMessageFormatter {

    /**
     * Matches either &#RRGGBB or a single &X legacy code.
     * Group 1 = hex digits (when it's a hex color).
     * Group 2 = legacy character (when it's a legacy code).
     */
    private static final Pattern CODE_PATTERN =
            Pattern.compile("&#([0-9A-Fa-f]{6})|&([0-9a-fk-orA-FK-OR])");

    /**
     * Parse a raw config string (with & codes and &#RRGGBB hex) into a Component.
     *
     * @param raw the raw message string
     * @return parsed {@link Component}
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        Style currentStyle = Style.EMPTY;
        int lastIndex = 0;

        Matcher matcher = CODE_PATTERN.matcher(raw);

        while (matcher.find()) {
            // Append the literal text segment before this code
            if (matcher.start() > lastIndex) {
                String segment = raw.substring(lastIndex, matcher.start());
                result.append(Component.literal(segment).withStyle(currentStyle));
            }

            if (matcher.group(1) != null) {
                // &#RRGGBB — hex color
                int rgb = Integer.parseInt(matcher.group(1), 16);
                // Preserve formatting flags (bold/italic/etc.), only change color
                currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));

            } else if (matcher.group(2) != null) {
                // &X — legacy code
                char code = Character.toLowerCase(matcher.group(2).charAt(0));

                if (code == 'r') {
                    // Reset
                    currentStyle = Style.EMPTY;
                } else {
                    ChatFormatting fmt = ChatFormatting.getByCode(code);
                    if (fmt != null) {
                        if (fmt.isFormat()) {
                            // Bold, italic, underline, strikethrough, obfuscated
                            currentStyle = currentStyle.applyFormat(fmt);
                        } else if (fmt.isColor()) {
                            // Named color — convert to TextColor so hex and legacy
                            // colors use the same mechanism
                            Integer colorInt = fmt.getColor();
                            if (colorInt != null) {
                                currentStyle = currentStyle.withColor(TextColor.fromRgb(colorInt));
                            }
                        }
                    }
                }
            }

            lastIndex = matcher.end();
        }

        // Append any trailing text after the last code
        if (lastIndex < raw.length()) {
            result.append(Component.literal(raw.substring(lastIndex)).withStyle(currentStyle));
        }

        return result;
    }

    /**
     * Parse a raw config string, replacing {@code %days%} with the given value first.
     *
     * @param raw  the raw message string
     * @param days the number of days to substitute for {@code %days%}
     * @return parsed {@link Component}
     */
    public static Component parse(String raw, long days) {
        return parse(raw.replace("%days%", String.valueOf(days)));
    }
}
