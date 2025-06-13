package com.example.hypixelbedwarsmod.data;

import net.minecraft.item.Item;

/**
 * Tracks the state of a player for detection purposes
 */
public class PlayerState {
    private Item lastHeldItem;
    private boolean hasDiamondArmor;
    private boolean hasAlertedArmor;
    private int lastEmeraldCount;
    private long lastArmorCheck;
    private long lastItemCheck;
    private long lastPotionCheck;

    public PlayerState() {
        this.lastHeldItem = null;
        this.hasDiamondArmor = false;
        this.hasAlertedArmor = false;
        this.lastEmeraldCount = 0;
        this.lastArmorCheck = 0;
        this.lastItemCheck = 0;
        this.lastPotionCheck = 0;
    }

    public Item getLastHeldItem() {
        return lastHeldItem;
    }

    public void setLastHeldItem(Item lastHeldItem) {
        this.lastHeldItem = lastHeldItem;
        this.lastItemCheck = System.currentTimeMillis();
    }

    public boolean hasDiamondArmor() {
        return hasDiamondArmor;
    }

    public void setHasDiamondArmor(boolean hasDiamondArmor) {
        this.hasDiamondArmor = hasDiamondArmor;
        this.lastArmorCheck = System.currentTimeMillis();
    }

    public boolean hasAlertedArmor() {
        return hasAlertedArmor;
    }

    public void setHasAlertedArmor(boolean hasAlertedArmor) {
        this.hasAlertedArmor = hasAlertedArmor;
    }

    public int getLastEmeraldCount() {
        return lastEmeraldCount;
    }

    public void setLastEmeraldCount(int lastEmeraldCount) {
        this.lastEmeraldCount = lastEmeraldCount;
    }

    public long getLastArmorCheck() {
        return lastArmorCheck;
    }

    public long getLastItemCheck() {
        return lastItemCheck;
    }

    public long getLastPotionCheck() {
        return lastPotionCheck;
    }

    public void setLastPotionCheck(long lastPotionCheck) {
        this.lastPotionCheck = lastPotionCheck;
    }

    /**
     * Reset state for cleanup
     */
    public void reset() {
        this.lastHeldItem = null;
        this.hasDiamondArmor = false;
        this.hasAlertedArmor = false;
        this.lastEmeraldCount = 0;
        this.lastArmorCheck = 0;
        this.lastItemCheck = 0;
        this.lastPotionCheck = 0;
    }

    /**
     * Check if this state is stale and should be cleaned up
     */
    public boolean isStale(long currentTime, long maxAge) {
        return (currentTime - Math.max(Math.max(lastArmorCheck, lastItemCheck), lastPotionCheck)) > maxAge;
    }
}
