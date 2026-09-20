package com.survivalutils;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public final class SurvivalUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "survivalutils";
    public static final SurvivalConfig CONFIG = new SurvivalConfig();

    public static long sessionStartMillis;
    public static double journeyDistance;
    public static double lastX;
    public static double lastZ;
    public static boolean hadPlayer;
    public static BlockPos lastDeath;
    public static BlockPos lastPortal;
    public static int tickCounter;
    public static int worldReadyTicks;
    public static boolean runtimeFaulted;

    private static KeyMapping menuKey;
    private static KeyMapping voiceKey;

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        VoiceCommands.initialize();
        sessionStartMillis = System.currentTimeMillis();

        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "controls")
        );
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.survivalutils.menu",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            category
        ));
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.survivalutils.voice",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(SurvivalUtilsClient::tick);
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "main_hud"),
            SurvivalHud::render
        );
    }

    private static void tick(Minecraft client) {
        try {
            while (menuKey != null && menuKey.consumeClick()) {
                client.setScreen(new SurvivalScreen(client.screen));
            }
            while (voiceKey != null && voiceKey.consumeClick()) {
                if (CONFIG.isEnabled(Feature.VOICE_COMMANDS)) {
                    VoiceCommands.toggle();
                    if (client.player != null) {
                        client.player.displayClientMessage(Component.literal(
                            VoiceCommands.isRunning() ? "§bVISOR §8// §aVoice commands ON" : "§bVISOR §8// §cVoice commands OFF"
                        ), true);
                    }
                }
            }

            if (client.player == null || client.level == null) {
                hadPlayer = false;
                worldReadyTicks = 0;
                return;
            }

            worldReadyTicks++;
            if (worldReadyTicks < 40) return;

            tickCounter++;
            if (CONFIG.isEnabled(Feature.VOICE_COMMANDS) && worldReadyTicks == 40 && !VoiceCommands.isRunning()) {
                VoiceCommands.start();
            }
            var player = client.player;

        if (!hadPlayer) {
            lastX = player.getX();
            lastZ = player.getZ();
            hadPlayer = true;
        } else {
            double dx = player.getX() - lastX;
            double dz = player.getZ() - lastZ;
            double moved = Math.sqrt(dx * dx + dz * dz);
            if (moved < 20.0) journeyDistance += moved;
            lastX = player.getX();
            lastZ = player.getZ();
        }

        if (CONFIG.isEnabled(Feature.DEATH_WAYPOINT) && player.isDeadOrDying()) {
            lastDeath = player.blockPosition();
        }

        if (CONFIG.isEnabled(Feature.PORTAL_MEMORY) && tickCounter % 20 == 0) {
            BlockPos center = player.blockPosition();
            outer:
            for (int x = -4; x <= 4; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (client.level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                            lastPortal = pos.immutable();
                            break outer;
                        }
                    }
                }
            }
        }

            if (tickCounter % 40 == 0 && CONFIG.isEnabled(Feature.SMART_DASHBOARD)) {
                if (player.getHealth() <= 6.0F) {
                    player.displayClientMessage(Component.literal("§cSurvival Utils: Low health"), true);
                } else if (player.getFoodData().getFoodLevel() <= 5) {
                    player.displayClientMessage(Component.literal("§6Survival Utils: Low hunger"), true);
                }
            }
        } catch (Throwable t) {
            runtimeFaulted = true;
        }
    }
}
