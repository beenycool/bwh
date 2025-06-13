package com.example.hypixelbedwarsmod.ui;

import com.example.hypixelbedwarsmod.core.ConfigManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern configuration screen with categories and intuitive controls
 */
public class ConfigScreen extends GuiScreen {
    private final ConfigManager configManager;
    private final List<ConfigButton> configButtons = new ArrayList<>();
    private int currentCategory = 0;
    
    // Categories
    private static final String[] CATEGORIES = {
        "General Alerts", "Item Detection", "Performance", "Advanced"
    };
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CATEGORY_BUTTON_WIDTH = 100;

    public ConfigScreen(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        
        // Category buttons
        int categoryY = 30;
        for (int i = 0; i < CATEGORIES.length; i++) {
            GuiButton categoryButton = new GuiButton(i + 1000, 
                20 + i * (CATEGORY_BUTTON_WIDTH + 5), categoryY, 
                CATEGORY_BUTTON_WIDTH, BUTTON_HEIGHT, CATEGORIES[i]);
            buttonList.add(categoryButton);
        }
        
        // Config buttons based on current category
        setupCategoryButtons();
        
        // Save and Cancel buttons
        buttonList.add(new GuiButton(999, width / 2 - 100, height - 30, 80, BUTTON_HEIGHT, "Save"));
        buttonList.add(new GuiButton(998, width / 2 + 20, height - 30, 80, BUTTON_HEIGHT, "Cancel"));
    }

    private void setupCategoryButtons() {
        // Clear existing config buttons
        buttonList.removeIf(button -> button.id < 1000 && button.id != 998 && button.id != 999);
        configButtons.clear();
        
        int startY = 60;
        int currentY = startY;
        int centerX = width / 2;
        
        switch (currentCategory) {
            case 0: // General Alerts
                addToggleButton(0, centerX - BUTTON_WIDTH / 2, currentY, "Armor Alerts", 
                    configManager.isArmorAlertsEnabled());
                currentY += 25;
                
                addToggleButton(1, centerX - BUTTON_WIDTH / 2, currentY, "Item Alerts", 
                    configManager.isItemAlertsEnabled());
                currentY += 25;
                
                addToggleButton(2, centerX - BUTTON_WIDTH / 2, currentY, "Emerald Alerts", 
                    configManager.isEmeraldAlertsEnabled());
                currentY += 25;
                
                addToggleButton(3, centerX - BUTTON_WIDTH / 2, currentY, "Sword Alerts", 
                    configManager.isSwordAlertsEnabled());
                currentY += 25;
                
                addToggleButton(4, centerX - BUTTON_WIDTH / 2, currentY, "Potion Alerts", 
                    configManager.isPotionAlertsEnabled());
                break;
                
            case 1: // Item Detection
                addToggleButton(10, centerX - BUTTON_WIDTH / 2, currentY, "Fireball Alerts", 
                    configManager.isFireballAlertsEnabled());
                currentY += 25;
                
                addToggleButton(11, centerX - BUTTON_WIDTH / 2, currentY, "Obsidian Alerts", 
                    configManager.isObsidianAlertsEnabled());
                currentY += 25;
                
                addToggleButton(12, centerX - BUTTON_WIDTH / 2, currentY, "Item ESP", 
                    configManager.isItemESPEnabled());
                currentY += 25;
                
                addToggleButton(13, centerX - BUTTON_WIDTH / 2, currentY, "Exclude Teammates", 
                    configManager.isTeammatesExcluded());
                break;
                
            case 2: // Performance
                addSliderButton(20, centerX - BUTTON_WIDTH / 2, currentY, "ESP Max Distance", 
                    configManager.getItemESPMaxDistance(), 20f, 200f);
                currentY += 25;
                
                addSliderButton(21, centerX - BUTTON_WIDTH / 2, currentY, "ESP Fade Range", 
                    configManager.getItemESPFadeRange(), 10f, configManager.getItemESPMaxDistance());
                break;
                
            case 3: // Advanced
                addToggleButton(30, centerX - BUTTON_WIDTH / 2, currentY, "Fireball Trajectory", 
                    configManager.isFireballTrajectoryAlertsEnabled());
                currentY += 25;
                
                addSliderButton(31, centerX - BUTTON_WIDTH / 2, currentY, "General Cooldown (s)", 
                    configManager.getGeneralCooldown() / 1000f, 1f, 30f);
                currentY += 25;
                
                addSliderButton(32, centerX - BUTTON_WIDTH / 2, currentY, "Obsidian Cooldown (s)", 
                    configManager.getObsidianCooldown() / 1000f, 1f, 60f);
                break;
        }
    }

