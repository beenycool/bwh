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
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import java.util.List;
import java.util.ArrayList;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.projectile.EntitySmallFireball;
@Mod(modid = HypixelBedWarsMod.MODID, name = HypixelBedWarsMod.NAME, version = HypixelBedWarsMod.VERSION)
public class HypixelBedWarsMod {
    public static final String MODID = "hypixelbedwarsmod";Mod.NAME, version = HypixelBedWarsMod.VERSION)
    public static final String NAME = "Hypixel Bed Wars Assistant";
    public static final String VERSION = "1.5";bedwarsmod";
    private static final Pattern BEDWARS_TITLE = Pattern.compile("BED WARS", Pattern.CASE_INSENSITIVE);
    private static final File CONFIG_FILE = new File("config/bedwars_assistant.cfg");
    private static final String CONFIG_CMD = ".bwconfig";compile("BED WARS", Pattern.CASE_INSENSITIVE);
    private static final File CONFIG_FILE = new File("config/bedwars_assistant.cfg");
    // Team color detection pattern - matches color codes at start of name
    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    private static final Map<String, EnumChatFormatting> COLOR_MAP = initializeColorMap();
    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    // Potion IDsc final Map<String, EnumChatFormatting> COLOR_MAP = initializeColorMap();
    private static final int SPEED_POTION_ID = 1;
    private static final int JUMP_BOOST_POTION_ID = 8;
    private static final int SPEED_POTION_ID = 1;
    // State trackingnal int JUMP_BOOST_POTION_ID = 8;
    private final Map<String, PlayerState> playerStates = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Set<String> detectedPlayers = new HashSet<>();shMap<>();
    private boolean inBedWarsGame = false;= new HashMap<>();
    private Team playerTeam;> detectedPlayers = new HashSet<>();
    private boolean inBedWarsGame = false;
    // Configuration options
    private boolean enableArmorAlerts = true;
    private boolean enableItemAlerts = true;
    private boolean enableEmeraldAlerts = true;
    private boolean enableSwordAlerts = true;
    private boolean enablePotionAlerts = true;;
    private boolean enableFireballAlerts = true;
    private boolean excludeTeammates = true; // New option to exclude teammates
    private boolean enableItemESP = true; // New option for item highlighting
    private float itemESPMaxDistance = 40.0F; // new configto exclude teammates
    private float itemESPFadeRange = 30.0F;   // new config item highlighting
    private boolean enableObsidianAlerts = true; new config
    private boolean enableFireballTrajectoryAlerts = true; // New option for fireball trajectory alerts    private float itemESPFadeRange = 30.0F;   // new config    private boolean enableFireballTrajectoryAlerts = true; // New option for fireball trajectory alerts
true;
    // Sound constants for different alerts// New option for fireball trajectory alerts
    private static final String SOUND_ARMOR = "random.orb";
    private static final String SOUND_DIAMOND_SWORD = "random.anvil_use";
    private static final String SOUND_FIREBALL = "mob.ghast.fireball";all";
    private static final String SOUND_POTION = "random.drink";il_use";
    private static final String SOUND_EMERALD = "random.levelup";
    private static final String SOUND_SPECIAL_ITEM = "random.successful_hit";    private static final String SOUND_POTION = "random.drink";    private static final String SOUND_SPECIAL_ITEM = "random.successful_hit";
tring SOUND_EMERALD = "random.levelup";
    // Colors for item ESPit";
    private static final int DIAMOND_COLOR = 0x00AAFF; // Cyan/light blue
    private static final int EMERALD_COLOR = 0x00FF00; // Green// Colors for item ESPprivate static final int EMERALD_COLOR = 0x00FF00; // Green
    MOND_COLOR = 0x00AAFF; // Cyan/light blue
    // Alert cooldown management
    private Map<String, Long> alertCooldowns = new HashMap<>();
    private static final long GENERAL_COOLDOWN = 5000; // 5 seconds between similar alerts
    private static final long OBSIDIAN_COOLDOWN = 10000; // 10 seconds between obsidian alertsprivate Map<String, Long> alertCooldowns = new HashMap<>();private static final long OBSIDIAN_COOLDOWN = 10000; // 10 seconds between obsidian alerts
    DOWN = 5000; // 5 seconds between similar alerts
    // Team detection patterns for Hypixel
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^(Red|Blue|Green|Yellow|Aqua|White|Pink|Gray) (.+)$", Pattern.CASE_INSENSITIVE);        private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^(Red|Blue|Green|Yellow|Aqua|White|Pink|Gray) (.+)$", Pattern.CASE_INSENSITIVE);
 patterns for Hypixel
    @Mod.EventHandler Pattern.compile("^(Red|Blue|Green|Yellow|Aqua|White|Pink|Gray) (.+)$", Pattern.CASE_INSENSITIVE);
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);e.EVENT_BUS.register(this);
        loadConfig();ublic void init(FMLInitializationEvent event) {   loadConfig();
    }        MinecraftForge.EVENT_BUS.register(this);    }

    private void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            saveConfig();adConfig() {fig();
            return;f (!CONFIG_FILE.exists()) {   return;
        }            saveConfig();        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {w BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {Reader(new FileReader(CONFIG_FILE))) {null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;            while ((line = reader.readLine()) != null) {                if (parts.length != 2) continue;
ne.split("=");
                switch (parts[0]) {ue;
                    case "enableArmorAlerts":
                        enableArmorAlerts = Boolean.parseBoolean(parts[1]);0]) {ArmorAlerts = Boolean.parseBoolean(parts[1]);
                        break;:
                    case "enableItemAlerts":;
                        enableItemAlerts = Boolean.parseBoolean(parts[1]);ItemAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableEmeraldAlerts":
                        enableEmeraldAlerts = Boolean.parseBoolean(parts[1]);EmeraldAlerts = Boolean.parseBoolean(parts[1]);
                        break;":
                    case "enableSwordAlerts":);
                        enableSwordAlerts = Boolean.parseBoolean(parts[1]);SwordAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enablePotionAlerts":
                        enablePotionAlerts = Boolean.parseBoolean(parts[1]);PotionAlerts = Boolean.parseBoolean(parts[1]);
                        break;
                    case "enableFireballAlerts":
                        enableFireballAlerts = Boolean.parseBoolean(parts[1]);FireballAlerts = Boolean.parseBoolean(parts[1]);
                        break;ts":
                    case "excludeTeammates":1]);
                        excludeTeammates = Boolean.parseBoolean(parts[1]);eTeammates = Boolean.parseBoolean(parts[1]);
                        break;s":
                    case "enableItemESP":]);
                        enableItemESP = Boolean.parseBoolean(parts[1]);ItemESP = Boolean.parseBoolean(parts[1]);
                        break;
                    case "itemESPMaxDistance":
                        itemESPMaxDistance = Float.parseFloat(parts[1]);PMaxDistance = Float.parseFloat(parts[1]);
                        break;":
                    case "itemESPFadeRange":);
                        itemESPFadeRange = Float.parseFloat(parts[1]);PFadeRange = Float.parseFloat(parts[1]);
                        break;
                    case "enableObsidianAlerts":
                        enableObsidianAlerts = Boolean.parseBoolean(parts[1]);ObsidianAlerts = Boolean.parseBoolean(parts[1]);
                        break;   case "enableObsidianAlerts":       break;
                    case "enableFireballTrajectoryAlerts":           enableObsidianAlerts = Boolean.parseBoolean(parts[1]);       case "enableFireballTrajectoryAlerts":
                        enableFireballTrajectoryAlerts = Boolean.parseBoolean(parts[1]);llTrajectoryAlerts = Boolean.parseBoolean(parts[1]);
                        break;FireballTrajectoryAlerts":
                }               enableFireballTrajectoryAlerts = Boolean.parseBoolean(parts[1]);       }
            }                   break;       }
        } catch (IOException e) {                }        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write("enableArmorAlerts=" + enableArmorAlerts + "\n");
            writer.write("enableItemAlerts=" + enableItemAlerts + "\n");LE))) {
            writer.write("enableEmeraldAlerts=" + enableEmeraldAlerts + "\n"););"\n");
            writer.write("enableSwordAlerts=" + enableSwordAlerts + "\n");"\n");+ "\n");
            writer.write("enablePotionAlerts=" + enablePotionAlerts + "\n"););
            writer.write("enableFireballAlerts=" + enableFireballAlerts + "\n"););+ "\n");
            writer.write("excludeTeammates=" + excludeTeammates + "\n");
            writer.write("enableItemESP=" + enableItemESP + "\n");ireballAlerts=" + enableFireballAlerts + "\n");temESP=" + enableItemESP + "\n");
            writer.write("itemESPMaxDistance=" + itemESPMaxDistance + "\n");eTeammates=" + excludeTeammates + "\n");PMaxDistance=" + itemESPMaxDistance + "\n");
            writer.write("itemESPFadeRange=" + itemESPFadeRange + "\n");   writer.write("enableItemESP=" + enableItemESP + "\n");   writer.write("itemESPFadeRange=" + itemESPFadeRange + "\n");
            writer.write("enableObsidianAlerts=" + enableObsidianAlerts + "\n");       writer.write("itemESPMaxDistance=" + itemESPMaxDistance + "\n");       writer.write("enableObsidianAlerts=" + enableObsidianAlerts + "\n");
            writer.write("enableFireballTrajectoryAlerts=" + enableFireballTrajectoryAlerts + "\n");            writer.write("itemESPFadeRange=" + itemESPFadeRange + "\n");            writer.write("enableFireballTrajectoryAlerts=" + enableFireballTrajectoryAlerts + "\n");
        } catch (IOException e) {write("enableObsidianAlerts=" + enableObsidianAlerts + "\n");Exception e) {
            e.printStackTrace();s=" + enableFireballTrajectoryAlerts + "\n");
        }
    }
}
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = event.message.getFormattedText();xt();
        String unformattedMessage = event.message.getUnformattedText();c void onChat(ClientChatReceivedEvent event) {tring unformattedMessage = event.message.getUnformattedText();
                String message = event.message.getFormattedText();        
        if (unformattedMessage.startsWith(CONFIG_CMD)) {t.message.getUnformattedText();h(CONFIG_CMD)) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiConfigScreen());                    Minecraft.getMinecraft().displayGuiScreen(new GuiConfigScreen());
            event.setCanceled(true);ge.startsWith(CONFIG_CMD)) {d(true);
        }
