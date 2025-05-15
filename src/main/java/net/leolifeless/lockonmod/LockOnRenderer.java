package net.leolifeless.lockonmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.awt.*;

public class LockOnRenderer {
    // Animation variables
    private static long lastTime = 0;
    private static float pulseSize = 0.0F;

    /**
     * Renders the lock-on indicator around the target entity
     */
    public static void renderLockOnIndicator(RenderLevelStageEvent event, Entity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || target == null) return;

        // Update pulse animation
        updatePulseAnimation();

        // Get the camera position
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        // Get position of the entity (center of their hitbox)
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

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
        minecraft.gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180f));

        // Set up render system
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Draw the lock-on indicator with pulse effect
        float indicatorSize = LockOnConfig.getIndicatorSize();
        float animatedSize = indicatorSize + pulseSize;
        drawLockOnCircle(poseStack, animatedSize, LockOnConfig.getIndicatorColor());

        // Clean up rendering
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Updates the pulse animation effect
     */
    private static void updatePulseAnimation() {
        long currentTime = System.currentTimeMillis();

        // First time initialization
        if (lastTime == 0) {
            lastTime = currentTime;
        }

        // Calculate pulse effect based on sine wave
        float pulseSpeed = LockOnConfig.getPulseSpeed();
        float pulseAmplitude = LockOnConfig.getPulseAmplitude();
        float time = currentTime / 1000.0F * pulseSpeed;
        pulseSize = (float) Math.sin(time) * pulseAmplitude;

        lastTime = currentTime;
    }

    /**
     * Draws a circular lock-on indicator
     */
    private static void drawLockOnCircle(PoseStack poseStack, float size, Color color) {
        // First draw the filled circle
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Start building the filled circle
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Add center vertex with lower alpha for a gradient effect
        Color centerColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 3);
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getAlpha())
                .endVertex();

        // Add vertices to form a circle
        int segments = 36; // Number of segments for the circle
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * size;
            float y = (float) Math.sin(angle) * size;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        // End building and draw the filled circle
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Draw the outline
        drawLockOnCircleOutline(poseStack, size, LockOnConfig.getOutlineColor());
    }

    /**
     * Draws the outline of the lock-on circle
     */
    private static void drawLockOnCircleOutline(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Start building the outline
        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Add vertices to form a circle outline
        int segments = 36; // More segments for smoother outline
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * size;
            float y = (float) Math.sin(angle) * size;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        // End building and draw the outline
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Draw a second, slightly larger outline for better visibility
        float outerSize = size * 1.1F;
        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * outerSize;
            float y = (float) Math.sin(angle) * outerSize;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 2)
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}