package com.example.hypixelbedwarsmod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.hypixelbedwarsmod.utils.ChatUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Robust configuration manager with JSON support and validation
 */
public class ConfigManager {
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE_NAME = "bedwars_assistant.json";
    private static final Path CONFIG_PATH = Paths.get(CONFIG_DIR, CONFIG_FILE_NAME);
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConfigData config;

    public ConfigManager() {
        this.config = new ConfigData();
    }

    public void loadConfig() {
        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                saveConfig();
                return;
            }

            String jsonContent = new String(Files.readAllBytes(CONFIG_PATH));
            JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            // Load configuration with validation
            config = gson.fromJson(jsonObject, ConfigData.class);
            
            // Validate configuration values
            validateConfig();
            
            ChatUtils.sendModMessage("Configuration loaded successfully!");
            
        } catch (Exception e) {
            ChatUtils.sendModMessage("Error loading config, using defaults: " + e.getMessage());
            config = new ConfigData(); // Use defaults
            saveConfig(); // Save defaults
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            
            String jsonContent = gson.toJson(config);
            Files.write(CONFIG_PATH, jsonContent.getBytes());
            
        } catch (IOException e) {
            ChatUtils.sendModMessage("Error saving config: " + e.getMessage());
        }
    }

    private void validateConfig() {
        // Validate distance values
        if (config.itemESPMaxDistance < 10 || config.itemESPMaxDistance > 200) {
            config.itemESPMaxDistance = 80.0f;
        }
        
        if (config.itemESPFadeRange < 5 || config.itemESPFadeRange > config.itemESPMaxDistance) {
            config.itemESPFadeRange = 60.0f;
        }

        // Validate cooldown values
        if (config.generalCooldown < 1000 || config.generalCooldown > 30000) {
            config.generalCooldown = 5000;
        }

        if (config.obsidianCooldown < 1000 || config.obsidianCooldown > 60000) {
            config.obsidianCooldown = 10000;
        }

        if (config.fireballAlertCooldown < 250 || config.fireballAlertCooldown > 5000) {
            config.fireballAlertCooldown = 750;
        }
    }

    // Getters for configuration values
    public boolean isArmorAlertsEnabled() { return config.enableArmorAlerts; }
    public boolean isItemAlertsEnabled() { return config.enableItemAlerts; }
    public boolean isEmeraldAlertsEnabled() { return config.enableEmeraldAlerts; }
    public boolean isSwordAlertsEnabled() { return config.enableSwordAlerts; }
    public boolean isPotionAlertsEnabled() { return config.enablePotionAlerts; }
    public boolean isFireballAlertsEnabled() { return config.enableFireballAlerts; }
    public boolean isObsidianAlertsEnabled() { return config.enableObsidianAlerts; }
    public boolean isFireballTrajectoryAlertsEnabled() { return config.enableFireballTrajectoryAlerts; }
    public boolean isTeammatesExcluded() { return config.excludeTeammates; }
    public boolean isItemESPEnabled() { return config.enableItemESP; }
    
    // NEW: Player Intent Detection
    public boolean isPlayerIntentDetectionEnabled() { return config.enablePlayerIntentDetection; }
    
    public float getItemESPMaxDistance() { return config.itemESPMaxDistance; }
    public float getItemESPFadeRange() { return config.itemESPFadeRange; }
    public long getGeneralCooldown() { return config.generalCooldown; }
    public long getObsidianCooldown() { return config.obsidianCooldown; }
    public long getFireballAlertCooldown() { return config.fireballAlertCooldown; }

    // Setters for configuration values
    public void setArmorAlertsEnabled(boolean enabled) { config.enableArmorAlerts = enabled; }
    public void setItemAlertsEnabled(boolean enabled) { config.enableItemAlerts = enabled; }
    public void setEmeraldAlertsEnabled(boolean enabled) { config.enableEmeraldAlerts = enabled; }
    public void setSwordAlertsEnabled(boolean enabled) { config.enableSwordAlerts = enabled; }
    public void setPotionAlertsEnabled(boolean enabled) { config.enablePotionAlerts = enabled; }
    public void setFireballAlertsEnabled(boolean enabled) { config.enableFireballAlerts = enabled; }
    public void setObsidianAlertsEnabled(boolean enabled) { config.enableObsidianAlerts = enabled; }
    public void setFireballTrajectoryAlertsEnabled(boolean enabled) { config.enableFireballTrajectoryAlerts = enabled; }
    public void setTeammatesExcluded(boolean excluded) { config.excludeTeammates = excluded; }
    public void setItemESPEnabled(boolean enabled) { config.enableItemESP = enabled; }
    
    // NEW: Player Intent Detection setter
    public void setPlayerIntentDetectionEnabled(boolean enabled) { config.enablePlayerIntentDetection = enabled; }
    
    // NEW: Per-Player mute system methods
    public boolean isPlayerMuted(String playerName) { 
        return config.mutedPlayers.contains(playerName.toLowerCase()); 
    }
    
    public void mutePlayer(String playerName) { 
        config.mutedPlayers.add(playerName.toLowerCase()); 
        saveConfig();
    }
    
    public void unmutePlayer(String playerName) { 
        config.mutedPlayers.remove(playerName.toLowerCase()); 
        saveConfig();
    }
    
    public java.util.Set<String> getMutedPlayers() { 
        return new java.util.HashSet<>(config.mutedPlayers); 
    }
    
    public void clearMutedPlayers() { 
        config.mutedPlayers.clear(); 
        saveConfig();
    }
    
    // NEW: Keybind getters and setters
    public int getMuteToggleKey() { return config.muteToggleKey; }
    public int getEspToggleKey() { return config.espToggleKey; }
    public int getConfigKey() { return config.configKey; }
    public int getHistoryToggleKey() { return config.historyToggleKey; }
    
    public void setMuteToggleKey(int key) { config.muteToggleKey = key; saveConfig(); }
    public void setEspToggleKey(int key) { config.espToggleKey = key; saveConfig(); }
    public void setConfigKey(int key) { config.configKey = key; saveConfig(); }
    public void setHistoryToggleKey(int key) { config.historyToggleKey = key; saveConfig(); }
    
    public void setItemESPMaxDistance(float distance) { config.itemESPMaxDistance = distance; }
    public void setItemESPFadeRange(float range) { config.itemESPFadeRange = range; }
    public void setGeneralCooldown(long cooldown) { config.generalCooldown = cooldown; }
    public void setObsidianCooldown(long cooldown) { config.obsidianCooldown = cooldown; }
    public void setFireballAlertCooldown(long cooldown) { config.fireballAlertCooldown = cooldown; }

    /**
     * Inner class to hold configuration data
     */
    public static class ConfigData {
        // Alert toggles
        public boolean enableArmorAlerts = true;
        public boolean enableItemAlerts = true;
        public boolean enableEmeraldAlerts = true;
        public boolean enableSwordAlerts = true;
        public boolean enablePotionAlerts = true;
        public boolean enableFireballAlerts = true;
        public boolean enableObsidianAlerts = true;
        public boolean enableFireballTrajectoryAlerts = true;
        
        // NEW: Advanced detection features
        public boolean enablePlayerIntentDetection = true;
        
        // NEW: Per-Player mute system
        public java.util.Set<String> mutedPlayers = new java.util.HashSet<>();
        
        // NEW: Keybind settings (using Minecraft key codes)
        public int muteToggleKey = 74; // J key
        public int espToggleKey = 75;  // K key  
        public int configKey = 76;     // L key
        public int historyToggleKey = 72; // H key
        
        // Gameplay options
        public boolean excludeTeammates = true;
        public boolean enableItemESP = true;
        
        // Distance and range settings
        public float itemESPMaxDistance = 80.0f;
        public float itemESPFadeRange = 60.0f;
        
        // Cooldown settings (in milliseconds)
        public long generalCooldown = 5000;
        public long obsidianCooldown = 10000;
        public long fireballAlertCooldown = 750;
    }
}
