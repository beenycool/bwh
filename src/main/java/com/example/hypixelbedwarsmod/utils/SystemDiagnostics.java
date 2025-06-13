package com.example.hypixelbedwarsmod.utils;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import net.minecraft.client.Minecraft;

/**
 * System diagnostic and health check utility
 */
public class SystemDiagnostics {
    
    public static boolean performStartupDiagnostics(ConfigManager configManager) {
        boolean allChecksPass = true;
        
        ChatUtils.sendModMessage("Running system diagnostics...");
        
        // Check Minecraft client
        if (Minecraft.getMinecraft() == null) {
            ChatUtils.sendError("Minecraft client not available");
            allChecksPass = false;
        } else {
            ChatUtils.sendSuccess("Minecraft client: OK");
        }
        
        // Check configuration
        if (configManager == null) {
            ChatUtils.sendError("Configuration manager not initialized");
            allChecksPass = false;
        } else {
            ChatUtils.sendSuccess("Configuration manager: OK");
        }
        
        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsagePercent > 80) {
            ChatUtils.sendWarning(String.format("High memory usage: %.1f%%", memoryUsagePercent));
        } else {
            ChatUtils.sendSuccess(String.format("Memory usage: %.1f%% (OK)", memoryUsagePercent));
        }
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        ChatUtils.sendModMessage("Java version: " + javaVersion);
        
        if (allChecksPass) {
            ChatUtils.sendSuccess("All diagnostics passed!");
        } else {
            ChatUtils.sendError("Some diagnostics failed - check console for details");
        }
        
        return allChecksPass;
    }
    
    public static void logPerformanceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // Convert to MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        ChatUtils.sendModMessage(String.format(
            "Performance: Memory %dMB/%dMB (%.1f%%), Processors: %d",
            usedMemory, maxMemory, 
            (double) usedMemory / maxMemory * 100,
            runtime.availableProcessors()
        ));
    }
    
    public static boolean checkModCompatibility() {
        // Check for common mod conflicts
        boolean compatible = true;
        
        try {
            // Check if forge is present (should be)
            Class.forName("net.minecraftforge.fml.common.Mod");
            ChatUtils.sendSuccess("Forge compatibility: OK");
        } catch (ClassNotFoundException e) {
            ChatUtils.sendError("Forge not detected - mod may not work correctly");
            compatible = false;
        }
        
        return compatible;
    }
}
