package net.leolifeless.lockonmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnHudRenderer {

    // Built-in crosshair textures (add these to your textures/gui/ folder)
    private static final ResourceLocation CROSSHAIR_CIRCLE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/circle.png");
    private static final ResourceLocation CROSSHAIR_SQUARE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/square.png");
    private static final ResourceLocation CROSSHAIR_DIAMOND = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/diamond.png");
    private static final ResourceLocation CROSSHAIR_CROSS = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/cross.png");

    private static final ResourceLocation[] BUILT_IN_CROSSHAIRS = {
            CROSSHAIR_CIRCLE, CROSSHAIR_SQUARE, CROSSHAIR_DIAMOND, CROSSHAIR_CROSS
    };

    private static int currentCrosshairIndex = 0;
    private static boolean useCustomIndicators = false; // Toggle between crosshairs and custom indicators

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Entity target = LockOnSystem.getTargetEntity();
        if (target == null || !target.isAlive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        MatrixStack matrixStack = event.getMatrixStack();
        FontRenderer fontRenderer = minecraft.font;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position in top-right corner
        int margin = 10;
        int lineHeight = 12;
        int maxWidth = 150;

        // Calculate content
        String targetText = "Target: " + target.getDisplayName().getString();
        double distance = minecraft.player.distanceTo(target);
        String distanceText = String.format("Distance: %.1f blocks", distance);
        String healthText = "";

        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            healthText = String.format("Health: %.0f/%.0f HP", living.getHealth(), living.getMaxHealth());
        }

        // Calculate panel dimensions
        int textWidth = Math.max(
                Math.max(fontRenderer.width(targetText), fontRenderer.width(distanceText)),
                fontRenderer.width(healthText)
        );
        int panelWidth = Math.min(maxWidth, textWidth + 20);
        int panelHeight = 45;

        // Position from top-right
        int panelX = screenWidth - panelWidth - margin;
        int panelY = margin;

        // Semi-transparent dark background
        Screen.fill(matrixStack, panelX - 5, panelY - 5, panelX + panelWidth + 5, panelY + panelHeight + 5, 0x80000000);

        int currentY = panelY + 5;

        // Target name
        if (LockOnConfig.showTargetName()) {
            fontRenderer.draw(matrixStack, targetText, panelX, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Distance
        if (LockOnConfig.showDistance()) {
            fontRenderer.draw(matrixStack, distanceText, panelX, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Health
        if (LockOnConfig.showHealthBar() && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            fontRenderer.draw(matrixStack, healthText, panelX, currentY, 0xFFFFFF);
            currentY += lineHeight + 2;

            renderCompactHealthBar(matrixStack, panelX, currentY, panelWidth - 10, living.getHealth(), living.getMaxHealth());
        }
    }

    private static void renderCompactHealthBar(MatrixStack matrixStack, int x, int y, int width, float health, float maxHealth) {
        if (maxHealth <= 0) return;

        float healthPercent = health / maxHealth;
        int healthWidth = (int)(width * healthPercent);

        // Background
        Screen.fill(matrixStack, x, y, x + width, y + 4, 0xFF333333);

        // Health bar color
        int healthColor;
        if (healthPercent > 0.6f) {
            healthColor = 0xFF00FF00;
        } else if (healthPercent > 0.3f) {
            healthColor = 0xFFFFFF00;
        } else {
            healthColor = 0xFFFF0000;
        }

        if (healthWidth > 0) {
            Screen.fill(matrixStack, x, y, x + healthWidth, y + 4, healthColor);
        }
    }

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }

        Entity target = LockOnSystem.getTargetEntity();
        if (target == null || !target.isAlive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        MatrixStack matrixStack = event.getMatrixStack();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Cancel default crosshair
        event.setCanceled(true);

        // Render custom textured crosshair
        renderTexturedCrosshair(matrixStack, screenWidth / 2, screenHeight / 2);
    }

    /**
     * Render crosshair using texture (either built-in crosshairs or custom indicators)
     */
    private static void renderTexturedCrosshair(MatrixStack matrixStack, int centerX, int centerY) {
        Minecraft minecraft = Minecraft.getInstance();

        // Set up rendering state
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        try {
            // Get the texture to use
            ResourceLocation texture = getCurrentCrosshairTexture();
            minecraft.getTextureManager().bind(texture);

            // Crosshair size - adjust based on your texture sizes
            int size = 32; // You can make this configurable
            int halfSize = size / 2;

            // Render the textured quad
            Matrix4f matrix = matrixStack.last().pose();
            BufferBuilder buffer = Tessellator.getInstance().getBuilder();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            // Bottom-left
            buffer.vertex(matrix, centerX - halfSize, centerY + halfSize, 0.0f)
                    .uv(0.0f, 1.0f).endVertex();

            // Bottom-right
            buffer.vertex(matrix, centerX + halfSize, centerY + halfSize, 0.0f)
                    .uv(1.0f, 1.0f).endVertex();

            // Top-right
            buffer.vertex(matrix, centerX + halfSize, centerY - halfSize, 0.0f)
                    .uv(1.0f, 0.0f).endVertex();

            // Top-left
            buffer.vertex(matrix, centerX - halfSize, centerY - halfSize, 0.0f)
                    .uv(0.0f, 0.0f).endVertex();

            buffer.end();
            WorldVertexBufferUploader.end(buffer);

        } catch (Exception e) {
            LockOnMod.LOGGER.error("Error rendering crosshair texture: {}", e.getMessage());
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }
    }

    /**
     * Get the current crosshair texture
     */
    private static ResourceLocation getCurrentCrosshairTexture() {
        if (useCustomIndicators) {
            // Use the custom indicator system
            return CustomIndicatorManager.getCurrentIndicatorTexture();
        } else {
            // Use built-in crosshairs
            return BUILT_IN_CROSSHAIRS[currentCrosshairIndex % BUILT_IN_CROSSHAIRS.length];
        }
    }

    /**
     * Cycle to next crosshair
     */
    public static void cycleCrosshair() {
        if (useCustomIndicators) {
            CustomIndicatorManager.cycleToNextIndicator();
            LockOnMod.LOGGER.info("Switched to custom indicator: {}", CustomIndicatorManager.getCurrentIndicatorName());
        } else {
            currentCrosshairIndex = (currentCrosshairIndex + 1) % BUILT_IN_CROSSHAIRS.length;
            LockOnMod.LOGGER.info("Switched to built-in crosshair: {}", currentCrosshairIndex + 1);
        }
    }

    /**
     * Toggle between custom indicators and built-in crosshairs
     */
    public static void toggleCrosshairMode() {
        useCustomIndicators = !useCustomIndicators;
        String mode = useCustomIndicators ? "Custom Indicators" : "Built-in Crosshairs";
        LockOnMod.LOGGER.info("Switched to: {}", mode);
    }

    /**
     * Set specific crosshair by index (for built-in crosshairs)
     */
    public static void setCrosshair(int index) {
        if (!useCustomIndicators && index >= 0 && index < BUILT_IN_CROSSHAIRS.length) {
            currentCrosshairIndex = index;
        }
    }

    /**
     * Get current crosshair info for display
     */
    public static String getCurrentCrosshairInfo() {
        if (useCustomIndicators) {
            return "Custom: " + CustomIndicatorManager.getCurrentIndicatorName();
        } else {
            return "Built-in: " + (currentCrosshairIndex + 1);
        }
    }

    /**
     * Check if using custom indicators
     */
    public static boolean isUsingCustomIndicators() {
        return useCustomIndicators;
    }
}