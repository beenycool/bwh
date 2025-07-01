package com.example.hypixelbedwarsmod.utils;

import com.example.hypixelbedwarsmod.utils.ChatUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * NEW: Auto-update checker to notify users of new releases
 */
public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/beenycool/bwh/releases/latest";
    private static final String CURRENT_VERSION = "2.0"; // Update this with actual version
    private static final long CHECK_INTERVAL = 3600000; // 1 hour in milliseconds
    
    private static long lastCheckTime = 0;
    private static boolean updateAvailable = false;
    private static String latestVersion = "";
    private static String downloadUrl = "";
    
    /**
     * Check for updates asynchronously
     */
    public static void checkForUpdatesAsync() {
        long currentTime = System.currentTimeMillis();
        
        // Don't check too frequently
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        AsyncProcessor.runAsync(() -> {
            try {
                JsonObject releaseData = fetchLatestRelease();
                if (releaseData != null) {
                    String latestTag = releaseData.get("tag_name").getAsString();
                    String cleanVersion = latestTag.replace("v", "").replace("V", "");
                    
                    if (isNewerVersion(CURRENT_VERSION, cleanVersion)) {
                        latestVersion = cleanVersion;
                        updateAvailable = true;
                        
                        // Get download URL
                        if (releaseData.has("assets") && 
                            releaseData.get("assets").getAsJsonArray().size() > 0) {
                            downloadUrl = releaseData.get("assets").getAsJsonArray()
                                .get(0).getAsJsonObject()
                                .get("browser_download_url").getAsString();
                        } else {
                            downloadUrl = releaseData.get("html_url").getAsString();
                        }
                        
                        // Notify on main thread
                        net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                            notifyUpdateAvailable();
                        });
                    }
                }
            } catch (Exception e) {
                // Silently fail - don't spam users with update check errors
                System.out.println("Update check failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fetch latest release data from GitHub API
     */
    private static JsonObject fetchLatestRelease() throws Exception {
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "BedWars-Helper-Mod");
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return null;
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return JsonParser.parseString(response.toString()).getAsJsonObject();
    }
    
    /**
     * Compare version strings
     */
    private static boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            int maxLength = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? 
                    Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? 
                    Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (Exception e) {
            return false; // Error parsing versions
        }
    }
    
    /**
     * Notify user of available update
     */
    private static void notifyUpdateAvailable() {
        ChatUtils.sendSuccess("🎉 BedWars Helper Update Available!");
        ChatUtils.sendInfo("Current: v" + CURRENT_VERSION + " → Latest: v" + latestVersion);
        ChatUtils.sendInfo("Download: " + downloadUrl);
        
        // Play a notification sound
        SoundUtils.playAlertSound(SoundUtils.SOUND_EMERALD);
    }
    
    /**
     * Manual update check (for command or GUI button)
     */
    public static void checkNow() {
        lastCheckTime = 0; // Reset cooldown
        checkForUpdatesAsync();
        ChatUtils.sendInfo("Checking for updates...");
    }
    
    /**
     * Get update status
     */
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public static String getLatestVersion() {
        return latestVersion;
    }
    
    public static String getDownloadUrl() {
        return downloadUrl;
    }
    
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * Reset update status (after user acknowledges)
     */
    public static void acknowledgeUpdate() {
        updateAvailable = false;
    }
}