package com.curvebreak.progression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.curvebreak.CurvebreakMod;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class CurvePlayerState {
    private CurvePlayerState() {}

    private static final AttachmentType<String> SELECTED_DIMENSION = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, "selected_dimension"),
        builder -> builder.initializer(() -> "haven").persistent(Codec.STRING).copyOnDeath()
    );

    private static final AttachmentType<Integer> CREDITS = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, "credits"),
        builder -> builder.initializer(() -> 0).persistent(Codec.INT).copyOnDeath()
    );

    private static final AttachmentType<String> VISITED = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, "visited_dimensions"),
        builder -> builder.initializer(() -> "").persistent(Codec.STRING).copyOnDeath()
    );

    public static String selected(Player player) {
        return player.getAttachedOrElse(SELECTED_DIMENSION, "haven");
    }

    public static void selected(Player player, String dimension) {
        player.setAttached(SELECTED_DIMENSION, dimension);
    }

    public static int credits(Player player) {
        return player.getAttachedOrElse(CREDITS, 0);
    }

    public static void addCredits(Player player, int amount) {
        player.setAttached(CREDITS, credits(player) + Math.max(0, amount));
    }

    public static boolean spendCredits(Player player, int amount) {
        if (amount < 0 || credits(player) < amount) return false;
        player.setAttached(CREDITS, credits(player) - amount);
        return true;
    }

    public static boolean markVisited(Player player, String dimension) {
        String raw = player.getAttachedOrElse(VISITED, "");
        Set<String> visited = new HashSet<>();
        if (!raw.isBlank()) visited.addAll(Arrays.asList(raw.split(",")));
        if (!visited.add(dimension)) return false;
        player.setAttached(VISITED, String.join(",", visited));
        return true;
    }

    public static int visitedCount(Player player) {
        String raw = player.getAttachedOrElse(VISITED, "");
        if (raw.isBlank()) return 0;
        return (int) Arrays.stream(raw.split(",")).filter(s -> !s.isBlank()).count();
    }
}
