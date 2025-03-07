package com.example.hypixelbedwarsmod;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo; 
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import java.util.List;
import java.util.ArrayList;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = HypixelBedWarsMod.MODID, name = HypixelBedWarsMod.NAME, version = HypixelBedWarsMod.VERSION)
public class HypixelBedWarsMod {
    public static final String MODID = "hypixelbedwarsmod";
    public static final String NAME = "Hypixel Bed Wars Assistant";
    public static final String VERSION = "1.5";
    private static final Pattern BEDWARS_TITLE = Pattern.compile("BED WARS", Pattern.CASE_INSENSITIVE);
    private static final File CONFIG_FILE = new File("config/bedwars_assistant.cfg");
    private static final String CONFIG_CMD = ".bwconfig";
    
    // Team color detection pattern - matches color codes at start of name
    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    private static final Map<String, EnumChatFormatting> COLOR_MAP = initializeColorMap();

    // State tracking
    private final Map<String, PlayerState> playerStates = new HashMap<>();
    private final Set<String> detectedPlayers = new HashSet<>();
    private boolean inBedWarsGame = false;

    // Configuration options
    private boolean enableArmorAlerts = true;
    private boolean enableItemAlerts = true;
    private boolean enableEmeraldAlerts = true;
    private boolean enableInvisAlerts = true;
    private boolean enableSwordAlerts = true;
    private boolean enablePotionAlerts = true;
    private boolean enableFireballAlerts = true;
    private boolean excludeTeammates = true; // New option to exclude teammates
    private boolean enableItemESP = true; // New option for item highlighting
    private float itemESPMaxDistance = 40.0F; // new config
    private float itemESPFadeRange = 30.0F;   // new config

    // Sound constants for different alerts
    private static final String SOUND_ARMOR = "random.orb";
    private static final String SOUND_DIAMOND_SWORD = "random.anvil_use";
    private static final String SOUND_FIREBALL = "mob.ghast.fireball";
    private static final String SOUND_POTION = "random.drink";
    private static final String SOUND_EMERALD = "random.levelup";
    private static final String SOUND_INVIS = "mob.bat.takeoff";
    private static final String SOUND_SPECIAL_ITEM = "random.successful_hit";

    // Colors for item ESP
    private static final int DIAMOND_COLOR = 0x00AAFF; // Cyan/light blue
    private static final int EMERALD_COLOR = 0x00FF00; // Green

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    private void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            saveConfig();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;

