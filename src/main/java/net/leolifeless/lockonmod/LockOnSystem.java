package net.leolifeless.lockonmod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = LockOnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LockOnSystem {
    private static Entity targetEntity = null;
    private static final float MAX_DISTANCE = 32.0F;  // Maximum distance to lock onto entities
    private static final float SEARCH_RADIUS = 10.0F; // Radius around player to search for entities

    /**
     * Handles the key input events for lock-on functionality
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Check if the lock-on key was just pressed
        if (LockOnKeybinds.lockOnKey.consumeClick()) {
            if (targetEntity == null) {
                // Find a target if none is selected
                findTarget(player);
            } else {
                // Clear target if one is already selected
                targetEntity = null;
            }
        }

        // Check if the cycle target key was just pressed
        if (LockOnKeybinds.cycleTargetKey.consumeClick() && targetEntity != null) {
            cycleTarget(player);
        }
    }

    /**
     * Updates the target entity during client tick events
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) return;

        // Check if target is still valid (not dead, not too far, etc.)
        if (targetEntity != null) {
            if (!targetEntity.isAlive() ||
                    targetEntity.distanceTo(player) > MAX_DISTANCE ||
                    targetEntity.level != player.level) {
                targetEntity = null;
            }
        }
    }

    /**
     * Renders the lock-on indicator during the world rendering
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (targetEntity != null && targetEntity.isAlive()) {
            // Here we would render a marker or indicator on the locked entity
            // This requires more complex rendering code which I'll cover in a separate method
            renderLockOnIndicator(event, targetEntity);
        }
    }

    /**
     * Finds a suitable target entity for lock-on
     */
    private static void findTarget(LocalPlayer player) {
        if (player.level == null) return;

        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookVector = player.getViewVector(1.0F).normalize();

        // Create a bounding box around the player to search for entities
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> nearbyEntities = player.level.getEntities(player, searchBox);

        // Filter and sort entities by relevance (living entities, distance, angle to look vector)
        List<Entity> potentialTargets = new ArrayList<>();

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity.isAlive()) {
                // Only consider living entities
                potentialTargets.add(entity);
            }
        }

        if (!potentialTargets.isEmpty()) {
            // Sort by how closely they align with where the player is looking
            potentialTargets.sort(Comparator.comparingDouble(entity -> {
                Vec3 directionToEntity = entity.position().subtract(eyePosition).normalize();
                // Higher dot product means the entity is more directly in front of the player
                return -directionToEntity.dot(lookVector);
            }));

            // Take the first entity (most aligned with player's look direction)
            targetEntity = potentialTargets.get(0);
        }
    }

    /**
     * Cycles to the next potential target
     */
    private static void cycleTarget(LocalPlayer player) {
        if (player.level == null || targetEntity == null) return;

        // Get all potential targets
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> nearbyEntities = player.level.getEntities(player, searchBox);

        List<Entity> potentialTargets = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity.isAlive()) {
                potentialTargets.add(entity);
            }
        }

        if (potentialTargets.size() <= 1) return;

        // Find current target index
        int currentIndex = potentialTargets.indexOf(targetEntity);
        if (currentIndex == -1) return;

        // Get next target (loop around to the beginning if necessary)
        int nextIndex = (currentIndex + 1) % potentialTargets.size();
        targetEntity = potentialTargets.get(nextIndex);
    }

    /**
     * Renders an indicator on the locked target
     * This is a placeholder - you'll need to implement actual rendering
     */
    private static void renderLockOnIndicator(RenderLevelStageEvent event, Entity targetEntity) {
        // This is where you would render something to indicate the locked-on target
        // For now this is a placeholder - rendering properly requires more complex code

        // Example implementation would:
        // 1. Set up rendering context (matrices, buffers)
        // 2. Calculate screen position for the target entity
        // 3. Render a marker (like a circle or crosshair) at that position

        // A complete implementation would need custom rendering with buffer builders
        // This goes beyond this starter code but would be the next step
    }

    /**
     * Returns the currently targeted entity
     */
    public static Entity getTargetEntity() {
        return targetEntity;
    }
}