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
    private static final float INDICATOR_SIZE = 0.5F; // Size of the lock-on indicator
    private static final Color INDICATOR_COLOR = new Color(255, 0, 0, 200); // Red with 80% opacity

    /**
     * Renders the lock-on indicator around the target entity
     */
    public static void renderLockOnIndicator(RenderLevelStageEvent event, Entity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || target == null) return;

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
        // Uses the lookAt method to orient toward the camera
        minecraft.gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180f));

        // Set up render system
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Draw a circle (or square) around the target
        drawLockOnCircle(poseStack, INDICATOR_SIZE, INDICATOR_COLOR);

        // Clean up rendering
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Draws a circular lock-on indicator
     */
    private static void drawLockOnCircle(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Start building
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Add center vertex
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();

        // Add vertices to form a circle
        int segments = 36; // Number of segments to make the circle smooth
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * size;
            float y = (float) Math.sin(angle) * size;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                    .endVertex();
        }

        // End building and draw
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Draw outline (optional)
        drawLockOnCircleOutline(poseStack, size, color);
    }

    /**
     * Draws the outline of the lock-on circle
     */
    private static void drawLockOnCircleOutline(PoseStack poseStack, float size, Color color) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Slightly larger outline for better visibility
        float outlineSize = size * 1.1F;

        // Start building
        bufferBuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Add vertices to form a circle outline
        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * outlineSize;
            float y = (float) Math.sin(angle) * outlineSize;

            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(255, 255, 255, 255) // White outline
                    .endVertex();
        }

        // End building and draw
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}