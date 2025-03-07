package com.example.hypixelbedwarsmod;

import net.minecraft.util.EnumChatFormatting;
import java.util.HashSet;
import java.util.Set;

public class Team {
    private final String color;
    private final EnumChatFormatting formatting;
    private final Set<String> players = new HashSet<>();
    private boolean hasBed = true;

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
        return players;
    }

    public void addPlayer(String player) {
        players.add(player);
    }

    public void removePlayer(String player) {
        players.remove(player);
    }

    public boolean hasBed() {
        return hasBed;
    }

    public void setBedState(boolean hasBed) {
        this.hasBed = hasBed;
    }
}