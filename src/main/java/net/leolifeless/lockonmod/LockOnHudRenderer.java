package net.leolifeless.lockonmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnHudRenderer {

    private static int currentCrosshairIndex = 0;
    private static final String[] CROSSHAIR_NAMES = {"Cross", "Circle", "Optimized Circle", "Square", "Diamond", "Brackets"};

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

        // Render custom crosshair
        renderLightweightCrosshair(matrixStack, screenWidth / 2, screenHeight / 2);
    }

    /**
     * Render crosshair using lightweight methods optimized for 1.16.5
     */
    private static void renderLightweightCrosshair(MatrixStack matrixStack, int centerX, int centerY) {
        int size = 8;      // Reduced size for cleaner look
        int thickness = 1;  // Thinner lines

        // Colors - using proper ARGB format for 1.16.5
        int whiteColor = 0xFFFFFFFF;
        int blackColor = 0xFF000000;

        switch (currentCrosshairIndex) {
            case 0: // Cross
                renderCleanCross(matrixStack, centerX, centerY, size, thickness, whiteColor, blackColor);
                break;
            case 1: // Circle (clean approximated)
                renderCleanCircle(matrixStack, centerX, centerY, size, whiteColor, blackColor);
                break;
            case 2: // Optimized Tessellator Circle
                renderTessellatorCircle(matrixStack, centerX, centerY, size, thickness);
                break;
            case 3: // Square
                renderCleanSquare(matrixStack, centerX, centerY, size, whiteColor, blackColor);
                break;
            case 4: // Diamond
                renderCleanDiamond(matrixStack, centerX, centerY, size, whiteColor, blackColor);
                break;
            case 5: // Brackets
                renderCleanBrackets(matrixStack, centerX, centerY, size, whiteColor, blackColor);
                break;
        }
    }

    private static void renderCleanCross(MatrixStack matrixStack, int centerX, int centerY, int size, int thickness, int whiteColor, int blackColor) {
        // Thin, clean cross with minimal outline
        // Horizontal line
        Screen.fill(matrixStack, centerX - size - 1, centerY - 1, centerX + size + 1, centerY + 1, blackColor);
        Screen.fill(matrixStack, centerX - size, centerY, centerX + size, centerY, whiteColor);

        // Vertical line
        Screen.fill(matrixStack, centerX - 1, centerY - size - 1, centerX + 1, centerY + size + 1, blackColor);
        Screen.fill(matrixStack, centerX, centerY - size, centerX, centerY + size, whiteColor);

        // Center dot for precision
        Screen.fill(matrixStack, centerX, centerY, centerX + 1, centerY + 1, whiteColor);
    }

    private static void renderCleanCircle(MatrixStack matrixStack, int centerX, int centerY, int size, int whiteColor, int blackColor) {
        // Clean circle using 8 small segments positioned precisely
        int radius = size;

        // Cardinal directions (4 main points)
        Screen.fill(matrixStack, centerX - 1, centerY - radius, centerX + 1, centerY - radius + 2, whiteColor); // Top
        Screen.fill(matrixStack, centerX - 1, centerY + radius - 2, centerX + 1, centerY + radius, whiteColor);     // Bottom
        Screen.fill(matrixStack, centerX - radius, centerY - 1, centerX - radius + 2, centerY + 1, whiteColor);     // Left
        Screen.fill(matrixStack, centerX + radius - 2, centerY - 1, centerX + radius, centerY + 1, whiteColor);     // Right

        // Diagonal points (4 corner approximations) - positioned more precisely
        int diagOffset = (int)(radius * 0.707); // cos(45°) for proper circle approximation

        Screen.fill(matrixStack, centerX - diagOffset - 1, centerY - diagOffset, centerX - diagOffset + 1, centerY - diagOffset + 1, whiteColor); // Top-left
        Screen.fill(matrixStack, centerX + diagOffset - 1, centerY - diagOffset, centerX + diagOffset + 1, centerY - diagOffset + 1, whiteColor); // Top-right
        Screen.fill(matrixStack, centerX - diagOffset - 1, centerY + diagOffset, centerX - diagOffset + 1, centerY + diagOffset + 1, whiteColor); // Bottom-left
        Screen.fill(matrixStack, centerX + diagOffset - 1, centerY + diagOffset, centerX + diagOffset + 1, centerY + diagOffset + 1, whiteColor); // Bottom-right

        // Center dot
        Screen.fill(matrixStack, centerX, centerY, centerX + 1, centerY + 1, whiteColor);
    }

    private static void renderCleanSquare(MatrixStack matrixStack, int centerX, int centerY, int size, int whiteColor, int blackColor) {
        // Clean, thin square outline
        int halfSize = size;

        // Top edge
        Screen.fill(matrixStack, centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY - halfSize + 1, whiteColor);
        // Bottom edge
        Screen.fill(matrixStack, centerX - halfSize, centerY + halfSize - 1, centerX + halfSize, centerY + halfSize, whiteColor);
        // Left edge
        Screen.fill(matrixStack, centerX - halfSize, centerY - halfSize, centerX - halfSize + 1, centerY + halfSize, whiteColor);
        // Right edge
        Screen.fill(matrixStack, centerX + halfSize - 1, centerY - halfSize, centerX + halfSize, centerY + halfSize, whiteColor);

        // Center dot
        Screen.fill(matrixStack, centerX, centerY, centerX + 1, centerY + 1, whiteColor);
    }

    private static void renderCleanDiamond(MatrixStack matrixStack, int centerX, int centerY, int size, int whiteColor, int blackColor) {
        // Clean diamond using precise lines
        int radius = size;

        // Draw diamond outline using 4 lines
        // Top-left to top-right
        for (int i = 0; i < radius; i++) {
            Screen.fill(matrixStack, centerX - i, centerY - radius + i, centerX - i + 1, centerY - radius + i + 1, whiteColor);
            Screen.fill(matrixStack, centerX + i, centerY - radius + i, centerX + i + 1, centerY - radius + i + 1, whiteColor);
        }

        // Bottom-left to bottom-right
        for (int i = 0; i < radius; i++) {
            Screen.fill(matrixStack, centerX - radius + i, centerY + i, centerX - radius + i + 1, centerY + i + 1, whiteColor);
            Screen.fill(matrixStack, centerX + radius - i, centerY + i, centerX + radius - i + 1, centerY + i + 1, whiteColor);
        }

        // Center dot
        Screen.fill(matrixStack, centerX, centerY, centerX + 1, centerY + 1, whiteColor);
    }

    private static void renderCleanBrackets(MatrixStack matrixStack, int centerX, int centerY, int size, int whiteColor, int blackColor) {
        // Clean, minimal brackets in corners
        int bracketSize = size / 2;
        int offset = size + 1;

        // Top-left bracket
        renderSingleCleanBracket(matrixStack, centerX - offset, centerY - offset, bracketSize, whiteColor, true, true);
        // Top-right bracket
        renderSingleCleanBracket(matrixStack, centerX + offset, centerY - offset, bracketSize, whiteColor, false, true);
        // Bottom-left bracket
        renderSingleCleanBracket(matrixStack, centerX - offset, centerY + offset, bracketSize, whiteColor, true, false);
        // Bottom-right bracket
        renderSingleCleanBracket(matrixStack, centerX + offset, centerY + offset, bracketSize, whiteColor, false, false);

        // Center dot
        Screen.fill(matrixStack, centerX, centerY, centerX + 1, centerY + 1, whiteColor);
    }

    private static void renderSingleCleanBracket(MatrixStack matrixStack, int x, int y, int size, int whiteColor, boolean flipX, boolean flipY) {
        int dirX = flipX ? -1 : 1;
        int dirY = flipY ? -1 : 1;

        // Horizontal line
        Screen.fill(matrixStack, x, y, x + dirX * size, y + 1, whiteColor);
        // Vertical line
        Screen.fill(matrixStack, x, y, x + 1, y + dirY * size, whiteColor);
    }

    /**
     * Optimized Tessellator circle using 1.16.5 compatible methods
     */
    private static void renderTessellatorCircle(MatrixStack matrixStack, int centerX, int centerY, int size, int thickness) {
        Matrix4f matrix = matrixStack.last().pose();

        // Proper 1.16.5 render state setup
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        float radius = size * 0.75f;
        int segments = 16; // Reduced for better performance

        // Render circle outline using LINE_STRIP
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = centerX + (float)Math.cos(angle) * radius;
            float y = centerY + (float)Math.sin(angle) * radius;
            buffer.vertex(matrix, x, y, 0).color(255, 255, 255, 255).endVertex();
        }

        tessellator.end();

        // Center dot
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float dotSize = 1.0f;
        buffer.vertex(matrix, centerX - dotSize, centerY - dotSize, 0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, centerX + dotSize, centerY - dotSize, 0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, centerX + dotSize, centerY + dotSize, 0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, centerX - dotSize, centerY + dotSize, 0).color(255, 255, 255, 255).endVertex();
        tessellator.end();

        // Restore render state
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Cycle to next crosshair
     */
    public static void cycleCrosshair() {
        currentCrosshairIndex = (currentCrosshairIndex + 1) % CROSSHAIR_NAMES.length;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    new net.minecraft.util.text.StringTextComponent("Crosshair: " + CROSSHAIR_NAMES[currentCrosshairIndex]),
                    true
            );
        }

        LockOnMod.LOGGER.info("Switched to crosshair: " + CROSSHAIR_NAMES[currentCrosshairIndex]);
    }

    /**
     * Get current crosshair info for display
     */
    public static String getCurrentCrosshairInfo() {
        return "Style: " + CROSSHAIR_NAMES[currentCrosshairIndex];
    }

    /**
     * Set specific crosshair by index
     */
    public static void setCrosshair(int index) {
        if (index >= 0 && index < CROSSHAIR_NAMES.length) {
            currentCrosshairIndex = index;
        }
    }

    /**
     * Get current crosshair index
     */
    public static int getCurrentCrosshairIndex() {
        return currentCrosshairIndex;
    }

    /**
     * Get available crosshair names
     */
    public static String[] getCrosshairNames() {
        return CROSSHAIR_NAMES.clone();
    }
}