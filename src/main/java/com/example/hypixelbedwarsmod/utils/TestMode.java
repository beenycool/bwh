package com.example.hypixelbedwarsmod.utils;

import com.example.hypixelbedwarsmod.ui.AlertOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * NEW: Test mode for simulating various in-game scenarios for rapid testing
 */
public class TestMode {
    
    private final AlertOverlay alertOverlay;
    private final Random random = new Random();
    private boolean testModeActive = false;
    private long lastTestEvent = 0;
    private int testScenarioIndex = 0;
    
    // Test scenarios
    private static final String[] TEST_SCENARIOS = {
        "Enemy with TNT detected",
        "Player approaching with pearls", 
        "Diamond armor upgrade detected",
        "Enemy has invisibility potion",
        "Fireball incoming - HIGH THREAT",
        "Bridge egg usage detected",
        "Magic milk activated",
        "Rush intent detected - CRITICAL",
        "Obsidian placement warning",
        "Speed potion effect detected"
    };
    
    private static final AlertOverlay.AlertType[] TEST_ALERT_TYPES = {
        AlertOverlay.AlertType.DANGER,
        AlertOverlay.AlertType.WARNING,
        AlertOverlay.AlertType.ARMOR,
        AlertOverlay.AlertType.WARNING,
        AlertOverlay.AlertType.FIREBALL,
        AlertOverlay.AlertType.ITEM,
        AlertOverlay.AlertType.ITEM,
        AlertOverlay.AlertType.DANGER,
        AlertOverlay.AlertType.WARNING,
        AlertOverlay.AlertType.INFO
    };
    
    public TestMode(AlertOverlay alertOverlay) {
        this.alertOverlay = alertOverlay;
    }
    
    /**
     * Start test mode
     */
    public void startTestMode() {
        testModeActive = true;
        testScenarioIndex = 0;
        lastTestEvent = System.currentTimeMillis();
        
        ChatUtils.sendSuccess("🧪 Test Mode Activated");
        ChatUtils.sendInfo("Simulating BedWars scenarios...");
        ChatUtils.sendInfo("Use '/bwh test stop' to deactivate");
        
        SoundUtils.playAlertSound(SoundUtils.SOUND_EMERALD);
    }
    
    /**
     * Stop test mode
     */
    public void stopTestMode() {
        testModeActive = false;
        
        ChatUtils.sendInfo("Test Mode Deactivated");
        SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
    }
    
    /**
     * Check if test mode is active
     */
    public boolean isActive() {
        return testModeActive;
    }
    
    /**
     * Update test mode - call this from main event loop
     */
    public void update() {
        if (!testModeActive) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Trigger test events every 3-5 seconds
        if (currentTime - lastTestEvent > (3000 + random.nextInt(2000))) {
            triggerTestEvent();
            lastTestEvent = currentTime;
        }
    }
    
    /**
     * Trigger a random test event
     */
    private void triggerTestEvent() {
        if (testScenarioIndex >= TEST_SCENARIOS.length) {
            // Cycle back to beginning
            testScenarioIndex = 0;
        }
        
        String scenario = TEST_SCENARIOS[testScenarioIndex];
        AlertOverlay.AlertType alertType = TEST_ALERT_TYPES[testScenarioIndex];
        
        // Create test alert
        String testMessage = generateTestMessage(scenario);
        alertOverlay.addAlert(testMessage, alertType);
        
        // Play appropriate sound
        String sound = getTestSound(alertType);
        SoundUtils.playAlertSound(sound);
        
        // Send to chat as well
        ChatUtils.sendAlert("[TEST] " + testMessage, getTestChatColor(alertType));
        
        testScenarioIndex++;
    }
    
    /**
     * Trigger specific test scenario
     */
    public void triggerScenario(String scenarioName) {
        switch (scenarioName.toLowerCase()) {
            case "tnt":
                simulatePlayerWithTNT();
                break;
            case "pearl":
                simulatePlayerWithPearls();
                break;
            case "rush":
                simulateRushIntent();
                break;
            case "armor":
                simulateArmorUpgrade();
                break;
            case "fireball":
                simulateFireball();
                break;
            case "potion":
                simulatePotion();
                break;
            case "bridge":
                simulateBridgeEgg();
                break;
            case "all":
                simulateAllScenarios();
                break;
            default:
                ChatUtils.sendError("Unknown test scenario: " + scenarioName);
                ChatUtils.sendInfo("Available: tnt, pearl, rush, armor, fireball, potion, bridge, all");
        }
    }
    
    /**
     * Generate realistic test message with fake player
     */
    private String generateTestMessage(String scenario) {
        String[] testPlayers = {"TestPlayer", "EnemyBot", "RushGuy", "PvPMaster", "BuilderPro"};
        String player = testPlayers[random.nextInt(testPlayers.length)];
        int distance = 10 + random.nextInt(30); // 10-40 blocks
        
        return EnumChatFormatting.RED + player + EnumChatFormatting.RESET + 
               " - " + scenario + " [" + distance + "m]";
    }
    
