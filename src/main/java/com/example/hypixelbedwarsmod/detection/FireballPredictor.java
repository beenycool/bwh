package com.example.hypixelbedwarsmod.detection;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import com.example.hypixelbedwarsmod.utils.ChatUtils;
import com.example.hypixelbedwarsmod.utils.MathUtils;
import com.example.hypixelbedwarsmod.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced fireball trajectory prediction and alerting system
 */
public class FireballPredictor {
    private static final double DANGER_THRESHOLD = 4.0; // Distance threshold for danger
    private static final double DIRECT_HIT_THRESHOLD = 2.0; // Distance threshold for direct hit
    private static final int PREDICTION_TICKS = 20; // Predict 1 second ahead
    private static final long ANALYSIS_COOLDOWN = 500; // Half second between analyses per fireball
    
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, Long> fireballAlertTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> analyzedFireballs = new ConcurrentHashMap<>();
    
    public FireballPredictor(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void trackFireballs(World world, EntityPlayer localPlayer) {
        if (!configManager.isFireballTrajectoryAlertsEnabled()) return;
        
        long currentTime = System.currentTimeMillis();
        
        for (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntitySmallFireball)) continue;
            
            EntitySmallFireball fireball = (EntitySmallFireball) entity;
            UUID fireballId = fireball.getUniqueID();
            
            // Skip if recently analyzed
            Long lastAnalysis = analyzedFireballs.get(fireballId);
            if (lastAnalysis != null && currentTime - lastAnalysis < ANALYSIS_COOLDOWN) {
                continue;
            }
            
            // Skip distant fireballs for performance
            double distanceToPlayer = fireball.getDistanceToEntity(localPlayer);
            if (distanceToPlayer > 30.0) continue;
            
            // Analyze fireball trajectory
            analyzeFireballTrajectory(fireball, localPlayer, currentTime);
            analyzedFireballs.put(fireballId, currentTime);
        }
        
