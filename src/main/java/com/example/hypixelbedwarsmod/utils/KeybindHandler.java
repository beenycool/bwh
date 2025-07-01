package com.example.hypixelbedwarsmod.utils;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import com.example.hypixelbedwarsmod.ui.AlertOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * NEW: Keybind handler for quick actions
 */
public class KeybindHandler {
    
    private final ConfigManager configManager;
    private final AlertOverlay alertOverlay;
    private String lastTargetedPlayer = null; // For mute toggle functionality
    
    public KeybindHandler(ConfigManager configManager, AlertOverlay alertOverlay) {
        this.configManager = configManager;
        this.alertOverlay = alertOverlay;
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Minecraft.getMinecraft().currentScreen != null) {
            return; // Don't process keybinds when in GUI
        }
        
        int keyPressed = Keyboard.getEventKey();
        boolean keyDown = Keyboard.getEventKeyState();
        
        if (!keyDown) return; // Only process key press, not release
        
        // Mute toggle key
        if (keyPressed == configManager.getMuteToggleKey()) {
            handleMuteToggle();
        }
        // ESP toggle key
        else if (keyPressed == configManager.getEspToggleKey()) {
            handleEspToggle();
        }
        // Config key
        else if (keyPressed == configManager.getConfigKey()) {
            handleConfigOpen();
        }
        // History toggle key
        else if (keyPressed == configManager.getHistoryToggleKey()) {
            handleHistoryToggle();
        }
    }
    
    /**
     * Handle mute toggle - mutes the player the user is looking at
     */
    private void handleMuteToggle() {
        EntityPlayer target = getTargetedPlayer();
        if (target != null) {
            String playerName = target.getName();
            
            if (configManager.isPlayerMuted(playerName)) {
                configManager.unmutePlayer(playerName);
                ChatUtils.sendSuccess("Unmuted " + playerName);
                SoundUtils.playSound(SoundUtils.SOUND_EMERALD);
            } else {
                configManager.mutePlayer(playerName);
                ChatUtils.sendInfo("Muted " + playerName);
                SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
            }
            
            lastTargetedPlayer = playerName;
        } else if (lastTargetedPlayer != null) {
            // Toggle mute for last targeted player if no current target
            if (configManager.isPlayerMuted(lastTargetedPlayer)) {
                configManager.unmutePlayer(lastTargetedPlayer);
                ChatUtils.sendSuccess("Unmuted " + lastTargetedPlayer);
            } else {
                configManager.mutePlayer(lastTargetedPlayer);
                ChatUtils.sendInfo("Muted " + lastTargetedPlayer);
            }
        } else {
            ChatUtils.sendError("Look at a player to mute/unmute them");
        }
    }
    
    /**
     * Handle ESP toggle
     */
    private void handleEspToggle() {
        boolean newState = !configManager.isItemESPEnabled();
        configManager.setItemESPEnabled(newState);
        
        if (newState) {
            ChatUtils.sendSuccess("Item ESP enabled");
            SoundUtils.playSound(SoundUtils.SOUND_EMERALD);
        } else {
            ChatUtils.sendInfo("Item ESP disabled");
            SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
        }
    }
    
    /**
     * Handle config screen opening
     */
    private void handleConfigOpen() {
        try {
            // This would need to be implemented with the actual config screen
            ChatUtils.sendInfo("Opening config... (Not yet implemented in UI)");
            SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
        } catch (Exception e) {
            ChatUtils.sendError("Failed to open config: " + e.getMessage());
        }
    }
    
    /**
     * Handle history panel toggle
     */
    private void handleHistoryToggle() {
        alertOverlay.toggleHistoryPanel();
        
        if (alertOverlay.isHistoryPanelVisible()) {
            ChatUtils.sendSuccess("Alert history shown");
        } else {
            ChatUtils.sendInfo("Alert history hidden");
        }
        
        SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
    }
    
    /**
     * Get the player the user is currently looking at
     */
    private EntityPlayer getTargetedPlayer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
            return (EntityPlayer) mc.objectMouseOver.entityHit;
        }
        return null;
    }
    
    /**
     * Get readable key name for display
     */
    public static String getKeyName(int keyCode) {
        if (keyCode == 0) {
            return "None";
        }
        return Keyboard.getKeyName(keyCode);
    }
    
    /**
     * Display current keybind configuration
     */
    public void displayKeybinds() {
        ChatUtils.sendInfo("=== BedWars Helper Keybinds ===");
        ChatUtils.sendInfo("Mute Toggle: " + getKeyName(configManager.getMuteToggleKey()));
        ChatUtils.sendInfo("ESP Toggle: " + getKeyName(configManager.getEspToggleKey()));
        ChatUtils.sendInfo("Config: " + getKeyName(configManager.getConfigKey()));
        ChatUtils.sendInfo("History: " + getKeyName(configManager.getHistoryToggleKey()));
    }
}