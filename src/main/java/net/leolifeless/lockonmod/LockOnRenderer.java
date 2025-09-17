package net.leolifeless.lockonmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.leolifeless.lockonmod.compat.ThirdPersonCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

import java.awt.Color;

/**
 * Enhanced renderer with third person compatibility for 1.16.5
 */
public class LockOnRenderer {

    private static final ResourceLocation CROSSHAIR_TEXTURE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/crosshair.png");
    private static final ResourceLocation CIRCLE_TEXTURE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/circle.png");
    private static final ResourceLocation DIAMOND_TEXTURE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/diamond.png");
    private static final ResourceLocation SQUARE_TEXTURE = new ResourceLocation(LockOnMod.MOD_ID, "textures/gui/square.png");

    // Animation variables
    private static long animationTime = 0;
    private static float pulsePhase = 0.0f;

    /**
     * Enhanced indicator rendering with third person support
     */
    public static void renderLockOnIndicator(MatrixStack matrixStack, Entity target, Vector3d targetPos,
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
                renderCircleIndicator(matrixStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case CROSSHAIR:
                renderCrosshairIndicator(matrixStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case DIAMOND:
                renderDiamondIndicator(matrixStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case SQUARE:
                renderSquareIndicator(matrixStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
            case CUSTOM:
                renderCustomIndicator(matrixStack, targetPos, adjustedSize, primaryColor, outlineColor, isThirdPerson);
                break;
        }

        // Render additional info if enabled
        if (LockOnConfig.showTargetName() || LockOnConfig.showTargetHealth() || LockOnConfig.showTargetDistance()) {
            renderTargetInfo(matrixStack, target, targetPos, adjustedSize, isThirdPerson);
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
     * Render circle indicator (1.16.5 compatible)
     */
    private static void renderCircleIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        matrixStack.pushPose();

        // Setup rendering for 1.16.5
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        Matrix4f matrix = matrixStack.last().pose();

        // Render outline first
        if (outline.getAlpha() > 0) {
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS mode

            int segments = isThirdPerson ? 32 : 24; // More segments for third person
            for (int i = 0; i < segments; i++) {
                float angle1 = (float)(2 * Math.PI * i / segments);
                float angle2 = (float)(2 * Math.PI * (i + 1) / segments);

                float x1 = (float)(pos.x + Math.cos(angle1) * size * 1.1f);
                float y1 = (float)(pos.y + Math.sin(angle1) * size * 1.1f);
                float x2 = (float)(pos.x + Math.cos(angle2) * size * 1.1f);
                float y2 = (float)(pos.y + Math.sin(angle2) * size * 1.1f);

                // Create quad for each segment
                buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
                buffer.vertex(matrix, x1, y1, (float)pos.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
                buffer.vertex(matrix, x2, y2, (float)pos.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
                buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
            }

            tessellator.end();
        }

        // Render main circle
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS mode

        int segments = isThirdPerson ? 32 : 24;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float)(2 * Math.PI * i / segments);
            float angle2 = (float)(2 * Math.PI * (i + 1) / segments);

            float x1 = (float)(pos.x + Math.cos(angle1) * size);
            float y1 = (float)(pos.y + Math.sin(angle1) * size);
            float x2 = (float)(pos.x + Math.cos(angle2) * size);
            float y2 = (float)(pos.y + Math.sin(angle2) * size);

            // Create quad for each segment
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, x1, y1, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, x2, y2, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        tessellator.end();

        // Cleanup
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render crosshair indicator (1.16.5 compatible)
     */
    private static void renderCrosshairIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                                 Color color, Color outline, boolean isThirdPerson) {
        matrixStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        float thickness = isThirdPerson ? size * 0.15f : size * 0.1f;
        float length = size;

        // Render crosshair lines
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS

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

        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render diamond indicator (1.16.5 compatible)
     */
    private static void renderDiamondIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                               Color color, Color outline, boolean isThirdPerson) {
        matrixStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        // Diamond vertices
        Vector3d[] vertices = {
                new Vector3d(pos.x, pos.y + size, pos.z),      // Top
                new Vector3d(pos.x + size, pos.y, pos.z),      // Right
                new Vector3d(pos.x, pos.y - size, pos.z),      // Bottom
                new Vector3d(pos.x - size, pos.y, pos.z)       // Left
        };

        // Render diamond outline
        if (outline.getAlpha() > 0) {
            buffer.begin(1, DefaultVertexFormats.POSITION_COLOR); // GL_LINES
            for (int i = 0; i < vertices.length; i++) {
                Vector3d vertex1 = vertices[i];
                Vector3d vertex2 = vertices[(i + 1) % vertices.length];

                buffer.vertex(matrix, (float)vertex1.x, (float)vertex1.y, (float)vertex1.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
                buffer.vertex(matrix, (float)vertex2.x, (float)vertex2.y, (float)vertex2.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha())
                        .endVertex();
            }
            tessellator.end();
        }

        // Render filled diamond
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS

        // Create diamond as two triangles (using quad mode)
        for (int i = 0; i < vertices.length; i++) {
            Vector3d vertex = vertices[i];
            Vector3d nextVertex = vertices[(i + 1) % vertices.length];

            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, (float)vertex.x, (float)vertex.y, (float)vertex.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, (float)nextVertex.x, (float)nextVertex.y, (float)nextVertex.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render square indicator (1.16.5 compatible)
     */
    private static void renderSquareIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        matrixStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        // Square vertices
        float halfSize = size * 0.7f; // Make it slightly smaller than circle

        // Render filled square
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS

        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        tessellator.end();

        // Render outline
        if (outline.getAlpha() > 0) {
            buffer.begin(1, DefaultVertexFormats.POSITION_COLOR); // GL_LINES

            // Draw outline lines
            Vector3d[] corners = {
                    new Vector3d(pos.x - halfSize, pos.y - halfSize, pos.z),
                    new Vector3d(pos.x + halfSize, pos.y - halfSize, pos.z),
                    new Vector3d(pos.x + halfSize, pos.y + halfSize, pos.z),
                    new Vector3d(pos.x - halfSize, pos.y + halfSize, pos.z)
            };

            for (int i = 0; i < corners.length; i++) {
                Vector3d corner1 = corners[i];
                Vector3d corner2 = corners[(i + 1) % corners.length];

                buffer.vertex(matrix, (float)corner1.x, (float)corner1.y, (float)corner1.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
                buffer.vertex(matrix, (float)corner2.x, (float)corner2.y, (float)corner2.z)
                        .color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()).endVertex();
            }

            tessellator.end();
        }

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render custom indicator using custom texture
     */
    private static void renderCustomIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                              Color color, Color outline, boolean isThirdPerson) {
        // Try to get custom indicator from CustomIndicatorManager
        ResourceLocation customTexture = CustomIndicatorManager.getCurrentIndicatorTexture();

        if (customTexture != null) {
            renderTexturedIndicator(matrixStack, pos, size, color, customTexture, isThirdPerson);
        } else {
            // Fallback to circle if no custom texture
            renderCircleIndicator(matrixStack, pos, size, color, outline, isThirdPerson);
        }
    }

    /**
     * Render textured indicator (1.16.5 compatible)
     */
    private static void renderTexturedIndicator(MatrixStack matrixStack, Vector3d pos, float size,
                                                Color color, ResourceLocation texture, boolean isThirdPerson) {
        matrixStack.pushPose();

        // Bind texture and setup rendering
        Minecraft.getInstance().getTextureManager().bind(texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.enableTexture();

        // Set color tint
        RenderSystem.color4f(
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        float halfSize = size;

        // Render textured quad
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR); // GL_QUADS with texture

        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .uv(0.0f, 0.0f)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y - halfSize), (float)pos.z)
                .uv(1.0f, 0.0f)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        buffer.vertex(matrix, (float)(pos.x + halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .uv(1.0f, 1.0f)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
        buffer.vertex(matrix, (float)(pos.x - halfSize), (float)(pos.y + halfSize), (float)pos.z)
                .uv(0.0f, 1.0f)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        tessellator.end();

        // Reset color and cleanup
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render target information text (1.16.5 compatible)
     */
    private static void renderTargetInfo(MatrixStack matrixStack, Entity target, Vector3d pos,
                                         float size, boolean isThirdPerson) {
        if (!LockOnConfig.showTargetName() && !LockOnConfig.showTargetHealth() && !LockOnConfig.showTargetDistance()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        matrixStack.pushPose();

        // Setup text rendering
        IRenderTypeBuffer.Impl renderTypeBuffer = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());

        // Calculate text position (above the indicator)
        float textY = (float)pos.y + size + 0.5f;

        // Get text color
        Color textColor = LockOnConfig.getTextColor();
        int colorInt = (textColor.getAlpha() << 24) |
                (textColor.getRed() << 16) |
                (textColor.getGreen() << 8) |
                textColor.getBlue();

        // Render target name
        if (LockOnConfig.showTargetName()) {
            String name = target.getDisplayName().getString();
            float textWidth = mc.font.width(name) * 0.5f;

            mc.font.drawInBatch(name, (float)pos.x - textWidth, textY, colorInt, false,
                    matrixStack.last().pose(), renderTypeBuffer, false, 0, 15728880);
            textY += 10;
        }

        // Render health info
        if (LockOnConfig.showTargetHealth() && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            String healthText = String.format("%.0f/%.0f HP", living.getHealth(), living.getMaxHealth());
            float textWidth = mc.font.width(healthText) * 0.5f;

            mc.font.drawInBatch(healthText, (float)pos.x - textWidth, textY, colorInt, false,
                    matrixStack.last().pose(), renderTypeBuffer, false, 0, 15728880);
            textY += 10;
        }

        // Render distance info
        if (LockOnConfig.showTargetDistance()) {
            float distance = mc.player.distanceTo(target);
            String distanceText;

            if (LockOnConfig.getDistanceUnit() == LockOnConfig.DistanceUnit.METERS) {
                distanceText = String.format("%.1fm", distance);
            } else {
                distanceText = String.format("%.1f blocks", distance);
            }

            float textWidth = mc.font.width(distanceText) * 0.5f;

            mc.font.drawInBatch(distanceText, (float)pos.x - textWidth, textY, colorInt, false,
                    matrixStack.last().pose(), renderTypeBuffer, false, 0, 15728880);
        }

        renderTypeBuffer.endBatch();
        matrixStack.popPose();
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

    /**
     * Get current pulse phase for external use
     */
    public static float getPulsePhase() {
        return pulsePhase;
    }

    /**
     * Check if glow effect should be rendered
     */
    public static boolean shouldRenderGlow() {
        return LockOnConfig.isGlowEnabled();
    }

    /**
     * Render glow effect around indicator (1.16.5 compatible)
     */
    public static void renderGlowEffect(MatrixStack matrixStack, Vector3d pos, float size, Color color) {
        if (!shouldRenderGlow()) return;

        matrixStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        // Create a larger, more transparent version for glow
        float glowSize = size * 1.5f;
        int glowAlpha = Math.max(20, color.getAlpha() / 4);
        Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), glowAlpha);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        // Render glow circle
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS

        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float)(2 * Math.PI * i / segments);
            float angle2 = (float)(2 * Math.PI * (i + 1) / segments);

            float x1 = (float)(pos.x + Math.cos(angle1) * glowSize);
            float y1 = (float)(pos.y + Math.sin(angle1) * glowSize);
            float x2 = (float)(pos.x + Math.cos(angle2) * glowSize);
            float y2 = (float)(pos.y + Math.sin(angle2) * glowSize);

            // Create quad for each segment (fade from center to edge)
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), glowColor.getAlpha())
                    .endVertex();
            buffer.vertex(matrix, x1, y1, (float)pos.z)
                    .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 0)
                    .endVertex();
            buffer.vertex(matrix, x2, y2, (float)pos.z)
                    .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 0)
                    .endVertex();
            buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), glowColor.getAlpha())
                    .endVertex();
        }

        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Render health bar below indicator (1.16.5 compatible)
     */
    public static void renderHealthBar(MatrixStack matrixStack, Vector3d pos, float size,
                                       LivingEntity target, boolean isThirdPerson) {
        if (!LockOnConfig.showHealthBar() || target == null) return;

        matrixStack.pushPose();

        float barWidth = size * 2.0f;
        float barHeight = size * 0.2f;
        float barY = (float)pos.y - size - barHeight - 0.2f;

        float healthPercent = target.getHealth() / target.getMaxHealth();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS

        // Background bar (dark)
        buffer.vertex(matrix, (float)pos.x - barWidth/2, barY, (float)pos.z)
                .color(0, 0, 0, 128).endVertex();
        buffer.vertex(matrix, (float)pos.x + barWidth/2, barY, (float)pos.z)
                .color(0, 0, 0, 128).endVertex();
        buffer.vertex(matrix, (float)pos.x + barWidth/2, barY + barHeight, (float)pos.z)
                .color(0, 0, 0, 128).endVertex();
        buffer.vertex(matrix, (float)pos.x - barWidth/2, barY + barHeight, (float)pos.z)
                .color(0, 0, 0, 128).endVertex();

        // Health bar (colored based on health)
        Color healthColor;
        if (healthPercent > 0.6f) {
            healthColor = Color.GREEN;
        } else if (healthPercent > 0.3f) {
            healthColor = Color.YELLOW;
        } else {
            healthColor = Color.RED;
        }

        float healthWidth = barWidth * healthPercent;
        buffer.vertex(matrix, (float)pos.x - barWidth/2, barY, (float)pos.z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        buffer.vertex(matrix, (float)pos.x - barWidth/2 + healthWidth, barY, (float)pos.z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        buffer.vertex(matrix, (float)pos.x - barWidth/2 + healthWidth, barY + barHeight, (float)pos.z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        buffer.vertex(matrix, (float)pos.x - barWidth/2, barY + barHeight, (float)pos.z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();

        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.popPose();
    }

    /**
     * Main rendering method that combines all effects
     */
    public static void renderCompleteIndicator(MatrixStack matrixStack, Entity target, Vector3d targetPos,
                                               float size, LockOnConfig.IndicatorType type, boolean isThirdPerson) {
        if (target == null || !target.isAlive()) return;

        // Render glow effect first (behind everything)
        if (LockOnConfig.isGlowEnabled()) {
            Color glowColor = calculateIndicatorColor(target,
                    Minecraft.getInstance().player.distanceTo(target), isThirdPerson);
            renderGlowEffect(matrixStack, targetPos, size, glowColor);
        }

        // Render main indicator
        renderLockOnIndicator(matrixStack, target, targetPos, size, type, isThirdPerson);

        // Render health bar if enabled
        if (target instanceof LivingEntity) {
            renderHealthBar(matrixStack, targetPos, size, (LivingEntity)target, isThirdPerson);
        }
    }
}