        // Cleanup old data
        cleanup();
    }

    private void analyzeFireballTrajectory(EntitySmallFireball fireball, EntityPlayer localPlayer, long currentTime) {
        UUID fireballId = fireball.getUniqueID();
        
        // Calculate fireball motion and player position
        double motionX = fireball.motionX;
        double motionY = fireball.motionY;
        double motionZ = fireball.motionZ;
        
        double playerX = localPlayer.posX;
        double playerY = localPlayer.posY + 1.0; // Aim for mid-height
        double playerZ = localPlayer.posZ;
        
        // Predict trajectory and find closest approach
        TrajectoryAnalysis analysis = predictTrajectory(fireball, playerX, playerY, playerZ);
        
        if (analysis.isDangerous) {
            sendFireballAlert(fireball, localPlayer, analysis, currentTime);
        }
    }

    private TrajectoryAnalysis predictTrajectory(EntitySmallFireball fireball, double playerX, double playerY, double playerZ) {
        double simulatedX = fireball.posX;
        double simulatedY = fireball.posY;
        double simulatedZ = fireball.posZ;
        
        double motionX = fireball.motionX;
        double motionY = fireball.motionY;
        double motionZ = fireball.motionZ;
        
        double closestApproach = Double.MAX_VALUE;
        boolean directHitLikely = false;
        boolean isDangerous = false;
        int ticksToClosest = 0;
        
        for (int i = 0; i < PREDICTION_TICKS; i++) {
            simulatedX += motionX;
            simulatedY += motionY;
            simulatedZ += motionZ;
            
            double distance = MathUtils.getDistance(simulatedX, simulatedY, simulatedZ, playerX, playerY, playerZ);
            
            if (distance < closestApproach) {
                closestApproach = distance;
                ticksToClosest = i;
            }
            
            if (distance < DIRECT_HIT_THRESHOLD) {
                directHitLikely = true;
                break;
            }
            
            if (distance < DANGER_THRESHOLD) {
                isDangerous = true;
            }
        }
        
        // Calculate approach velocity
        double approachVelocity = MathUtils.getApproachVelocity(fireball, 
            createVirtualEntity(playerX, playerY, playerZ));
        
        return new TrajectoryAnalysis(closestApproach, directHitLikely, isDangerous, 
                                    approachVelocity > 0, ticksToClosest, approachVelocity);
    }

    private void sendFireballAlert(EntitySmallFireball fireball, EntityPlayer localPlayer, 
                                 TrajectoryAnalysis analysis, long currentTime) {
        UUID fireballId = fireball.getUniqueID();
        
        // Check alert cooldown
        Long lastAlert = fireballAlertTimestamps.get(fireballId);
        if (lastAlert != null && currentTime - lastAlert < configManager.getFireballAlertCooldown()) {
            return;
        }
        
        // Only alert if fireball is approaching
        if (!analysis.isApproaching) return;
        
        String shooterName = getShooterName(fireball);
        String message;
        String sound;
        EnumChatFormatting color;
        
        if (analysis.directHitLikely) {
            message = EnumChatFormatting.RED + "⚠ FIREBALL INCOMING! MOVE NOW! ⚠";
            sound = SoundUtils.SOUND_EXPLODE;
            color = EnumChatFormatting.RED;
            SoundUtils.playCriticalSound(sound);
        } else if (analysis.closestApproach < 3.0) {
            message = EnumChatFormatting.GOLD + "⚠ Fireball nearby! Dodge! ⚠";
            sound = SoundUtils.SOUND_FIREBALL;
            color = EnumChatFormatting.GOLD;
            SoundUtils.playWarningSound(sound);
        } else {
            message = EnumChatFormatting.YELLOW + "Fireball approaching from " + shooterName;
            sound = SoundUtils.SOUND_FIREBALL;
            color = EnumChatFormatting.YELLOW;
            SoundUtils.playAlertSound(sound);
        }
        
        // Add distance and timing information
        double currentDistance = fireball.getDistanceToEntity(localPlayer);
        double timeToImpact = analysis.ticksToClosest / 20.0; // Convert ticks to seconds
        
        String detailedMessage = message + String.format(" (%.1fm, %.1fs)", currentDistance, timeToImpact);
        
        ChatUtils.sendAlert(detailedMessage, color);
        fireballAlertTimestamps.put(fireballId, currentTime);
    }

    private String getShooterName(EntitySmallFireball fireball) {
        Entity shooter = fireball.shootingEntity;
        if (shooter instanceof EntityPlayer) {
            return ((EntityPlayer) shooter).getName();
        }
        return "unknown";
    }

    private Entity createVirtualEntity(double x, double y, double z) {
        // Create a virtual entity at the player's position for calculations
        // This is a simplified approach for calculation purposes
        return new Entity(null) {
            {
                this.posX = x;
                this.posY = y;
                this.posZ = z;
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;
            }
            
            @Override
            protected void entityInit() {}
            
            @Override
            protected void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound compound) {}
            
            @Override
            protected void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound compound) {}
        };
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = 10000; // 10 seconds
        
        // Remove old fireballs from tracking maps
        fireballAlertTimestamps.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expirationTime);
        analyzedFireballs.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expirationTime);
    }

    /**
     * Inner class to hold trajectory analysis results
     */
    private static class TrajectoryAnalysis {
        final double closestApproach;
        final boolean directHitLikely;
        final boolean isDangerous;
        final boolean isApproaching;
        final int ticksToClosest;
        final double approachVelocity;
        
        TrajectoryAnalysis(double closestApproach, boolean directHitLikely, boolean isDangerous,
                          boolean isApproaching, int ticksToClosest, double approachVelocity) {
            this.closestApproach = closestApproach;
            this.directHitLikely = directHitLikely;
            this.isDangerous = isDangerous;
            this.isApproaching = isApproaching;
            this.ticksToClosest = ticksToClosest;
            this.approachVelocity = approachVelocity;
        }
    }
}
