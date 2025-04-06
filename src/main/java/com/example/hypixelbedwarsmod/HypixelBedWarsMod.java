package main.java.com.example.hypixelbedwarsmod;
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
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = HypixelBedWarsMod.MODID, name = HypixelBedWarsMod.NAME, version = HypixelBedWarsMod.VERSION)
public class HypixelBedWarsMod {
    public static final String MODID = "hypixelbedwarsmod";
    public static final String NAME = "Hypixel Bed Wars Assistant";
    public static final String VERSION = "1.6";
    private static final Pattern BEDWARS_TITLE = Pattern.compile("BED WARS", Pattern.CASE_INSENSITIVE);
    private static final File CONFIG_FILE = new File("config/bedwars_assistant.cfg");
    private static final String CONFIG_CMD = ".bwconfig";
    
    // Team color detection pattern - matches color codes at start of name
    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    private static final Map<String, EnumChatFormatting> COLOR_MAP = initializeColorMap();
    
    // Potion IDs
    private static final int SPEED_POTION_ID = 1;
    private static final int JUMP_BOOST_POTION_ID = 8;
    private static final int INVISIBILITY_POTION_ID = 14;

    // State tracking
    private final Map<String, PlayerState> playerStates = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Set<String> detectedPlayers = new HashSet<>();
    private boolean inBedWarsGame = false;
    private Team playerTeam;
    
    // Alert cooldowns (player name -> item type -> last alert time)
    private final Map<String, Map<String, Long>> alertCooldowns = new HashMap<>();
    private final Map<EntityFireball, FireballInfo> trackedFireballs = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> itemEntityCounts = new ConcurrentHashMap<>();

    // Configuration options
    private boolean enableArmorAlerts = true;
    private boolean enableItemAlerts = true;
    private boolean enableEmeraldAlerts = true;
    private boolean enableSwordAlerts = true;
    private boolean enablePotionAlerts = true;
    private boolean enableFireballAlerts = true;
    private boolean excludeTeammates = true;
    private boolean enableItemESP = true;
    private boolean enableFireballTracking = true;
    private float itemESPMaxDistance = 80.0F;
    private float itemESPFadeRange = 60.0F;
    private long alertCooldownMs = 10000; // 10 seconds cooldown between similar alerts

    // Sound constants for different alerts
    private static final String SOUND_ARMOR = "random.orb";
    private static final String SOUND_DIAMOND_SWORD = "random.anvil_use";
    private static final String SOUND_FIREBALL = "mob.ghast.fireball";
    private static final String SOUND_INCOMING_FIREBALL = "mob.ghast.scream";
    private static final String SOUND_POTION = "random.drink";
    private static final String SOUND_EMERALD = "random.levelup";
    private static final String SOUND_INVIS = "mob.bat.takeoff";
    private static final String SOUND_SPECIAL_ITEM = "random.successful_hit";

    // Colors for item ESP
    private static final int DIAMOND_COLOR = 0x00AAFF; // Cyan/light blue
    private static final int EMERALD_COLOR = 0x00FF00; // Green

    private static final long SCOREBOARD_CHECK_INTERVAL_MS = 1000;
    private long lastScoreboardCheck = 0;

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
                    case "enableFireballTracking":
                        enableFireballTracking = Boolean.parseBoolean(parts[1]);
                        break;
                    case "itemESPMaxDistance":
                        itemESPMaxDistance = Float.parseFloat(parts[1]);
                        break;
                    case "itemESPFadeRange":
                        itemESPFadeRange = Float.parseFloat(parts[1]);
                        break;
                    case "alertCooldownMs":
                        alertCooldownMs = Long.parseLong(parts[1]);
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
            writer.write("enableSwordAlerts=" + enableSwordAlerts + "\n");
            writer.write("enablePotionAlerts=" + enablePotionAlerts + "\n");
            writer.write("enableFireballAlerts=" + enableFireballAlerts + "\n");
            writer.write("enableFireballTracking=" + enableFireballTracking + "\n");
            writer.write("excludeTeammates=" + excludeTeammates + "\n");
            writer.write("enableItemESP=" + enableItemESP + "\n");
            writer.write("itemESPMaxDistance=" + itemESPMaxDistance + "\n");
            writer.write("itemESPFadeRange=" + itemESPFadeRange + "\n");
            writer.write("alertCooldownMs=" + alertCooldownMs + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = event.message.getFormattedText();
        String unformattedMessage = event.message.getUnformattedText();
        
        if (unformattedMessage.startsWith(CONFIG_CMD)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiConfigScreen());
            event.setCanceled(true);
        }

