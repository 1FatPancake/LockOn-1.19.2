package net.leolifeless.lockonmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

/**
 * Renders lock-on information on the player's HUD (1.19.2 compatible)
 */
@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnHudRenderer {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Entity target = LockOnSystem.getTargetEntity();
        if (target == null || !target.isAlive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Font font = minecraft.font;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position the HUD elements in the top-center of the screen
        int hudWidth = 200;
        int hudX = (screenWidth - hudWidth) / 2; // Center horizontally
        int hudY = 20; // 20 pixels from top
        int lineHeight = 12;
        int currentY = hudY;

        // Background panel
        int panelWidth = hudWidth;
        int panelHeight = calculateHudHeight(target);
        renderHudBackground(poseStack, hudX - 10, hudY - 5, panelWidth, panelHeight);

        // Get text color from config
        Color textColor = LockOnConfig.getTextColor();
        int colorInt = (textColor.getAlpha() << 24) |
                (textColor.getRed() << 16) |
                (textColor.getGreen() << 8) |
                textColor.getBlue();

        // Show target name
        if (LockOnConfig.showTargetName()) {
            String nameLabel = "Target: ";
            String name = target.getDisplayName().getString();

            font.draw(poseStack, nameLabel, hudX, currentY, 0xFFFFFF);
            font.draw(poseStack, name, hudX + font.width(nameLabel), currentY, colorInt);
            currentY += lineHeight;
        }

        // Show distance
        if (LockOnConfig.showDistance()) {
            float distance = minecraft.player.distanceTo(target);
            String distanceLabel = "Distance: ";
            String distanceText;

            if (LockOnConfig.getDistanceUnit() == LockOnConfig.DistanceUnit.METERS) {
                distanceText = String.format("%.1fm", distance);
            } else {
                distanceText = String.format("%.1f blocks", distance);
            }

            font.draw(poseStack, distanceLabel, hudX, currentY, 0xFFFFFF);
            font.draw(poseStack, distanceText, hudX + font.width(distanceLabel), currentY, colorInt);
            currentY += lineHeight;
        }

        // Show health information (text only)
        if (LockOnConfig.showHealthBar() && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();

            String healthLabel = "Health: ";
            String healthText = String.format("%.0f/%.0f HP", health, maxHealth);

            font.draw(poseStack, healthLabel, hudX, currentY, 0xFFFFFF);
            font.draw(poseStack, healthText, hudX + font.width(healthLabel), currentY, colorInt);
            currentY += lineHeight;
        }
    }

    /**
     * Renders a semi-transparent background for the HUD (1.19.2 compatible)
     */
    private static void renderHudBackground(PoseStack poseStack, int x, int y, int width, int height) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Dark background with transparency
        bufferBuilder.vertex(matrix, x, y, 0).color(0, 0, 0, 128).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).color(0, 0, 0, 128).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(0, 0, 0, 128).endVertex();
        bufferBuilder.vertex(matrix, x, y + height, 0).color(0, 0, 0, 128).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        // Light border - Top
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + 1, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x, y + 1, 0).color(85, 85, 85, 255).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Light border - Bottom
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y + height - 1, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height - 1, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x, y + height, 0).color(85, 85, 85, 255).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Light border - Left
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + 1, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + 1, y + height, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x, y + height, 0).color(85, 85, 85, 255).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Light border - Right
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x + width - 1, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(85, 85, 85, 255).endVertex();
        bufferBuilder.vertex(matrix, x + width - 1, y + height, 0).color(85, 85, 85, 255).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Calculates the height needed for the HUD panel
     */
    private static int calculateHudHeight(Entity target) {
        int height = 10; // Base padding
        int lineHeight = 12;

        if (LockOnConfig.showTargetName()) {
            height += lineHeight;
        }

        if (LockOnConfig.showDistance()) {
            height += lineHeight;
        }

        if (LockOnConfig.showHealthBar() && target instanceof LivingEntity) {
            height += lineHeight; // Just text, no health bar
        }

        return height;
    }
}