e);
        if (!isHypixelBedWars()) return;

        // Detect game startf (!isHypixelBedWars()) return;/ Detect game start
        if (message.contains("§r§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r §r§f §r§f§lBed Wars§r")) {        if (message.contains("§r§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r §r§f §r§f§lBed Wars§r")) {
            inBedWarsGame = true;
            initializeGame();▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r §r§f §r§f§lBed Wars§r")) {
            return;
        }

        // Track bed destruction
        if (message.matches(".*§r§f§lBED DESTRUCTION > .* bed was destroyed by .*")) {
            String[] parts = message.split(" bed was destroyed by ");
            if (parts.length == 2) {
                String teamColor = getTeamColor(parts[0]);parts = message.split(" bed was destroyed by ");g teamColor = getTeamColor(parts[0]);
                if (teamColor != null && teams.containsKey(teamColor)) {arts.length == 2) {f (teamColor != null && teams.containsKey(teamColor)) {
                    teams.get(teamColor).setBedState(false);   String teamColor = getTeamColor(parts[0]);       teams.get(teamColor).setBedState(false);
                    if (playerTeam != null && teamColor.equals(playerTeam.getColor())) {       if (teamColor != null && teams.containsKey(teamColor)) {           if (playerTeam != null && teamColor.equals(playerTeam.getColor())) {
                        sendAlert(EnumChatFormatting.RED + "YOUR TEAM'S BED WAS DESTROYED!", true, "random.anvil_land");                    teams.get(teamColor).setBedState(false);                        sendAlert(EnumChatFormatting.RED + "YOUR TEAM'S BED WAS DESTROYED!", true, "random.anvil_land");
                    }erTeam != null && teamColor.equals(playerTeam.getColor())) {
                }ng.RED + "YOUR TEAM'S BED WAS DESTROYED!", true, "random.anvil_land");
            }
        }

        // Track final kills
        if (message.contains("§r§fFINAL KILL!")) {
            String killedPlayer = extractPlayerName(message);
            if (killedPlayer != null) {
                for (Team team : teams.values()) {
                    if (team.getPlayers().contains(killedPlayer)) {yer != null) {eam.getPlayers().contains(killedPlayer)) {
                        team.removePlayer(killedPlayer); : teams.values()) {emovePlayer(killedPlayer);
                        if (team.getPlayers().isEmpty() && !team.hasBed()) {f (team.getPlayers().contains(killedPlayer)) {   if (team.getPlayers().isEmpty() && !team.hasBed()) {
                            String teamElimMsg = team.getFormatting() + "TEAM ELIMINATED > " + team.getColor() + " team has been eliminated!";       team.removePlayer(killedPlayer);           String teamElimMsg = team.getFormatting() + "TEAM ELIMINATED > " + team.getColor() + " team has been eliminated!";
                            sendAlert(teamElimMsg, true, "random.levelup");           if (team.getPlayers().isEmpty() && !team.hasBed()) {               sendAlert(teamElimMsg, true, "random.levelup");
                        }                   String teamElimMsg = team.getFormatting() + "TEAM ELIMINATED > " + team.getColor() + " team has been eliminated!";               }
                        break;                            sendAlert(teamElimMsg, true, "random.levelup");                        break;
                    }
                }
            }
        }

        // Detect game end
        if (message.contains("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") ||▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") ||
            message.contains("§r§e§lVICTORY!") ||/ Detect game end   message.contains("§r§e§lVICTORY!") ||
            message.contains("§r§c§lGAME OVER!")) {        if (message.contains("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") ||            message.contains("§r§c§lGAME OVER!")) {
            inBedWarsGame = false;CTORY!") ||
            detectedPlayers.clear();
            return;
        }

        // Detect elimination messages/ Detect elimination messages
        if (unformattedMessage.contains("You have been eliminated!") || (unformattedMessage.contains("You have been eliminated!") ||
            unformattedMessage.contains("You died!") ||        // Detect elimination messages            unformattedMessage.contains("You died!") ||
            unformattedMessage.contains("was killed by")) {ttedMessage.contains("You have been eliminated!") ||ttedMessage.contains("was killed by")) {
            detectedPlayers.clear();
        })) {
    }            detectedPlayers.clear();    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
{
        checkBedWarsStatus();        if (event.phase != TickEvent.Phase.END) return;        checkBedWarsStatus();
        if (!inBedWarsGame) return;
        checkBedWarsStatus();
        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;raft().thePlayer;
        World world = Minecraft.getMinecraft().theWorld;

        if (localPlayer == null || world == null) return;

        // Remove dead players from tracking
        Set<String> tabPlayers = getTabListPlayers();
        playerStates.keySet().removeIf(name -> !tabPlayers.contains(name));        // Track fireballs        playerStates.keySet().removeIf(name -> !tabPlayers.contains(name));
        detectedPlayers.removeIf(name -> !tabPlayers.contains(name)););

        for (Entity entity : world.getLoadedEntityList()) {or (Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityPlayer) || entity == localPlayer) continue;  if (!(entity instanceof EntityPlayer) || entity == localPlayer) continue;
        // Remove dead players from tracking
            EntityPlayer player = (EntityPlayer) entity;ListPlayers();tyPlayer) entity;
            processPlayer(player, localPlayer);rs.contains(name));
        }(name));
    }
 entity : world.getLoadedEntityList()) {
    private void checkBedWarsStatus() {   if (!(entity instanceof EntityPlayer) || entity == localPlayer) continue;te void checkBedWarsStatus() {
        World world = Minecraft.getMinecraft().theWorld;        World world = Minecraft.getMinecraft().theWorld;
        if (world == null || world.getScoreboard() == null) {
            inBedWarsGame = false;
            return;   }       return;
        }    }        }

        ScoreObjective sidebar = world.getScoreboard().getObjectiveInDisplaySlot(1);
        inBedWarsGame = sidebar != null && BEDWARS_TITLE.matcher(sidebar.getDisplayName()).find();layName()).find();
    }        if (world == null || world.getScoreboard() == null) {    }

    // Modified to include localPlayer for team checks
    private void processPlayer(EntityPlayer player, EntityPlayer localPlayer) {}ate void processPlayer(EntityPlayer player, EntityPlayer localPlayer) {
        if (isRespawning(player)) return; // Skip respawning players
 world.getScoreboard().getObjectiveInDisplaySlot(1);
        String playerName = player.getName();TLE.matcher(sidebar.getDisplayName()).find();
        String teamColor = getTeamColor(player.getDisplayName().getFormattedText());
        
        // Update team assignment if needed
        if (teamColor != null) {layer localPlayer) {
            String teamName = getTeamName(teamColor);awning players
            if (teamName != null && teams.containsKey(teamName)) {
                Team playerTeam = teams.get(teamName);Name = player.getName();playerTeam = teams.get(teamName);
                if (!playerTeam.getPlayers().contains(playerName)) {playName().getFormattedText());(playerName)) {
                    // Remove from other teams if necessaryove from other teams if necessary
                    for (Team team : teams.values()) {date team assignment if needed       for (Team team : teams.values()) {
                        team.removePlayer(playerName);f (teamColor != null) {               team.removePlayer(playerName);
                    }            String teamName = getTeamName(teamColor);                    }
                    playerTeam.addPlayer(playerName);Key(teamName)) {e);
                }
            }(!playerTeam.getPlayers().contains(playerName)) {
        }           // Remove from other teams if necessary
                    for (Team team : teams.values()) {
        // Skip teammates if the option is enabled
        if (excludeTeammates && isSameTeam(player, localPlayer)) {                    }        if (excludeTeammates && isSameTeam(player, localPlayer)) {
            return;.addPlayer(playerName);
        }

        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());

        // Process core alerts
        if (enableArmorAlerts) checkArmor(player, state);
        if (enableItemAlerts) checkHeldItem(player, state);            return;        if (enableItemAlerts) checkHeldItem(player, state);
        if (enableEmeraldAlerts) checkEmeralds(player, state);, state);
        if (enableSwordAlerts) checkDiamondSword(player, state);
        if (enablePotionAlerts) checkPotions(player, state);layerName, k -> new PlayerState()););
        if (enableFireballAlerts && enableItemAlerts) checkFireball(player, state);

        // Team-specific checks
        if (teamColor != null) {
            Team team = teams.get(getTeamName(teamColor));
            if (team != null && !team.hasBed()) {
                // Additional alert for low-health players without a bedePotionAlerts) checkPotions(player, state);/ Additional alert for low-health players without a bed
                if (player.getHealth() <= 10.0F) { // 5 hearts or lessnableFireballAlerts && enableItemAlerts) checkFireball(player, state);   if (player.getHealth() <= 10.0F) { // 5 hearts or less
                    sendAlert(getColoredPlayerName(player) +  sendAlert(getColoredPlayerName(player) +
                            EnumChatFormatting.RED + " is low health with NO BED! " +   // Team-specific checks                       EnumChatFormatting.RED + " is low health with NO BED! " +
                            getDistanceString(player), true, "random.successful_hit");        if (teamColor != null) {                            getDistanceString(player), true, "random.successful_hit");
                }
            }
        }layers without a bed
    } less

    private void checkArmor(EntityPlayer player, PlayerState state) {is low health with NO BED! " +State state) {
        ItemStack leggings = player.getCurrentArmor(1);                            getDistanceString(player), true, "random.successful_hit");        ItemStack leggings = player.getCurrentArmor(1);
        ItemStack boots = player.getCurrentArmor(0);
        boolean hasDiamond = leggings != null && boots != null &&
                leggings.getItem() == Items.diamond_leggings &&
                boots.getItem() == Items.diamond_boots;

        if (hasDiamond && !detectedPlayers.contains(player.getName())) { state) {etName())) {
            int currentEmeralds = countEmeralds(player.inventory);
            int remaining = Math.max(currentEmeralds - 6, 0);
            if (inBedWarsGame) {asDiamond = leggings != null && boots != null &&nBedWarsGame) {
                if (inBedWarsGame) {   leggings.getItem() == Items.diamond_leggings &&   if (inBedWarsGame) {
                    sendAlert(getColoredPlayerName(player) +oots;(player) +
                        EnumChatFormatting.AQUA + " bought Diamond Armor! (" +      EnumChatFormatting.AQUA + " bought Diamond Armor! (" +
                        remaining + " Emeralds remaining)", true, SOUND_ARMOR);   if (hasDiamond && !detectedPlayers.contains(player.getName())) {                   remaining + " Emeralds remaining)", true, SOUND_ARMOR);
                }            int currentEmeralds = countEmeralds(player.inventory);                }
            }
            detectedPlayers.add(player.getName());
        }e) {
    }edPlayerName(player) +
     EnumChatFormatting.AQUA + " bought Diamond Armor! (" +
    private void checkHeldItem(EntityPlayer player, PlayerState state) {               remaining + " Emeralds remaining)", true, SOUND_ARMOR);te void checkHeldItem(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();                }        ItemStack heldStack = player.getHeldItem();
        if (heldStack == null) {
            state.lastHeldItem = null;);
            return;
        }

        Item currentItem = heldStack.getItem(); player, PlayerState state) {m();
        if (currentItem != state.lastHeldItem) {
            state.lastHeldItem = currentItem;
            
            // Only alert for specific items when they're first held
            if (currentItem == Items.bow) {
                sendAlertWithCooldown(player.getName() + "_bow", 
                    getColoredPlayerName(player) +
                    EnumChatFormatting.AQUA + " is holding a BOW " +
                    getDistanceString(player), true, SOUND_SPECIAL_ITEM);
            } else if (currentItem == Items.ender_pearl) {
                sendAlertWithCooldown(player.getName() + "_pearl",
                    getColoredPlayerName(player) +
                    EnumChatFormatting.AQUA + " has ENDER PEARLS " +
                    getDistanceString(player), true, SOUND_SPECIAL_ITEM);
            } else if (currentItem == Item.getItemFromBlock(Blocks.obsidian) && enableObsidianAlerts) {       EnumChatFormatting.AQUA + " is holding a BOW " + else if (currentItem == Item.getItemFromBlock(Blocks.obsidian) && enableObsidianAlerts) {
                sendAlertWithCooldown(player.getName() + "_obsidian",           getDistanceString(player), true, SOUND_SPECIAL_ITEM);       sendAlertWithCooldown(player.getName() + "_obsidian",
                    getColoredPlayerName(player) +       } else if (currentItem == Items.ender_pearl) {               getColoredPlayerName(player) +
                    EnumChatFormatting.DARK_PURPLE + " is holding OBSIDIAN " +            sendAlertWithCooldown(player.getName() + "_pearl",                EnumChatFormatting.DARK_PURPLE + " is holding OBSIDIAN " +
                    getDistanceString(player), true, SOUND_SPECIAL_ITEM);
            } ENDER PEARLS " +
        }
    }ItemFromBlock(Blocks.obsidian) && enableObsidianAlerts) {
    ) + "_obsidian",
    private void checkDiamondSword(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem(); " +
        if (heldStack != null && heldStack.getItem() == Items.diamond_sword && !state.lastHeldDiamondSword) {mondSword) {
            state.lastHeldDiamondSword = true;
            sendAlert(getColoredPlayerName(player) +    sendAlert(getColoredPlayerName(player) + 
                    EnumChatFormatting.AQUA + " has a DIAMOND SWORD! " +               EnumChatFormatting.AQUA + " has a DIAMOND SWORD! " +
                    getDistanceString(player), true, SOUND_DIAMOND_SWORD);                getDistanceString(player), true, SOUND_DIAMOND_SWORD);
        } else if (heldStack == null || heldStack.getItem() != Items.diamond_sword) {e) {mond_sword) {
            state.lastHeldDiamondSword = false;
        }tack.getItem() == Items.diamond_sword && !state.lastHeldDiamondSword) {
    }    state.lastHeldDiamondSword = true;
    er) + 
    private void checkFireball(EntityPlayer player, PlayerState state) {
        ItemStack heldStack = player.getHeldItem();), true, SOUND_DIAMOND_SWORD);Item();
        if (heldStack == null) return;tItem() != Items.diamond_sword) {
        
        Item currentItem = heldStack.getItem();
        if (currentItem == Items.fire_charge && !state.wasHoldingFireball) {
            state.wasHoldingFireball = true;
            sendAlert(getColoredPlayerName(player) +te void checkFireball(EntityPlayer player, PlayerState state) {   sendAlert(getColoredPlayerName(player) +
                    EnumChatFormatting.RED + " is holding a FIREBALL! " +   ItemStack heldStack = player.getHeldItem();               EnumChatFormatting.RED + " is holding a FIREBALL! " +
                    getDistanceString(player), true, SOUND_FIREBALL);    if (heldStack == null) return;                getDistanceString(player), true, SOUND_FIREBALL);
        } else if (currentItem != Items.fire_charge) {
            state.wasHoldingFireball = false;
        }if (currentItem == Items.fire_charge && !state.wasHoldingFireball) {}
    }
    (player) +
    private void checkPotions(EntityPlayer player, PlayerState state) {ing a FIREBALL! " +yerState state) {
        Map<Integer, Boolean> currentPotions = new HashMap<>();
        charge) {
        for (PotionEffect effect : player.getActivePotionEffects()) {e.wasHoldingFireball = false;ionEffect effect : player.getActivePotionEffects()) {
            int id = effect.getPotionID();
            // Only track Speed and Jump Boost potions
            if (id == SPEED_POTION_ID || id == JUMP_BOOST_POTION_ID) {
                currentPotions.put(id, true);
                
                // Alert for newly acquired potions
                if (!state.activePotions.containsKey(id)) {ts()) {
                    long now = System.currentTimeMillis();
                    // Only alert if last alert was over 10 seconds ago
                    if (!state.potionAlertTimestamps.containsKey(id) 
                            || now - state.potionAlertTimestamps.get(id) > 10000) {
                        String potionName = getPotionName(id);ring potionName = getPotionName(id);
                        sendAlert(getColoredPlayerName(player) +/ Alert for newly acquired potions       sendAlert(getColoredPlayerName(player) +
                                EnumChatFormatting.LIGHT_PURPLE + " drank " + potionName + "! " +   if (!state.activePotions.containsKey(id)) {                   EnumChatFormatting.LIGHT_PURPLE + " drank " + potionName + "! " +
                                getDistanceString(player), true, SOUND_POTION);           long now = System.currentTimeMillis();                       getDistanceString(player), true, SOUND_POTION);
                        state.potionAlertTimestamps.put(id, now);            // Only alert if last alert was over 10 seconds ago                state.potionAlertTimestamps.put(id, now);
                    }nAlertTimestamps.containsKey(id) 
                }{
            }
        }
        _PURPLE + " drank " + potionName + "! " +
        // Check for expired potions
        for (Map.Entry<Integer, Boolean> entry : state.activePotions.entrySet()) {;ions.entrySet()) {
            if (!currentPotions.containsKey(entry.getKey())) {       }f (!currentPotions.containsKey(entry.getKey())) {
                String potionName = getPotionName(entry.getKey());       }       String potionName = getPotionName(entry.getKey());
                sendAlert(getColoredPlayerName(player) +    }        sendAlert(getColoredPlayerName(player) +
                        EnumChatFormatting.GRAY + "'s " + potionName + " wore off. " +. " +
                        getDistanceString(player), false, null);                      getDistanceString(player), false, null);
            }    // Check for expired potions        }
        } for (Map.Entry<Integer, Boolean> entry : state.activePotions.entrySet()) { }
        ) {
        state.activePotions = currentPotions;         String potionName = getPotionName(entry.getKey()); state.activePotions = currentPotions;
    }
    ore off. " +
    /**e, null);
     * Gets the player's name with their team color applied
     */}
    private String getColoredPlayerName(EntityPlayer player) {
        String formattedName = player.getDisplayName().getFormattedText();xt();
        String colorCode = getTeamColor(formattedName);g colorCode = getTeamColor(formattedName);
        String playerName = player.getName();ng playerName = player.getName();
        
        if (colorCode != null && COLOR_MAP.containsKey(colorCode)) {plied(colorCode)) {
            return COLOR_MAP.get(colorCode) + playerName;*/       return COLOR_MAP.get(colorCode) + playerName;
        }    private String getColoredPlayerName(EntityPlayer player) {        }
        FormattedText();
        // Default to yellow if no team color foundde = getTeamColor(formattedName);yellow if no team color found
        return EnumChatFormatting.YELLOW + playerName;
    }

    private int countEmeralds(InventoryPlayer inventory) {e) + playerName;ayer inventory) {
        int count = 0; = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {r (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i); yellow if no team color found stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.emerald) {   return EnumChatFormatting.YELLOW + playerName;       if (stack != null && stack.getItem() == Items.emerald) {
                count += stack.stackSize;    }                count += stack.stackSize;
            }
        }
        return count;   int count = 0;   return count;
    }        for (int i = 0; i < inventory.getSizeInventory(); i++) {    }