        if (!isHypixelBedWars()) return;

        // Detect game start
        if (message.contains("§r§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r §r§f §r§f§lBed Wars§r")) {
            inBedWarsGame = true;
            initializeGame();
            return;
        }

        // Track bed destruction
        if (message.matches(".*§r§f§lBED DESTRUCTION > .* bed was destroyed by .*")) {
            String[] parts = message.split(" bed was destroyed by ");
            if (parts.length == 2) {
                String teamColor = getTeamColor(parts[0]);
                if (teamColor != null && teams.containsKey(teamColor)) {
                    teams.get(teamColor).setBedState(false);
                    if (playerTeam != null && teamColor.equals(playerTeam.getColor())) {
                        sendAlert(EnumChatFormatting.RED + "YOUR TEAM'S BED WAS DESTROYED!", true, "random.anvil_land");
                    }
                }
            }
        }

        // Track final kills
        if (message.contains("§r§fFINAL KILL!")) {
            String killedPlayer = extractPlayerName(message);
            if (killedPlayer != null) {
                for (Team team : teams.values()) {
                    if (team.getPlayers().contains(killedPlayer)) {
                        team.removePlayer(killedPlayer);
                        if (team.getPlayers().isEmpty() && !team.hasBed()) {
                            String teamElimMsg = team.getFormatting() + "TEAM ELIMINATED > " + team.getColor() + " team has been eliminated!";
                            sendAlert(teamElimMsg, true, "random.levelup");
                        }
                        break;
                    }
                }
            }
        }

        // Detect game end
        if (message.contains("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") ||
            message.contains("§r§e§lVICTORY!") ||
            message.contains("§r§c§lGAME OVER!")) {
            inBedWarsGame = false;
            detectedPlayers.clear();
            return;
        }

        // Detect elimination messages
        if (unformattedMessage.contains("You have been eliminated!") ||
            unformattedMessage.contains("You died!") ||
            unformattedMessage.contains("was killed by")) {
            detectedPlayers.clear();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        if (now - lastScoreboardCheck >= SCOREBOARD_CHECK_INTERVAL_MS) {
            lastScoreboardCheck = now;
            checkBedWarsStatus();
        }
        if (!inBedWarsGame) return;

        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        World world = Minecraft.getMinecraft().theWorld;

        if (localPlayer == null || world == null) return;

        // Remove dead players from tracking
        Set<String> tabPlayers = getTabListPlayers();
        playerStates.keySet().removeIf(name -> !tabPlayers.contains(name));
        detectedPlayers.removeIf(name -> !tabPlayers.contains(name));

        // Update item entity counts
        updateItemEntityCounts(world);
        
        // Process fireballs if enabled
        if (enableFireballTracking) {
            processFireballs(world, localPlayer);
        }

        for (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityPlayer) || entity == localPlayer) continue;

