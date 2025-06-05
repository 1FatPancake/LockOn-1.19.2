package net.leolifeless.lockonmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.platform.NativeImage;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages custom indicator textures from multiple sources (1.20.1 compatible)
 */
public class CustomIndicatorManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Built-in indicators (shipped with the mod)
    private static final Map<String, ResourceLocation> BUILT_IN_INDICATORS = new HashMap<>();

    // Discovered custom indicators
    private static final Map<String, ResourceLocation> CUSTOM_INDICATORS = new HashMap<>();
    private static final Map<String, File> USER_INDICATORS = new HashMap<>();

    // Current selected indicator
    private static String currentIndicatorName = "default";

    // Paths for user custom indicators
    private static final String CUSTOM_INDICATORS_FOLDER = "config/lockonmod/custom_indicators";
    private static final String README_FILE = "README.txt";

    static {
        // Initialize built-in indicators using the helper method from LockOnMod
        BUILT_IN_INDICATORS.put("default", LockOnMod.location("textures/gui/custom_indicator.png"));
        BUILT_IN_INDICATORS.put("crosshair_alt", LockOnMod.location("textures/gui/crosshair_alt.png"));
        BUILT_IN_INDICATORS.put("target_circle", LockOnMod.location("textures/gui/target_circle.png"));
        BUILT_IN_INDICATORS.put("reticle", LockOnMod.location("textures/gui/reticle.png"));
    }

    /**
     * Initialize the custom indicator system
     */
    public static void initialize() {
        createCustomIndicatorsFolder();
        loadUserIndicators();
        discoverResourcePackIndicators();

        LOGGER.info("Custom Indicator Manager initialized with {} total indicators", getTotalIndicatorCount());
    }

    /**
     * Creates the custom indicators folder and README
     */
    private static void createCustomIndicatorsFolder() {
        try {
            Path customPath = Paths.get(CUSTOM_INDICATORS_FOLDER);
            if (!Files.exists(customPath)) {
                Files.createDirectories(customPath);
                LOGGER.info("Created custom indicators folder: {}", customPath.toAbsolutePath());
            }

            // Create README file with instructions
            Path readmePath = customPath.resolve(README_FILE);
            if (!Files.exists(readmePath)) {
                createReadmeFile(readmePath);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to create custom indicators folder", e);
        }
    }

    /**
     * Creates a helpful README file for users
     */
    private static void createReadmeFile(Path readmePath) throws IOException {
        String readme = """
            TargetLock Mod - Custom Indicators
            ================================
            
            Place your custom indicator images in this folder to use them in the mod.
            
            REQUIREMENTS:
            - File format: PNG only
            - Recommended size: 64x64 pixels (square images work best)
            - Use transparency (alpha channel) for best results
            - File names should not contain spaces or special characters
            
            SUPPORTED SIZES:
            - 16x16, 32x32, 64x64, 128x128, 256x256
            - Non-square images will be stretched to fit
            
            HOW TO USE:
            1. Place your PNG files in this folder
            2. Restart Minecraft or reload resource packs (F3+T)
            3. In the mod's config, cycle through indicator types to find your custom ones
            4. Or use the keybind to cycle through available indicators in-game
            
            EXAMPLES:
            - my_crosshair.png
            - sniper_scope.png
            - anime_target.png
            
            NOTE: The indicator color from the mod's config will be applied to your image.
            Use white/light colors in your image for best color tinting results.
            
            TROUBLESHOOTING:
            - If your indicator doesn't appear, check the game logs for errors
            - Make sure the file is a valid PNG image
            - Restart the game after adding new indicators
            """;

        Files.write(readmePath, readme.getBytes());
        LOGGER.info("Created README file for custom indicators");
    }

    /**
     * Loads user-provided indicators from the config folder
     */
    private static void loadUserIndicators() {
        USER_INDICATORS.clear();

        try {
            Path customPath = Paths.get(CUSTOM_INDICATORS_FOLDER);
            if (!Files.exists(customPath)) return;

            try (Stream<Path> files = Files.list(customPath)) {
                files.filter(path -> path.toString().toLowerCase().endsWith(".png"))
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            File file = path.toFile();
                            String name = getFileNameWithoutExtension(file.getName());

                            // Validate the image
                            if (isValidIndicatorImage(file)) {
                                USER_INDICATORS.put(name, file);
                                LOGGER.debug("Loaded user indicator: {}", name);
                            } else {
                                LOGGER.warn("Skipped invalid indicator image: {}", file.getName());
                            }
                        });
            }

            LOGGER.info("Loaded {} user indicators from {}", USER_INDICATORS.size(), customPath);

        } catch (IOException e) {
            LOGGER.error("Failed to load user indicators", e);
        }
    }

    /**
     * Discovers indicators provided by resource packs
     */
    private static void discoverResourcePackIndicators() {
        CUSTOM_INDICATORS.clear();

        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) return;

            ResourceManager resourceManager = minecraft.getResourceManager();

            // Look for custom indicators in resource packs
            String basePath = "textures/gui/indicators/";

            // Check for specific known patterns
            String[] possibleNames = {
                    "custom_1", "custom_2", "custom_3", "custom_4", "custom_5",
                    "user_crosshair", "user_reticle", "user_target", "user_scope"
            };

            for (String name : possibleNames) {
                ResourceLocation location = LockOnMod.location(basePath + name + ".png");
                Optional<Resource> resource = resourceManager.getResource(location);

                if (resource.isPresent()) {
                    CUSTOM_INDICATORS.put(name, location);
                    LOGGER.debug("Found resource pack indicator: {}", name);
                }
            }

            LOGGER.info("Discovered {} indicators from resource packs", CUSTOM_INDICATORS.size());

        } catch (Exception e) {
            LOGGER.error("Failed to discover resource pack indicators", e);
        }
    }

    /**
     * Validates that an image file is suitable for use as an indicator
     */
    private static boolean isValidIndicatorImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) return false;

            int width = image.getWidth();
            int height = image.getHeight();

            // Check if it's a reasonable size
            if (width < 8 || height < 8 || width > 512 || height > 512) {
                LOGGER.warn("Image {} has invalid dimensions: {}x{}", file.getName(), width, height);
                return false;
            }

            // Warn about non-square images
            if (width != height) {
                LOGGER.warn("Image {} is not square ({}x{}), it may appear stretched", file.getName(), width, height);
            }

            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to validate image: {}", file.getName(), e);
            return false;
        }
    }

    /**
     * Gets all available indicator names
     */
    public static List<String> getAllIndicatorNames() {
        List<String> names = new ArrayList<>();
        names.addAll(BUILT_IN_INDICATORS.keySet());
        names.addAll(CUSTOM_INDICATORS.keySet());
        names.addAll(USER_INDICATORS.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Gets the ResourceLocation for a given indicator name
     */
    public static ResourceLocation getIndicatorTexture(String name) {
        // Check built-in first
        if (BUILT_IN_INDICATORS.containsKey(name)) {
            return BUILT_IN_INDICATORS.get(name);
        }

        // Check resource pack indicators
        if (CUSTOM_INDICATORS.containsKey(name)) {
            return CUSTOM_INDICATORS.get(name);
        }

        // Check user indicators (these need special handling)
        if (USER_INDICATORS.containsKey(name)) {
            // For user files, we need to register them as dynamic textures
            return registerUserTexture(name, USER_INDICATORS.get(name));
        }

        // Fall back to default
        return BUILT_IN_INDICATORS.get("default");
    }

    /**
     * Registers a user texture file as a dynamic texture (1.20.1 compatible)
     */
    private static ResourceLocation registerUserTexture(String name, File file) {
        // Create a unique resource location for this user texture
        ResourceLocation location = LockOnMod.location("dynamic/user_" + name);

        try {
            // Load the image
            BufferedImage bufferedImage = ImageIO.read(file);

            // Convert BufferedImage to NativeImage (1.20.1 requirement)
            NativeImage nativeImage = convertBufferedImageToNativeImage(bufferedImage);

            // Register it with Minecraft's texture manager
            Minecraft.getInstance().getTextureManager().register(location,
                    new net.minecraft.client.renderer.texture.DynamicTexture(nativeImage));

            LOGGER.debug("Registered user texture: {} -> {}", name, location);
            return location;

        } catch (IOException e) {
            LOGGER.error("Failed to register user texture: {}", file.getName(), e);
            return BUILT_IN_INDICATORS.get("default");
        }
    }

    /**
     * Converts BufferedImage to NativeImage for 1.20.1 compatibility
     */
    private static NativeImage convertBufferedImageToNativeImage(BufferedImage bufferedImage) throws IOException {
        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        // Create NativeImage from byte array
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        return NativeImage.read(bais);
    }

    /**
     * Cycles to the next available indicator
     */
    public static String cycleToNextIndicator() {
        List<String> names = getAllIndicatorNames();
        if (names.isEmpty()) return "default";

        int currentIndex = names.indexOf(currentIndicatorName);
        int nextIndex = (currentIndex + 1) % names.size();

        currentIndicatorName = names.get(nextIndex);
        return currentIndicatorName;
    }

    /**
     * Cycles to the previous available indicator
     */
    public static String cycleToPreviousIndicator() {
        List<String> names = getAllIndicatorNames();
        if (names.isEmpty()) return "default";

        int currentIndex = names.indexOf(currentIndicatorName);
        int prevIndex = (currentIndex - 1 + names.size()) % names.size();

        currentIndicatorName = names.get(prevIndex);
        return currentIndicatorName;
    }

    /**
     * Sets the current indicator by name
     */
    public static void setCurrentIndicator(String name) {
        if (getAllIndicatorNames().contains(name)) {
            currentIndicatorName = name;
        } else {
            LOGGER.warn("Unknown indicator name: {}", name);
        }
    }

    /**
     * Gets the current indicator name
     */
    public static String getCurrentIndicatorName() {
        return currentIndicatorName;
    }

    /**
     * Gets the current indicator texture
     */
    public static ResourceLocation getCurrentIndicatorTexture() {
        return getIndicatorTexture(currentIndicatorName);
    }

    /**
     * Refreshes the indicator lists (call when resource packs change)
     */
    public static void refresh() {
        loadUserIndicators();
        discoverResourcePackIndicators();

        // Ensure current indicator is still valid
        if (!getAllIndicatorNames().contains(currentIndicatorName)) {
            currentIndicatorName = "default";
        }

        LOGGER.info("Refreshed indicators, now have {} total", getTotalIndicatorCount());
    }

    /**
     * Gets the total number of available indicators
     */
    public static int getTotalIndicatorCount() {
        return BUILT_IN_INDICATORS.size() + CUSTOM_INDICATORS.size() + USER_INDICATORS.size();
    }

    /**
     * Gets filename without extension
     */
    private static String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
    }

    /**
     * Gets indicator info for display
     */
    public static String getIndicatorInfo(String name) {
        if (BUILT_IN_INDICATORS.containsKey(name)) {
            return "Built-in: " + name;
        } else if (CUSTOM_INDICATORS.containsKey(name)) {
            return "Resource Pack: " + name;
        } else if (USER_INDICATORS.containsKey(name)) {
            return "User: " + name;
        }
        return "Unknown: " + name;
    }
}