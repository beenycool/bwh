package com.example.hypixelbedwarsmod.detection;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import com.example.hypixelbedwarsmod.data.PlayerState;
import com.example.hypixelbedwarsmod.data.Team;
import com.example.hypixelbedwarsmod.utils.ChatUtils;
import com.example.hypixelbedwarsmod.utils.MathUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles player detection and team management with optimized performance
 */
public class PlayerTracker {
    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^(Red|Blue|Green|Yellow|Aqua|White|Pink|Gray) (.+)$", Pattern.CASE_INSENSITIVE);
    
    private static final Map<String, EnumChatFormatting> COLOR_MAP = initializeColorMap();
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes
    
    private final ConfigManager configManager;
    private final Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<String, Long> alertCooldowns = new ConcurrentHashMap<>();
    
    private Team playerTeam;
    private long lastCleanup = System.currentTimeMillis();

    public PlayerTracker(ConfigManager configManager) {
        this.configManager = configManager;
        initializeTeams();
    }

    /**
     * NEW: Process chat message for team detection
     */
    public void processChatMessage(IChatComponent chatComponent) {
        if (chatComponent == null) return;
        
        String message = chatComponent.getUnformattedText();
        String formattedMessage = chatComponent.getFormattedText();
        
        // Check for team chat patterns
        Matcher teamChatMatcher = TEAM_CHAT_PATTERN.matcher(message);
        if (teamChatMatcher.matches()) {
            String playerName = cleanPlayerName(teamChatMatcher.group(1));
            // Players in team chat are teammates
            markAsTeammate(playerName);
            return;
        }
        
        // Check for party chat (likely teammates)
        Matcher partyChatMatcher = PARTY_CHAT_PATTERN.matcher(message);
        if (partyChatMatcher.matches()) {
            String playerName = cleanPlayerName(partyChatMatcher.group(1));
            markAsTeammate(playerName);
            return;
        }
        
        // Extract team colors from formatted text
        extractTeamFromFormattedText(formattedMessage);
    }
    
