package net.leolifeless.lockonmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Enhanced renderer with third person compatibility
 */
public class LockOnRenderer {

    private static final ResourceLocation CROSSHAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(LockOnMod.MOD_ID, "textures/gui/crosshair.png");
    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(LockOnMod.MOD_ID, "textures/gui/circle.png");
    private static final ResourceLocation DIAMOND_TEXTURE = ResourceLocation.fromNamespaceAndPath(LockOnMod.MOD_ID, "textures/gui/diamond.png");
    private static final ResourceLocation SQUARE_TEXTURE = ResourceLocation.fromNamespaceAndPath(LockOnMod.MOD_ID, "textures/gui/square.png");

    // Animation variables
    private static long animationTime = 0;
    private static float pulsePhase = 0.0f;

    /**
     * Enhanced indicator rendering with third person support
     */
    public static void renderLockOnIndicator(PoseStack poseStack, Entity target, Vec3 targetPos,
                                             float baseSize, LockOnConfig.IndicatorType type,
                                             boolean isThirdPerson) {

        if (target == null || !target.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Update animation
        updateAnimation();

        // Calculate adjusted size for third person
        float adjustedSize = baseSize;
        if (isThirdPerson) {
            adjustedSize = ThirdPersonCompatibility.getAdjustedIndicatorSize(baseSize);
        }

        // Calculate distance-based scaling
        double distance = mc.player.distanceTo(target);
        float distanceScale = calculateDistanceScale(distance, isThirdPerson);
        adjustedSize *= distanceScale;

        // Apply pulse animation if enabled
        if (LockOnConfig.isPulseEnabled()) {
            float pulseScale = 1.0f + (0.2f * (float)Math.sin(pulsePhase));
            adjustedSize *= pulseScale;
        }

        // Calculate colors
        Color primaryColor = calculateIndicatorColor(target, distance, isThirdPerson);
        Color outlineColor = calculateOutlineColor(target, distance, isThirdPerson);

        // Render the indicator
        switch (type) {
            case CIRCLE:
                renderCircleIndicator(poseStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case CROSSHAIR:
                renderCrosshairIndicator(poseStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case DIAMOND:
                renderDiamondIndicator(poseStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case SQUARE:
                renderSquareIndicator(poseStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case CUSTOM:
                renderCustomIndicator(poseStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
        }

        // Render additional info if enabled
        if (LockOnConfig.showTargetName() || LockOnConfig.showTargetHealth() || LockOnConfig.showTargetDistance()) {
            renderTargetInfo(poseStack, target, targetPos, adjustedSize, isThirdPerson);
        }
    }

    /**
     * Calculate distance-based scaling with third person adjustments
     */
    private static float calculateDistanceScale(double distance, boolean isThirdPerson) {
        float minScale = 0.5f;
        float maxScale = 2.0f;
        double maxDistance = LockOnConfig.getMaxLockOnDistance();

        // Adjust max distance for third person
        if (isThirdPerson) {
            maxDistance = ThirdPersonCompatibility.getAdjustedTargetingRange(maxDistance);
        }

        // Linear scaling based on distance
        float scale = (float)(1.0 + (distance / maxDistance));
        return Math.max(minScale, Math.min(maxScale, scale));
    }

    /**
     * Calculate indicator color with dynamic options
     */
    private static Color calculateIndicatorColor(Entity target, double distance, boolean isThirdPerson) {
        Color baseColor = LockOnConfig.getIndicatorColor();

        // Dynamic health coloring
        if (LockOnConfig.isDynamicHealthColorEnabled() && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            float healthPercent = living.getHealth() / living.getMaxHealth();

            if (healthPercent > 0.6f) {
                baseColor = Color.GREEN;
            } else if (healthPercent > 0.3f) {
                baseColor = Color.ORANGE;
            } else {
                baseColor = Color.RED;
            }
        }

        // Dynamic distance coloring
        if (LockOnConfig.isDynamicDistanceColorEnabled()) {
            double maxDistance = LockOnConfig.getMaxLockOnDistance();
            if (isThirdPerson) {
                maxDistance = ThirdPersonCompatibility.getAdjustedTargetingRange(maxDistance);
            }

            float distancePercent = (float)(distance / maxDistance);

            if (distancePercent < 0.3f) {
                baseColor = Color.CYAN;
            } else if (distancePercent < 0.7f) {
                baseColor = Color.YELLOW;
            } else {
                baseColor = Color.MAGENTA;
            }
        }

        // Third person color adjustment (slightly more transparent)
        if (isThirdPerson) {
            int alpha = Math.max(100, baseColor.getAlpha() - 30);
            baseColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        }

        return baseColor;
    }

    /**
     * Calculate outline color
     */
    private static Color calculateOutlineColor(Entity target, double distance, boolean isThirdPerson) {
        Color baseOutline = LockOnConfig.getOutlineColor();

        // Make outline more prominent in third person
        if (isThirdPerson) {
            int alpha = Math.min(255, baseOutline.getAlpha() + 50);
            baseOutline = new Color(baseOutline.getRed(), baseOutline.getGreen(), baseOutline.getBlue(), alpha);
        }

        return baseOutline;
    }

    /**
     * Render circle indicator
     */
    private static void renderCircleIndicator(PoseStack poseStack, Vec3 pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        poseStack.pushPose();

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Create circle vertices
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        Matrix4f matrix = poseStack.last().pose();

        // Render outline first
        if (outline.getAlpha() > 0) {
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                    .endVertex();

            int segments = isThirdPerson ? 32 : 24; // More segments for third person
            for (int i = 0; i <= segments; i++) {
                float angle = (float)(2 * Math.PI * i / segments);
                float x = (float)(pos.x + Math.cos(angle) * size * 1.1f);
                float y = (float)(pos.y + Math.sin(angle) * size * 1.1f);
                buffer.vertex(matrix, x, y, (float)pos.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
            }

            tesselator.end();
        }

        // Render main circle
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        int segments = isThirdPerson ? 32 : 24;
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = (float)(pos.x + Math.cos(angle) * size);
            float y = (float)(pos.y + Math.sin(angle) * size);
            buffer.vertex(matrix, x, y, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        tesselator.end();

        // Cleanup
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Render crosshair indicator
     */
    private static void renderCrosshairIndicator(PoseStack poseStack, Vec3 pos, float size,
                                                 Color color, Color outline, boolean isThirdPerson) {
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        float thickness = isThirdPerson ? size * 0.15f : size * 0.1f;
        float length = size;

        // Render crosshair lines
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Horizontal line
        buffer.vertex(matrix, (float)(pos.x - length), (float)(pos.y - thickness), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + length), (float)(pos.y - thickness), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + length), (float)(pos.y + thickness), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x - length), (float)(pos.y + thickness), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        // Vertical line
        buffer.vertex(matrix, (float)(pos.x - thickness), (float)(pos.y - length), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + thickness), (float)(pos.y - length), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + thickness), (float)(pos.y + length), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x - thickness), (float)(pos.y + length), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        tesselator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Render diamond indicator
     */
    private static void renderDiamondIndicator(PoseStack poseStack, Vec3 pos, float size,
                                               Color color, Color outline, boolean isThirdPerson) {
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Diamond vertices
        Vec3[] vertices = {
                new Vec3(pos.x, pos.y + size, pos.z),      // Top
                new Vec3(pos.x + size, pos.y, pos.z),      // Right
                new Vec3(pos.x, pos.y - size, pos.z),      // Bottom
                new Vec3(pos.x - size, pos.y, pos.z)       // Left
        };

        // Render diamond outline
        if (outline.getAlpha() > 0) {
            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < vertices.length; i++) {
                Vec3 vertex = vertices[i];
                buffer.vertex(matrix, (float)vertex.x, (float)vertex.y, (float)vertex.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
            }
            // Close the diamond
            buffer.vertex(matrix, (float)vertices[0].x, (float)vertices[0].y, (float)vertices[0].z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                    .endVertex();
            tesselator.end();
        }

        // Render filled diamond
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        for (int i = 0; i < vertices.length; i++) {
            Vec3 vertex = vertices[i];
            buffer.vertex(matrix, (float)vertex.x, (float)vertex.y, (float)vertex.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }
        // Close the fan
        buffer.vertex(matrix, (float)vertices[0].x, (float)vertices[0].y, (float)vertices[0].z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        tesselator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Render square indicator
     */
    private static void renderSquareIndicator(PoseStack poseStack, Vec3 pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Square vertices
        float halfSize = size * 0.7f; // Make it slightly smaller than circle

        // Render filled square
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        tesselator.end();

        // Render outline
        if (outline.getAlpha() > 0) {
            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
            buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y - halfSize), (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
            buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y + halfSize), (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
            buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y + halfSize), (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
            buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)pos.z)
                    .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();

            tesselator.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Render custom indicator using custom texture
     */
    private static void renderCustomIndicator(PoseStack poseStack, Vec3 pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        // Try to get custom indicator from CustomIndicatorManager
        ResourceLocation customTexture = CustomIndicatorManager.getCurrentIndicatorTexture();

        if (customTexture != null) {
            renderTexturedIndicator(poseStack, pos, size, color, customTexture, isThirdPerson);
        } else {
            // Fallback to circle if no custom texture
            renderCircleIndicator(poseStack, pos, size, color, outline, isThirdPerson);
        }
    }

    /**
     * Render textured indicator
     */
    private static void renderTexturedIndicator(PoseStack poseStack, Vec3 pos, float size,
                                                Color color, ResourceLocation texture, boolean isThirdPerson) {
        // Implementation for textured rendering would go here
        // For now, fallback to circle
        renderCircleIndicator(poseStack, pos, size, color, Color.WHITE, isThirdPerson);
    }

    /**
     * Render target information text
     */
    private static void renderTargetInfo(PoseStack poseStack, Entity target, Vec3 pos,
                                         float size, boolean isThirdPerson) {
        // Implementation for rendering target name, health, distance info
        // This would render text above/below the indicator
    }

    /**
     * Update animation variables
     */
    private static void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        animationTime = currentTime;
        pulsePhase += 0.05f;
        if (pulsePhase > 2 * Math.PI) {
            pulsePhase -= 2 * Math.PI;
        }
    }
}