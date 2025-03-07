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

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Mod(modid = HypixelBedWarsMod.MODID, name = HypixelBedWarsMod.NAME, version = HypixelBedWarsMod.VERSION)
public class HypixelBedWarsMod {
    public static final String MODID = "hypixelbedwarsmod";
    public static final String NAME = "Hypixel Bed Wars Assistant";
    public static final String VERSION = "1.5";
    private static final Pattern BEDWARS_TITLE = Pattern.compile("BED WARS", Pattern.CASE_INSENSITIVE);
    private static final File CONFIG_FILE = new File("config/bedwars_assistant.cfg");
    private static final String CONFIG_CMD = ".bwconfig";

    // State tracking
    private final Map<String, PlayerState> playerStates = new HashMap<>();
    private final Set<String> detectedPlayers = new HashSet<>();
    private boolean inBedWarsGame = false;

    // Configuration options
    private boolean enableArmorAlerts = true;
    private boolean enableItemAlerts = true;
    private boolean enableEmeraldAlerts = true;
    private boolean enableInvisAlerts = true;

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
            processPlayer(player);
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

    private void processPlayer(EntityPlayer player) {
        if (isRespawning(player)) return; // Skip respawning players

        String playerName = player.getDisplayName().getUnformattedText();
        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());

        if (enableArmorAlerts) checkArmor(player, state);
        if (enableItemAlerts) checkHeldItem(player, state);
        if (enableEmeraldAlerts) checkEmeralds(player, state);
        if (enableInvisAlerts) checkInvisibility(player, state);
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
            sendAlert(EnumChatFormatting.YELLOW + player.getName() +
                    EnumChatFormatting.AQUA + " bought Diamond Armor! (" +
                    remaining + " Emeralds remaining)", true);
            detectedPlayers.add(player.getName());
        }
    }

    private void checkHeldItem(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack == null) return;

        Item currentItem = heldStack.getItem();
        if (currentItem != state.lastHeldItem) {
            state.lastHeldItem = currentItem;
            if (currentItem == Items.bow || currentItem == Items.ender_pearl ||
                currentItem == Item.getItemFromBlock(Blocks.obsidian) || currentItem == Items.spawn_egg) {
                sendAlert(EnumChatFormatting.YELLOW + player.getName() +
                        EnumChatFormatting.AQUA + " is holding " +
                        heldStack.getDisplayName() + " " +
                        getDistanceString(player), true);
            }
        }
    }

    private void checkEmeralds(EntityPlayer player, PlayerState state) {
        int current = countEmeralds(player.inventory);
        if (current > state.lastEmeralds) {
            sendAlert(EnumChatFormatting.YELLOW + player.getName() +
                    EnumChatFormatting.AQUA + " collected " +
                    (current - state.lastEmeralds) + " Emerald(s)" +
                    getDistanceString(player), true);
            state.lastEmeralds = current;
        }
    }

    private void checkInvisibility(EntityPlayer player, PlayerState state) {
        boolean hasInvis = false;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotionID() == Potion.invisibility.getId() && 
                effect.getDuration() > 590 && effect.getDuration() <= 600) { // 29.5-30 seconds
                hasInvis = true;
                break;
            }
        }

        if (hasInvis != state.wasInvisible) {
            state.wasInvisible = hasInvis;
            sendAlert(EnumChatFormatting.YELLOW + player.getName() +
                    (hasInvis ? EnumChatFormatting.AQUA + " is invisible!" :
                            EnumChatFormatting.AQUA + " is visible again!") +
                    getDistanceString(player), true);
        }
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
    }

    private boolean isHypixelBedWars() {
        return Minecraft.getMinecraft().getCurrentServerData() != null &&
                Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel");
    }
}