    /**
     * Extract team information from formatted chat text
     */
    private void extractTeamFromFormattedText(String formattedText) {
        // Look for color codes followed by player names
        Pattern colorPlayerPattern = Pattern.compile("§([0-9a-fk-or])([a-zA-Z0-9_]+)");
        Matcher matcher = colorPlayerPattern.matcher(formattedText);
        
        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String playerName = matcher.group(2);
            
            EnumChatFormatting color = getColorFromCode(colorCode);
            if (color != null && COLOR_TO_TEAM.containsKey(color)) {
                String teamName = COLOR_TO_TEAM.get(color);
                setPlayerTeam(playerName, teamName, color);
            }
        }
    }
    
    /**
     * Clean player name by removing ranks and formatting
     */
    private String cleanPlayerName(String rawName) {
        if (rawName == null) return "";
        
        // Remove rank prefixes like [MVP+], [VIP], etc.
        Matcher rankMatcher = RANK_PATTERN.matcher(rawName.trim());
        if (rankMatcher.matches()) {
            return rankMatcher.group(1).trim();
        }
        
        return rawName.trim();
    }
    
    /**
     * Mark a player as teammate
     */
    private void markAsTeammate(String playerName) {
        String cleanName = cleanPlayerName(playerName);
        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        if (localPlayer != null) {
            // Assume same team as local player
            String localTeam = getPlayerTeam(localPlayer.getName());
            if (localTeam != null) {
                setPlayerTeam(cleanName, localTeam, getCurrentPlayerTeamColor());
            }
        }
    }
    
    /**
     * Get color from formatting code
     */
    private EnumChatFormatting getColorFromCode(String code) {
        switch (code.toLowerCase()) {
            case "4": case "c": return EnumChatFormatting.RED;
            case "1": case "9": return EnumChatFormatting.BLUE;
            case "2": case "a": return EnumChatFormatting.GREEN;
            case "e": case "6": return EnumChatFormatting.YELLOW;
            case "b": case "3": return EnumChatFormatting.AQUA;
            case "f": case "7": return EnumChatFormatting.WHITE;
            case "8": return EnumChatFormatting.GRAY;
            case "5": case "d": return EnumChatFormatting.DARK_PURPLE;
            default: return null;
        }
    }

    private static Map<String, EnumChatFormatting> initializeColorMap() {
        Map<String, EnumChatFormatting> map = new HashMap<>();
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

    private void initializeTeams() {
        teams.put("Red", new Team("Red", EnumChatFormatting.RED));
        teams.put("Blue", new Team("Blue", EnumChatFormatting.BLUE));
        teams.put("Green", new Team("Green", EnumChatFormatting.GREEN));
        teams.put("Yellow", new Team("Yellow", EnumChatFormatting.YELLOW));
        teams.put("Aqua", new Team("Aqua", EnumChatFormatting.AQUA));
        teams.put("White", new Team("White", EnumChatFormatting.WHITE));
        teams.put("Pink", new Team("Pink", EnumChatFormatting.LIGHT_PURPLE));
        teams.put("Gray", new Team("Gray", EnumChatFormatting.GRAY));
    }

    public void updatePlayers(World world, EntityPlayer localPlayer) {
        // Update player team if needed
        updatePlayerTeam(localPlayer);
        
        // Process scoreboard for team information
        processScoreboard(world);
        
        // Cleanup old data periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            cleanup();
            lastCleanup = currentTime;
        }
    }

    public void processPlayer(EntityPlayer player, EntityPlayer localPlayer) {
        if (player == localPlayer) return;
        
        String playerName = player.getName();
        
        // Skip teammates if option is enabled
        if (configManager.isTeammatesExcluded() && isSameTeam(player, localPlayer)) {
            return;
        }

        // Get or create player state
        PlayerState state = playerStates.computeIfAbsent(playerName, k -> new PlayerState());
        
        // Update team assignment
        updatePlayerTeamAssignment(player);
        
        // Check for low health players without bed
        checkLowHealthNoBed(player);
    }

    private void updatePlayerTeam(EntityPlayer localPlayer) {
        String teamColor = getTeamColor(localPlayer.getDisplayName().getFormattedText());
        if (teamColor != null) {
            String teamName = getTeamName(teamColor);
            if (teamName != null && teams.containsKey(teamName)) {
                playerTeam = teams.get(teamName);
                playerTeam.addPlayer(localPlayer.getName());
            }
        }
    }

    private void updatePlayerTeamAssignment(EntityPlayer player) {
        String teamColor = getTeamColor(player.getDisplayName().getFormattedText());
        if (teamColor != null) {
            String teamName = getTeamName(teamColor);
            if (teamName != null && teams.containsKey(teamName)) {
                Team team = teams.get(teamName);
                String playerName = player.getName();
                
                if (!team.hasPlayer(playerName)) {
                    // Remove from other teams
                    teams.values().forEach(t -> t.removePlayer(playerName));
                    team.addPlayer(playerName);
                }
            }
        }
    }

    private void checkLowHealthNoBed(EntityPlayer player) {
        String teamColor = getTeamColor(player.getDisplayName().getFormattedText());
        if (teamColor != null) {
            String teamName = getTeamName(teamColor);
            Team team = teams.get(teamName);
            
            if (team != null && !team.hasBed() && player.getHealth() <= 10.0F) {
                String alertKey = player.getName() + "_low_health";
                long currentTime = System.currentTimeMillis();
                
                Long lastAlert = alertCooldowns.get(alertKey);
                if (lastAlert == null || currentTime - lastAlert > configManager.getGeneralCooldown()) {
                    String message = getColoredPlayerName(player) + 
                        EnumChatFormatting.RED + " is low health with NO BED! " +
                        ChatUtils.formatDistance(player.getDistanceToEntity(getCurrentPlayer()));
                    
                    ChatUtils.sendAlert(message, EnumChatFormatting.RED);
                    alertCooldowns.put(alertKey, currentTime);
                }
            }
        }
    }

    private void processScoreboard(World world) {
        ScoreObjective objective = world.getScoreboard().getObjectiveInDisplaySlot(1);
        if (objective == null) return;
        
        // Process scoreboard entries for team bed status
        // This would require parsing the scoreboard content for bed elimination messages
        // Implementation depends on Hypixel's specific scoreboard format
    }

    public boolean isSameTeam(EntityPlayer player1, EntityPlayer player2) {
        if (playerTeam == null) return false;
        
        String color1 = getTeamColor(player1.getDisplayName().getFormattedText());
        String color2 = getTeamColor(player2.getDisplayName().getFormattedText());
        
        return color1 != null && color1.equals(color2);
    }

    public String getTeamColor(String displayName) {
        Matcher matcher = TEAM_COLOR_PATTERN.matcher(displayName);
        return matcher.find() ? matcher.group(1) : null;
    }

    public String getTeamName(String colorCode) {
        EnumChatFormatting formatting = COLOR_MAP.get(colorCode);
        if (formatting == null) return null;
        
        return teams.values().stream()
            .filter(team -> team.getFormatting() == formatting)
            .map(Team::getColor)
            .findFirst()
            .orElse(null);
    }

    public String getColoredPlayerName(EntityPlayer player) {
        String teamColor = getTeamColor(player.getDisplayName().getFormattedText());
        if (teamColor != null) {
            EnumChatFormatting formatting = COLOR_MAP.get(teamColor);
            if (formatting != null) {
                return ChatUtils.formatPlayerName(player.getName(), formatting);
            }
        }
        return player.getName();
    }

    public PlayerState getPlayerState(String playerName) {
        return playerStates.get(playerName);
    }

    public Team getPlayerTeam() {
        return playerTeam;
    }

    public Map<String, Team> getTeams() {
        return new HashMap<>(teams);
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up old player states
        playerStates.entrySet().removeIf(entry -> 
            entry.getValue().isStale(currentTime, CLEANUP_INTERVAL));
        
        // Clean up old alert cooldowns
        alertCooldowns.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > configManager.getGeneralCooldown() * 2);
        
        // Clean up empty teams
        teams.values().forEach(team -> {
            if (team.isEmpty() && currentTime - team.getLastUpdate() > CLEANUP_INTERVAL) {
                // Team is empty and old, could be cleaned up
                // But we keep teams for consistency in Bed Wars
            }
        });
    }

    public void reset() {
        playerStates.clear();
        alertCooldowns.clear();
        playerTeam = null;
        
        // Reset teams but keep structure
        teams.values().forEach(team -> {
            team.getPlayers().clear();
            team.setBedState(true);
        });
    }

    private EntityPlayer getCurrentPlayer() {
        return net.minecraft.client.Minecraft.getMinecraft().thePlayer;
    }
    
    /**
     * NEW: Enhanced team detection methods for smart team assignment
     */
    public void setPlayerTeamInfo(String playerName, String teamName, EnumChatFormatting color) {
        String key = playerName.toLowerCase();
        playerTeams.put(key, teamName);
        teamColors.put(key, color.toString());
        playerDisplayNames.put(key, playerName);
    }
    
    public String getPlayerTeamName(String playerName) {
        return playerTeams.get(playerName.toLowerCase());
    }
    
    public EnumChatFormatting getPlayerTeamColorFormatting(String playerName) {
        String colorStr = teamColors.get(playerName.toLowerCase());
        if (colorStr != null) {
            try {
                return EnumChatFormatting.valueOf(colorStr);
            } catch (Exception e) {
                return EnumChatFormatting.WHITE;
            }
        }
        return EnumChatFormatting.WHITE;
    }
    
    /**
     * Process chat messages for team detection
     */
    public void processChatMessage(String message) {
        if (message == null || message.isEmpty()) return;
        
        // Look for team chat patterns
        if (message.startsWith("[TEAM]")) {
            // Extract player name from team chat
            String[] parts = message.split(": ", 2);
            if (parts.length > 0) {
                String playerPart = parts[0].replace("[TEAM]", "").trim();
                String cleanName = cleanPlayerName(playerPart);
                markAsTeammate(cleanName);
            }
        }
        
        // Look for party chat (likely teammates)
        if (message.startsWith("Party >")) {
            String[] parts = message.split(": ", 2);
            if (parts.length > 0) {
                String playerPart = parts[0].replace("Party >", "").trim();
                String cleanName = cleanPlayerName(playerPart);
                markAsTeammate(cleanName);
            }
        }
    }
    
    private String cleanPlayerName(String rawName) {
        if (rawName == null) return "";
        
        // Remove rank prefixes like [MVP+], [VIP], etc.
        String cleaned = rawName.replaceAll("\\[.+?\\]\\s*", "").trim();
        return cleaned;
    }
    
    private void markAsTeammate(String playerName) {
        EntityPlayer localPlayer = getCurrentPlayer();
        if (localPlayer != null) {
            String localTeam = getPlayerTeamName(localPlayer.getName());
            if (localTeam != null) {
                setPlayerTeamInfo(playerName, localTeam, getPlayerTeamColorFormatting(localPlayer.getName()));
            }
        }
    }
}
}
