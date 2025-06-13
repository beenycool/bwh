package com.example.hypixelbedwarsmod.core;

import com.example.hypixelbedwarsmod.detection.FireballPredictor;
import com.example.hypixelbedwarsmod.detection.ItemDetector;
import com.example.hypixelbedwarsmod.detection.PlayerTracker;
import com.example.hypixelbedwarsmod.ui.AlertOverlay;
import com.example.hypixelbedwarsmod.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Pattern;

/**
 * Centralized event handler with optimized performance
 */
public class EventHandler {
    private static final Pattern BEDWARS_TITLE = Pattern.compile("BED WARS", Pattern.CASE_INSENSITIVE);
    
    private final ConfigManager configManager;
    private final PlayerTracker playerTracker;
    private final ItemDetector itemDetector;
    private final FireballPredictor fireballPredictor;
    private final AlertOverlay alertOverlay;
    
    private boolean inBedWarsGame = false;
    private int tickCounter = 0;
    private static final int TICK_INTERVAL = 20; // Process every second instead of every tick

    public EventHandler(ConfigManager configManager, PlayerTracker playerTracker, 
                       ItemDetector itemDetector, FireballPredictor fireballPredictor) {
        this.configManager = configManager;
        this.playerTracker = playerTracker;
        this.itemDetector = itemDetector;
        this.fireballPredictor = fireballPredictor;
        this.alertOverlay = new AlertOverlay();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Check if we're in a Bed Wars game
        checkBedWarsGame(mc);

        // Only process every TICK_INTERVAL ticks to reduce performance impact
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        if (!inBedWarsGame) return;

        World world = mc.theWorld;
        EntityPlayer localPlayer = mc.thePlayer;

        try {
            // Update player tracking
            playerTracker.updatePlayers(world, localPlayer);

            // Process item detection with distance culling
            processNearbyPlayers(world, localPlayer);

            // Track fireballs if enabled
            if (configManager.isFireballTrajectoryAlertsEnabled()) {
                fireballPredictor.trackFireballs(world, localPlayer);
            }

            // Cleanup old data periodically
            if (tickCounter % (TICK_INTERVAL * 30) == 0) { // Every 30 seconds
                playerTracker.cleanup();
                fireballPredictor.cleanup();
            }

        } catch (Exception e) {
            // Prevent crashes from affecting gameplay
            ChatUtils.sendModMessage("Error in event processing: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!inBedWarsGame || !configManager.isItemESPEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        try {
            itemDetector.renderItemESP(mc.theWorld, mc.thePlayer, event.partialTicks);
        } catch (Exception e) {
            // Prevent rendering errors from crashing
            ChatUtils.sendModMessage("Error in ESP rendering: " + e.getMessage());
        }
    }

    private void checkBedWarsGame(Minecraft mc) {
        boolean wasInGame = inBedWarsGame;
        inBedWarsGame = false;

        if (mc.thePlayer != null && mc.thePlayer.worldObj != null) {
            ScoreObjective objective = mc.thePlayer.worldObj.getScoreboard().getObjectiveInDisplaySlot(1);
            if (objective != null && BEDWARS_TITLE.matcher(objective.getDisplayName()).find()) {
                inBedWarsGame = true;
            }
        }

        // Notify when entering/leaving Bed Wars
        if (inBedWarsGame && !wasInGame) {
            ChatUtils.sendModMessage("Bed Wars game detected! Alerts enabled.");
            playerTracker.reset();
        } else if (!inBedWarsGame && wasInGame) {
            ChatUtils.sendModMessage("Left Bed Wars game. Alerts disabled.");
            playerTracker.reset();
            fireballPredictor.cleanup();
        }
    }

    private void processNearbyPlayers(World world, EntityPlayer localPlayer) {
        double maxDetectionRange = Math.max(configManager.getItemESPMaxDistance(), 50.0);
        
        for (EntityPlayer player : world.playerEntities) {
            if (player == localPlayer) continue;
            
            // Distance-based culling for performance
            double distance = player.getDistanceToEntity(localPlayer);
            if (distance > maxDetectionRange) continue;

            // Process player detection
            playerTracker.processPlayer(player, localPlayer);
            
            // Process item alerts if enabled
            if (configManager.isItemAlertsEnabled()) {
                itemDetector.processPlayer(player, localPlayer);
            }
        }
    }

    public boolean isInBedWarsGame() {
        return inBedWarsGame;
    }

    public AlertOverlay getAlertOverlay() {
        return alertOverlay;
    }
}
