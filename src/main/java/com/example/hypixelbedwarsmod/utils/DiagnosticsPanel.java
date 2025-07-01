package com.example.hypixelbedwarsmod.utils;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * NEW: Advanced diagnostics panel for mod health monitoring
 */
public class DiagnosticsPanel extends Gui {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ConfigManager configManager;
    
    private boolean visible = false;
    private long lastFPSUpdate = 0;
    private int currentFPS = 0;
    private List<DiagnosticEntry> entries = new ArrayList<>();
    private long lastDiagnosticUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second
    
    public DiagnosticsPanel(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public void render() {
        if (!visible || mc.currentScreen != null) return;
        
        updateDiagnostics();
        renderPanel();
    }
    
    public void toggle() {
        visible = !visible;
        if (visible) {
            updateDiagnostics();
        }
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    private void updateDiagnostics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDiagnosticUpdate < UPDATE_INTERVAL) {
            return;
        }
        
        lastDiagnosticUpdate = currentTime;
        entries.clear();
        
        // FPS Information
        updateFPS();
        entries.add(new DiagnosticEntry("FPS", String.valueOf(currentFPS), getFPSHealthColor()));
        
        // Memory Usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        entries.add(new DiagnosticEntry("Memory", 
            usedMemory + "/" + maxMemory + " MB (" + String.format("%.1f", memoryUsagePercent) + "%)",
            getMemoryHealthColor(memoryUsagePercent)));
        
        // JVM Memory Details
        try {
            MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long heapUsed = heapUsage.getUsed() / 1024 / 1024;
            long heapMax = heapUsage.getMax() / 1024 / 1024;
            double heapPercent = (double) heapUsed / heapMax * 100;
            
            entries.add(new DiagnosticEntry("Heap", 
                heapUsed + "/" + heapMax + " MB (" + String.format("%.1f", heapPercent) + "%)",
                getMemoryHealthColor(heapPercent)));
        } catch (Exception e) {
            entries.add(new DiagnosticEntry("Heap", "N/A", 0xFFFFFF));
        }
        
        // Thread Count
        int threadCount = Thread.activeCount();
        entries.add(new DiagnosticEntry("Threads", String.valueOf(threadCount), 
            threadCount > 20 ? 0xFFAA00 : 0x00FF00));
        
        // Mod Status
        entries.add(new DiagnosticEntry("Mod Status", "Active", 0x00FF00));
        
        // Configuration Status
        int enabledFeatures = countEnabledFeatures();
        entries.add(new DiagnosticEntry("Features", enabledFeatures + " enabled", 0x00AAFF));
        
        // Performance Impact Estimation
        String impact = estimatePerformanceImpact();
        int impactColor = getImpactColor(impact);
        entries.add(new DiagnosticEntry("Performance", impact, impactColor));
        
        // World Information
        if (mc.theWorld != null) {
            int entityCount = mc.theWorld.loadedEntityList.size();
            entries.add(new DiagnosticEntry("Entities", String.valueOf(entityCount),
                entityCount > 200 ? 0xFFAA00 : 0x00FF00));
                
            int playerCount = mc.theWorld.playerEntities.size();
            entries.add(new DiagnosticEntry("Players", String.valueOf(playerCount), 0x00AAFF));
        }
        
        // Network Status
        if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
            entries.add(new DiagnosticEntry("Network", "Connected", 0x00FF00));
        } else {
            entries.add(new DiagnosticEntry("Network", "Disconnected", 0xFF0000));
        }
    }
    
    private void updateFPS() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFPSUpdate > 1000) {
            currentFPS = Minecraft.getDebugFPS();
            lastFPSUpdate = currentTime;
        }
    }
    
    private int getFPSHealthColor() {
        if (currentFPS >= 60) return 0x00FF00; // Green
        if (currentFPS >= 30) return 0xFFAA00; // Orange
        return 0xFF0000; // Red
    }
    
    private int getMemoryHealthColor(double percentage) {
        if (percentage < 60) return 0x00FF00; // Green
        if (percentage < 80) return 0xFFAA00; // Orange
        return 0xFF0000; // Red
    }
    
    private int countEnabledFeatures() {
        int count = 0;
        if (configManager.isArmorAlertsEnabled()) count++;
        if (configManager.isItemAlertsEnabled()) count++;
        if (configManager.isEmeraldAlertsEnabled()) count++;
        if (configManager.isSwordAlertsEnabled()) count++;
        if (configManager.isPotionAlertsEnabled()) count++;
        if (configManager.isFireballAlertsEnabled()) count++;
        if (configManager.isObsidianAlertsEnabled()) count++;
        if (configManager.isFireballTrajectoryAlertsEnabled()) count++;
        if (configManager.isPlayerIntentDetectionEnabled()) count++;
        if (configManager.isItemESPEnabled()) count++;
        return count;
    }
    
    private String estimatePerformanceImpact() {
        int enabledFeatures = countEnabledFeatures();
        int impact = 0;
        
        // Calculate impact based on enabled features
        if (configManager.isItemESPEnabled()) impact += 2;
        if (configManager.isFireballTrajectoryAlertsEnabled()) impact += 3;
        if (configManager.isPlayerIntentDetectionEnabled()) impact += 2;
        impact += enabledFeatures; // Base impact per feature
        
        if (impact <= 5) return "Low";
        if (impact <= 10) return "Medium";
        return "High";
    }
    
    private int getImpactColor(String impact) {
        switch (impact) {
            case "Low": return 0x00FF00;
            case "Medium": return 0xFFAA00;
            case "High": return 0xFF0000;
            default: return 0xFFFFFF;
        }
    }
    
    private void renderPanel() {
        FontRenderer fontRenderer = mc.fontRendererObj;
        int screenWidth = mc.displayWidth / 2;
        int screenHeight = mc.displayHeight / 2;
        
        // Panel dimensions
        int panelWidth = 250;
        int lineHeight = 12;
        int panelHeight = entries.size() * lineHeight + 30;
        int panelX = screenWidth - panelWidth - 10;
        int panelY = 10;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        
        // Background
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);
        
        // Border
        drawRect(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        
        // Title
        String title = "BedWars Helper Diagnostics";
        fontRenderer.drawString(title, panelX + 5, panelY + 5, 0xFFFFFF);
        
        // Separator line
        drawRect(panelX + 5, panelY + 18, panelX + panelWidth - 5, panelY + 19, 0xFF555555);
        
        // Diagnostic entries
        int yOffset = panelY + 25;
        for (DiagnosticEntry entry : entries) {
            // Label
            fontRenderer.drawString(entry.label + ":", panelX + 5, yOffset, 0xAAAAAA);
            
            // Value with color coding
            int valueX = panelX + 5 + fontRenderer.getStringWidth(entry.label + ": ");
            fontRenderer.drawString(entry.value, valueX, yOffset, entry.color);
            
            yOffset += lineHeight;
        }
        
        // Footer with instructions
        String instruction = "Press 'I' to toggle";
        fontRenderer.drawString(instruction, panelX + 5, panelY + panelHeight - 12, 0x888888);
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    /**
     * Get diagnostic summary for commands
     */
    public String getDiagnosticSummary() {
        updateDiagnostics();
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== BedWars Helper Diagnostics ===\n");
        
        for (DiagnosticEntry entry : entries) {
            summary.append(entry.label).append(": ").append(entry.value).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Reset diagnostic counters
     */
    public void reset() {
        lastFPSUpdate = 0;
        lastDiagnosticUpdate = 0;
        entries.clear();
    }
    
    /**
     * Diagnostic entry data structure
     */
    private static class DiagnosticEntry {
        final String label;
        final String value;
        final int color;
        
        DiagnosticEntry(String label, String value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }
}