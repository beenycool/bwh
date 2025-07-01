package com.example.hypixelbedwarsmod.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NEW: Async processing utility for heavy detection tasks
 * Keeps the main game thread smooth by offloading intensive operations
 */
public class AsyncProcessor {
    private static final ExecutorService executor;
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    
    static {
        // Create a thread pool for async processing
        executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "BedWars-AsyncProcessor-" + threadCounter.incrementAndGet());
                t.setDaemon(true); // Don't prevent JVM shutdown
                t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority than main thread
                return t;
            }
        });
    }
    
    /**
     * Execute a task asynchronously
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }
    
    /**
     * Execute a task asynchronously and return a result
     */
    public static <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }
    
    /**
     * Execute a task asynchronously with a callback on the main thread
     */
    public static <T> void runAsyncWithCallback(
            java.util.function.Supplier<T> task, 
            java.util.function.Consumer<T> callback) {
        
        CompletableFuture.supplyAsync(task, executor)
            .thenAcceptAsync(callback, runnable -> {
                // Execute callback on main thread (Minecraft thread)
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(runnable);
            });
    }
    
    /**
     * Process scoreboard data asynchronously
     */
    public static void processScoreboardAsync(net.minecraft.scoreboard.Scoreboard scoreboard, 
                                            java.util.function.Consumer<java.util.Map<String, String>> callback) {
        
        supplyAsync(() -> {
            java.util.Map<String, String> teamData = new java.util.HashMap<>();
            
            try {
                // Heavy scoreboard processing
                for (net.minecraft.scoreboard.ScoreObjective objective : scoreboard.getScoreObjectives()) {
                    for (net.minecraft.scoreboard.Score score : scoreboard.getSortedScores(objective)) {
                        String playerName = score.getPlayerName();
                        net.minecraft.scoreboard.ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
                        if (team != null) {
                            teamData.put(playerName.toLowerCase(), team.getRegisteredName());
                        }
                    }
                }
            } catch (Exception e) {
                ChatUtils.sendError("Error processing scoreboard: " + e.getMessage());
            }
            
            return teamData;
        }).thenAcceptAsync(callback, runnable -> {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(runnable);
        });
    }
    
    /**
     * Calculate trajectory predictions asynchronously
     */
    public static void calculateTrajectoriesAsync(
            java.util.List<net.minecraft.entity.projectile.EntitySmallFireball> fireballs,
            net.minecraft.entity.player.EntityPlayer target,
            java.util.function.Consumer<java.util.List<TrajectoryResult>> callback) {
        
        supplyAsync(() -> {
            java.util.List<TrajectoryResult> results = new java.util.ArrayList<>();
            
            for (net.minecraft.entity.projectile.EntitySmallFireball fireball : fireballs) {
                try {
                    // Heavy trajectory calculation
                    TrajectoryResult result = calculateTrajectory(fireball, target);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    // Log error but continue processing
                }
            }
            
            return results;
        }).thenAcceptAsync(callback, runnable -> {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(runnable);
        });
    }
    
    /**
     * Simple trajectory calculation for async processing
     */
    private static TrajectoryResult calculateTrajectory(
            net.minecraft.entity.projectile.EntitySmallFireball fireball,
            net.minecraft.entity.player.EntityPlayer target) {
        
        // Simplified trajectory calculation
        double dx = target.posX - fireball.posX;
        double dy = target.posY - fireball.posY;
        double dz = target.posZ - fireball.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        double speed = Math.sqrt(fireball.motionX * fireball.motionX + 
                               fireball.motionY * fireball.motionY + 
                               fireball.motionZ * fireball.motionZ);
        
        double timeToTarget = distance / speed;
        boolean isDangerous = distance < 10.0 && timeToTarget < 3.0;
        
        return new TrajectoryResult(fireball.getUniqueID(), distance, timeToTarget, isDangerous);
    }
    
    /**
     * Shutdown the executor (call when mod is unloaded)
     */
    public static void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Result class for trajectory calculations
     */
    public static class TrajectoryResult {
        public final java.util.UUID fireballId;
        public final double closestDistance;
        public final double timeToTarget;
        public final boolean isDangerous;
        
        public TrajectoryResult(java.util.UUID fireballId, double closestDistance, 
                              double timeToTarget, boolean isDangerous) {
            this.fireballId = fireballId;
            this.closestDistance = closestDistance;
            this.timeToTarget = timeToTarget;
            this.isDangerous = isDangerous;
        }
    }
}