i);
    private String getDistanceString(EntityPlayer player) {() == Items.emerald) {yer player) {
        return EnumChatFormatting.GRAY + "(" + (int) player.getDistanceToEntity(Minecraft.getMinecraft().thePlayer) + "m)";raft.getMinecraft().thePlayer) + "m)";
    }

    private boolean isRespawning(EntityPlayer player) {
        // Check position and facing direction
        boolean atSpawn = Math.abs(player.posX - 0.5) < 0.2 && olean atSpawn = Math.abs(player.posX - 0.5) < 0.2 && 
                          Math.abs(player.posZ - 0.5) < 0.2 &&     private String getDistanceString(EntityPlayer player) {                          Math.abs(player.posZ - 0.5) < 0.2 && 
                          player.posY >= 120 && player.posY <= 121;+ (int) player.getDistanceToEntity(Minecraft.getMinecraft().thePlayer) + "m)";&& player.posY <= 121;
        boolean facingSouth = Math.abs(player.rotationYaw - 180) < 10;
        return atSpawn && facingSouth;
    }ng(EntityPlayer player) {
/ Check position and facing direction
    private Set<String> getTabListPlayers() {        boolean atSpawn = Math.abs(player.posX - 0.5) < 0.2 &&     private Set<String> getTabListPlayers() {
        Set<String> tabPlayers = new HashSet<>();
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().thePlayer.sendQueue == null) {Y <= 121;Minecraft.getMinecraft().thePlayer.sendQueue == null) {
            return tabPlayers;h.abs(player.rotationYaw - 180) < 10;
        }eturn atSpawn && facingSouth;

        for (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {r (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {
            String name = info.getGameProfile().getName();    private Set<String> getTabListPlayers() {            String name = info.getGameProfile().getName();
            tabPlayers.add(name);w HashSet<>();
        }aft.getMinecraft().thePlayer.sendQueue == null) {
        return tabPlayers;            return tabPlayers;        return tabPlayers;
    }

    private long lastSoundTime = 0;for (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {ate long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN = 1000; // 1 second

    private void sendAlert(String message, boolean sound) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));eturn tabPlayers;inecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
           
        if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {        if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {
            Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
            lastSoundTime = System.currentTimeMillis();
        }
    }
Player.addChatMessage(new ChatComponentText(message));
    private void sendAlert(String message, boolean sound, String soundName) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message)); System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {tMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        
        if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {astSoundTime = System.currentTimeMillis();ound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) {
            if (soundName != null) {
                Minecraft.getMinecraft().thePlayer.playSound(soundName, 1.0F, 1.0F);   Minecraft.getMinecraft().thePlayer.playSound(soundName, 1.0F, 1.0F);
            } else {  } else {
                Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);    private void sendAlert(String message, boolean sound, String soundName) {                Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
            } Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));     }
            lastSoundTime = System.currentTimeMillis();
        } if (sound && System.currentTimeMillis() - lastSoundTime > SOUND_COOLDOWN) { }
    }
ySound(soundName, 1.0F, 1.0F);
    /**
     * Sends an alert with a cooldown to prevent spam        Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);ends an alert with a cooldown to prevent spam
     */
    private void sendAlertWithCooldown(String alertKey, String message, boolean sound, String soundName) {);y, String message, boolean sound, String soundName) {
        long currentTime = System.currentTimeMillis();
        long cooldownTime = alertKey.contains("obsidian") ? OBSIDIAN_COOLDOWN : GENERAL_COOLDOWN;wnTime = alertKey.contains("obsidian") ? OBSIDIAN_COOLDOWN : GENERAL_COOLDOWN;
        
        // Check if the alert is on cooldown
        if (!alertCooldowns.containsKey(alertKey) || nds an alert with a cooldown to prevent spamf (!alertCooldowns.containsKey(alertKey) || 
            currentTime - alertCooldowns.get(alertKey) > cooldownTime) {*/       currentTime - alertCooldowns.get(alertKey) > cooldownTime) {
                private void sendAlertWithCooldown(String alertKey, String message, boolean sound, String soundName) {            
            sendAlert(message, sound, soundName); long currentTime = System.currentTimeMillis();     sendAlert(message, sound, soundName);
            alertCooldowns.put(alertKey, currentTime);COOLDOWN;
        }  }
    }

    /**
     * Checks if two players are on the same team by comparing their name color prefixes
     */       sendAlert(message, sound, soundName);*/
    private boolean isSameTeam(EntityPlayer player1, EntityPlayer player2) {        alertCooldowns.put(alertKey, currentTime);private boolean isSameTeam(EntityPlayer player1, EntityPlayer player2) {
        String team1 = getTeamColor(player1.getDisplayName().getFormattedText()); } String team1 = getTeamColor(player1.getDisplayName().getFormattedText());
        String team2 = getTeamColor(player2.getDisplayName().getFormattedText());
        return team1 != null && team1.equals(team2);
    }
    mparing their name color prefixes
    /**
     * Extracts the team color code from a player's formatted namem(EntityPlayer player1, EntityPlayer player2) {r code from a player's formatted name
     * @return The color code or null if no color code foundplayer1.getDisplayName().getFormattedText());l if no color code found
     */tring team2 = getTeamColor(player2.getDisplayName().getFormattedText());
    private String getTeamColor(String formattedName) { != null && team1.equals(team2);etTeamColor(String formattedName) {
        Matcher matcher = TEAM_COLOR_PATTERN.matcher(formattedName);   Matcher matcher = TEAM_COLOR_PATTERN.matcher(formattedName);
        if (matcher.find()) {            if (matcher.find()) {
            return matcher.group(1);
        }name
        return null;
    }

    private static Map<String, EnumChatFormatting> initializeColorMap() {);Map() {
        Map<String, EnumChatFormatting> map = new HashMap<>();
        map.put("c", EnumChatFormatting.RED);        // Red Team
        map.put("9", EnumChatFormatting.BLUE);       // Blue Team
        map.put("e", EnumChatFormatting.YELLOW);     // Yellow Team
        map.put("a", EnumChatFormatting.GREEN);      // Green Teamatting.GREEN);      // Green Team
        map.put("d", EnumChatFormatting.LIGHT_PURPLE); // Pink Teamp.put("d", EnumChatFormatting.LIGHT_PURPLE); // Pink Team
        map.put("7", EnumChatFormatting.GRAY);       // Gray Team    private static Map<String, EnumChatFormatting> initializeColorMap() {        map.put("7", EnumChatFormatting.GRAY);       // Gray Team
        map.put("f", EnumChatFormatting.WHITE);      // White Team Map<String, EnumChatFormatting> map = new HashMap<>(); map.put("f", EnumChatFormatting.WHITE);      // White Team
        map.put("8", EnumChatFormatting.DARK_GRAY);  // Dark Gray TeamTeam Gray Team
        return map;lue Team
    } map.put("e", EnumChatFormatting.YELLOW);     // Yellow Team
n Team
    /**Pink Team
     * Gets all teams from the scoreboard with their membersmap.put("7", EnumChatFormatting.GRAY);       // Gray Teamets all teams from the scoreboard with their members
     * @return Map of team names to lists of player nameshite Team
     */
    private Map<String, List<String>> getScoreboardTeams() {ring>> getScoreboardTeams() {
        Map<String, List<String>> teams = new HashMap<>();tring, List<String>> teams = new HashMap<>();
        
        if (Minecraft.getMinecraft().theWorld == null || 
            Minecraft.getMinecraft().theWorld.getScoreboard() == null) {
            return teams;ames
        }
        > getScoreboardTeams() {
        // Get teams from the scoreboardtring, List<String>> teams = new HashMap<>();t teams from the scoreboard
        for (net.minecraft.scoreboard.Team team : Minecraft.getMinecraft().theWorld.getScoreboard().getTeams()) {ms()) {
            List<String> members = new ArrayList<>();f (Minecraft.getMinecraft().theWorld == null ||    List<String> members = new ArrayList<>();
            for (String member : team.getMembershipCollection()) {    Minecraft.getMinecraft().theWorld.getScoreboard() == null) {    for (String member : team.getMembershipCollection()) {
                members.add(member);ams;rs.add(member);
            }   }       }
            teams.put(team.getRegisteredName(), members);            teams.put(team.getRegisteredName(), members);
        } // Get teams from the scoreboard }
        t.getMinecraft().theWorld.getScoreboard().getTeams()) {
        return teams;     List<String> members = new ArrayList<>(); return teams;
    }ction()) {
    
    /**    }
     * Maps Hypixel team names to our internal team colorsme(), members);rnal team colors
     */
    private String mapTeamNameToColor(String teamName) {
        if (teamName == null) return null;
        
        teamName = teamName.toLowerCase();
        if (teamName.contains("red")) return "RED";
        if (teamName.contains("blue")) return "BLUE";olors
        if (teamName.contains("green")) return "GREEN";
        if (teamName.contains("yellow")) return "YELLOW";apTeamNameToColor(String teamName) {.contains("yellow")) return "YELLOW";
        if (teamName.contains("aqua")) return "AQUA";   if (teamName == null) return null;   if (teamName.contains("aqua")) return "AQUA";
        if (teamName.contains("white")) return "WHITE";                if (teamName.contains("white")) return "WHITE";
        if (teamName.contains("pink")) return "PINK";Case();")) return "PINK";
        if (teamName.contains("gray")) return "GRAY";ontains("red")) return "RED";ontains("gray")) return "GRAY";
        return null;lue")) return "BLUE";
    }ins("green")) return "GREEN";
        if (teamName.contains("yellow")) return "YELLOW";
    private void initializeGame() {ns("aqua")) return "AQUA";Game() {
        teams.clear();
        detectedPlayers.clear();
        playerTeam = null;

        // Initialize teams
        teams.put("RED", new Team("RED", EnumChatFormatting.RED));
        teams.put("BLUE", new Team("BLUE", EnumChatFormatting.BLUE));
        teams.put("YELLOW", new Team("YELLOW", EnumChatFormatting.YELLOW));
        teams.put("GREEN", new Team("GREEN", EnumChatFormatting.GREEN));        detectedPlayers.clear();        teams.put("GREEN", new Team("GREEN", EnumChatFormatting.GREEN));
        teams.put("PINK", new Team("PINK", EnumChatFormatting.LIGHT_PURPLE));
        teams.put("GRAY", new Team("GRAY", EnumChatFormatting.GRAY));
        teams.put("WHITE", new Team("WHITE", EnumChatFormatting.WHITE));
        teams.put("DARK_GRAY", new Team("DARK_GRAY", EnumChatFormatting.DARK_GRAY));tting.DARK_GRAY));
;
        // Get players from scoreboard teams first (more reliable)tting.YELLOW));iable)
        Map<String, List<String>> scoreboardTeams = getScoreboardTeams();.GREEN));rdTeams();
        for (Map.Entry<String, List<String>> entry : scoreboardTeams.entrySet()) {INK", new Team("PINK", EnumChatFormatting.LIGHT_PURPLE));ry<String, List<String>> entry : scoreboardTeams.entrySet()) {
            String teamColor = mapTeamNameToColor(entry.getKey());tting.GRAY));getKey());
            if (teamColor != null && teams.containsKey(teamColor)) {);
                for (String playerName : entry.getValue()) {
                    teams.get(teamColor).addPlayer(playerName);
                    s from scoreboard teams first (more reliable)
                    // Check if this is the local playerg, List<String>> scoreboardTeams = getScoreboardTeams();   // Check if this is the local player
                    if (Minecraft.getMinecraft().thePlayer != null && Map.Entry<String, List<String>> entry : scoreboardTeams.entrySet()) {       if (Minecraft.getMinecraft().thePlayer != null && 
                        playerName.equals(Minecraft.getMinecraft().thePlayer.getName())) {   String teamColor = mapTeamNameToColor(entry.getKey());               playerName.equals(Minecraft.getMinecraft().thePlayer.getName())) {
                        playerTeam = teams.get(teamColor);            if (teamColor != null && teams.containsKey(teamColor)) {                        playerTeam = teams.get(teamColor);
                    }
                }mColor).addPlayer(playerName);
            }
        }s the local player

        // Fallback to color code detection if scoreboard teams are not available))) {
        if (playerTeam == null) {
            EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
            if (localPlayer != null) {ocalPlayer != null) {
                String teamColor = getTeamColor(localPlayer.getDisplayName().getFormattedText());   String teamColor = getTeamColor(localPlayer.getDisplayName().getFormattedText());
                if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {       if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {
                    playerTeam = teams.get(getTeamName(teamColor));                    playerTeam = teams.get(getTeamName(teamColor));
                    playerTeam.addPlayer(localPlayer.getName());s are not available);
                }
            }thePlayer;
        }if (localPlayer != null) {
ame().getFormattedText());
        // Update all player teams from tab list (as fallback)ms.containsKey(getTeamName(teamColor))) {list (as fallback)
        for (NetworkPlayerInfo info : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {TeamName(teamColor));t.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) {
            String playerName = info.getGameProfile().getName();));();
            
            // Skip if player already assigned through scoreboard teamsssigned through scoreboard teams
            boolean alreadyAssigned = false;dyAssigned = false;
            for (Team team : teams.values()) { : teams.values()) {
                if (team.getPlayers().contains(playerName)) {pdate all player teams from tab list (as fallback)    if (team.getPlayers().contains(playerName)) {
                    alreadyAssigned = true; : Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()) { = true;
                    break;
                }
            }
            
            if (!alreadyAssigned) {Team team : teams.values()) {alreadyAssigned) {
                String formattedName = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : "";   if (team.getPlayers().contains(playerName)) {   String formattedName = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : "";
                String teamColor = getTeamColor(formattedName);           alreadyAssigned = true;       String teamColor = getTeamColor(formattedName);
                if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {            break;        if (teamColor != null && teams.containsKey(getTeamName(teamColor))) {
                    teams.get(getTeamName(teamColor)).addPlayer(playerName);or)).addPlayer(playerName);
                }
            }
        }   if (!alreadyAssigned) {
                   String formattedName = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : "";   
        // Debug info about teams                String teamColor = getTeamColor(formattedName);        // Debug info about teams
        if (playerTeam != null) {tainsKey(getTeamName(teamColor))) {
            sendAlert(EnumChatFormatting.GREEN + "Detected your team: " + playerTeam.getFormatting() + playerTeam.getColor(), false, null);t(getTeamName(teamColor)).addPlayer(playerName);atFormatting.GREEN + "Detected your team: " + playerTeam.getFormatting() + playerTeam.getColor(), false, null);
        }
    }

    private String getTeamName(String colorCode) {
        switch (colorCode) {
            case "c": return "RED";
            case "9": return "BLUE";ing.GREEN + "Detected your team: " + playerTeam.getFormatting() + playerTeam.getColor(), false, null);
            case "e": return "YELLOW";
            case "a": return "GREEN";
            case "d": return "PINK";d": return "PINK";
            case "7": return "GRAY";rivate String getTeamName(String colorCode) {       case "7": return "GRAY";
            case "f": return "WHITE";        switch (colorCode) {            case "f": return "WHITE";
            case "8": return "DARK_GRAY";
            default: return null;
        }W";
    }

    private String extractPlayerName(String message) {ase "7": return "GRAY";tring extractPlayerName(String message) {
        String[] parts = message.split(" ");   case "f": return "WHITE";tring[] parts = message.split(" ");
        for (String part : parts) {: return "DARK_GRAY";part : parts) {
            if (part.startsWith("§") && !part.contains("§l") && !part.contains("§r")) {       default: return null;       if (part.startsWith("§") && !part.contains("§l") && !part.contains("§r")) {
                return part.replaceAll("§[0-9a-fk-or]", "");        }                return part.replaceAll("§[0-9a-fk-or]", "");
            }
        }
        return null;{
    }String[] parts = message.split(" ");

    @SubscribeEvent§l") && !part.contains("§r")) {
    public void onRenderWorld(RenderWorldLastEvent event) {        return part.replaceAll("§[0-9a-fk-or]", "");ic void onRenderWorld(RenderWorldLastEvent event) {
        if (!enableItemESP || !inBedWarsGame) return;
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World world = Minecraft.getMinecraft().theWorld;
        
        if (player == null || world == null) return;
        {
        // Merge nearby items of the same type to prevent clutter!enableItemESP || !inBedWarsGame) return;erge nearby items of the same type to prevent clutter
        Map<String, ItemStackGroup> itemGroups = new HashMap<>();
        t().thePlayer;
        for (Entity entity : world.getLoadedEntityList()) {d world = Minecraft.getMinecraft().theWorld;(Entity entity : world.getLoadedEntityList()) {
            if (!(entity instanceof EntityItem)) continue;
            
            EntityItem item = (EntityItem) entity;
            ItemStack stack = item.getEntityItem();
            ng, ItemStackGroup> itemGroups = new HashMap<>();
            // Only render diamonds and emeralds
            if (stack != null && (stack.getItem() == Items.diamond || stack.getItem() == Items.emerald)) {Items.emerald)) {
                double distanceSq = player.getDistanceSqToEntity(item);
                if (distanceSq > itemESPMaxDistance * itemESPMaxDistance) continue;
                
                // Create a key based on the item type and rounded positionon
                int gridSize = 2; // Items within 2 blocks will be groupedhin 2 blocks will be grouped
                String key = stack.getItem().getUnlocalizedName() + "_" +lizedName() + "_" +
                             Math.round(item.posX / gridSize) + "_" +getItem() == Items.emerald)) {
                             Math.round(item.posY / gridSize) + "_" +
                             Math.round(item.posZ / gridSize);ntinue;
                             
                if (!itemGroups.containsKey(key)) {ded position
                    itemGroups.put(key, new ItemStackGroup(stack.getItem(),  blocks will be groupedckGroup(stack.getItem(), 
                                                          item.posX, item.posY, item.posZ,alizedName() + "_" +     item.posX, item.posY, item.posZ,
                                                          stack.stackSize));
                } else {            Math.round(item.posY / gridSize) + "_" + else {
                    ItemStackGroup group = itemGroups.get(key);                Math.round(item.posZ / gridSize);       ItemStackGroup group = itemGroups.get(key);
                    group.count += stack.stackSize;                               group.count += stack.stackSize;
                    // Update position to the average        if (!itemGroups.containsKey(key)) {            // Update position to the average
                    group.updatePosition(item.posX, item.posY, item.posZ);t(key, new ItemStackGroup(stack.getItem(), osition(item.posX, item.posY, item.posZ);
                }X, item.posY, item.posZ,
            }                                                     stack.stackSize));       }
        }            } else {    }
        
        // Render the item groups
        renderItemGroups(itemGroups.values(), event.partialTicks);e position to the averagemGroups.values(), event.partialTicks);
    }em.posX, item.posY, item.posZ);
    
    private void renderItemGroups(Collection<ItemStackGroup> itemGroups, float partialTicks) {float partialTicks) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);renderItemGroups(itemGroups.values(), event.partialTicks);GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F); // Thicker lines for better visibility
        
        // Get player position for relative renderingGL11.glDisable(GL11.GL_TEXTURE_2D);// Get player position for relative rendering
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;GHTING);ecraft.getMinecraft().thePlayer;
        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;yer.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;GL11.GL_ONE_MINUS_SRC_ALPHA); + (player.posZ - player.lastTickPosZ) * partialTicks;
        ines for better visibility
        // Render each item groupr each item group
        for (ItemStackGroup group : itemGroups) {
            double relX = group.x - playerX;
            double relY = group.y - playerY;le playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;double relY = group.y - playerY;
            double relZ = group.z - playerZ;tialTicks;
            osZ - player.lastTickPosZ) * partialTicks;
            int color = group.item == Items.diamond ? DIAMOND_COLOR : EMERALD_COLOR;                    int color = group.item == Items.diamond ? DIAMOND_COLOR : EMERALD_COLOR;
            float boxSize = 0.4F; // Slightly larger box for better visibility
            
            float distance = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);            double relX = group.x - playerX;            float distance = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (distance > itemESPMaxDistance) continue;

            float alpha = 1.0F - (distance / itemESPFadeRange);float alpha = 1.0F - (distance / itemESPFadeRange);
            alpha = Math.max(0.2F, Math.min(0.8F, alpha)); // clamp for semi-transparencyOR : EMERALD_COLOR;mp for semi-transparency