            EntityPlayer player = (EntityPlayer) entity;
            processPlayer(player, localPlayer);
        }
    }

    /**
     * Updates counts of items in the world for ESP rendering
     */
    private void updateItemEntityCounts(World world) {
        // Clear old counts
        itemEntityCounts.clear();
        
        // Count items
        for (Entity entity : world.getLoadedEntityList()) {
            if (entity instanceof EntityItem) {
                EntityItem item = (EntityItem) entity;
                ItemStack stack = item.getEntityItem();
                
                if (stack != null && (stack.getItem() == Items.diamond || stack.getItem() == Items.emerald)) {
                    // Store the actual stack size for this entity
                    itemEntityCounts.put(entity.getEntityId(), stack.stackSize);
                }
            }
        }
    }
    
    /**
     * Processes fireballs in the world, tracking their trajectory and alerting the player
     */
    private void processFireballs(World world, EntityPlayer localPlayer) {
        // Clean up old fireballs that might have disappeared without us noticing
        trackedFireballs.entrySet().removeIf(entry -> 
            entry.getKey().isDead || !world.loadedEntityList.contains(entry.getKey()));
        
        // Check for new fireballs and track existing ones
        for (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityFireball)) continue;
            
            EntityFireball fireball = (EntityFireball) entity;
            
            // Add new fireballs to tracking
            if (!trackedFireballs.containsKey(fireball)) {
                Entity shooter = fireball.shootingEntity;
                String shooterName = shooter instanceof EntityPlayer ? ((EntityPlayer) shooter).getName() : "Unknown";
                trackedFireballs.put(fireball, new FireballInfo(shooter, shooterName, System.currentTimeMillis()));
                
                // Alert about new fireball if we can identify who shot it
                if (shooter instanceof EntityPlayer && !shooter.equals(localPlayer)) {
                    if (!isCooldownActive(shooterName, "fireball_shot")) {
                        sendAlert(getColoredPlayerName((EntityPlayer) shooter) + 
                                EnumChatFormatting.GOLD + " fired a FIREBALL!", true, SOUND_FIREBALL);
                        setAlertCooldown(shooterName, "fireball_shot", 5000); // 5 second cooldown
                    }
                }
            }
            
            // Check if fireball is heading towards the player
            FireballInfo info = trackedFireballs.get(fireball);
            if (!info.hasCheckedImpact) {
                checkFireballImpact(fireball, localPlayer, info);
            }
        }
    }
    
    /**
     * Checks if a fireball is likely to impact the player
     */
    private void checkFireballImpact(EntityFireball fireball, EntityPlayer player, FireballInfo info) {
        // Mark as checked so we don't spam alerts
        info.hasCheckedImpact = true;
        
        // Skip if it's the player's own fireball
        if (fireball.shootingEntity == player) return;
        
        // Calculate fireball trajectory
        Vec3 fireballPos = new Vec3(fireball.posX, fireball.posY, fireball.posZ);
        Vec3 fireballMotion = new Vec3(fireball.motionX, fireball.motionY, fireball.motionZ);
        Vec3 fireballEndPos = fireballPos.addVector(
            fireballMotion.xCoord * 20, // predict 20 ticks ahead
            fireballMotion.yCoord * 20,
            fireballMotion.zCoord * 20
        );
        
        // Get player hitbox
        double expandAmount = 2.0; // Expand hitbox for prediction
        AxisAlignedBB playerBox = player.getEntityBoundingBox().expand(expandAmount, expandAmount, expandAmount);
        
        // Perform ray trace to check for collision
        MovingObjectPosition intercept = playerBox.calculateIntercept(fireballPos, fireballEndPos);
        if (intercept != null) {
            // Calculate time to impact
            double fireballSpeed = Math.sqrt(
                fireball.motionX * fireball.motionX + 
                fireball.motionY * fireball.motionY + 
                fireball.motionZ * fireball.motionZ
            );
            double distance = player.getDistanceToEntity(fireball);
            double timeToImpact = fireballSpeed > 0 ? distance / fireballSpeed : 0;
            
            // Alert player if impact is likely
            sendAlert(EnumChatFormatting.RED + "INCOMING FIREBALL! " + 
                    (timeToImpact > 0 ? String.format("%.1f seconds to impact!", timeToImpact) : "MOVE NOW!"), 
                    true, SOUND_INCOMING_FIREBALL);
            info.alertedImpact = true;
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

        String playerName = player.getName();
        String teamColor = getTeamColor(player.getDisplayName().getFormattedText());
        
        // Update team assignment if needed
        if (teamColor != null) {
            String teamName = getTeamName(teamColor);
            if (teamName != null && teams.containsKey(teamName)) {
                Team playerTeam = teams.get(teamName);
                if (!playerTeam.getPlayers().contains(playerName)) {
                    // Remove from other teams if necessary
                    for (Team team : teams.values()) {
                        team.removePlayer(playerName);
                    }
                    playerTeam.addPlayer(playerName);
                }
            }
        }

        // Skip teammates if the option is enabled
        if (excludeTeammates && isSameTeam(player, localPlayer)) {
            return;
        }

        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());

        // Process core alerts
        if (enableArmorAlerts) checkArmor(player, state);
        if (enableItemAlerts) checkHeldItem(player, state);
        if (enableEmeraldAlerts) checkEmeralds(player, state);
        if (enableSwordAlerts) checkDiamondSword(player, state);
        if (enablePotionAlerts) checkPotions(player, state);
        if (enableFireballAlerts && enableItemAlerts) checkFireball(player, state);

        // Team-specific checks
        if (teamColor != null) {
            Team team = teams.get(getTeamName(teamColor));
            if (team != null && !team.hasBed()) {
                // Additional alert for low-health players without a bed
                if (player.getHealth() <= 10.0F) { // 5 hearts or less
                    if (!isCooldownActive(playerName, "low_health")) {
                        sendAlert(getColoredPlayerName(player) +
                                EnumChatFormatting.RED + " is low health with NO BED! " +
                                getDistanceString(player), true, "random.successful_hit");
                        setAlertCooldown(playerName, "low_health", 8000); // 8 second cooldown
                    }
                }
            }
        }

        // Check for obsidian blocks only if player is holding it
        if (enableItemAlerts) {
            ItemStack heldStack = player.getHeldItem();
            if (heldStack != null && Item.getItemFromBlock(Blocks.obsidian) == heldStack.getItem()) {
                if (!isCooldownActive(playerName, "obsidian_held")) {
                    sendAlert(getColoredPlayerName(player) +
                            EnumChatFormatting.DARK_PURPLE + " is holding OBSIDIAN! " +
                            getDistanceString(player), true, "random.anvil_land");
                    setAlertCooldown(playerName, "obsidian_held", 10000); // 10 second cooldown
                }
            }
        }
    }

    private void checkArmor(EntityPlayer player, PlayerState state) {
        ItemStack leggings = player.getCurrentArmor(1);
        ItemStack boots = player.getCurrentArmor(0);
        boolean hasDiamond = leggings != null && boots != null &&
                leggings.getItem() == Items.diamond_leggings &&
                boots.getItem() == Items.diamond_boots;

        if (hasDiamond && !state.hasAlertedDiamondArmor) {
            int currentEmeralds = countEmeralds(player.inventory);
            int remaining = Math.max(currentEmeralds - 6, 0);
            if (inBedWarsGame) {
                if (!isCooldownActive(player.getName(), "diamond_armor")) {
                    sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.AQUA + " bought Diamond Armor! (" +
                        remaining + " Emeralds remaining)", true, SOUND_ARMOR);
                    setAlertCooldown(player.getName(), "diamond_armor", alertCooldownMs);
                    state.hasAlertedDiamondArmor = true;
                }
            }
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
            if (currentItem == Items.bow && !isCooldownActive(player.getName(), "bow_held")) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.AQUA + " is holding a BOW " +
                        getDistanceString(player), true, SOUND_SPECIAL_ITEM);
                setAlertCooldown(player.getName(), "bow_held", alertCooldownMs);
            } else if (currentItem == Items.ender_pearl && !isCooldownActive(player.getName(), "pearl_held")) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.AQUA + " has ENDER PEARLS " +
                        getDistanceString(player), true, SOUND_SPECIAL_ITEM);
                setAlertCooldown(player.getName(), "pearl_held", alertCooldownMs);
            }
        }
    }
    
    private void checkDiamondSword(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();
        if (heldStack != null && heldStack.getItem() == Items.diamond_sword && !state.lastHeldDiamondSword) {
            state.lastHeldDiamondSword = true;
            if (!isCooldownActive(player.getName(), "diamond_sword")) {
                sendAlert(getColoredPlayerName(player) + 
                        EnumChatFormatting.AQUA + " has a DIAMOND SWORD! " +
                        getDistanceString(player), true, SOUND_DIAMOND_SWORD);
                setAlertCooldown(player.getName(), "diamond_sword", alertCooldownMs);
            }
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
            if (!isCooldownActive(player.getName(), "fireball_held")) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.RED + " is holding a FIREBALL! " +
                        getDistanceString(player), true, SOUND_FIREBALL);
                setAlertCooldown(player.getName(), "fireball_held", 5000); // 5 second cooldown for fireballs
            }
        } else if (currentItem != Items.fire_charge) {
            state.wasHoldingFireball = false;
        }
    }
    
    private void checkPotions(EntityPlayer player, PlayerState state) {
        Map<Integer, Boolean> currentPotions = new HashMap<>();
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            int id = effect.getPotionID();
            // Only track Speed, Jump Boost, and Invisibility potions
            if (id == SPEED_POTION_ID || id == JUMP_BOOST_POTION_ID || id == INVISIBILITY_POTION_ID) {
                currentPotions.put(id, true);
                
                // Alert for newly acquired potions
                if (!state.activePotions.containsKey(id)) {
                    String potionName = getPotionName(id);
                    String alertKey = "potion_" + id;
                    if (!isCooldownActive(player.getName(), alertKey)) {
                        sendAlert(getColoredPlayerName(player) +
                                EnumChatFormatting.LIGHT_PURPLE + " drank " + potionName + "! " +
                                getDistanceString(player), true, 
                                id == INVISIBILITY_POTION_ID ? SOUND_INVIS : SOUND_POTION);
                        setAlertCooldown(player.getName(), alertKey, 8000); // 8 second cooldown for potions
                    }
                }
            }
        }
        
        // Check for expired potions
        for (Map.Entry<Integer, Boolean> entry : state.activePotions.entrySet()) {
            if (!currentPotions.containsKey(entry.getKey())) {
                String potionName = getPotionName(entry.getKey());
                int potionId = entry.getKey();
                if (!isCooldownActive(player.getName(), "potion_expired_" + potionId)) {
                    sendAlert(getColoredPlayerName(player) +
                            EnumChatFormatting.GRAY + "'s " + potionName + " wore off. " +
                            getDistanceString(player), false, null);
                    setAlertCooldown(player.getName(), "potion_expired_" + potionId, 10000); // 10 second cooldown
                }
            }
        }
        
        state.activePotions = currentPotions;
    }
    
    private void checkEmeralds(EntityPlayer player, PlayerState state) {
        int emeralds = countEmeralds(player.inventory);
        if (emeralds >= 4 && emeralds > state.lastEmeralds) {
            if (!isCooldownActive(player.getName(), "emeralds")) {
                sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.GREEN + " has " + emeralds + " emeralds! " +
                        getDistanceString(player), true, SOUND_EMERALD);
                setAlertCooldown(player.getName(), "emeralds", 5000); // 5 second cooldown for emerald alerts
            }
        }
        state.lastEmeralds = emeralds;
    }
    
    /**
     * Checks if an alert cooldown is active
     */
    private boolean isCooldownActive(String playerName, String alertType) {
        if (!alertCooldowns.containsKey(playerName)) return false;
        Map<String, Long> playerCooldowns = alertCooldowns.get(playerName);
        if (!playerCooldowns.containsKey(alertType)) return false;
        
        long lastAlertTime = playerCooldowns.get(alertType);
        return System.currentTimeMillis() - lastAlertTime < alertCooldownMs;
    }
    
    /**
     * Sets an alert cooldown
     */
    private void setAlertCooldown(String playerName, String alertType, long cooldownMs) {
        Map<String, Long> playerCooldowns = alertCooldowns.computeIfAbsent(playerName, k -> new HashMap<>());
        playerCooldowns.put(alertType, System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enableItemESP) return;
        
        if (!isHypixelBedWars()) return;

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
        
        // Render the items with improved ESP
        renderImprovedItemESP(itemsToRender, event.partialTicks);
        
        // If fireball tracking is enabled, render fireball warnings
        if (enableFireballTracking) {
            renderFireballWarnings(event.partialTicks);
        }
    }
    
    /**
     * Renders warning indicators for approaching fireballs
     */
    private void renderFireballWarnings(float partialTicks) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0F);
        
        // Get player position for relative rendering
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        
        for (Map.Entry<EntityFireball, FireballInfo> entry : trackedFireballs.entrySet()) {
            EntityFireball fireball = entry.getKey();
            
            // Skip if it's the player's own fireball
            if (fireball.shootingEntity == player) continue;
            
            double fireballX = fireball.lastTickPosX + (fireball.posX - fireball.lastTickPosX) * partialTicks;
            double fireballY = fireball.lastTickPosY + (fireball.posY - fireball.lastTickPosY) * partialTicks;
            double fireballZ = fireball.lastTickPosZ + (fireball.posZ - fireball.lastTickPosZ) * partialTicks;
            
            double relX = fireballX - playerX;
            double relY = fireballY - playerY;
            double relZ = fireballZ - playerZ;
            
            // Render a directional arrow toward the fireball
            double distance = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (distance < 20.0) { // Only show warning for nearby fireballs
                // Color goes from yellow to red based on proximity
                float intensity = (float) (1.0 - (distance / 20.0));
                Color warningColor = new Color(1.0f, 1.0f - intensity, 0.0f);
                
                GL11.glColor4f(
                    warningColor.getRed() / 255.0f,
                    warningColor.getGreen() / 255.0f,
                    warningColor.getBlue() / 255.0f,
                    0.7f
                );
                
                // Draw an arrow pointing to the fireball
                drawDirectionalArrow(relX, relY, relZ, 1.0, distance);
            }
        }
        
        // Restore GL state
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws an arrow pointing toward the given coordinates
     */
    private void drawDirectionalArrow(double x, double y, double z, double size, double distance) {
        // Normalize direction vector
        double dirX = x / distance;
        double dirY = y / distance;
        double dirZ = z / distance;
        
        // Calculate perpendicular vectors for arrow head
        double perpX = -dirZ;
        double perpZ = dirX;
        double perpLength = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (perpLength > 0) {
            perpX /= perpLength;
            perpZ /= perpLength;
        }
        
        // Arrow length scales with distance (shorter when closer)
        double arrowLength = Math.min(2.0, distance * 0.2); 
        
        // Starting point (towards player)
        double startX = x - dirX * arrowLength;
        double startY = y;
        double startZ = z - dirZ * arrowLength;
        
        // Arrow head width
        double headWidth = 0.4;
        
        // Draw arrow line
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0, 0, 0); // Start at player position
        GL11.glVertex3d(startX, startY, startZ); // End at arrow base
        GL11.glEnd();
        
        // Draw arrow head
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(x, y, z); // Tip
        GL11.glVertex3d(startX + perpX * headWidth, startY, startZ + perpZ * headWidth); // Right corner
        GL11.glVertex3d(startX - perpX * headWidth, startY, startZ - perpZ * headWidth); // Left corner
        GL11.glEnd();
    }
    
    /**
     * Enhanced item ESP with better visuals and accurate item counts
     */
    private void renderImprovedItemESP(List<EntityItem> items, float partialTicks) {
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

            // Get actual item count from our tracking map
            int itemCount = 1; // default count
            if (itemEntityCounts.containsKey(item.getEntityId())) {
                itemCount = itemEntityCounts.get(item.getEntityId());
            } else {
                // Fallback to the item's stack size
                itemCount = stack.stackSize;
            }

            drawImprovedBox(relX, relY, relZ, boxSize, color, alpha);
            drawItemCount(relX, relY, relZ, itemCount, color);
        }
        
        // Restore GL state
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws an improved ESP box with smooth corners and better visibility
     */
    private void drawImprovedBox(double x, double y, double z, float size, int color, float alpha) {
        // Extract RGB components
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        
        // Draw outer glow effect (slightly larger, more transparent box)
        GL11.glColor4f(r, g, b, alpha * 0.3F);
        drawCube(x, y, z, size * 1.1F);
        
        // Draw inner box with higher opacity
        GL11.glColor4f(r, g, b, alpha * 0.7F);
        drawCube(x, y, z, size);
        
        // Draw outline with a slightly brighter color
        GL11.glColor4f(Math.min(1.0F, r * 1.2F), Math.min(1.0F, g * 1.2F), Math.min(1.0F, b * 1.2F), alpha + 0.3F);
        drawCubeOutline(x, y, z, size);
    }
    
    /**
     * Draws a solid cube
     */
    private void drawCube(double x, double y, double z, float size) {
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
    }
    
    /**
     * Draws a cube outline
     */
    private void drawCubeOutline(double x, double y, double z, float size) {
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
        // Extract RGB components for text color
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        
        // Set rendering to face the player
        GL11.glPushMatrix();
        GL11.glTranslated(x, y + 0.5, z); // Raise text slightly above the box
        
        // Make text face the player by applying inverse rotation
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        GL11.glRotatef(-player.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(player.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.025F, -0.025F, 0.025F); // Scale the text to a reasonable size
        
        // Re-enable textures for rendering text
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Ensure text is always visible
        
        // Draw the text with shadow for better visibility
        String text = String.valueOf(count);
        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        
        // Draw shadow first
        Minecraft.getMinecraft().fontRendererObj.drawString(text, -textWidth / 2 + 1,
            -Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2 + 1,
            0xFF000000, false);
            
        // Draw actual text
        Minecraft.getMinecraft().fontRendererObj.drawString(text, -textWidth / 2,
            -Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2,
            (0xFF << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255),
            false);
        
        // Restore state for continued rendering
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    /**
     * Identifies teams using the scoreboard or player display names
     * Integrated from provided code sample
     */
    private Map<String, List<String>> getTeamsFromScoreboard() {
        Map<String, List<String>> teams = new HashMap<>();
        
        try {
            if (Minecraft.getMinecraft().theWorld != null && 
                Minecraft.getMinecraft().theWorld.getScoreboard() != null) {
                
                for (net.minecraft.scoreboard.Team team : Minecraft.getMinecraft().theWorld.getScoreboard().getTeams()) {
                    List<String> members = new ArrayList<>();
                    for (String member : team.getMembershipCollection()) {
                        members.add(member);
                    }
                    teams.put(team.getRegisteredName(), members);
                }
            }
        } catch (Exception e) {
            // Fallback to our existing team detection if scoreboard fails
        }
        
        return teams;
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
        map.put("c", EnumChatFormatting.RED);        // Red Team
        map.put("9", EnumChatFormatting.BLUE);       // Blue Team
        map.put("e", EnumChatFormatting.YELLOW);     // Yellow Team
        map.put("a", EnumChatFormatting.GREEN);      // Green Team
        map.put("d", EnumChatFormatting.LIGHT_PURPLE); // Pink Team
        map.put("7", EnumChatFormatting.GRAY);       // Gray Team
        map.put("f", EnumChatFormatting.WHITE);      // White Team
        map.put("8", EnumChatFormatting.DARK_GRAY);  // Dark Gray Team
        return map;
    }

    private void initializeGame() {
        teams.clear();
        detectedPlayers.clear();
        playerTeam = null;

        // Initialize teams
        teams.put("RED", new Team("RED", EnumChatFormatting.RED));
        teams.put("BLUE", new Team("BLUE", EnumChatFormatting.BLUE));
        teams.put("YELLOW", new Team("YELLOW", EnumChatFormatting.YELLOW));
        teams.put("GREEN", new Team("GREEN", EnumChatFormatting.GREEN));
        teams.put("PINK", new Team("PINK", EnumChatFormatting.LIGHT_PURPLE));
        teams.put("GRAY", new Team("GRAY", EnumChatFormatting.GRAY));
        teams.put("WHITE", new Team("WHITE", EnumChatFormatting.WHITE));
        teams.put("DARK_GRAY", new Team("DARK_GRAY", EnumChatFormatting.DARK_GRAY));

        // Detect player's team
        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        if (localPlayer != null) {
            String teamColor = getTeamColor(localPlayer.getDisplayName().getFormattedText());
            if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {
                playerTeam = teams.get(getTeamName(teamColor));
                playerTeam.addPlayer(localPlayer.getName());
            }
        }

        // Update all player teams from tab list
        for (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {
            String playerName = info.getGameProfile().getName();
            String formattedName = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : "";
            String teamColor = getTeamColor(formattedName);
            if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {
                teams.get(getTeamName(teamColor)).addPlayer(playerName);
            }
        }
    }

    private String getTeamName(String colorCode) {
        switch (colorCode) {
            case "c": return "RED";
            case "9": return "BLUE";
            case "e": return "YELLOW";
            case "a": return "GREEN";
            case "d": return "PINK";
            case "7": return "GRAY";
            case "f": return "WHITE";
            case "8": return "DARK_GRAY";
            default: return null;
        }
    }

    private String extractPlayerName(String message) {
        String[] parts = message.split(" ");
        for (String part : parts) {
            if (part.startsWith("§") && !part.contains("§l") && !part.contains("§r")) {
                return part.replaceAll("§[0-9a-fk-or]", "");
            }
        }
        return null;
    }

    public class GuiConfigScreen extends GuiScreen {
        private final int BUTTON_WIDTH = 180;
        private final int BUTTON_HEIGHT = 20;
        private final int PADDING = 5;
        private int scrollOffset = 0;

        @Override
        public void initGui() {
            this.buttonList.clear();
            int y = height / 8 + scrollOffset;

            // Draw Alert Settings header
            drawSectionHeader("Alert Settings", y);
            y += 20;

            buttonList.add(createToggleButton(0, "Armor Alerts", enableArmorAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(1, "Item Alerts", enableItemAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(2, "Emerald Alerts", enableEmeraldAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(3, "Diamond Sword Alerts", enableSwordAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(4, "Potion Alerts", enablePotionAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(5, "Fireball Alerts", enableFireballAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(8, "Fireball Tracking", enableFireballTracking, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(6, "Exclude Teammates", excludeTeammates, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(7, "Item ESP", enableItemESP, y));
            y += BUTTON_HEIGHT + PADDING;

            // Team Status Section
            if (inBedWarsGame && !teams.isEmpty()) {
                y += 15;
                drawSectionHeader("Team Status", y);
                y += 20;

                for (Team team : teams.values()) {
                    if (!team.getPlayers().isEmpty()) {
                        drawTeamStatus(team, y);
                        y += 15;
                    }
                }
            }
        }

        private void drawTeamStatus(Team team, int y) {
            String status = team.getFormatting() + team.getColor() +
                          (team.hasBed() ? " ✔ " : " ✘ ") +
                          team.getPlayers().size() + " players";
            drawString(fontRendererObj, status, width/2 - 85, y, 0xFFFFFF);
        }

        private void drawSectionHeader(String text, int y) {
            String header = "§l" + text + "§r";
            drawCenteredString(fontRendererObj, header, width/2, y, 0xFFFFFF);
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
                case 3: enableSwordAlerts = !enableSwordAlerts; break;
                case 4: enablePotionAlerts = !enablePotionAlerts; break;
                case 5: enableFireballAlerts = !enableFireballAlerts; break;
                case 6: excludeTeammates = !excludeTeammates; break;
                case 7: enableItemESP = !enableItemESP; break;
                case 8: enableFireballTracking = !enableFireballTracking; break;
            }
            saveConfig();
            initGui();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            
            // Handle mouse scroll
            int dWheel = org.lwjgl.input.Mouse.getDWheel();
            if (dWheel != 0) {
                scrollOffset += (dWheel > 0) ? 20 : -20;
                int maxScroll = inBedWarsGame && !teams.isEmpty() ? -200 : -100;
                scrollOffset = Math.max(maxScroll, Math.min(0, scrollOffset));
                initGui();
            }

            // Draw title with formatting
            String title = "§l§eBed Wars Assistant Settings§r";
            drawCenteredString(fontRendererObj, title, width/2, 15, 0xFFFFFF);

            // Draw team info if in game
            if (inBedWarsGame && playerTeam != null) {
                String yourTeam = "Your Team: " + playerTeam.getFormatting() + playerTeam.getColor();
                drawCenteredString(fontRendererObj, yourTeam, width/2, 30, 0xFFFFFF);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            int mouseX = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
        }
    }

    private static class PlayerState {
        private Item lastHeldItem;
        private boolean lastHeldDiamondSword;
        private boolean wasHoldingFireball;
        private boolean hasAlertedDiamondArmor;
        private int lastEmeralds;
        private Map<Integer, Boolean> activePotions = new HashMap<>();
    }

    /**
     * Checks if the player is currently in a Hypixel Bed Wars game
     */
    private boolean isHypixelBedWars() {
        return inBedWarsGame;
    }

    /**
     * Gets the readable name of a potion effect by its ID
     */
    private String getPotionName(int potionId) {
        switch (potionId) {
            case SPEED_POTION_ID:
                return "Speed Potion";
            case JUMP_BOOST_POTION_ID:
                return "Jump Potion";
            case INVISIBILITY_POTION_ID:
                return "Invisibility Potion";
            default:
                return "Potion";
        }
    }
    
    /**
     * Class to store information about tracked fireballs
     */
    private static class FireballInfo {
        private final Entity shooter;
        private final String shooterName;
        private final long spawnTime;
        private boolean hasCheckedImpact = false;
        private boolean alertedImpact = false;
        
        public FireballInfo(Entity shooter, String shooterName, long spawnTime) {
            this.shooter = shooter;
            this.shooterName = shooterName;
            this.spawnTime = spawnTime;
        }
    }
}