                switch (parts[0]) {
                    case "enableArmorAlerts":
                        enableArmorAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableItemAlerts":
                        enableItemAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableEmeraldAlerts":
                        enableEmeraldAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableInvisAlerts":
                        enableInvisAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableSwordAlerts":
                        enableSwordAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enablePotionAlerts":
                        enablePotionAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableFireballAlerts":
                        enableFireballAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "excludeTeammates":
                        excludeTeammates = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableItemESP":
                        enableItemESP = Boolean.parseBoolean(parts[1]);
                        break;
                    case "itemESPMaxDistance":
                        itemESPMaxDistance = Float.parseFloat(parts[1]);
                        break;
                    case "itemESPFadeRange":
                        itemESPFadeRange = Float.parseFloat(parts[1]);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write("enableArmorAlerts=" + enableArmorAlerts + "\n");
            writer.write("enableItemAlerts=" + enableItemAlerts + "\n");
            writer.write("enableEmeraldAlerts=" + enableEmeraldAlerts + "\n");
            writer.write("enableInvisAlerts=" + enableInvisAlerts + "\n");
            writer.write("enableSwordAlerts=" + enableSwordAlerts + "\n");
            writer.write("enablePotionAlerts=" + enablePotionAlerts + "\n");
            writer.write("enableFireballAlerts=" + enableFireballAlerts + "\n");
            writer.write("excludeTeammates=" + excludeTeammates + "\n");
            writer.write("enableItemESP=" + enableItemESP + "\n");
            writer.write("itemESPMaxDistance=" + itemESPMaxDistance + "\n");
            writer.write("itemESPFadeRange=" + itemESPFadeRange + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        if (message.startsWith(CONFIG_CMD)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiConfigScreen());
            event.setCanceled(true);
        }

        if (!isHypixelBedWars()) return;

        // Detect elimination messages
        if (message.contains("You have been eliminated!") || 
            message.contains("You died!") || 
            message.contains("was killed by")) {
            detectedPlayers.clear();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        checkBedWarsStatus();
        if (!inBedWarsGame) return;

        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        World world = Minecraft.getMinecraft().theWorld;

        if (localPlayer == null || world == null) return;

        // Remove dead players from tracking
        Set<String> tabPlayers = getTabListPlayers();
        playerStates.keySet().removeIf(name -> !tabPlayers.contains(name));
        detectedPlayers.removeIf(name -> !tabPlayers.contains(name));

        for (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityPlayer) || entity == localPlayer) continue;

            EntityPlayer player = (EntityPlayer) entity;
            processPlayer(player, localPlayer);
        }
    }

    private void checkBedWarsStatus() {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null || world.getScoreboard() == null) {
            inBedWarsGame = false;
            return;
        }

        ScoreObjective sidebar = world.getScoreboard().getObjectiveInDisplaySlot(1);
        inBedWarsGame = sidebar != null && BEDWARS_TITLE.matcher(sidebar.getDisplayName()).find();
    }

    // Modified to include localPlayer for team checks
    private void processPlayer(EntityPlayer player, EntityPlayer localPlayer) {
        if (isRespawning(player)) return; // Skip respawning players
        
        // Skip teammates if the option is enabled
        if (excludeTeammates && isSameTeam(player, localPlayer)) {
            return;
        }

        String playerName = player.getDisplayName().getUnformattedText();
        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());

        if (enableArmorAlerts) checkArmor(player, state);
        if (enableItemAlerts) checkHeldItem(player, state);
        if (enableEmeraldAlerts) checkEmeralds(player, state);
        if (enableInvisAlerts) checkInvisibility(player, state);
        if (enableSwordAlerts) checkDiamondSword(player, state);
        if (enablePotionAlerts) checkPotions(player, state);
        if (enableFireballAlerts && enableItemAlerts) checkFireball(player, state);
    }

    private void checkArmor(EntityPlayer player, PlayerState state) {
        ItemStack leggings = player.getCurrentArmor(1);
        ItemStack boots = player.getCurrentArmor(0);
        boolean hasDiamond = leggings != null && boots != null &&
                leggings.getItem() == Items.diamond_leggings &&
                boots.getItem() == Items.diamond_boots;

        if (hasDiamond && !detectedPlayers.contains(player.getName())) {
            int currentEmeralds = countEmeralds(player.inventory);
            int remaining = Math.max(currentEmeralds - 6, 0);
            sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.AQUA + " bought Diamond Armor! (" +
                    remaining + " Emeralds remaining)", true, SOUND_ARMOR);
            detectedPlayers.add(player.getName());
        }
    }

    private void checkHeldItem(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack == null) {
            state.lastHeldItem = null;
            return;
        }

        Item currentItem = heldStack.getItem();
        if (currentItem != state.lastHeldItem) {
            state.lastHeldItem = currentItem;
            
            // Only alert for specific items when they're first held
            if (currentItem == Items.bow) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.AQUA + " is holding a BOW " +
                        getDistanceString(player), true, SOUND_SPECIAL_ITEM);
            } else if (currentItem == Items.ender_pearl) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.AQUA + " has ENDER PEARLS " +
                        getDistanceString(player), true, SOUND_SPECIAL_ITEM);
            }
        }
    }
    
    private void checkDiamondSword(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack != null && heldStack.getItem() == Items.diamond_sword && !state.lastHeldDiamondSword) {
            state.lastHeldDiamondSword = true;
            sendAlert(getColoredPlayerName(player) + 
                    EnumChatFormatting.AQUA + " has a DIAMOND SWORD! " +
                    getDistanceString(player), true, SOUND_DIAMOND_SWORD);
        } else if (heldStack == null || heldStack.getItem() != Items.diamond_sword) {
            state.lastHeldDiamondSword = false;
        }
    }
    
    private void checkFireball(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack == null) return;
        
        Item currentItem = heldStack.getItem();
        if (currentItem == Items.fire_charge && !state.wasHoldingFireball) {
            state.wasHoldingFireball = true;
            sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.RED + " is holding a FIREBALL! " +
                    getDistanceString(player), true, SOUND_FIREBALL);
        } else if (currentItem != Items.fire_charge) {
            state.wasHoldingFireball = false;
        }
    }
    
    private void checkPotions(EntityPlayer player, PlayerState state) {
        Map<Integer, Boolean> currentPotions = new HashMap<>();
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            int id = effect.getPotionID();
            currentPotions.put(id, true);
            
            // Alert for newly acquired potions
            if (!state.activePotions.containsKey(id)) {
                String potionName = getPotionName(id);
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.LIGHT_PURPLE + " drank " + potionName + "! " +
                        getDistanceString(player), true, SOUND_POTION);
            }
        }
        
        // Check for expired potions
        for (Map.Entry<Integer, Boolean> entry : state.activePotions.entrySet()) {
            if (!currentPotions.containsKey(entry.getKey())) {
                String potionName = getPotionName(entry.getKey());
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.GRAY + "'s " + potionName + " wore off. " +
                        getDistanceString(player), false, null);
            }
        }
        
        state.activePotions = currentPotions;
    }
    
    private void checkInvisibility(EntityPlayer player, PlayerState state) {
        boolean isInvisible = player.isInvisible();
        if (isInvisible && !state.wasInvisible) {
            state.wasInvisible = true;
            sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.DARK_PURPLE + " is now INVISIBLE! " +
                    getDistanceString(player), true, SOUND_INVIS);
        } else if (!isInvisible && state.wasInvisible) {
            state.wasInvisible = false;
            sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.GRAY + " is no longer invisible. " +
                    getDistanceString(player), false, null);
        }
    }
    
    private void checkEmeralds(EntityPlayer player, PlayerState state) {
        int emeralds = countEmeralds(player.inventory);
        if (emeralds >= 4 && emeralds > state.lastEmeralds) {
            sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.GREEN + " has " + emeralds + " emeralds! " +
                    getDistanceString(player), true, SOUND_EMERALD);
        }
        state.lastEmeralds = emeralds;
    }
    
    /**
     * Gets the player's name with their team color applied
     */
    private String getColoredPlayerName(EntityPlayer player) {
        String formattedName = player.getDisplayName().getFormattedText();
        String colorCode = getTeamColor(formattedName);
        String playerName = player.getName();
        
        if (colorCode != null && COLOR_MAP.containsKey(colorCode)) {
            return COLOR_MAP.get(colorCode) + playerName;
        }
        
        // Default to yellow if no team color found
        return EnumChatFormatting.YELLOW + playerName;
    }

    private int countEmeralds(InventoryPlayer inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.emerald) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private String getDistanceString(EntityPlayer player) {
        return EnumChatFormatting.GRAY + "(" + (int) player.getDistanceToEntity(Minecraft.getMinecraft().thePlayer) + "m)";
    }

    private boolean isRespawning(EntityPlayer player) {
        // Check position and facing direction
        boolean atSpawn = Math.abs(player.posX - 0.5) < 0.2 && 
                          Math.abs(player.posZ - 0.5) < 0.2 && 
                          player.posY >= 120 && player.posY <= 121;
        boolean facingSouth = Math.abs(player.rotationYaw - 180) < 10;
        return atSpawn && facingSouth;
    }

    private Set<String> getTabListPlayers() {
        Set<String> tabPlayers = new HashSet<>();
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().thePlayer.sendQueue == null) {
            return tabPlayers;
        }

        for (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {
            String name = info.getGameProfile().getName();
            tabPlayers.add(name);
        }
        return tabPlayers;
    }

    private long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN = 1000; // 1 second

    private void sendAlert(String message, boolean sound) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        
        if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {
            Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
            lastSoundTime = System.currentTimeMillis();
        }
    }

    private void sendAlert(String message, boolean sound, String soundName) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        
        if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {
            if (soundName != null) {
                Minecraft.getMinecraft().thePlayer.playSound(soundName, 1.0F, 1.0F);
            } else {
                Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
            }
            lastSoundTime = System.currentTimeMillis();
        }
    }

    /**
     * Checks if two players are on the same team by comparing their name color prefixes
     */
    private boolean isSameTeam(EntityPlayer player1, EntityPlayer player2) {
        String team1 = getTeamColor(player1.getDisplayName().getFormattedText());
        String team2 = getTeamColor(player2.getDisplayName().getFormattedText());
        return team1 != null && team1.equals(team2);
    }
    
    /**
     * Extracts the team color code from a player's formatted name
     * @return The color code or null if no color code found
     */
    private String getTeamColor(String formattedName) {
        Matcher matcher = TEAM_COLOR_PATTERN.matcher(formattedName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Map<String, EnumChatFormatting> initializeColorMap() {
        Map<String, EnumChatFormatting> map = new HashMap<>();
        // Map Minecraft color codes to EnumChatFormatting
        map.put("0", EnumChatFormatting.BLACK);
        map.put("1", EnumChatFormatting.DARK_BLUE);
        map.put("2", EnumChatFormatting.DARK_GREEN);
        map.put("3", EnumChatFormatting.DARK_AQUA);
        map.put("4", EnumChatFormatting.DARK_RED);
        map.put("5", EnumChatFormatting.DARK_PURPLE);
        map.put("6", EnumChatFormatting.GOLD);
        map.put("7", EnumChatFormatting.GRAY);
        map.put("8", EnumChatFormatting.DARK_GRAY);
        map.put("9", EnumChatFormatting.BLUE);
        map.put("a", EnumChatFormatting.GREEN);
        map.put("b", EnumChatFormatting.AQUA);
        map.put("c", EnumChatFormatting.RED);
        map.put("d", EnumChatFormatting.LIGHT_PURPLE);
        map.put("e", EnumChatFormatting.YELLOW);
        map.put("f", EnumChatFormatting.WHITE);
        return map;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!inBedWarsGame || !enableItemESP) return;
        
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World world = Minecraft.getMinecraft().theWorld;
        
        if (player == null || world == null) return;
        
        // Get all loaded entities
        List<EntityItem> itemsToRender = new ArrayList<>();
        
        for (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityItem)) continue;
            
            EntityItem item = (EntityItem) entity;
            ItemStack stack = item.getEntityItem();
            
            // Only render diamonds and emeralds
            if (stack != null && (stack.getItem() == Items.diamond || stack.getItem() == Items.emerald)) {
                itemsToRender.add(item);
            }
        }
        
        // Render the items
        renderItemESP(itemsToRender, event.partialTicks);
    }
    
    private void renderItemESP(List<EntityItem> items, float partialTicks) {
        // Save GL state
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);
        
        // Get player position for relative rendering
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        
        // Render each item
        for (EntityItem item : items) {
            double itemX = item.lastTickPosX + (item.posX - item.lastTickPosX) * partialTicks;
            double itemY = item.lastTickPosY + (item.posY - item.lastTickPosY) * partialTicks;
            double itemZ = item.lastTickPosZ + (item.posZ - item.lastTickPosZ) * partialTicks;
            
            double relX = itemX - playerX;
            double relY = itemY - playerY;
            double relZ = itemZ - playerZ;
            
            ItemStack stack = item.getEntityItem();
            int color = stack.getItem() == Items.diamond ? DIAMOND_COLOR : EMERALD_COLOR;
            float boxSize = 0.35F; // Size of the box
            
            float distance = player.getDistanceToEntity(item);
            if (distance > itemESPMaxDistance) continue; // skip distant items

            float alpha = 1.0F - (distance / itemESPFadeRange);
            alpha = Math.max(0.2F, Math.min(1.0F, alpha)); // clamp between 0.2 and 1.0

            drawBox(relX, relY, relZ, boxSize, color, alpha);
            drawItemCount(relX, relY + boxSize + 0.25, relZ, stack.stackSize, color);
        }
        
        // Restore GL state
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
    
    private void drawBox(double x, double y, double z, float size, int color, float alpha) {
        // Extract RGB components
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        
        GL11.glColor4f(r, g, b, alpha); // Semi-transparent
        
        // Draw filled box
        GL11.glBegin(GL11.GL_QUADS);
        
        // Bottom face
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        
        // Top face
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z - size);
        
        // Front face
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        
        // Back face
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        
        // Left face
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z - size);
        
        // Right face
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);
        
        GL11.glEnd();
        
        // Draw outline with a slightly darker color
        GL11.glColor4f(r * 0.8F, g * 0.8F, b * 0.8F, alpha + 0.2F);
        GL11.glBegin(GL11.GL_LINES);
        
        // Bottom face
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z - size);
        
        // Top face
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z - size);
        
        // Connecting edges
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        
        GL11.glEnd();
    }
    
    private void drawItemCount(double x, double y, double z, int count, int color) {
        // Extract RGB components
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        
        // Set rendering to face the player
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        
        // Make text face the player by applying inverse rotation
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        GL11.glRotatef(-player.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(player.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.025F, -0.025F, 0.025F); // Scale the text to a reasonable size
        
        // Re-enable textures for rendering text
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        
        // Draw the text centered
        String text = String.valueOf(count);
        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, -textWidth / 2, -4, color | 0xFF000000);
        
        // Restore state for continued rendering
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    public class GuiConfigScreen extends GuiScreen {
        private final int BUTTON_WIDTH = 180;
        private final int BUTTON_HEIGHT = 20;
        private final int PADDING = 5;

        @Override
        public void initGui() {
            this.buttonList.clear();
            int y = height / 4;
            
            buttonList.add(createToggleButton(0, "Armor Alerts", enableArmorAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(1, "Item Alerts", enableItemAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(2, "Emerald Alerts", enableEmeraldAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(3, "Invisibility Alerts", enableInvisAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(4, "Diamond Sword Alerts", enableSwordAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(5, "Potion Alerts", enablePotionAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(6, "Fireball Alerts", enableFireballAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(7, "Exclude Teammates", excludeTeammates, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(8, "Item ESP", enableItemESP, y));
        }

        private GuiButton createToggleButton(int id, String text, boolean state, int y) {
            return new GuiButton(id, width/2 - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    text + ": " + (state ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            switch (button.id) {
                case 0: enableArmorAlerts = !enableArmorAlerts; break;
                case 1: enableItemAlerts = !enableItemAlerts; break;
                case 2: enableEmeraldAlerts = !enableEmeraldAlerts; break;
                case 3: enableInvisAlerts = !enableInvisAlerts; break;
                case 4: enableSwordAlerts = !enableSwordAlerts; break;
                case 5: enablePotionAlerts = !enablePotionAlerts; break;
                case 6: enableFireballAlerts = !enableFireballAlerts; break;
                case 7: excludeTeammates = !excludeTeammates; break;
                case 8: enableItemESP = !enableItemESP; break;
            }
            saveConfig();
            initGui();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Bed Wars Assistant Settings", width/2, 40, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private static class PlayerState {
        int lastEmeralds = 0;
        boolean wasInvisible = false;
        Item lastHeldItem = null;
        boolean lastHeldDiamondSword = false;
        boolean wasHoldingFireball = false;
        Map<Integer, Boolean> activePotions = new HashMap<>();
    }

    private boolean isHypixelBedWars() {
        return Minecraft.getMinecraft().getCurrentServerData() != null &&
                Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel");
    }

    private String getPotionName(int potionId) {
        Potion potion = Potion.potionTypes[potionId];
        if (potion != null) {
            return potion.getName();
        }
        return "Unknown Potion";
    }
}
