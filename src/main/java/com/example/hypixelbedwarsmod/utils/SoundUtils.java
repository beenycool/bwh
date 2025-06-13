package com.example.hypixelbedwarsmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for sound operations with cooldown management
 */
public class SoundUtils {
    
    // Sound constants
    public static final String SOUND_ARMOR = "random.orb";
    public static final String SOUND_DIAMOND_SWORD = "random.anvil_use";
    public static final String SOUND_FIREBALL = "mob.ghast.fireball";
    public static final String SOUND_POTION = "random.drink";
    public static final String SOUND_EMERALD = "random.levelup";
    public static final String SOUND_INVIS = "mob.bat.takeoff";
    public static final String SOUND_SPECIAL_ITEM = "random.successful_hit";
    public static final String SOUND_EXPLODE = "random.explode";
    
    private static final Map<String, Long> soundCooldowns = new HashMap<>();
    private static final long DEFAULT_SOUND_COOLDOWN = 1000; // 1 second between same sounds

    /**
     * Play a sound with cooldown management
     */
    public static void playSound(String soundName, float volume, float pitch) {
        String cooldownKey = soundName + "_" + volume + "_" + pitch;
        long currentTime = System.currentTimeMillis();
        
        Long lastPlayed = soundCooldowns.get(cooldownKey);
        if (lastPlayed != null && currentTime - lastPlayed < DEFAULT_SOUND_COOLDOWN) {
            return; // Sound is still on cooldown
        }
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getSoundHandler() != null) {
                PositionedSoundRecord sound = PositionedSoundRecord.create(
                    new ResourceLocation(soundName), volume, pitch
                );
                mc.getSoundHandler().playSound(sound);
                soundCooldowns.put(cooldownKey, currentTime);
            }
        } catch (Exception e) {
            ChatUtils.sendModMessage("Error playing sound: " + e.getMessage());
        }
    }

    /**
     * Play a sound with default volume and pitch
     */
    public static void playSound(String soundName) {
        playSound(soundName, 1.0f, 1.0f);
    }

    /**
     * Play an alert sound (slightly louder)
     */
    public static void playAlertSound(String soundName) {
        playSound(soundName, 1.2f, 1.0f);
    }

    /**
     * Play a warning sound (louder and higher pitch)
     */
    public static void playWarningSound(String soundName) {
        playSound(soundName, 1.5f, 1.2f);
    }

    /**
     * Play a critical alert sound (loudest and highest pitch)
     */
    public static void playCriticalSound(String soundName) {
        playSound(soundName, 2.0f, 1.5f);
    }

    /**
     * Clear sound cooldowns (useful for cleanup)
     */
    public static void clearCooldowns() {
        soundCooldowns.clear();
    }

    /**
     * Clear old cooldowns (older than 30 seconds)
     */
    public static void cleanupOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        soundCooldowns.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 30000);
    }

    /**
     * Set custom cooldown for a sound
     */
    public static void setSoundCooldown(String soundName, float volume, float pitch, long cooldownMs) {
        SoundKey key = new SoundKey(soundName, volume, pitch);
        soundCooldowns.put(key, System.currentTimeMillis() + cooldownMs);
    }

    /**
     * Check if a sound is on cooldown
     */
    public static boolean isSoundOnCooldown(String soundName, float volume, float pitch) {
        SoundKey key = new SoundKey(soundName, volume, pitch);
        Long lastPlayed = soundCooldowns.get(key);
        return lastPlayed != null && 
               System.currentTimeMillis() - lastPlayed < DEFAULT_SOUND_COOLDOWN;
    }
}
