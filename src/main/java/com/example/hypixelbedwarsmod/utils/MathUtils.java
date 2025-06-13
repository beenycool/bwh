package com.example.hypixelbedwarsmod.utils;

import net.minecraft.entity.Entity;

/**
 * Utility class for mathematical operations
 */
public class MathUtils {

    /**
     * Calculate 3D distance between two entities
     */
    public static double getDistance(Entity entity1, Entity entity2) {
        return getDistance(entity1.posX, entity1.posY, entity1.posZ, 
                          entity2.posX, entity2.posY, entity2.posZ);
    }

    /**
     * Calculate 3D distance between two points
     */
    public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate 2D distance (ignore Y coordinate)
     */
    public static double getDistance2D(Entity entity1, Entity entity2) {
        return getDistance2D(entity1.posX, entity1.posZ, entity2.posX, entity2.posZ);
    }

    /**
     * Calculate 2D distance between two points
     */
    public static double getDistance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculate approach velocity (how fast entity1 is approaching entity2)
     * Positive value means approaching, negative means moving away
     */
    public static double getApproachVelocity(Entity moving, Entity target) {
        // Direction vector from moving entity to target
        double dx = target.posX - moving.posX;
        double dy = target.posY - moving.posY;
        double dz = target.posZ - moving.posZ;
        
        // Normalize direction vector
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1) return 0; // Avoid division by zero
        
        dx /= distance;
        dy /= distance;
        dz /= distance;
        
        // Project velocity onto direction vector
        return dx * moving.motionX + dy * moving.motionY + dz * moving.motionZ;
    }

    /**
     * Predict entity position after given time
     */
    public static double[] predictPosition(Entity entity, double timeInTicks) {
        return new double[]{
            entity.posX + entity.motionX * timeInTicks,
            entity.posY + entity.motionY * timeInTicks,
            entity.posZ + entity.motionZ * timeInTicks
        };
    }

    /**
     * Calculate closest approach distance between two moving entities
     */
    public static double getClosestApproach(Entity entity1, Entity entity2, int maxTicks) {
        double closestDistance = Double.MAX_VALUE;
        
        double x1 = entity1.posX, y1 = entity1.posY, z1 = entity1.posZ;
        double x2 = entity2.posX, y2 = entity2.posY, z2 = entity2.posZ;
        
        for (int i = 0; i < maxTicks; i++) {
            x1 += entity1.motionX;
            y1 += entity1.motionY;
            z1 += entity1.motionZ;
            
            x2 += entity2.motionX;
            y2 += entity2.motionY;
            z2 += entity2.motionZ;
            
            double distance = getDistance(x1, y1, z1, x2, y2, z2);
            if (distance < closestDistance) {
                closestDistance = distance;
            }
        }
        
        return closestDistance;
    }

    /**
     * Clamp a value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a float value between min and max
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linear interpolation
     */
    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Check if a point is within a certain range of another point
     */
    public static boolean isWithinRange(double x1, double y1, double z1, 
                                       double x2, double y2, double z2, double range) {
        return getDistance(x1, y1, z1, x2, y2, z2) <= range;
    }

    /**
     * Calculate angle between two vectors
     */
    public static double getAngleBetween(double x1, double y1, double z1, 
                                        double x2, double y2, double z2) {
        double dot = x1 * x2 + y1 * y2 + z1 * z2;
        double mag1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        double mag2 = Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
        
        if (mag1 == 0 || mag2 == 0) return 0;
        
        return Math.acos(clamp(dot / (mag1 * mag2), -1.0, 1.0));
    }

    /**
     * Convert radians to degrees
     */
    public static double toDegrees(double radians) {
        return radians * 180.0 / Math.PI;
    }

    /**
     * Convert degrees to radians
     */
    public static double toRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }
}
