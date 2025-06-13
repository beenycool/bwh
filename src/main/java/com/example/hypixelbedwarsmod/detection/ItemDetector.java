package com.example.hypixelbedwarsmod.detection;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import com.example.hypixelbedwarsmod.data.PlayerState;
import com.example.hypixelbedwarsmod.utils.ChatUtils;
import com.example.hypixelbedwarsmod.utils.RenderUtils;
import com.example.hypixelbedwarsmod.utils.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles item detection, alerts, and ESP rendering with optimized performance
 */
public class ItemDetector {
    // Item colors for ESP
    private static final int DIAMOND_COLOR = 0x00AAFF; // Cyan/light blue
    private static final int EMERALD_COLOR = 0x00FF00; // Green
    private static final int GOLD_COLOR = 0xFFD700;    // Gold
    private static final int IRON_COLOR = 0xC0C0C0;    // Silver
    
    // Potion IDs
    private static final int SPEED_POTION_ID = 1;
    private static final int JUMP_BOOST_POTION_ID = 8;
    private static final int INVISIBILITY_POTION_ID = 14;
    
    private final ConfigManager configManager;
    private final PlayerTracker playerTracker;
    private final Map<String, Long> alertCooldowns = new ConcurrentHashMap<>();
    
    // Cache for frequently accessed data
    // Removed unused lastHeldItemCache variable to improve code clarity.
    private long lastCacheCleanup = System.currentTimeMillis();
    
    public ItemDetector(ConfigManager configManager) {
        this.configManager = configManager;
        this.playerTracker = null; // Will be injected
    }

    public void processPlayer(EntityPlayer player, EntityPlayer localPlayer) {
        if (player == localPlayer) return;
        
        String playerName = player.getName();
        PlayerState state = getOrCreatePlayerState(playerName);
        
        // Process different types of detection
        if (configManager.isArmorAlertsEnabled()) {
            checkArmor(player, state);
        }
        
        if (configManager.isItemAlertsEnabled()) {
            checkHeldItem(player, state);
        }
        
        if (configManager.isEmeraldAlertsEnabled()) {
            checkEmeralds(player, state);
        }
        
        if (configManager.isSwordAlertsEnabled()) {
            checkDiamondSword(player, state);
        }
        
        if (configManager.isPotionAlertsEnabled()) {
            checkPotions(player, state);
        }
        
        if (configManager.isObsidianAlertsEnabled()) {
            checkObsidian(player, state);
        }
    }

    private void checkArmor(EntityPlayer player, PlayerState state) {
        ItemStack leggings = player.getCurrentArmor(1);
        ItemStack boots = player.getCurrentArmor(0);
        
        boolean hasDiamond = leggings != null && boots != null &&
                leggings.getItem() == Items.diamond_leggings &&
                boots.getItem() == Items.diamond_boots;
        
        if (hasDiamond && !state.hasAlertedArmor()) {
            int currentEmeralds = countEmeralds(player.inventory);
            int remaining = Math.max(currentEmeralds - 6, 0); // Diamond armor costs 6 emeralds
            
            String message = getColoredPlayerName(player) + 
                EnumChatFormatting.AQUA + " bought Diamond Armor! " +
                ChatUtils.formatRemainingItems(remaining, "Emeralds");
            
            sendAlertWithSound(message, SoundUtils.SOUND_ARMOR);
            state.setHasAlertedArmor(true);
            state.setHasDiamondArmor(true);
        }
    }

    private void checkHeldItem(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack == null) {
            state.setLastHeldItem(null);
            return;
        }
        
