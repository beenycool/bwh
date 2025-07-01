package com.example.hypixelbedwarsmod.core;

import com.example.hypixelbedwarsmod.detection.FireballPredictor;
import com.example.hypixelbedwarsmod.detection.ItemDetector;
import com.example.hypixelbedwarsmod.detection.PlayerTracker;
import com.example.hypixelbedwarsmod.ui.ConfigScreen;
import com.example.hypixelbedwarsmod.utils.ChatUtils;
import com.example.hypixelbedwarsmod.utils.SystemDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = BedWarsCore.MODID, name = BedWarsCore.NAME, version = BedWarsCore.VERSION)
public class BedWarsCore {
    public static final String MODID = "hypixelbedwarsmod";
    public static final String NAME = "Hypixel Bed Wars Assistant";
    public static final String VERSION = "2.0";

    private static final String CONFIG_CMD = ".bwconfig";
    private static final String HELP_CMD = ".bwhelp";
    private static final String DIAG_CMD = ".bwdiag";

    private EventHandler eventHandler;
    private ConfigManager configManager;
    private PlayerTracker playerTracker;
    private ItemDetector itemDetector;
    private FireballPredictor fireballPredictor;
    
    private static BedWarsCore instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        
        try {
            // Initialize configuration
            configManager = new ConfigManager();
            configManager.loadConfig();

            // Initialize components
            playerTracker = new PlayerTracker(configManager);
            itemDetector = new ItemDetector(configManager);
            fireballPredictor = new FireballPredictor(configManager);
            
            // Initialize event handler
            eventHandler = new EventHandler(configManager, playerTracker, itemDetector, fireballPredictor);

            // Register events
            MinecraftForge.EVENT_BUS.register(this);
            MinecraftForge.EVENT_BUS.register(eventHandler);

            // Run startup diagnostics
            boolean diagnosticsPass = SystemDiagnostics.performStartupDiagnostics(configManager);
            
            if (diagnosticsPass) {
                ChatUtils.sendModMessage("Hypixel Bed Wars Assistant v" + VERSION + " initialized successfully!");
                ChatUtils.sendModMessage("Use " + CONFIG_CMD + " to configure, " + HELP_CMD + " for help");
            } else {
                ChatUtils.sendError("Some initialization checks failed - mod may not work optimally");
            }
            
        } catch (Exception e) {
            ChatUtils.sendError("Failed to initialize mod: " + e.getMessage());
            ChatUtils.sendError("Check console for details. Some features may not work correctly.");
            System.err.println("[BWA] Initialization error: " + e.getMessage());
            e.printStackTrace(); // Keep for debugging purposes
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String unformattedMessage = event.message.getUnformattedText();
        
        if (unformattedMessage.startsWith(CONFIG_CMD)) {
            Minecraft.getMinecraft().displayGuiScreen(new ConfigScreen(configManager));
            event.setCanceled(true);
        } else if (unformattedMessage.startsWith(HELP_CMD)) {
            showHelpMessage();
            event.setCanceled(true);
        } else if (unformattedMessage.startsWith(DIAG_CMD)) {
            SystemDiagnostics.logPerformanceMetrics();
            SystemDiagnostics.checkModCompatibility();
            event.setCanceled(true);
        }
    }
    
    private void showHelpMessage() {
        ChatUtils.sendModMessage("=== Hypixel Bed Wars Assistant v" + VERSION + " ===");
        ChatUtils.sendMessage("Commands:");
        ChatUtils.sendMessage("  " + CONFIG_CMD + " - Open configuration menu");
        ChatUtils.sendMessage("  " + HELP_CMD + " - Show this help message");
        ChatUtils.sendMessage("  " + DIAG_CMD + " - Run system diagnostics");
        ChatUtils.sendMessage("");
        ChatUtils.sendMessage("Features:");
        ChatUtils.sendMessage("  • Player armor and item detection");
        ChatUtils.sendMessage("  • Advanced fireball trajectory prediction");
        ChatUtils.sendMessage("  • Item ESP with distance-based rendering");
        ChatUtils.sendMessage("  • Team-aware filtering and alerts");
        ChatUtils.sendMessage("  • Configurable cooldowns and thresholds");
    }

    public static BedWarsCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public ItemDetector getItemDetector() {
        return itemDetector;
    }

    public FireballPredictor getFireballPredictor() {
        return fireballPredictor;
    }
    
    public EventHandler getEventHandler() {
        return eventHandler;
    }
}
