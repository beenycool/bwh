package com.example.hypixelbedwarsmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/**
 * Utility class for rendering operations
 */
public class RenderUtils {
    
    /**
     * Render a colored box around an entity
     */
    public static void renderEntityBox(Entity entity, float red, float green, float blue, float alpha, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        AxisAlignedBB box = new AxisAlignedBB(
            boundingBox.minX - entity.posX + x,
            boundingBox.minY - entity.posY + y,
            boundingBox.minZ - entity.posZ + z,
            boundingBox.maxX - entity.posX + x,
            boundingBox.maxY - entity.posY + y,
            boundingBox.maxZ - entity.posZ + z
        );
        
        renderBox(box, red, green, blue, alpha);
    }

    /**
     * Render a colored outline box
     */
    public static void renderBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // Render outline
        GlStateManager.color(red, green, blue, alpha);
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        
        // Bottom face
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        
        // Vertical lines
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();

        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Calculate color with distance-based fading
     */
    public static float[] calculateFadedColor(float distance, float maxDistance, float fadeRange, 
                                            float[] baseColor) {
        float alpha = 1.0f;
        
        if (distance > fadeRange) {
            float fadeDistance = maxDistance - fadeRange;
            float fadeFactor = Math.max(0, (maxDistance - distance) / fadeDistance);
            alpha = fadeFactor * 0.8f + 0.2f; // Minimum 20% alpha
        }
        
        return new float[]{baseColor[0], baseColor[1], baseColor[2], alpha};
    }

    /**
     * Convert hex color to RGB array
     */
    public static float[] hexToRGB(int hex) {
        float red = ((hex >> 16) & 0xFF) / 255.0f;
        float green = ((hex >> 8) & 0xFF) / 255.0f;
        float blue = (hex & 0xFF) / 255.0f;
        return new float[]{red, green, blue};
    }

    /**
     * Get camera position
     */
    public static double[] getCameraPos() {
        Minecraft mc = Minecraft.getMinecraft();
        double x = mc.getRenderManager().viewerPosX;
        double y = mc.getRenderManager().viewerPosY;
        double z = mc.getRenderManager().viewerPosZ;
        return new double[]{x, y, z};
    }

    /**
     * Translate to camera-relative coordinates
     */
    public static void translateToCamera(double x, double y, double z) {
        double[] cameraPos = getCameraPos();
        GlStateManager.translate(x - cameraPos[0], y - cameraPos[1], z - cameraPos[2]);
    }
}
