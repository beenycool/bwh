package com.example.hypixelbedwarsmod.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Advanced overlay system for displaying alerts and information
 */
public class AlertOverlay extends Gui {
    private final List<AlertMessage> activeAlerts = new ArrayList<>();
    // NEW: Alert History for persistent tracking
    private final List<AlertMessage> alertHistory = new ArrayList<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private static final int MAX_ALERTS = 5;
    private static final int MAX_HISTORY = 50; // Keep last 50 alerts in history
    private static final int ALERT_DURATION = 5000; // 5 seconds
    private static final int FADE_DURATION = 1000; // 1 second fade
    
    // NEW: History panel display state
    private boolean showHistoryPanel = false;
    
    public void addAlert(String message, AlertType type) {
        AlertMessage alert = new AlertMessage(message, type, System.currentTimeMillis());
        
        // Remove oldest alert if we're at capacity
        if (activeAlerts.size() >= MAX_ALERTS) {
            activeAlerts.remove(0);
        }
        
        activeAlerts.add(alert);
        
        // NEW: Add to history
        alertHistory.add(alert);
        
        // Maintain history size limit
        if (alertHistory.size() > MAX_HISTORY) {
            alertHistory.remove(0);
        }
    }

    public void render() {
        if (mc.currentScreen != null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Remove expired active alerts
        Iterator<AlertMessage> iterator = activeAlerts.iterator();
        while (iterator.hasNext()) {
            AlertMessage alert = iterator.next();
            if (currentTime - alert.timestamp > ALERT_DURATION) {
                iterator.remove();
            }
        }
        
        // Render active alerts
        if (!activeAlerts.isEmpty()) {
            renderAlerts(currentTime);
        }
        
        // NEW: Render history panel if enabled
        if (showHistoryPanel) {
            renderHistoryPanel(currentTime);
        }
    }

    private void renderAlerts(long currentTime) {
        if (activeAlerts.isEmpty()) return;
        
        FontRenderer fontRenderer = mc.fontRendererObj;
        int screenWidth = mc.displayWidth / 2; // Scaled width
        int screenHeight = mc.displayHeight / 2; // Scaled height
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        
        int yOffset = 10;
        for (int i = activeAlerts.size() - 1; i >= 0; i--) {
            AlertMessage alert = activeAlerts.get(i);
            float alpha = calculateAlpha(alert, currentTime);
            
            if (alpha <= 0) continue;
            
            renderAlert(alert, yOffset, alpha, fontRenderer, screenWidth);
            yOffset += 25;
        }
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderAlert(AlertMessage alert, int yOffset, float alpha, FontRenderer fontRenderer, int screenWidth) {
        String message = alert.message;
        int messageWidth = fontRenderer.getStringWidth(message);
        int x = screenWidth - messageWidth - 10;
        int y = yOffset;
        
        // Background
        int backgroundColor = (int)(alpha * 128) << 24; // Semi-transparent black
        drawRect(x - 5, y - 2, x + messageWidth + 5, y + fontRenderer.FONT_HEIGHT + 2, backgroundColor);
        
        // Border
        int borderColor = alert.type.getColor() | ((int)(alpha * 255) << 24);
        drawRect(x - 6, y - 3, x - 5, y + fontRenderer.FONT_HEIGHT + 3, borderColor);
        
        // Text
        int textColor = 0xFFFFFF | ((int)(alpha * 255) << 24);
        fontRenderer.drawString(message, x, y, textColor);
    }

    private float calculateAlpha(AlertMessage alert, long currentTime) {
        long age = currentTime - alert.timestamp;
        
        if (age < ALERT_DURATION - FADE_DURATION) {
            return 1.0f; // Full opacity
        } else {
            // Fade out
            long fadeTime = age - (ALERT_DURATION - FADE_DURATION);
            return Math.max(0, 1.0f - (float)fadeTime / FADE_DURATION);
        }
    }

    public void clear() {
        activeAlerts.clear();
    }

    public int getActiveAlertCount() {
        return activeAlerts.size();
    }
    
    // NEW: History panel methods
    public void toggleHistoryPanel() {
        showHistoryPanel = !showHistoryPanel;
    }
    
    public void setHistoryPanelVisible(boolean visible) {
        showHistoryPanel = visible;
    }
    
    public boolean isHistoryPanelVisible() {
        return showHistoryPanel;
    }
    
    public List<AlertMessage> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
    
    public void clearHistory() {
        alertHistory.clear();
    }
    
    /**
     * NEW: Render the alert history panel
     */
    private void renderHistoryPanel(long currentTime) {
        if (alertHistory.isEmpty()) return;
        
        FontRenderer fontRenderer = mc.fontRendererObj;
        int screenWidth = mc.displayWidth / 2;
        int screenHeight = mc.displayHeight / 2;
        
        // Panel dimensions
        int panelWidth = 300;
        int panelHeight = 200;
        int panelX = 10;
        int panelY = screenHeight - panelHeight - 10;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        
        // Background
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);
        
        // Border
        drawRect(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        
        // Title
        String title = "Alert History (" + alertHistory.size() + ")";
        fontRenderer.drawString(title, panelX + 5, panelY + 5, 0xFFFFFF);
        
        // Alerts (show last 10)
        int startIndex = Math.max(0, alertHistory.size() - 10);
        int yOffset = panelY + 20;
        
        for (int i = startIndex; i < alertHistory.size() && yOffset < panelY + panelHeight - 15; i++) {
            AlertMessage alert = alertHistory.get(i);
            
            // Format timestamp
            long minutesAgo = (currentTime - alert.timestamp) / 60000;
            String timeText = minutesAgo < 1 ? "now" : minutesAgo + "m ago";
            
            // Truncate message if too long
            String message = alert.message;
            if (fontRenderer.getStringWidth(message) > panelWidth - 60) {
                message = fontRenderer.trimStringToWidth(message, panelWidth - 60) + "...";
            }
            
            // Draw time
            fontRenderer.drawString(timeText, panelX + 5, yOffset, 0xAAAAAA);
            
            // Draw message with type color
            fontRenderer.drawString(message, panelX + 50, yOffset, alert.type.getColor());
            
            yOffset += 12;
        }
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    // NEW: History panel methods
    public void toggleHistoryPanel() {
        showHistoryPanel = !showHistoryPanel;
    }
    
    public void setHistoryPanelVisible(boolean visible) {
        showHistoryPanel = visible;
    }
    
    public boolean isHistoryPanelVisible() {
        return showHistoryPanel;
    }
    
    public List<AlertMessage> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
    
    public void clearHistory() {
        alertHistory.clear();
    }
    
    /**
     * NEW: Render the alert history panel
     */
    private void renderHistoryPanel(long currentTime) {
        if (alertHistory.isEmpty()) return;
        
        FontRenderer fontRenderer = mc.fontRendererObj;
        int screenWidth = mc.displayWidth / 2;
        int screenHeight = mc.displayHeight / 2;
        
        // Panel dimensions
        int panelWidth = 300;
        int panelHeight = 200;
        int panelX = 10;
        int panelY = screenHeight - panelHeight - 10;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        
        // Background
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);
        
        // Border
        drawRect(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        
        // Title
        String title = "Alert History (" + alertHistory.size() + ")";
        fontRenderer.drawString(title, panelX + 5, panelY + 5, 0xFFFFFF);
        
        // Alerts (show last 10)
        int startIndex = Math.max(0, alertHistory.size() - 10);
        int yOffset = panelY + 20;
        
        for (int i = startIndex; i < alertHistory.size() && yOffset < panelY + panelHeight - 15; i++) {
            AlertMessage alert = alertHistory.get(i);
            
            // Format timestamp
            long minutesAgo = (currentTime - alert.timestamp) / 60000;
            String timeText = minutesAgo < 1 ? "now" : minutesAgo + "m ago";
            
            // Truncate message if too long
            String message = alert.message;
            if (fontRenderer.getStringWidth(message) > panelWidth - 60) {
                message = fontRenderer.trimStringToWidth(message, panelWidth - 60) + "...";
            }
            
            // Draw time
            fontRenderer.drawString(timeText, panelX + 5, yOffset, 0xAAAAAA);
            
            // Draw message with type color
            fontRenderer.drawString(message, panelX + 50, yOffset, alert.type.getColor());
            
            yOffset += 12;
        }
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Alert message data structure
     */
    private static class AlertMessage {
        final String message;
        final AlertType type;
        final long timestamp;

        AlertMessage(String message, AlertType type, long timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    /**
     * Types of alerts with associated colors
     */
    public enum AlertType {
        INFO(0x00AAFF),      // Blue
        WARNING(0xFFAA00),   // Orange
        DANGER(0xFF0000),    // Red
        SUCCESS(0x00FF00),   // Green
        FIREBALL(0xFF6600),  // Orange-red
        ARMOR(0x00FFFF),     // Cyan
        ITEM(0xFFFF00);      // Yellow

        private final int color;

        AlertType(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}
