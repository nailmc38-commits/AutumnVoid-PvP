package com.survivalutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public final class SurvivalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("survival-utils.json");
    private final EnumMap<Feature, Boolean> enabled = new EnumMap<>(Feature.class);

    public SurvivalConfig() {
        for (Feature feature : Feature.values()) {
            enabled.put(feature, feature.defaultEnabled);
        }
    }

    public boolean isEnabled(Feature feature) {
        return enabled.getOrDefault(feature, feature.defaultEnabled);
    }

    public void toggle(Feature feature) {
        enabled.put(feature, !isEnabled(feature));
        save();
    }

    public void set(Feature feature, boolean value) {
        enabled.put(feature, value);
        save();
    }

    public int enabledCount() {
        int count = 0;
        for (boolean value : enabled.values()) if (value) count++;
        return count;
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
                if (root.has(feature.name())) {
                    enabled.put(feature, root.get(feature.name()).getAsBoolean());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<Feature, Boolean> entry : enabled.entrySet()) {
            root.addProperty(entry.getKey().name(), entry.getValue());
        }
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }
}
