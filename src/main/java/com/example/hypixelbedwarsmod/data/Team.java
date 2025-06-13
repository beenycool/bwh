package com.example.hypixelbedwarsmod.data;

import net.minecraft.util.EnumChatFormatting;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a team in Bed Wars with thread-safe operations
 */
public class Team {
    private final String color;
    private final EnumChatFormatting formatting;
    private final Set<String> players = ConcurrentHashMap.newKeySet();
    private volatile boolean hasBed = true;
    private volatile long lastUpdate = System.currentTimeMillis();

    public Team(String color, EnumChatFormatting formatting) {
        this.color = color;
        this.formatting = formatting;
    }

    public String getColor() {
        return color;
    }

    public EnumChatFormatting getFormatting() {
        return formatting;
    }

    public Set<String> getPlayers() {
        return new HashSet<>(players); // Return defensive copy
    }

    public void addPlayer(String player) {
        players.add(player);
        lastUpdate = System.currentTimeMillis();
    }

    public void removePlayer(String player) {
        players.remove(player);
        lastUpdate = System.currentTimeMillis();
    }

    public boolean hasPlayer(String player) {
        return players.contains(player);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean hasBed() {
        return hasBed;
    }

    public void setBedState(boolean hasBed) {
        this.hasBed = hasBed;
        lastUpdate = System.currentTimeMillis();
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Team{color='%s', players=%d, hasBed=%b}", color, players.size(), hasBed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Team team = (Team) obj;
        return color.equals(team.color);
    }

    @Override
    public int hashCode() {
        return color.hashCode();
    }
}