/ Slightly larger box for better visibility
            drawBox(relX, relY, relZ, boxSize, color, alpha);
            drawItemCount(relX, relY, relZ, group.count, color);loat distance = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);rawItemCount(relX, relY, relZ, group.count, color);
               if (distance > itemESPMaxDistance) continue;   
            // Add a beam effect for valuable items (more than 3)dd a beam effect for valuable items (more than 3)
            if (group.count >= 3) {.0F - (distance / itemESPFadeRange); >= 3) {
                drawBeam(relX, relY, relZ, color, alpha);h.min(0.8F, alpha)); // clamp for semi-transparencyelZ, color, alpha);
            }
        }xSize, color, alpha);
        , group.count, color);
        // Restore GL state
        GL11.glDisable(GL11.GL_BLEND);ffect for valuable items (more than 3).GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);       if (group.count >= 3) {   GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);            drawBeam(relX, relY, relZ, color, alpha);    GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
    GL11.glDisable(GL11.GL_BLEND);
    private void drawBeam(double x, double y, double z, int color, float alpha) {loat alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0F;GL11.glEnable(GL11.GL_LIGHTING);float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;TEST);FF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        
        double beamHeight = 20.0; // Height of the beam
        
        GL11.glBegin(GL11.GL_LINES);Beam(double x, double y, double z, int color, float alpha) {GL11.GL_LINES);
        GL11.glColor4f(r, g, b, alpha * 0.6F);float r = ((color >> 16) & 0xFF) / 255.0F;GL11.glColor4f(r, g, b, alpha * 0.6F);
        GL11.glVertex3d(x, y, z);255.0F;
        GL11.glColor4f(r, g, b, 0.0F); // Fade to transparent at the tophe top
        GL11.glVertex3d(x, y + beamHeight, z);
        GL11.glEnd(); beam
        
        // Draw pulsing circle at the base
        long time = System.currentTimeMillis() % 2000; % 2000;
        float pulse = (float)Math.sin(time / 2000.0 * Math.PI * 2) * 0.5F + 0.5F;/ 2000.0 * Math.PI * 2) * 0.5F + 0.5F;
        float circleRadius = 0.3F + pulse * 0.15F;transparent at the top
        
        GL11.glBegin(GL11.GL_LINE_LOOP);L11.glEnd();L11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glColor4f(r, g, b, alpha * 0.7F);pha * 0.7F);
        for (int i = 0; i < 20; i++) {   // Draw pulsing circle at the base   for (int i = 0; i < 20; i++) {
            double angle = i / 20.0 * Math.PI * 2;    long time = System.currentTimeMillis() % 2000;        double angle = i / 20.0 * Math.PI * 2;
            GL11.glVertex3d(x + Math.cos(angle) * circleRadius, y + 0.1, z + Math.sin(angle) * circleRadius);dius);
        } + pulse * 0.15F;
        GL11.glEnd();
    }
    7F);
    private void drawBox(double x, double y, double z, float size, int color, float alpha) {for (int i = 0; i < 20; i++) {ate void drawBox(double x, double y, double z, float size, int color, float alpha) {
        // Extract RGB components
        float r = ((color >> 16) & 0xFF) / 255.0F;    GL11.glVertex3d(x + Math.cos(angle) * circleRadius, y + 0.1, z + Math.sin(angle) * circleRadius);float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;55.0F;
        float b = (color & 0xFF) / 255.0F;
        
        GL11.glColor4f(r, g, b, alpha * 0.4F); // More transparent fill0.4F); // More transparent fill
         float size, int color, float alpha) {
        // Draw filled box
        GL11.glBegin(GL11.GL_QUADS);
        
        // Bottom facefloat b = (color & 0xFF) / 255.0F;// Bottom face
        GL11.glVertex3d(x - size, y - size, z - size);ze, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size); transparent fill
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        
        // Top face// Top face
        GL11.glVertex3d(x - size, y + size, z - size);e3d(x - size, y + size, z - size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z - size);
        
        // Front face// Front face
        GL11.glVertex3d(x - size, y - size, z + size);3d(x - size, y - size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        
        // Back face// Back face
        GL11.glVertex3d(x - size, y - size, z - size);ex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        
        // Left face// Left face
        GL11.glVertex3d(x - size, y - size, z - size);d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z - size);
        
        // Right face// Right face
        GL11.glVertex3d(x + size, y - size, z - size);d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);GL11.glVertex3d(x - size, y - size, z - size);GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);
        size);
        GL11.glEnd();
        // Right face
        // Draw outline with a brighter color for better visibilityd(x + size, y - size, z - size);e with a brighter color for better visibility
        GL11.glColor4f(r * 1.2F, g * 1.2F, b * 1.2F, alpha);lpha);
        GL11.glLineWidth(2.5F); // Thicker lines
        GL11.glBegin(GL11.GL_LINES);
        
        // Bottom face
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);er visibility
        GL11.glVertex3d(x + size, y - size, z - size);lpha);
        GL11.glVertex3d(x + size, y - size, z + size);GL11.glLineWidth(2.5F); // Thicker linesGL11.glVertex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y - size, z + size);n(GL11.GL_LINES);ex3d(x + size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y - size, z - size);
        
        // Top face
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y + size, z + size);GL11.glVertex3d(x - size, y - size, z + size);GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size); size, y - size, z - size); size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
        GL11.glVertex3d(x - size, y + size, z - size);
        
        // Connecting edges
        GL11.glVertex3d(x - size, y - size, z - size);
        GL11.glVertex3d(x - size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z - size);
        GL11.glVertex3d(x + size, y + size, z - size);GL11.glVertex3d(x - size, y + size, z + size);GL11.glVertex3d(x + size, y + size, z - size);
        GL11.glVertex3d(x + size, y - size, z + size);3d(x - size, y + size, z - size);3d(x + size, y - size, z + size);
        GL11.glVertex3d(x + size, y + size, z + size);      GL11.glVertex3d(x + size, y + size, z + size);
        GL11.glVertex3d(x - size, y - size, z + size);    // Connecting edges    GL11.glVertex3d(x - size, y - size, z + size);
        GL11.glVertex3d(x - size, y + size, z + size);
         y + size, z - size);
        GL11.glEnd();ze);
    }ize);
    , z + size);
    private void drawItemCount(double x, double y, double z, int count, int color) {GL11.glVertex3d(x + size, y + size, z + size);ate void drawItemCount(double x, double y, double z, int count, int color) {
        // Extract RGB components z + size);
        float r = ((color >> 16) & 0xFF) / 255.0F;size, y + size, z + size); 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;GL11.glEnd();float b = (color & 0xFF) / 255.0F;
        
        // Set rendering to face the player
        GL11.glPushMatrix();nt count, int color) {
        GL11.glTranslated(x, y + 0.6, z); // Position slightly above the box
        
        // Make text face the player by applying inverse rotationfloat g = ((color >> 8) & 0xFF) / 255.0F;// Make text face the player by applying inverse rotation
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;.thePlayer;
        GL11.glRotatef(-player.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(player.rotationPitch, 1.0F, 0.0F, 0.0F);erch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.03F, -0.03F, 0.03F); // Scale the text to a reasonable size text to a reasonable size
        GL11.glTranslated(x, y + 0.6, z); // Position slightly above the box
        // Re-enable textures for rendering text
        GL11.glEnable(GL11.GL_TEXTURE_2D);er by applying inverse rotationURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glRotatef(-player.rotationYaw, 0.0F, 1.0F, 0.0F);GL11.glDisable(GL11.GL_DEPTH_TEST);
        (player.rotationPitch, 1.0F, 0.0F, 0.0F);
        // Draw text shadow for better visibility
        String text = "x" + count;
        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);tures for rendering textMinecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        .glEnable(GL11.GL_TEXTURE_2D);
        // Draw shadowle(GL11.GL_LIGHTING);ow
        Minecraft.getMinecraft().fontRendererObj.drawString(text, -textWidth / 2 + 1,
            -Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2 + 1,
            0xFF000000);better visibility
            String text = "x" + count;    
        // Draw textMinecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, -textWidth / 2, / 2,
            -Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2,_HEIGHT / 2,
            color | 0xFF000000);erObj.drawString(text, -textWidth / 2 + 1,
        inecraft().fontRendererObj.FONT_HEIGHT / 2 + 1,
        // Restore state       0xFF000000);   // Restore state
        GL11.glEnable(GL11.GL_DEPTH_TEST);            GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);ererObj.drawString(text, -textWidth / 2,);
        GL11.glPopMatrix();tMinecraft().fontRendererObj.FONT_HEIGHT / 2,);
    });
    
    // Class to represent a group of nearby items of the same typerby items of the same type
    private static class ItemStackGroup {GL11.glEnable(GL11.GL_DEPTH_TEST);ate static class ItemStackGroup {
        public Item item;
        public double x, y, z;L_TEXTURE_2D);;
        public int count;x();t;
        public int elements = 1;
        
        public ItemStackGroup(Item item, double x, double y, double z, int count) {up of nearby items of the same typetem item, double x, double y, double z, int count) {
            this.item = item;te static class ItemStackGroup {   this.item = item;
            this.x = x;public Item item;    this.x = x;
            this.y = y;
            this.z = z;t;
            this.count = count;
        }
        le y, double z, int count) {
        public void updatePosition(double newX, double newY, double newZ) {
            elements++;   this.x = x;   elements++;
            // Calculate weighted average position       this.y = y;       // Calculate weighted average position
            x = (x * (elements - 1) + newX) / elements;            this.z = z;            x = (x * (elements - 1) + newX) / elements;
            y = (y * (elements - 1) + newY) / elements;
            z = (z * (elements - 1) + newZ) / elements;
        }
    }ble newX, double newY, double newZ) {

    public class GuiConfigScreen extends GuiScreen {            // Calculate weighted average position    public class GuiConfigScreen extends GuiScreen {
        private final int BUTTON_WIDTH = 180;x * (elements - 1) + newX) / elements;inal int BUTTON_WIDTH = 180;
        private final int BUTTON_HEIGHT = 20;- 1) + newY) / elements;N_HEIGHT = 20;
        private final int PADDING = 5;+ newZ) / elements;5;
        private int scrollOffset = 0;
    }
        @Override
        public void initGui() {{
            this.buttonList.clear();l int BUTTON_WIDTH = 180;tonList.clear();
            int y = height / 8 + scrollOffset;        private final int BUTTON_HEIGHT = 20;            int y = height / 8 + scrollOffset;

            // Draw Alert Settings header
            drawSectionHeader("Alert Settings", y);
            y += 20;

            buttonList.add(createToggleButton(0, "Armor Alerts", enableArmorAlerts, y));0, "Armor Alerts", enableArmorAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(1, "Item Alerts", enableItemAlerts, y));y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(2, "Emerald Alerts", enableEmeraldAlerts, y));ings", y);tton(2, "Emerald Alerts", enableEmeraldAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(3, "Diamond Sword Alerts", enableSwordAlerts, y));rdAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(4, "Potion Alerts", enablePotionAlerts, y));tton(4, "Potion Alerts", enablePotionAlerts, y));
            y += BUTTON_HEIGHT + PADDING;, y));
            buttonList.add(createToggleButton(5, "Fireball Alerts", enableFireballAlerts, y));tton(5, "Fireball Alerts", enableFireballAlerts, y));
            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(6, "Exclude Teammates", excludeTeammates, y));tton(6, "Exclude Teammates", excludeTeammates, y));
            y += BUTTON_HEIGHT + PADDING;            buttonList.add(createToggleButton(3, "Diamond Sword Alerts", enableSwordAlerts, y));            y += BUTTON_HEIGHT + PADDING;
            buttonList.add(createToggleButton(7, "Item ESP", enableItemESP, y));ADDING;oggleButton(7, "Item ESP", enableItemESP, y));
            y += BUTTON_HEIGHT + PADDING;tion Alerts", enablePotionAlerts, y));
            buttonList.add(createToggleButton(8, "Obsidian Alerts", enableObsidianAlerts, y));HEIGHT + PADDING;dd(createToggleButton(8, "Obsidian Alerts", enableObsidianAlerts, y));
            y += BUTTON_HEIGHT + PADDING;reball Alerts", enableFireballAlerts, y));
            buttonList.add(createToggleButton(9, "Fireball Trajectory Alerts", enableFireballTrajectoryAlerts, y));HEIGHT + PADDING;
            y += BUTTON_HEIGHT + PADDING;            buttonList.add(createToggleButton(6, "Exclude Teammates", excludeTeammates, y));            // Team Status Section

            // Team Status SectionESP", enableItemESP, y));
            if (inBedWarsGame && !teams.isEmpty()) {
                y += 15;eToggleButton(8, "Obsidian Alerts", enableObsidianAlerts, y));
                drawSectionHeader("Team Status", y);ON_HEIGHT + PADDING;
                y += 20;nList.add(createToggleButton(9, "Fireball Trajectory Alerts", enableFireballTrajectoryAlerts, y));or (Team team : teams.values()) {
 += BUTTON_HEIGHT + PADDING;       if (!team.getPlayers().isEmpty()) {
                for (Team team : teams.values()) {      drawTeamStatus(team, y);
                    if (!team.getPlayers().isEmpty()) {            // Team Status Section                        y += 15;
                        drawTeamStatus(team, y);
                        y += 15;
                    }
                }
            }
        }       for (Team team : teams.values()) {rivate void drawTeamStatus(Team team, int y) {
                    if (!team.getPlayers().isEmpty()) {            String status = team.getFormatting() + team.getColor() +
        private void drawTeamStatus(Team team, int y) {
            String status = team.getFormatting() + team.getColor() +";
                          (team.hasBed() ? " ✔ " : " ✘ ") +
                          team.getPlayers().size() + " players";       }
            drawString(fontRendererObj, status, width/2 - 85, y, 0xFFFFFF);            }
        }

        private void drawSectionHeader(String text, int y) {
            String header = "§l" + text + "§r";   String status = team.getFormatting() + team.getColor() +
            drawCenteredString(fontRendererObj, header, width/2, y, 0xFFFFFF);                          (team.hasBed() ? " ✔ " : " ✘ ") +
        }         team.getPlayers().size() + " players";uiButton createToggleButton(int id, String text, boolean state, int y) {
85, y, 0xFFFFFF);H/2, y, BUTTON_WIDTH, BUTTON_HEIGHT,
        private GuiButton createToggleButton(int id, String text, boolean state, int y) {atting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
            return new GuiButton(id, width/2 - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    text + ": " + (state ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
        }
FFFFFF);
        @Override
        protected void actionPerformed(GuiButton button) {
            switch (button.id) {olean state, int y) {
                case 0: enableArmorAlerts = !enableArmorAlerts; break; y, BUTTON_WIDTH, BUTTON_HEIGHT,erts; break;
                case 1: enableItemAlerts = !enableItemAlerts; break; EnumChatFormatting.RED + "OFF"));
                case 2: enableEmeraldAlerts = !enableEmeraldAlerts; break;ase 4: enablePotionAlerts = !enablePotionAlerts; break;
                case 3: enableSwordAlerts = !enableSwordAlerts; break;ableFireballAlerts; break;
                case 4: enablePotionAlerts = !enablePotionAlerts; break;ludeTeammates = !excludeTeammates; break;
                case 5: enableFireballAlerts = !enableFireballAlerts; break;rotected void actionPerformed(GuiButton button) {       case 7: enableItemESP = !enableItemESP; break;
                case 6: excludeTeammates = !excludeTeammates; break;            switch (button.id) {                case 8: enableObsidianAlerts = !enableObsidianAlerts; break;
                case 7: enableItemESP = !enableItemESP; break;ase 0: enableArmorAlerts = !enableArmorAlerts; break;
                case 8: enableObsidianAlerts = !enableObsidianAlerts; break;
                case 9: enableFireballTrajectoryAlerts = !enableFireballTrajectoryAlerts; break;dAlerts = !enableEmeraldAlerts; break;
            }    case 3: enableSwordAlerts = !enableSwordAlerts; break;
            saveConfig();onAlerts = !enablePotionAlerts; break;
            initGui();allAlerts; break;
        }eTeammates = !excludeTeammates; break;(int mouseX, int mouseY, float partialTicks) {
break;
        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {toryAlerts; break;
            drawDefaultBackground();t.Mouse.getDWheel();
            aveConfig();f (dWheel != 0) {
            // Handle mouse scroll            initGui();                scrollOffset += (dWheel > 0) ? 20 : -20;
            int dWheel = org.lwjgl.input.Mouse.getDWheel();: -100;
            if (dWheel != 0) {
                scrollOffset += (dWheel > 0) ? 20 : -20;
                int maxScroll = inBedWarsGame && !teams.isEmpty() ? -200 : -100;        public void drawScreen(int mouseX, int mouseY, float partialTicks) {            }
                scrollOffset = Math.max(maxScroll, Math.min(0, scrollOffset));
                initGui();
            }

            // Draw title with formattingf (dWheel != 0) {
            String title = "§l§eBed Wars Assistant Settings§r";                scrollOffset += (dWheel > 0) ? 20 : -20;            // Draw team info if in game
            drawCenteredString(fontRendererObj, title, width/2, 15, 0xFFFFFF);mpty() ? -200 : -100;
       scrollOffset = Math.max(maxScroll, Math.min(0, scrollOffset));       String yourTeam = "Your Team: " + playerTeam.getFormatting() + playerTeam.getColor();
            // Draw team info if in game                initGui();                drawCenteredString(fontRendererObj, yourTeam, width/2, 30, 0xFFFFFF);
            if (inBedWarsGame && playerTeam != null) {
                String yourTeam = "Your Team: " + playerTeam.getFormatting() + playerTeam.getColor();
                drawCenteredString(fontRendererObj, yourTeam, width/2, 30, 0xFFFFFF);tingmouseY, partialTicks);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }       // Draw team info if in game   public void handleMouseInput() throws IOException {
            if (inBedWarsGame && playerTeam != null) {            super.handleMouseInput();
        @Overrider Team: " + playerTeam.getFormatting() + playerTeam.getColor();ut.Mouse.getEventX() * width / mc.displayWidth;
        public void handleMouseInput() throws IOException {endererObj, yourTeam, width/2, 30, 0xFFFFFF);jgl.input.Mouse.getEventY() * height / mc.displayHeight - 1;
            super.handleMouseInput();
            int mouseX = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1; mouseY, partialTicks);
        }
    }
   @Override   public boolean lastHeldDiamondSword = false;
    private static class PlayerState {        public void handleMouseInput() throws IOException {        public boolean wasHoldingFireball = false;
        public Item lastHeldItem = null;
        public boolean lastHeldDiamondSword = false;g.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth; Boolean> activePotions = new HashMap<>();
        public boolean wasHoldingFireball = false;.Mouse.getEventY() * height / mc.displayHeight - 1;stamps = new HashMap<>();
        public int lastEmeralds = 0;
        public Map<Integer, Boolean> activePotions = new HashMap<>();
        public Map<Integer, Long> potionAlertTimestamps = new HashMap<>(); getPotionName(int potionId) {
    }rivate static class PlayerState {   switch (potionId) {
        public Item lastHeldItem = null;            case SPEED_POTION_ID: return "Speed";
    private String getPotionName(int potionId) {ord = false;eturn "Jump Boost";
        switch (potionId) {
            case SPEED_POTION_ID: return "Speed";
            case JUMP_BOOST_POTION_ID: return "Jump Boost";   public Map<Integer, Boolean> activePotions = new HashMap<>();
            default: return "Unknown Potion";        public Map<Integer, Long> potionAlertTimestamps = new HashMap<>();
        }
    } &&
Data().serverIP.toLowerCase().contains("hypixel.net");
    private boolean isHypixelBedWars() {
        return Minecraft.getMinecraft().getCurrentServerData() != null &&rn "Speed";
               Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net");            case JUMP_BOOST_POTION_ID: return "Jump Boost";    private static class Team {
    }

    private static class Team {
        private final String color;olean hasBed = true;
        private final EnumChatFormatting formatting;    private boolean isHypixelBedWars() {
        private final Set<String> players = new HashSet<>();aft().getCurrentServerData() != null &&EnumChatFormatting formatting) {
        private boolean hasBed = true;getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net");color;
his.formatting = formatting;
        public Team(String color, EnumChatFormatting formatting) {        }
            this.color = color;
            this.formatting = formatting;olor;() {
        }rivate final EnumChatFormatting formatting;   return color;
        private final Set<String> players = new HashSet<>();        }
        public String getColor() {
            return color;
        }ublic Team(String color, EnumChatFormatting formatting) {   return formatting;
            this.color = color;        }
        public EnumChatFormatting getFormatting() {matting;
            return formatting;
        } players;
        public String getColor() {        }
        public Set<String> getPlayers() {
            return players;
        } hasBed;
        public EnumChatFormatting getFormatting() {        }
        public boolean hasBed() {
            return hasBed;
        }asBed = hasBed;
        public Set<String> getPlayers() {        }
        public void setBedState(boolean hasBed) {
            this.hasBed = hasBed;
        }s.add(playerName);
   public boolean hasBed() {   }
        public void addPlayer(String playerName) {           return hasBed;
            players.add(playerName);        }        public void removePlayer(String playerName) {








}    }        }            players.remove(playerName);        public void removePlayer(String playerName) {        }




































































































































































}    }        fireballAlertTimestamps.entrySet().removeIf(entry -> currentTime - entry.getValue() > expirationTime);        analyzedFireballs.entrySet().removeIf(entry -> currentTime - entry.getValue() > expirationTime);                long expirationTime = 10000; // 10 seconds        long currentTime = System.currentTimeMillis();    private void cleanupFireballTracking() {     */     * Remove old fireballs from tracking maps    /**        }        return dx * vx + dy * vy + dz * vz;                double vz = fireball.motionZ;        double vy = fireball.motionY;        double vx = fireball.motionX;        // Project fireball velocity onto this direction                dz /= distance;        dy /= distance;        dx /= distance;                if (distance < 0.1) return 0; // Avoid division by zero        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);        // Get direction vector from fireball to player                double dz = player.posZ - fireball.posZ;        double dy = player.posY - fireball.posY;        double dx = player.posX - fireball.posX;    private double getFireballApproachVelocity(EntitySmallFireball fireball, EntityPlayer player) {     */     * Positive value means approaching, negative means moving away     * Calculate how fast the fireball is approaching the player    /**        }        }            }                cleanupFireballTracking();                // Clean up old fireballs from tracking maps                                }                    }                        }                            }                                        true, "random.orb");                                        (shooter != null ? getColoredPlayerName((EntityPlayer)shooter) : "unknown"),                                 sendAlert(EnumChatFormatting.YELLOW + "Fireball approaching from " +                             } else {                                sendAlert(EnumChatFormatting.GOLD + "⚠ Fireball nearby! Dodge! ⚠", true, SOUND_FIREBALL);                            } else if (closestApproach < 3.0) {                                sendAlert(EnumChatFormatting.RED + "⚠ FIREBALL INCOMING! MOVE NOW! ⚠", true, "random.explode");                            if (directHitLikely) {                            // Different messages based on danger level                                                        fireballAlertTimestamps.put(fireballId, currentTime);                                                        currentTime - fireballAlertTimestamps.get(fireballId) > FIREBALL_ALERT_COOLDOWN) {                        if (!fireballAlertTimestamps.containsKey(fireballId) ||                        // Check if we've alerted about this fireball recently                    if (approachVelocity > 0) {                    // Only alert if it's approaching the player (positive velocity)                                        double approachVelocity = getFireballApproachVelocity(fireball, localPlayer);                    // Calculate how fast the fireball is approaching the player                if (dangerousFireball) {                // Send an alert if the fireball is dangerous                                }                    }                        }                            break;                            directHitLikely = true;                        if (distance < 2.0) {                        // Check if likely to hit directly                                                dangerousFireball = true;                    if (distance < 4.0) {                    // Check if dangerous                                        }                        closestApproach = distance;                    if (distance < closestApproach) {                                        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);                    double dz = simulatedZ - playerZ;                    double dy = simulatedY - playerY;                    double dx = simulatedX - playerX;                    // Calculate distance to player                                        simulatedZ += motionZ;                    simulatedY += motionY;                    simulatedX += motionX;                for (int i = 0; i < 20; i++) { // Simulate 1 second (20 ticks)                                double simulatedZ = fireball.posZ;                double simulatedY = fireball.posY;                double simulatedX = fireball.posX;                // Simulate fireball path                                double closestApproach = Double.MAX_VALUE;                boolean directHitLikely = false;                boolean dangerousFireball = false;                // Predict where the fireball will be in the future                                double playerZ = localPlayer.posZ;                double playerY = localPlayer.posY + 1.0; // Aim for mid-height                double playerX = localPlayer.posX;                // Get player's position                                double motionZ = fireball.motionZ;                double motionY = fireball.motionY;                double motionX = fireball.motionX;                // Calculate fireball motion direction                                String shooterName = shooter instanceof EntityPlayer ? ((EntityPlayer) shooter).getName() : "Unknown";                Entity shooter = fireball.shootingEntity;                // Get thrower if possible                                if (distanceToPlayer > 30.0) continue;                // Only care about fireballs within 30 blocks                                double distanceToPlayer = fireball.getDistanceToEntity(localPlayer);                // Calculate if the fireball is headed toward the player                                analyzedFireballs.put(fireballId, currentTime);                // Mark this fireball as analyzed                                }                    continue;                    currentTime - analyzedFireballs.get(fireballId) < 500) {                if (analyzedFireballs.containsKey(fireballId) &&                 long currentTime = System.currentTimeMillis();                // Skip already analyzed fireballs (process each fireball max once per half-second)                                UUID fireballId = fireball.getUniqueID();                EntitySmallFireball fireball = (EntitySmallFireball) entity;            if (entity instanceof EntitySmallFireball) {        for (Entity entity : world.getLoadedEntityList()) {    private void trackFireballs(World world, EntityPlayer localPlayer) {     */     * Track fireballs and predict their trajectory    /**        private static final long FIREBALL_ALERT_COOLDOWN = 750; // ms between alerts for the same fireball    private Map<UUID, Long> fireballAlertTimestamps = new HashMap<>();    private Map<UUID, Long> analyzedFireballs = new HashMap<>();    // Map to track fireballs and their last known analysis time    }        }            players.remove(playerName);        public void removePlayer(String playerName) {        }            players.add(playerName);        public void addPlayer(String playerName) {        }            this.hasBed = hasBed;        public void setBedState(boolean hasBed) {            players.remove(playerName);
        }
    }
}