        Item currentItem = heldStack.getItem();
        if (currentItem != state.getLastHeldItem()) {
            state.setLastHeldItem(currentItem);
            
            // Check for specific items
            checkSpecificItem(player, currentItem, heldStack);
        }
    }
    
    private void checkSpecificItem(EntityPlayer player, Item item, ItemStack stack) {
        String playerName = player.getName();
        
        if (item == Items.bow) {
            sendAlertWithCooldown(playerName + "_bow",
                getColoredPlayerName(player) + EnumChatFormatting.AQUA + " is holding a BOW " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_SPECIAL_ITEM);
                
        } else if (item == Items.ender_pearl) {
            sendAlertWithCooldown(playerName + "_pearl",
                getColoredPlayerName(player) + EnumChatFormatting.AQUA + " has ENDER PEARLS " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_SPECIAL_ITEM);
                
        } else if (item == Item.getItemFromBlock(Blocks.obsidian)) {
            sendAlertWithCooldown(playerName + "_obsidian",
                getColoredPlayerName(player) + EnumChatFormatting.DARK_PURPLE + " is holding OBSIDIAN " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_SPECIAL_ITEM);
                
        } else if (item == Items.diamond_sword) {
            sendAlertWithCooldown(playerName + "_diamond_sword",
                getColoredPlayerName(player) + EnumChatFormatting.AQUA + " has a DIAMOND SWORD " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_DIAMOND_SWORD);
        }
    }

    private void checkEmeralds(EntityPlayer player, PlayerState state) {
        int currentEmeralds = countEmeralds(player.inventory);
        if (currentEmeralds > state.getLastEmeraldCount() && currentEmeralds >= 10) {
            String message = getColoredPlayerName(player) + 
                EnumChatFormatting.GREEN + " has " + currentEmeralds + " EMERALDS " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer()));
            
            sendAlertWithCooldown(player.getName() + "_emeralds", message, SoundUtils.SOUND_EMERALD);
        }
        state.setLastEmeraldCount(currentEmeralds);
    }

    private void checkDiamondSword(EntityPlayer player, PlayerState state) {
        if (hasItem(player.inventory, Items.diamond_sword)) {
            String alertKey = player.getName() + "_diamond_sword_inv";
            sendAlertWithCooldown(alertKey,
                getColoredPlayerName(player) + EnumChatFormatting.AQUA + " has a DIAMOND SWORD " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_DIAMOND_SWORD);
        }
    }

    private void checkPotions(EntityPlayer player, PlayerState state) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            checkSpecificPotion(player, effect);
        }
    }
    
    private void checkSpecificPotion(EntityPlayer player, PotionEffect effect) {
        String playerName = player.getName();
        String alertKey = playerName + "_" + effect.getPotionID();
        
        if (effect.getPotionID() == INVISIBILITY_POTION_ID) {
            sendAlertWithCooldown(alertKey,
                getColoredPlayerName(player) + EnumChatFormatting.GRAY + " is INVISIBLE! " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_INVIS);
                
        } else if (effect.getPotionID() == SPEED_POTION_ID && effect.getAmplifier() >= 1) {
            sendAlertWithCooldown(alertKey,
                getColoredPlayerName(player) + EnumChatFormatting.YELLOW + " has SPEED " +
                (effect.getAmplifier() + 1) + " " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_POTION);
                
        } else if (effect.getPotionID() == JUMP_BOOST_POTION_ID && effect.getAmplifier() >= 1) {
            sendAlertWithCooldown(alertKey,
                getColoredPlayerName(player) + EnumChatFormatting.GREEN + " has JUMP BOOST " +
                (effect.getAmplifier() + 1) + " " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_POTION);
        }
    }

    private void checkObsidian(EntityPlayer player, PlayerState state) {
        if (hasItem(player.inventory, Item.getItemFromBlock(Blocks.obsidian))) {
            String alertKey = player.getName() + "_obsidian_inv";
            sendAlertWithCooldown(alertKey,
                getColoredPlayerName(player) + EnumChatFormatting.DARK_PURPLE + " has OBSIDIAN " +
                ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer())),
                SoundUtils.SOUND_SPECIAL_ITEM,
                configManager.getObsidianCooldown());
        }
    }

    public void renderItemESP(World world, EntityPlayer localPlayer, float partialTicks) {
        if (!configManager.isItemESPEnabled()) return;
        
        float maxDistance = configManager.getItemESPMaxDistance();
        float fadeRange = configManager.getItemESPFadeRange();
        
        for (EntityItem entityItem : world.getEntitiesWithinAABB(EntityItem.class, 
                localPlayer.getEntityBoundingBox().expand(maxDistance, maxDistance, maxDistance))) {
            
            if (entityItem.isDead) continue;
            
            float distance = entityItem.getDistanceToEntity(localPlayer);
            if (distance > maxDistance) continue;
            
            ItemStack stack = entityItem.getEntityItem();
            if (stack == null) continue;
            
            int color = getItemESPColor(stack.getItem());
            if (color == 0) continue; // Not an item we want to highlight
            
            float[] baseColor = RenderUtils.hexToRGB(color);
            float[] fadedColor = RenderUtils.calculateFadedColor(distance, maxDistance, fadeRange, baseColor);
            
            RenderUtils.renderEntityBox(entityItem, fadedColor[0], fadedColor[1], fadedColor[2], fadedColor[3], partialTicks);
        }
    }

    private int getItemESPColor(Item item) {
        if (item == Items.diamond || item == Items.diamond_sword || 
            item == Items.diamond_pickaxe || item == Items.diamond_axe) {
            return DIAMOND_COLOR;
        } else if (item == Items.emerald) {
            return EMERALD_COLOR;
        } else if (item == Items.gold_ingot || item == Items.golden_apple) {
            return GOLD_COLOR;
        } else if (item == Items.iron_ingot || item == Items.iron_sword) {
            return IRON_COLOR;
        }
        return 0; // No highlight
    }

    private int countEmeralds(InventoryPlayer inventory) {
        int count = 0;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null && stack.getItem() == Items.emerald) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private boolean hasItem(InventoryPlayer inventory, Item item) {
        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private void sendAlertWithSound(String message, String sound) {
        ChatUtils.sendAlert(message, EnumChatFormatting.YELLOW);
        SoundUtils.playAlertSound(sound);
    }

    private void sendAlertWithCooldown(String cooldownKey, String message, String sound) {
        sendAlertWithCooldown(cooldownKey, message, sound, configManager.getGeneralCooldown());
    }

    private void sendAlertWithCooldown(String cooldownKey, String message, String sound, long cooldown) {
        long currentTime = System.currentTimeMillis();
        Long lastAlert = alertCooldowns.get(cooldownKey);
        
        if (lastAlert == null || currentTime - lastAlert > cooldown) {
            sendAlertWithSound(message, sound);
            alertCooldowns.put(cooldownKey, currentTime);
        }
    }

    private PlayerState getOrCreatePlayerState(String playerName) {
        // This would normally come from PlayerTracker, but for now create a simple cache
        // In the full implementation, this should be injected or accessed through PlayerTracker
        return new PlayerState();
    }

    private String getColoredPlayerName(EntityPlayer player) {
        // This would normally come from PlayerTracker
        // For now, return the simple name
        return player.getName();
    }

    private EntityPlayer getCurrentPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up old alert cooldowns
        alertCooldowns.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > configManager.getGeneralCooldown() * 2);
        
        // Clean up cache periodically
        if (currentTime - lastCacheCleanup > 60000) { // Every minute
            lastCacheCleanup = currentTime;
        }
    }
}
