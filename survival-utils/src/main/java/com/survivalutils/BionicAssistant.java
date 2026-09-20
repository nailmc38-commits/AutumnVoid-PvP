package com.survivalutils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class BionicAssistant {
    private BionicAssistant() {}

    private static final String BASE = "http://127.0.0.1:1234";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    public static volatile String status = "OFFLINE";
    public static volatile String modelName = "Bionic";
    public static volatile String lastQuestion = "Press F8 to talk to Bionic.";
    public static volatile String lastAnswer = "LM Studio connection not checked yet.";
    public static volatile boolean thinking = false;

    public static void probe() {
        resolveModel().thenAccept(model -> {
            if (model != null && !model.isBlank()) {
                modelName = model;
                status = "ONLINE";
            }
        }).exceptionally(ex -> {
            status = "OFFLINE";
            return null;
        });
    }

    public static void ask(Minecraft client, String question) {
        if (question == null || question.isBlank() || thinking) return;
        if (client == null || client.player == null || client.level == null) return;

        lastQuestion = question.trim();
        lastAnswer = "Analyzing...";
        status = "THINKING";
        thinking = true;

        String context = buildContext(client);
        resolveModel().thenCompose(model -> {
            if (model == null || model.isBlank()) {
                return CompletableFuture.failedFuture(new IllegalStateException("No LM Studio model found"));
            }
            modelName = model;
            return request(model, lastQuestion, context);
        }).thenAccept(answer -> {
            lastAnswer = answer == null || answer.isBlank() ? "No response returned." : answer.trim();
            status = "ONLINE";
            thinking = false;
        }).exceptionally(ex -> {
            lastAnswer = "Bionic is offline. Start the LM Studio local server on port 1234.";
            status = "OFFLINE";
            thinking = false;
            return null;
        });
    }

    private static CompletableFuture<String> resolveModel() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/v1/models"))
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("LM Studio returned " + response.statusCode());
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray data = root.getAsJsonArray("data");
                if (data == null || data.isEmpty()) throw new IllegalStateException("No model available");
                for (var element : data) {
                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("id")) {
                        String id = obj.get("id").getAsString();
                        if (!id.isBlank()) return id;
                    }
                }
                throw new IllegalStateException("No model id available");
            });
    }

    private static CompletableFuture<String> request(String model, String question, String context) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.35);
        body.addProperty("max_tokens", 220);

        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
            "You are Bionic, a concise futuristic Minecraft survival assistant built into the player's visor. " +
            "Use the provided live Minecraft context when relevant. Never claim you can see information that is not in the context. " +
            "Give short useful answers, usually under 90 words. You may answer normal Minecraft questions too.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", "LIVE MINECRAFT CONTEXT:\n" + context + "\n\nPLAYER QUESTION:\n" + question);
        messages.add(user);

        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/v1/chat/completions"))
            .timeout(Duration.ofSeconds(35))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("LM Studio returned " + response.statusCode());
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray choices = root.getAsJsonArray("choices");
                if (choices == null || choices.isEmpty()) throw new IllegalStateException("No choices");
                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (message == null || !message.has("content")) throw new IllegalStateException("No content");
                return message.get("content").getAsString();
            });
    }

    private static String buildContext(Minecraft client) {
        var player = client.player;
        var level = client.level;
        StringBuilder b = new StringBuilder();

        b.append(String.format(Locale.ROOT, "Health: %.1f/%.1f\n", player.getHealth(), player.getMaxHealth()));
        b.append("Hunger: ").append(player.getFoodData().getFoodLevel()).append("/20\n");
        b.append("XP level: ").append(player.experienceLevel).append('\n');
        b.append(String.format(Locale.ROOT, "Position: %.0f %.0f %.0f\n", player.getX(), player.getY(), player.getZ()));
        b.append("Dimension: ").append(level.dimension().identifier().getPath()).append('\n');
        b.append("Biome: ").append(level.getBiome(player.blockPosition()).unwrapKey()
            .map(k -> k.identifier().getPath()).orElse("unknown")).append('\n');
        b.append("World time: ").append(Math.floorMod(level.getDayTime(), 24000L)).append(" ticks\n");
        b.append("Light: ").append(level.getMaxLocalRawBrightness(player.blockPosition())).append('\n');

        if (client.hitResult instanceof BlockHitResult hit) {
            var state = level.getBlockState(hit.getBlockPos());
            b.append("Target block: ").append(state.getBlock().getName().getString())
                .append(" at ").append(hit.getBlockPos().getX()).append(' ')
                .append(hit.getBlockPos().getY()).append(' ')
                .append(hit.getBlockPos().getZ()).append('\n');
        }

        b.append("Armor: ");
        boolean armor = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            armor = true;
            b.append(slot.getName()).append('=').append(stack.getHoverName().getString());
            if (stack.isDamageableItem()) {
                int pct = Math.max(0, Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
                b.append('(').append(pct).append("%)");
            }
            b.append("; ");
        }
        if (!armor) b.append("none");
        b.append('\n');

        Map<String, Integer> inv = new LinkedHashMap<>();
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            inv.merge(name, stack.getCount(), Integer::sum);
        }
        b.append("Inventory: ");
        int shown = 0;
        for (var entry : inv.entrySet()) {
            if (shown++ >= 45) {
                b.append("...");
                break;
            }
            b.append(entry.getKey()).append(" x").append(entry.getValue()).append("; ");
        }
        if (inv.isEmpty()) b.append("empty");
        b.append('\n');

        Map<String, Integer> nearby = new LinkedHashMap<>();
        for (var entity : level.getEntities(player, player.getBoundingBox().inflate(12.0), e -> e != player)) {
            String name = entity.getName().getString();
            nearby.merge(name, 1, Integer::sum);
        }
        b.append("Nearby entities within 12 blocks: ");
        if (nearby.isEmpty()) {
            b.append("none");
        } else {
            int shownEntities = 0;
            for (var entry : nearby.entrySet()) {
                if (shownEntities++ >= 15) {
                    b.append("...");
                    break;
                }
                b.append(entry.getKey()).append(" x").append(entry.getValue()).append("; ");
            }
        }

        return b.toString();
    }
}
