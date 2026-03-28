package net.leolifeless.lockonmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Lock-On HUD Renderer — three variants, drag to reposition.
 *
 * Switch variant via config: hudVariant = CLASSIC | MINIMAL | COMPACT
 */
@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnHudRenderer {

    // Saved positions per variant (-1 = use default)
    private static int classicX = -1, classicY = -1;
    private static int minimalX = -1, minimalY = -1;
    private static int compactX = -1, compactY = -1;

    // Drag state
    private static boolean dragging = false;
    private static int dragOffsetX = 0, dragOffsetY = 0;

    // Colors
    private static final int C_BG         = 0xD9000000;
    private static final int C_BG_COMPACT = 0xE0000000;
    private static final int C_ACCENT     = 0xFFE24B4A;
    private static final int C_WHITE      = 0xFFFFFFFF;
    private static final int C_LABEL      = 0x80FFFFFF;
    private static final int C_DIM        = 0x59FFFFFF;
    private static final int C_BAR_BG     = 0x1AFFFFFF;
    private static final int C_BORDER     = 0x1EFFFFFF;

    // =========================================================
    //  RENDER
    // =========================================================

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Entity target = LockOnSystem.getTargetEntity();
        if (target == null || !target.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        LockOnConfig.HudVariant variant = LockOnConfig.getHudVariant();
        int[] pos = resolvePosition(variant, sw, sh);
        int x = pos[0], y = pos[1];

        // Update position while dragging
        if (dragging) {
            double sx = (double) mc.getWindow().getScreenWidth()  / sw;
            double sy = (double) mc.getWindow().getScreenHeight() / sh;
            x = (int)(mc.mouseHandler.xpos() / sx) - dragOffsetX;
            y = (int)(mc.mouseHandler.ypos() / sy) - dragOffsetY;
            x = Math.max(0, Math.min(sw - variantWidth(variant),  x));
            y = Math.max(0, Math.min(sh - variantHeight(variant), y));
            savePosition(variant, x, y);
        }

        PoseStack ps = event.getGuiGraphics().pose();
        Font font = mc.font;

        switch (variant) {
            case CLASSIC -> renderClassic(event, ps, font, target, mc, x, y);
            case MINIMAL -> renderMinimal(event, ps, font, target, mc, x, y);
            case COMPACT -> renderCompact(event, ps, font, target, mc, x, y);
        }
    }

    // =========================================================
    //  MOUSE — drag
    // =========================================================

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton event) {
        if (!LockOnSystem.hasTarget()) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        LockOnConfig.HudVariant variant = LockOnConfig.getHudVariant();
        int[] pos = resolvePosition(variant, sw, sh);

        double sx = (double) mc.getWindow().getScreenWidth()  / sw;
        double sy = (double) mc.getWindow().getScreenHeight() / sh;
        int mx = (int)(mc.mouseHandler.xpos() / sx);
        int my = (int)(mc.mouseHandler.ypos() / sy);

        boolean inside = mx >= pos[0] && mx <= pos[0] + variantWidth(variant)
                && my >= pos[1] && my <= pos[1] + variantHeight(variant);

        if (event.getAction() == GLFW.GLFW_PRESS && inside) {
            dragging = true;
            dragOffsetX = mx - pos[0];
            dragOffsetY = my - pos[1];
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            dragging = false;
        }
    }

    // =========================================================
    //  VARIANT: CLASSIC
    // =========================================================

    private static void renderClassic(RenderGuiEvent.Post event, PoseStack ps,
                                      Font font, Entity target, Minecraft mc,
                                      int x, int y) {
        final int W = 220, H = 68, A = 4, PAD = 10;
        Matrix4f m = ps.last().pose();

        beginBlend();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x, y, W, H, C_BG);
        BufferUploader.drawWithShader(buf.end());

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x,     y,     W, 1, C_BORDER);
        fillRect(buf, m, x,     y+H-1, W, 1, C_BORDER);
        fillRect(buf, m, x,     y,     1, H, C_BORDER);
        fillRect(buf, m, x+W-1, y,     1, H, C_BORDER);
        BufferUploader.drawWithShader(buf.end());

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x, y, A, H, C_ACCENT);
        BufferUploader.drawWithShader(buf.end());

        endBlend();

        int tx = x + A + PAD, ty = y + 8;
        drawStr(event, font, "TARGET", tx, ty, C_LABEL);
        ty += 13;
        drawStr(event, font, truncate(font, target.getDisplayName().getString(), W - A - PAD - 8), tx, ty, C_WHITE);
        ty += 16;

        if (target instanceof LivingEntity living) {
            float hp = living.getHealth(), max = living.getMaxHealth();
            float pct = Math.max(0f, Math.min(1f, hp / max));

            drawStr(event, font, "HEALTH", tx, ty, C_LABEL);
            String ht = (int) hp + " / " + (int) max;
            drawStr(event, font, ht, x + W - font.width(ht) - 6, ty, C_ACCENT);
            ty += 13;

            int bw = W - A - PAD - 8;
            beginBlend();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, tx, ty, bw, 4, C_BAR_BG);
            BufferUploader.drawWithShader(buf.end());
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, tx, ty, Math.round(bw * pct), 4, hpColor(pct));
            BufferUploader.drawWithShader(buf.end());
            endBlend();
            ty += 9;
        }

        if (mc.player != null)
            drawStr(event, font, String.format("%.1fm", mc.player.distanceTo(target)), tx, ty, C_DIM);
    }

    // =========================================================
    //  VARIANT: MINIMAL
    // =========================================================

    private static void renderMinimal(RenderGuiEvent.Post event, PoseStack ps,
                                      Font font, Entity target, Minecraft mc,
                                      int x, int y) {
        final int W = 160, H = 36, PAD = 10;
        Matrix4f m = ps.last().pose();

        beginBlend();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x, y, W, H, 0xBF000000);
        BufferUploader.drawWithShader(buf.end());

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x, y,     W, 1, 0x14FFFFFF);
        fillRect(buf, m, x, y+H-1, W, 1, 0x14FFFFFF);
        BufferUploader.drawWithShader(buf.end());

        endBlend();

        drawStr(event, font, truncate(font, target.getDisplayName().getString(), W - PAD*2).toUpperCase(), x + PAD, y + 11, C_LABEL);

        if (target instanceof LivingEntity living) {
            float hp = living.getHealth(), max = living.getMaxHealth();
            float pct = Math.max(0f, Math.min(1f, hp / max));
            int bw = W - PAD * 2;

            beginBlend();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, x + PAD, y + 16, bw, 3, C_BAR_BG);
            BufferUploader.drawWithShader(buf.end());
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, x + PAD, y + 16, Math.round(bw * pct), 3, hpColor(pct));
            BufferUploader.drawWithShader(buf.end());
            endBlend();

            String hpStr   = (int) hp + " / " + (int) max;
            String distStr = mc.player != null ? String.format("%.1fm", mc.player.distanceTo(target)) : "";
            drawStr(event, font, hpStr,   x + PAD, y + 28, C_DIM);
            drawStr(event, font, distStr, x + W - font.width(distStr) - PAD, y + 28, C_DIM);
        }
    }

    // =========================================================
    //  VARIANT: COMPACT
    // =========================================================

    private static void renderCompact(RenderGuiEvent.Post event, PoseStack ps,
                                      Font font, Entity target, Minecraft mc,
                                      int x, int y) {
        final int W = 110, H = 52, PAD = 8;
        Matrix4f m = ps.last().pose();

        beginBlend();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x, y, W, H, C_BG_COMPACT);
        BufferUploader.drawWithShader(buf.end());

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(buf, m, x,     y,     W, 1, C_ACCENT);
        fillRect(buf, m, x,     y+H-1, W, 1, C_ACCENT);
        fillRect(buf, m, x,     y,     1, H, C_ACCENT);
        fillRect(buf, m, x+W-1, y,     1, H, C_ACCENT);
        BufferUploader.drawWithShader(buf.end());

        endBlend();

        drawStr(event, font, truncate(font, target.getDisplayName().getString(), W - PAD*2), x + PAD, y + 14, C_ACCENT);

        if (target instanceof LivingEntity living) {
            float hp = living.getHealth(), max = living.getMaxHealth();
            float pct = Math.max(0f, Math.min(1f, hp / max));
            int bw = W - PAD * 2;

            beginBlend();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, x + PAD, y + 20, bw, 5, C_BAR_BG);
            BufferUploader.drawWithShader(buf.end());
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(buf, m, x + PAD, y + 20, Math.round(bw * pct), 5, hpColor(pct));
            BufferUploader.drawWithShader(buf.end());
            endBlend();

            drawStr(event, font, (int) hp + " / " + (int) max, x + PAD, y + 34, C_LABEL);
            if (mc.player != null)
                drawStr(event, font, String.format("%.1fm", mc.player.distanceTo(target)), x + PAD, y + 45, C_DIM);
        }
    }

    // =========================================================
    //  HELPERS
    // =========================================================

    private static int variantWidth(LockOnConfig.HudVariant v) {
        return switch (v) {
            case CLASSIC -> 220;
            case MINIMAL -> 160;
            case COMPACT -> 110;
        };
    }

    private static int variantHeight(LockOnConfig.HudVariant v) {
        return switch (v) {
            case CLASSIC -> 68;
            case MINIMAL -> 36;
            case COMPACT -> 52;
        };
    }

    private static int[] resolvePosition(LockOnConfig.HudVariant v, int sw, int sh) {
        return switch (v) {
            case CLASSIC -> classicX < 0 ? new int[]{(sw - 220) / 2, 16} : new int[]{classicX, classicY};
            case MINIMAL -> minimalX < 0 ? new int[]{(sw - 160) / 2, 16} : new int[]{minimalX, minimalY};
            case COMPACT -> compactX < 0 ? new int[]{sw - 110 - 16, 16}  : new int[]{compactX, compactY};
        };
    }

    private static void savePosition(LockOnConfig.HudVariant v, int x, int y) {
        switch (v) {
            case CLASSIC -> { classicX = x; classicY = y; }
            case MINIMAL -> { minimalX = x; minimalY = y; }
            case COMPACT -> { compactX = x; compactY = y; }
        }
    }

    private static int hpColor(float pct) {
        if (pct > 0.5f) {
            float t = (pct - 0.5f) * 2f;
            return 0xFF000000 | (Math.round(255 * (1f - t)) << 16) | (180 << 8);
        }
        return 0xFF000000 | (226 << 16) | (Math.round(180 * pct * 2f) << 8);
    }

    private static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "...") > maxWidth)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private static void drawStr(RenderGuiEvent.Post event, Font font,
                                String text, int x, int y, int color) {
        event.getGuiGraphics().drawString(font, text, x, y, color, false);
    }

    private static void beginBlend() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void endBlend() { RenderSystem.disableBlend(); }

    private static void fillRect(BufferBuilder buf, Matrix4f m,
                                 int x, int y, int w, int h, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        buf.vertex(m, x,     y,     0).color(r, g, b, a).endVertex();
        buf.vertex(m, x,     y + h, 0).color(r, g, b, a).endVertex();
        buf.vertex(m, x + w, y + h, 0).color(r, g, b, a).endVertex();
        buf.vertex(m, x + w, y,     0).color(r, g, b, a).endVertex();
    }
}