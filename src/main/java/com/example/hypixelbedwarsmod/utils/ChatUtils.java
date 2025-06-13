package com.example.hypixelbedwarsmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Utility class for chat and messaging operations
 */
public class ChatUtils {
    private static final String MOD_PREFIX = EnumChatFormatting.DARK_AQUA + "[BWA] " + EnumChatFormatting.RESET;

    /**
     * Send a message with the mod prefix
     */
    public static void sendModMessage(String message) {
        sendMessage(MOD_PREFIX + message);
    }

    /**
     * Send a message to chat
     */
    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    /**
     * Send an alert message with color formatting
     */
    public static void sendAlert(String message, EnumChatFormatting color) {
        sendMessage(color + message);
    }

    /**
     * Send an error message
     */
    public static void sendError(String message) {
        sendAlert("Error: " + message, EnumChatFormatting.RED);
    }

    /**
     * Send a success message
     */
    public static void sendSuccess(String message) {
        sendAlert(message, EnumChatFormatting.GREEN);
    }

    /**
     * Send a warning message
     */
    public static void sendWarning(String message) {
        sendAlert("Warning: " + message, EnumChatFormatting.YELLOW);
    }

    /**
     * Format a player name with color
     */
    public static String formatPlayerName(String playerName, EnumChatFormatting color) {
        return color + playerName + EnumChatFormatting.RESET;
    }

    /**
     * Strip color codes from a string
     */
    public static String stripColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * Format distance string
     */
    public static String formatDistance(double distance) {
        return String.format("(%.1fm)", distance);
    }

    /**
     * Format remaining items count
     */
    public static String formatRemainingItems(int count, String itemType) {
        if (count <= 0) {
            return "(0 " + itemType + " remaining)";
        }
        return String.format("(%d %s remaining)", count, itemType);
    }
}
