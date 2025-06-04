package net.leolifeless.lockonmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.awt.*;

import static net.leolifeless.lockonmod.LockOnMod.MOD_ID;

public class LockOnRenderer {
    // Animation variables
    private static long lastTime = 0;
    private static float pulseSize = 0.0F;
    private static float glowSize = 0.0F;
    private static float rotationAngle = 0.0F;

    // Custom texture for the indicator
    private static final ResourceLocation CUSTOM_INDICATOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/custom_indicator.png");

    /**
     * Main rendering method with enhanced features - now only renders the indicator
     */
    public static void renderLockOnIndicator(RenderLevelStageEvent event, Entity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || target == null) return;

        // Update animations
        updateAnimations();

        // Get the camera position
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        // Get position of the entity with configurable offset
        float heightOffset = LockOnConfig.getCameraOffset();
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * heightOffset, 0);

        // Calculate relative position
        double x = targetPos.x - cameraPos.x;
        double y = targetPos.y - cameraPos.y;
        double z = targetPos.z - cameraPos.z;

        // Set up rendering
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Move to the target position
        poseStack.translate(x, y, z);

        // Make the indicator always face the camera
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180f));

        // Set up render system
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Calculate dynamic size
        float indicatorSize = LockOnConfig.getIndicatorSize();
        float animatedSize = indicatorSize;

        if (LockOnConfig.isPulseEnabled()) {
            animatedSize += pulseSize;
        }

        // Calculate dynamic color
        Color indicatorColor = calculateDynamicColor(target, minecraft.player.distanceTo(target));

        // Render glow effect if enabled
        if (LockOnConfig.isGlowEnabled()) {
            renderGlowEffect(poseStack, animatedSize, indicatorColor);
        }

        // Render main indicator based on type
        LockOnConfig.IndicatorType type = LockOnConfig.getIndicatorType();
        switch (type) {
            case CIRCLE:
                renderCircleIndicator(poseStack, animatedSize, indicatorColor);
                break;
            case CROSSHAIR:
                renderCrosshairIndicator(poseStack, animatedSize, indicatorColor);
                break;
            case DIAMOND:
                renderDiamondIndicator(poseStack, animatedSize, indicatorColor);
                break;
            case SQUARE:
                renderSquareIndicator(poseStack, animatedSize, indicatorColor);
                break;
            case CUSTOM:
                renderCustomIndicator(poseStack, animatedSize, indicatorColor);
                break;
        }

        // Clean up rendering
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Renders custom image indicator with rotation and color tinting
     */
    private static void renderCustomIndicator(PoseStack poseStack, float size, Color color) {
        poseStack.pushPose();

        // Apply rotation animation
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotationAngle));

        // Set up texture rendering
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, CUSTOM_INDICATOR_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Define the quad vertices for the texture
        float halfSize = size;

        // Bottom-left
        bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0.0F)
                .uv(0.0F, 1.0F)  // Bottom-left UV
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        // Bottom-right
        bufferBuilder.vertex(matrix, halfSize, -halfSize, 0.0F)
                .uv(1.0F, 1.0F)  // Bottom-right UV
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        // Top-right
        bufferBuilder.vertex(matrix, halfSize, halfSize, 0.0F)
                .uv(1.0F, 0.0F)  // Top-right UV
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        // Top-left
        bufferBuilder.vertex(matrix, -halfSize, halfSize, 0.0F)
                .uv(0.0F, 0.0F)  // Top-left UV
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        poseStack.popPose();
    }

    /**
     * Updates all animation effects
     */
    private static void updateAnimations() {
        long currentTime = System.currentTimeMillis();

        // First time initialization
        if (lastTime == 0) {
            lastTime = currentTime;
        }

        float deltaTime = (currentTime - lastTime) / 1000.0F;

        // Calculate pulse effect
        if (LockOnConfig.isPulseEnabled()) {
            float pulseSpeed = LockOnConfig.getPulseSpeed();
            float pulseAmplitude = LockOnConfig.getPulseAmplitude();
            float time = currentTime / 1000.0F * pulseSpeed;
            pulseSize = (float) Math.sin(time) * pulseAmplitude;
        }

        // Calculate glow effect
        if (LockOnConfig.isGlowEnabled()) {
            float glowSpeed = LockOnConfig.getPulseSpeed() * 0.7F;
            float time = currentTime / 1000.0F * glowSpeed;
            glowSize = (float) Math.sin(time * 0.5) * LockOnConfig.getGlowIntensity();
        }

        // Calculate rotation for animated indicators
        rotationAngle += deltaTime * 45.0F; // 45 degrees per second
        if (rotationAngle >= 360.0F) {
            rotationAngle -= 360.0F;
        }

        lastTime = currentTime;
    }

    /**
     * Calculates dynamic color based on configuration
     */
    private static Color calculateDynamicColor(Entity target, float distance) {
        Color baseColor = LockOnConfig.getIndicatorColor();

        if (LockOnConfig.isDynamicColorBasedOnHealth() && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            float healthPercent = living.getHealth() / living.getMaxHealth();

            // Interpolate from red (low health) to green (high health)
            int red = (int) (255 * (1 - healthPercent) + baseColor.getRed() * healthPercent);
            int green = (int) (255 * healthPercent + baseColor.getGreen() * (1 - healthPercent));
            int blue = baseColor.getBlue();

            baseColor = new Color(Math.min(255, red), Math.min(255, green), blue, baseColor.getAlpha());
        }

        if (LockOnConfig.isDynamicColorBasedOnDistance()) {
            float maxDistance = LockOnConfig.getMaxLockOnDistance();
            float distancePercent = Math.min(1.0F, distance / maxDistance);

            // Interpolate from blue (close) to red (far)
            int red = (int) (255 * distancePercent + baseColor.getRed() * (1 - distancePercent));
            int green = baseColor.getGreen();
            int blue = (int) (255 * (1 - distancePercent) + baseColor.getBlue() * distancePercent);

            baseColor = new Color(Math.min(255, red), green, Math.min(255, blue), baseColor.getAlpha());
        }

        return baseColor;
    }

    /**
     * Renders glow effect around the indicator
     */
    private static void renderGlowEffect(PoseStack poseStack, float size, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float glowRadius = size * (1.5F + glowSize);
        Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(10, color.getAlpha() / 4));

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Center vertex
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 0)
                .endVertex();

        // Glow circle
        int segments = 24;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * glowRadius;
            float y = (float) Math.sin(angle) * glowRadius;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), glowColor.getAlpha())
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    /**
     * Renders circle indicator (original)
     */
    private static void renderCircleIndicator(PoseStack poseStack, float size, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Filled circle
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        Color centerColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 3);
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getAlpha())
                .endVertex();

        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * size;
            float y = (float) Math.sin(angle) * size;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());

        // Outline
        renderCircleOutline(poseStack, size, LockOnConfig.getOutlineColor());
    }

    /**
     * Renders crosshair indicator
     */
    private static void renderCrosshairIndicator(PoseStack poseStack, float size, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float thickness = size * 0.1F;
        float length = size;

        // Horizontal bar
        bufferBuilder.vertex(matrix, -length, -thickness, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, length, -thickness, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, length, thickness, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, -length, thickness, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        // Vertical bar
        bufferBuilder.vertex(matrix, -thickness, -length, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, thickness, -length, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, thickness, length, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, -thickness, length, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    /**
     * Renders diamond indicator
     */
    private static void renderDiamondIndicator(PoseStack poseStack, float size, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Center vertex
        Color centerColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 3);
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getAlpha())
                .endVertex();

        // Diamond vertices
        bufferBuilder.vertex(matrix, 0.0F, size, 0.0F) // Top
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, size, 0.0F, 0.0F) // Right
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, 0.0F, -size, 0.0F) // Bottom
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, -size, 0.0F, 0.0F) // Left
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, 0.0F, size, 0.0F) // Close the shape
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        // Diamond outline
        renderDiamondOutline(poseStack, size, LockOnConfig.getOutlineColor());
    }

    /**
     * Renders square indicator
     */
    private static void renderSquareIndicator(PoseStack poseStack, float size, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Center vertex
        Color centerColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 3);
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getAlpha())
                .endVertex();

        // Square vertices
        bufferBuilder.vertex(matrix, -size, -size, 0.0F) // Bottom-left
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, size, -size, 0.0F) // Bottom-right
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, size, size, 0.0F) // Top-right
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, -size, size, 0.0F) // Top-left
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        bufferBuilder.vertex(matrix, -size, -size, 0.0F) // Close the shape
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        // Square outline
        renderSquareOutline(poseStack, size, LockOnConfig.getOutlineColor());
    }

    // Outline rendering methods
    private static void renderCircleOutline(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * size;
            float y = (float) Math.sin(angle) * size;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void renderDiamondOutline(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, 0.0F, size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, size, 0.0F, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, 0.0F, -size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, -size, 0.0F, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, 0.0F, size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void renderSquareOutline(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, -size, -size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, size, -size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, size, size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, -size, size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        bufferBuilder.vertex(matrix, -size, -size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}