package com.survivalutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public final class SurvivalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("survival-utils.json");

    private final EnumMap<Feature, DisplayMode> modes = new EnumMap<>(Feature.class);

    private String hudSide = "LEFT";
    private int maxHudLines = 10;
    private boolean voiceAutoStart = true;
    private boolean voiceChip = true;
    private double voiceConfidence = 0.55;

    public SurvivalConfig() {
        for (Feature feature : Feature.values()) {
            modes.put(feature, feature.defaultEnabled ? DisplayMode.HUD : DisplayMode.OFF);
        }

        // Voice-friendly defaults: useful on demand without cluttering the screen.
        setDefaultVoice(Feature.FPS_HUD);
        setDefaultVoice(Feature.PING_HUD);
        setDefaultVoice(Feature.SPEED_HUD);
        setDefaultVoice(Feature.WEATHER_HUD);
        setDefaultVoice(Feature.TARGET_BLOCK_HUD);
        setDefaultVoice(Feature.HELD_ITEM_COUNT);
        setDefaultVoice(Feature.DIMENSION_HUD);
        setDefaultVoice(Feature.INVENTORY_SPACE);
        setDefaultVoice(Feature.TORCH_COUNT);
        setDefaultVoice(Feature.ARROW_COUNT);
        setDefaultVoice(Feature.ROCKET_COUNT);
        setDefaultVoice(Feature.PEARL_COUNT);
        setDefaultVoice(Feature.LIGHT_WARNING);
        setDefaultVoice(Feature.TOOL_RECOMMENDATION);
        setDefaultVoice(Feature.HOSTILES_NEARBY);
        setDefaultVoice(Feature.MEMORY_USAGE);
        setDefaultVoice(Feature.GAMEMODE_HUD);
    }

    private void setDefaultVoice(Feature feature) {
        if (!feature.defaultEnabled && supportsVoice(feature)) {
            modes.put(feature, DisplayMode.VOICE);
        }
    }

    public DisplayMode mode(Feature feature) {
        return modes.getOrDefault(feature, feature.defaultEnabled ? DisplayMode.HUD : DisplayMode.OFF);
    }

    public boolean isEnabled(Feature feature) {
        return mode(feature).active();
    }

    public boolean showHud(Feature feature) {
        return mode(feature).hud();
    }

    public boolean voiceEnabled(Feature feature) {
        return supportsVoice(feature) && mode(feature).voice();
    }

    public void cycleMode(Feature feature) {
        modes.put(feature, mode(feature).next(supportsVoice(feature)));
        save();
    }

    public void setMode(Feature feature, DisplayMode mode) {
        if (!supportsVoice(feature) && (mode == DisplayMode.VOICE || mode == DisplayMode.BOTH)) {
            mode = DisplayMode.HUD;
        }
        modes.put(feature, mode);
        save();
    }

    // Backwards-compatible helpers used by older code paths.
    public void toggle(Feature feature) {
        cycleMode(feature);
    }

    public void set(Feature feature, boolean value) {
        modes.put(feature, value ? DisplayMode.HUD : DisplayMode.OFF);
        save();
    }

    public int enabledCount() {
        int count = 0;
        for (DisplayMode mode : modes.values()) if (mode.active()) count++;
        return count;
    }

    public String hudSide() {
        return hudSide;
    }

    public void toggleHudSide() {
        hudSide = hudSide.equals("RIGHT") ? "LEFT" : "RIGHT";
        save();
    }

    public int maxHudLines() {
        return maxHudLines;
    }

    public void cycleMaxHudLines() {
        maxHudLines = switch (maxHudLines) {
            case 6 -> 10;
            case 10 -> 14;
            case 14 -> 18;
            default -> 6;
        };
        save();
    }

    public boolean voiceAutoStart() {
        return voiceAutoStart;
    }

    public void toggleVoiceAutoStart() {
        voiceAutoStart = !voiceAutoStart;
        save();
    }

    public boolean voiceChip() {
        return voiceChip;
    }

    public void toggleVoiceChip() {
        voiceChip = !voiceChip;
        save();
    }

    public double voiceConfidence() {
        return voiceConfidence;
    }

    public void cycleVoiceConfidence() {
        if (voiceConfidence < 0.54) voiceConfidence = 0.55;
        else if (voiceConfidence < 0.64) voiceConfidence = 0.65;
        else if (voiceConfidence < 0.74) voiceConfidence = 0.75;
        else voiceConfidence = 0.45;
        save();
    }

    public static boolean supportsVoice(Feature feature) {
        return switch (feature) {
            case DAY_NIGHT_HUD, COORDS_HUD, BIOME_HUD, ARMOR_HUD, EFFECT_TIMERS,
                 FOOD_HUD, XP_HUD, FPS_HUD, PING_HUD, SPEED_HUD, DIRECTION_HUD,
                 WEATHER_HUD, CHUNK_HUD, TARGET_BLOCK_HUD, HELD_ITEM_COUNT,
                 DIMENSION_HUD, SESSION_TIMER,
                 DURABILITY_WARNINGS, INVENTORY_SPACE, FOOD_COUNT, TORCH_COUNT,
                 ARROW_COUNT, ROCKET_COUNT, PEARL_COUNT, BED_ALERT, WATER_BUCKET_ALERT,
                 PORTAL_MEMORY, JOURNEY_STATS, ALTITUDE_HINT, NETHER_COORD_CONVERTER,
                 LIGHT_WARNING, WORLD_DAY_COUNTER,
                 TOOL_RECOMMENDATION, PICKAXE_DURABILITY, ORE_SESSION_TRACKER,
                 DIAMOND_Y_HINT, ANCIENT_DEBRIS_Y_HINT, INVENTORY_ORE_SUMMARY,
                 CROP_READY_COUNT, BONE_MEAL_COUNT, SEED_COUNT, HOE_DURABILITY,
                 VILLAGER_HELPER, EMERALD_COUNT,
                 BLOCK_COUNT, PALETTE_COUNTS, SCAFFOLD_COUNT, BUILD_HEIGHT, BUILD_LIGHT,
                 LOW_HEALTH_ALERT, LOW_HUNGER_ALERT, SHIELD_DURABILITY, BOW_DURABILITY,
                 ELYTRA_DURABILITY, HOSTILES_NEARBY, CREEPER_ALERT, SKELETON_ALERT,
                 FIRE_ALERT, DROWNING_ALERT, FREEZE_ALERT,
                 MEMORY_USAGE, CLOCK_HUD, SPRINT_STATE, GAMEMODE_HUD,
                 DURABILITY_PERCENTAGES, POSITION_COPY_HINT, DEBUG_MINI -> true;
            default -> false;
        };
    }

    public void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try {
            JsonObject root = GSON.fromJson(Files.readString(PATH), JsonObject.class);
            if (root == null) return;

            for (Feature feature : Feature.values()) {
                if (!root.has(feature.name())) continue;
                JsonElement element = root.get(feature.name());

                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                    modes.put(feature, element.getAsBoolean() ? DisplayMode.HUD : DisplayMode.OFF);
                } else if (element.isJsonPrimitive()) {
                    try {
                        DisplayMode loaded = DisplayMode.valueOf(element.getAsString());
                        if (!supportsVoice(feature) && (loaded == DisplayMode.VOICE || loaded == DisplayMode.BOTH)) {
                            loaded = DisplayMode.HUD;
                        }
                        modes.put(feature, loaded);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (root.has("_hudSide")) hudSide = root.get("_hudSide").getAsString();
            if (root.has("_maxHudLines")) maxHudLines = Math.max(6, Math.min(18, root.get("_maxHudLines").getAsInt()));
            if (root.has("_voiceAutoStart")) voiceAutoStart = root.get("_voiceAutoStart").getAsBoolean();
            if (root.has("_voiceChip")) voiceChip = root.get("_voiceChip").getAsBoolean();
            if (root.has("_voiceConfidence")) voiceConfidence = Math.max(0.40, Math.min(0.85, root.get("_voiceConfidence").getAsDouble()));
        } catch (Exception ignored) {
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<Feature, DisplayMode> entry : modes.entrySet()) {
            root.addProperty(entry.getKey().name(), entry.getValue().name());
        }

        root.addProperty("_hudSide", hudSide);
        root.addProperty("_maxHudLines", maxHudLines);
        root.addProperty("_voiceAutoStart", voiceAutoStart);
        root.addProperty("_voiceChip", voiceChip);
        root.addProperty("_voiceConfidence", voiceConfidence);

        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }
}