    /**
     * Get appropriate test sound
     */
    private String getTestSound(AlertOverlay.AlertType alertType) {
        switch (alertType) {
            case DANGER:
                return SoundUtils.SOUND_EXPLODE;
            case FIREBALL:
                return SoundUtils.SOUND_FIREBALL;
            case ARMOR:
                return SoundUtils.SOUND_ARMOR;
            case ITEM:
                return SoundUtils.SOUND_SPECIAL_ITEM;
            case WARNING:
                return SoundUtils.SOUND_POTION;
            default:
                return SoundUtils.SOUND_SPECIAL_ITEM;
        }
    }
    
    /**
     * Get chat color for test alert type
     */
    private EnumChatFormatting getTestChatColor(AlertOverlay.AlertType alertType) {
        switch (alertType) {
            case DANGER:
                return EnumChatFormatting.RED;
            case WARNING:
                return EnumChatFormatting.YELLOW;
            case FIREBALL:
                return EnumChatFormatting.GOLD;
            case ARMOR:
                return EnumChatFormatting.AQUA;
            case ITEM:
                return EnumChatFormatting.GREEN;
            default:
                return EnumChatFormatting.WHITE;
        }
    }
    
    // Specific scenario simulators
    private void simulatePlayerWithTNT() {
        alertOverlay.addAlert(generateTestMessage("carrying TNT - RUSH THREAT!"), AlertOverlay.AlertType.DANGER);
        SoundUtils.playWarningSound(SoundUtils.SOUND_EXPLODE);
        ChatUtils.sendAlert("[TEST] TNT threat simulation", EnumChatFormatting.RED);
    }
    
    private void simulatePlayerWithPearls() {
        alertOverlay.addAlert(generateTestMessage("has ENDER PEARLS"), AlertOverlay.AlertType.WARNING);
        SoundUtils.playAlertSound(SoundUtils.SOUND_SPECIAL_ITEM);
        ChatUtils.sendAlert("[TEST] Ender pearl detection", EnumChatFormatting.YELLOW);
    }
    
    private void simulateRushIntent() {
        alertOverlay.addAlert(generateTestMessage("INCOMING RUSH! (TNT/Pearls)"), AlertOverlay.AlertType.DANGER);
        SoundUtils.playCriticalSound(SoundUtils.SOUND_EXPLODE);
        ChatUtils.sendAlert("[TEST] Rush intent prediction", EnumChatFormatting.DARK_RED);
    }
    
    private void simulateArmorUpgrade() {
        alertOverlay.addAlert(generateTestMessage("bought Diamond Armor!"), AlertOverlay.AlertType.ARMOR);
        SoundUtils.playSound(SoundUtils.SOUND_ARMOR);
        ChatUtils.sendAlert("[TEST] Armor upgrade detected", EnumChatFormatting.AQUA);
    }
    
    private void simulateFireball() {
        alertOverlay.addAlert("FIREBALL WARNING - INCOMING!", AlertOverlay.AlertType.FIREBALL);
        SoundUtils.playWarningSound(SoundUtils.SOUND_FIREBALL);
        ChatUtils.sendAlert("[TEST] Fireball trajectory alert", EnumChatFormatting.GOLD);
    }
    
    private void simulatePotion() {
        alertOverlay.addAlert(generateTestMessage("is INVISIBLE!"), AlertOverlay.AlertType.WARNING);
        SoundUtils.playSound(SoundUtils.SOUND_INVIS);
        ChatUtils.sendAlert("[TEST] Potion effect detection", EnumChatFormatting.GRAY);
    }
    
    private void simulateBridgeEgg() {
        alertOverlay.addAlert(generateTestMessage("has BRIDGE EGG"), AlertOverlay.AlertType.ITEM);
        SoundUtils.playSound(SoundUtils.SOUND_SPECIAL_ITEM);
        ChatUtils.sendAlert("[TEST] Bridge egg detection", EnumChatFormatting.YELLOW);
    }
    
    private void simulateAllScenarios() {
        ChatUtils.sendInfo("Simulating all scenarios in sequence...");
        
        // Trigger all scenarios with delays
        AsyncProcessor.runAsync(() -> {
            try {
                simulatePlayerWithTNT();
                Thread.sleep(1000);
                
                simulatePlayerWithPearls();
                Thread.sleep(1000);
                
                simulateRushIntent();
                Thread.sleep(1000);
                
                simulateArmorUpgrade();
                Thread.sleep(1000);
                
                simulateFireball();
                Thread.sleep(1000);
                
                simulatePotion();
                Thread.sleep(1000);
                
                simulateBridgeEgg();
                
                // Notify completion on main thread
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    ChatUtils.sendSuccess("All test scenarios completed!");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Display available test commands
     */
    public void showHelp() {
        ChatUtils.sendInfo("=== BedWars Helper Test Mode ===");
        ChatUtils.sendInfo("/bwh test start - Start automatic testing");
        ChatUtils.sendInfo("/bwh test stop - Stop testing");
        ChatUtils.sendInfo("/bwh test <scenario> - Test specific scenario");
        ChatUtils.sendInfo("Scenarios: tnt, pearl, rush, armor, fireball, potion, bridge, all");
    }
}