    private void addToggleButton(int id, int x, int y, String name, boolean currentValue) {
        String displayText = name + ": " + (currentValue ? 
            EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF");
        GuiButton button = new GuiButton(id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, displayText);
        buttonList.add(button);
        configButtons.add(new ConfigButton(id, name, currentValue));
    }

    private void addSliderButton(int id, int x, int y, String name, float currentValue, float min, float max) {
        String displayText = String.format("%s: %.1f", name, currentValue);
        GuiSlider slider = new GuiSlider(new GuiSlider.FormatHelper() {
            @Override
            public String getText(int id, String name, float value) {
                return String.format("%s: %.1f", name, min + value * (max - min));
            }
        }, id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, name, min, max, currentValue, null);
        
        buttonList.add(slider);
        configButtons.add(new ConfigButton(id, name, currentValue, min, max));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        
        if (button.id >= 1000) {
            // Category buttons
            currentCategory = button.id - 1000;
            initGui(); // Refresh GUI
            return;
        }
        
        if (button.id == 999) {
            // Save button
            saveConfig();
            mc.displayGuiScreen(null);
            return;
        }
        
        if (button.id == 998) {
            // Cancel button
            mc.displayGuiScreen(null);
            return;
        }
        
        // Toggle buttons
        ConfigButton configButton = getConfigButton(button.id);
        if (configButton != null && configButton.isToggle()) {
            configButton.toggle();
            updateButtonText(button, configButton);
        }
    }

    private void saveConfig() {
        for (ConfigButton configButton : configButtons) {
            switch (configButton.id) {
                case 0: configManager.setArmorAlertsEnabled(configButton.boolValue); break;
                case 1: configManager.setItemAlertsEnabled(configButton.boolValue); break;
                case 2: configManager.setEmeraldAlertsEnabled(configButton.boolValue); break;
                case 3: configManager.setSwordAlertsEnabled(configButton.boolValue); break;
                case 4: configManager.setPotionAlertsEnabled(configButton.boolValue); break;
                case 10: configManager.setFireballAlertsEnabled(configButton.boolValue); break;
                case 11: configManager.setObsidianAlertsEnabled(configButton.boolValue); break;
                case 12: configManager.setItemESPEnabled(configButton.boolValue); break;
                case 13: configManager.setTeammatesExcluded(configButton.boolValue); break;
                case 20: configManager.setItemESPMaxDistance(configButton.floatValue); break;
                case 21: configManager.setItemESPFadeRange(configButton.floatValue); break;
                case 30: configManager.setFireballTrajectoryAlertsEnabled(configButton.boolValue); break;
                case 31: configManager.setGeneralCooldown((long)(configButton.floatValue * 1000)); break;
                case 32: configManager.setObsidianCooldown((long)(configButton.floatValue * 1000)); break;
            }
        }
        
        // Update sliders from GUI
        for (GuiButton button : buttonList) {
            if (button instanceof GuiSlider) {
                GuiSlider slider = (GuiSlider) button;
                ConfigButton configButton = getConfigButton(slider.id);
                if (configButton != null) {
                    configButton.floatValue = configButton.min + slider.getSliderValue() * 
                        (configButton.max - configButton.min);
                }
            }
        }
        
        configManager.saveConfig();
    }

    private void updateButtonText(GuiButton button, ConfigButton configButton) {
        String displayText = configButton.name + ": " + (configButton.boolValue ? 
            EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF");
        button.displayString = displayText;
    }

    private ConfigButton getConfigButton(int id) {
        return configButtons.stream()
            .filter(cb -> cb.id == id)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Title
        String title = "Bed Wars Assistant Configuration";
        drawCenteredString(fontRendererObj, title, width / 2, 10, 0xFFFFFF);
        
        // Category highlight
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (i == currentCategory) {
                GuiButton categoryButton = buttonList.get(i + 1000);
                drawRect(categoryButton.xPosition - 2, categoryButton.yPosition - 2,
                        categoryButton.xPosition + categoryButton.width + 2,
                        categoryButton.yPosition + categoryButton.height + 2, 0x80FFFFFF);
            }
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Help text
        String helpText = "Use .bwconfig in chat to open this menu";
        drawCenteredString(fontRendererObj, EnumChatFormatting.GRAY + helpText, 
            width / 2, height - 45, 0xFFFFFF);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Helper class to store configuration button data
     */
    private static class ConfigButton {
        final int id;
        final String name;
        boolean boolValue;
        float floatValue;
        final float min;
        final float max;
        final boolean isToggle;

        ConfigButton(int id, String name, boolean value) {
            this.id = id;
            this.name = name;
            this.boolValue = value;
            this.isToggle = true;
            this.min = 0;
            this.max = 0;
        }

        ConfigButton(int id, String name, float value, float min, float max) {
            this.id = id;
            this.name = name;
            this.floatValue = value;
            this.min = min;
            this.max = max;
            this.isToggle = false;
        }

        boolean isToggle() {
            return isToggle;
        }

        void toggle() {
            if (isToggle) {
                boolValue = !boolValue;
            }
        }
    }
}
