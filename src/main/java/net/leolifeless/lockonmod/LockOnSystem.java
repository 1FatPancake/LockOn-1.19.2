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

    // Interpolation settings for smooth camera movement
    private static final float ROTATION_SPEED = 0.25F; // Lower value = smoother but slower camera
    private static final float MIN_ROTATION_SPEED = 0.3F; // Minimum rotation speed
    private static final float DISTANCE_WEIGHT = 0.5F; // How much distance affects rotation speed

    // Previous rotation values for interpolation
    private static float prevYaw = 0F;
    private static float prevPitch = 0F;
    private static boolean wasLocked = false;

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
                wasLocked = false;
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
                // Try to find a new target automatically when current one becomes invalid
                findTarget(player);

                // If no new target found, clear lock-on state
                if (targetEntity == null) {
                    wasLocked = false;
                }
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
            // Use the renderer to show the indicator
            LockOnRenderer.renderLockOnIndicator(event, targetEntity);
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
                double dotProduct = directionToEntity.dot(lookVector);

                // Consider distance as a secondary factor when targets are in similar direction
                double distance = entity.distanceTo(player);
                double weight = 0.8; // Weight for direction vs distance

                // Combined score: Higher is better (closer to look direction and closer to player)
                return -(dotProduct * weight + (1 - weight) * (1 - distance/MAX_DISTANCE));
            }));

            // Take the first entity (most aligned with player's look direction)
            targetEntity = potentialTargets.get(0);
            wasLocked = true;
        }
    }

    /**
     * Follows the target entity with smooth camera movement
     */
    @SubscribeEvent
    public static void onCameraSetup(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) return;

        // Only update camera if we have a target
        if (targetEntity != null && targetEntity.isAlive() && player.isAlive()) {
            // Get positions
            Vec3 playerPos = player.getEyePosition();

            // Target position with predictive targeting based on entity velocity
            Vec3 targetPos = targetEntity.position()
                    .add(0, targetEntity.getBbHeight() * 0.5, 0)
                    .add(targetEntity.getDeltaMovement().scale(0.5)); // Predict movement

            // Calculate direction vector from player to target
            Vec3 directionVec = targetPos.subtract(playerPos).normalize();

            // Convert direction to rotation (yaw and pitch)
            double horizontalDistance = Math.sqrt(directionVec.x * directionVec.x + directionVec.z * directionVec.z);
            float targetYaw = (float) (Math.atan2(directionVec.z, directionVec.x) * 180.0 / Math.PI) - 90.0F;
            float targetPitch = (float) -(Math.atan2(directionVec.y, horizontalDistance) * 180.0 / Math.PI);

            // Get current rotations
            float currentYaw = player.getYRot();
            float currentPitch = player.getXRot();

            // Calculate the shortest path for yaw rotation
            float yawDiff = targetYaw - currentYaw;
            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;

            // Initialize previous values if first frame of lock-on
            if (!wasLocked) {
                prevYaw = currentYaw;
                prevPitch = currentPitch;
                wasLocked = true;
            }

            // Adaptive rotation speed based on angle difference and distance
            float distance = player.distanceTo(targetEntity);
            float adaptiveSpeed = ROTATION_SPEED * (1 + (yawDiff * yawDiff / 900)) *
                    (1 + DISTANCE_WEIGHT * (distance / MAX_DISTANCE));

            // Ensure minimum rotation speed for responsiveness
            adaptiveSpeed = Math.max(MIN_ROTATION_SPEED, Math.min(adaptiveSpeed, 0.3F));

            // Apply double smoothing for more natural camera movement
            float smoothYaw = interpolateRotation(prevYaw, currentYaw + yawDiff * adaptiveSpeed, 0.7F);
            float smoothPitch = interpolateRotation(prevPitch, currentPitch + (targetPitch - currentPitch) * adaptiveSpeed, 0.7F);

            // Update previous values for next frame
            prevYaw = smoothYaw;
            prevPitch = smoothPitch;

            // Apply the rotations to the player
            player.setYRot(smoothYaw);
            player.setXRot(smoothPitch);
        }
    }

    /**
     * Smoothly interpolates between two angles
     */
    private static float interpolateRotation(float prev, float current, float factor) {
        float diff = current - prev;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;

        return prev + diff * factor;
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

        // Reset smoothing values when switching targets
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            prevYaw = localPlayer.getYRot();
            prevPitch = localPlayer.getXRot();
        }
    }

    /**
     * Returns the currently targeted entity
     */
    public static Entity getTargetEntity() {
        return targetEntity;
    }
}