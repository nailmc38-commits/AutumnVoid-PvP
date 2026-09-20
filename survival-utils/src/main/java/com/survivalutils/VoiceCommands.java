package com.survivalutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

public final class VoiceCommands {
    private VoiceCommands() {}

    public static volatile String status = "OFF";
    public static volatile String lastHeard = "Say 'help' for commands.";
    public static volatile String lastResponse = "Voice commands are off.";
    public static volatile String microphone = "System Default";

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final Path MIC_PATH = FabricLoader.getInstance().getConfigDir().resolve("survival-utils-mic.txt");

    private static Process process;
    private static TargetDataLine captureLine;
    private static String selectedMicName = "System Default";

    public static void initialize() {
        try {
            if (Files.exists(MIC_PATH)) {
                String saved = Files.readString(MIC_PATH).trim();
                if (!saved.isBlank()) selectedMicName = saved;
            }
        } catch (IOException ignored) {}

        if (!microphoneNames().contains(selectedMicName)) {
            selectedMicName = "System Default";
        }
        microphone = selectedMicName;
    }

    public static boolean isRunning() {
        return running.get();
    }

    public static String selectedMicDisplay() {
        return selectedMicName == null || selectedMicName.isBlank() ? "System Default" : selectedMicName;
    }

    public static List<String> microphoneNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("System Default");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.getTargetLineInfo().length > 0) names.add(info.getName());
            } catch (Throwable ignored) {}
        }
        return new ArrayList<>(names);
    }

    public static synchronized boolean selectMicrophone(String name) {
        try {
            if (name == null || name.isBlank()) name = "System Default";
            List<String> available = microphoneNames();
            if (!available.contains(name)) {
                status = "NO MIC";
                lastResponse = "That microphone is no longer available.";
                return false;
            }

            if (running.get()) stop();

            selectedMicName = name;
            microphone = name;
            Files.createDirectories(MIC_PATH.getParent());
            Files.writeString(MIC_PATH, "System Default".equals(name) ? "" : name);

            status = "OFF";
            lastResponse = "Mic set to " + shortName(name) + ". Press F8 to listen.";
            return true;
        } catch (Throwable t) {
            status = "ERROR";
            lastResponse = "Could not change microphone.";
            return false;
        }
    }

    public static void toggle() {
        if (running.get()) stop();
        else start();
    }

    public static synchronized void start() {
        if (running.get()) return;

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            status = "UNSUPPORTED";
            lastResponse = "Voice commands currently require Windows.";
            return;
        }

        try {
            AudioFormat format = chooseFormat();
            if (format == null || captureLine == null) {
                status = "NO MIC";
                lastResponse = "Could not open " + shortName(selectedMicDisplay()) + ".";
                return;
            }

            Path script = FabricLoader.getInstance().getConfigDir().resolve("survival-utils-voice.ps1");
            Files.createDirectories(script.getParent());
            Files.writeString(script, powershellScript(), StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", script.toAbsolutePath().toString(),
                Integer.toString(Math.round(format.getSampleRate())),
                String.format(Locale.ROOT, "%.2f", SurvivalUtilsClient.CONFIG.voiceConfidence())
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            running.set(true);
            status = "STARTING";
            lastResponse = "Opening " + shortName(selectedMicDisplay()) + "...";

            Process active = process;
            Thread.ofVirtual().name("survival-utils-voice-reader").start(() -> readLoop(active));
            Thread.ofVirtual().name("survival-utils-voice-capture").start(() -> captureLoop(active));
        } catch (Throwable t) {
            closeCapture();
            running.set(false);
            status = "ERROR";
            lastResponse = "Could not start microphone recognition.";
        }
    }

    private static AudioFormat chooseFormat() {
        closeCapture();
        Mixer selected = resolveSelectedMixer();

        float[] rates = new float[]{16000F, 48000F, 44100F};
        for (float rate : rates) {
            AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
            try {
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine line = selected == null
                    ? (TargetDataLine) AudioSystem.getLine(lineInfo)
                    : (TargetDataLine) selected.getLine(lineInfo);
                line.open(format);
                captureLine = line;
                return format;
            } catch (Throwable ignored) {
                closeCapture();
            }
        }
        return null;
    }

    private static Mixer resolveSelectedMixer() {
        if ("System Default".equals(selectedMicName)) return null;

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (!info.getName().equals(selectedMicName)) continue;
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.getTargetLineInfo().length > 0) return mixer;
            } catch (Throwable ignored) {}
        }

        selectedMicName = "System Default";
        microphone = selectedMicName;
        return null;
    }

    private static void captureLoop(Process p) {
        TargetDataLine line = captureLine;
        if (line == null) return;

        try (OutputStream out = p.getOutputStream()) {
            line.start();
            byte[] buffer = new byte[4096];
            while (running.get() && p.isAlive()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            }
        } catch (Throwable ignored) {
        } finally {
            closeCapture();
        }
    }

    public static synchronized void stop() {
        running.set(false);
        status = "OFF";
        lastResponse = "Voice commands disabled.";
        closeCapture();

        if (process != null) {
            try { process.getOutputStream().close(); } catch (Throwable ignored) {}
            try { process.destroy(); } catch (Throwable ignored) {}
            process = null;
        }
    }

    private static void closeCapture() {
        TargetDataLine line = captureLine;
        captureLine = null;
        if (line != null) {
            try { line.stop(); } catch (Throwable ignored) {}
            try { line.flush(); } catch (Throwable ignored) {}
            try { line.close(); } catch (Throwable ignored) {}
        }
    }

    private static void readLoop(Process p) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.isEmpty()) continue;

                if ("__READY__".equals(value)) {
                    status = "LISTENING";
                    lastResponse = "Listening. Say 'help'.";
                    continue;
                }

                if (value.startsWith("CMD:")) {
                    String command = value.substring(4).trim().toLowerCase(Locale.ROOT);
                    lastHeard = command;
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) client.execute(() -> handle(client, command));
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (running.get()) {
                running.set(false);
                status = "ERROR";
                lastResponse = "Microphone listener stopped.";
                closeCapture();
            }
        }
    }

    private static void handle(Minecraft client, String command) {
        if (client.player == null || client.level == null) {
            respond(client, "World is not ready.");
            return;
        }

        Feature required = featureForCommand(command);
        if (required != null && !SurvivalUtilsClient.CONFIG.voiceEnabled(required)) {
            respond(client, required.title + " is HUD-only/off. Set it to VOICE or BOTH in F9.");
            return;
        }

        var player = client.player;
        var level = client.level;

        switch (command) {
            case "status" -> {
                int armor = lowestArmor(player);
                respond(client, String.format(Locale.ROOT,
                    "HP %.1f/%.1f, hunger %d/20, armor %s, XP %d, hostiles %d.",
                    player.getHealth(), player.getMaxHealth(), player.getFoodData().getFoodLevel(),
                    armor < 0 ? "none" : armor + "%", player.experienceLevel,
                    level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0)).size()));
            }
            case "dimension" -> respond(client, "Dimension: " + pretty(level.dimension().identifier().getPath()) + ".");
            case "coordinates", "position" -> respond(client, String.format(Locale.ROOT,
                "X %.0f, Y %.0f, Z %.0f.", player.getX(), player.getY(), player.getZ()));
            case "health" -> respond(client, String.format(Locale.ROOT,
                "Health %.1f of %.1f.", player.getHealth(), player.getMaxHealth()));
            case "food", "hunger" -> respond(client, "Hunger " + player.getFoodData().getFoodLevel() + " of 20.");
            case "armor" -> {
                int lowest = lowestArmor(player);
                respond(client, lowest < 0 ? "No armor equipped." : "Lowest armor durability " + lowest + "%.");
            }
            case "biome" -> respond(client, "Biome: " + level.getBiome(player.blockPosition()).unwrapKey()
                .map(k -> pretty(k.identifier().getPath())).orElse("Unknown") + ".");
            case "time", "day night" -> {
                long t = Math.floorMod(level.getDayTime(), 24000L);
                boolean day = t < 13000L;
                long remain = day ? 13000L - t : 24000L - t;
                long seconds = remain / 20L;
                respond(client, (day ? "Day. " : "Night. ") + (seconds / 60L) + "m " + (seconds % 60L) + "s remaining.");
            }
            case "inventory" -> respond(client, "Inventory: " + freeSlots(player) + " empty slots. Holding " + heldName(player) + ".");
            case "free slots" -> respond(client, freeSlots(player) + " empty inventory slots.");
            case "danger", "hostiles" -> {
                var hostiles = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0));
                double nearest = hostiles.stream().mapToDouble(player::distanceTo).min().orElse(-1.0);
                respond(client, hostiles.isEmpty() ? "No hostiles within 16 blocks."
                    : hostiles.size() + " hostiles nearby. Nearest about " + Math.round(nearest) + " blocks.");
            }
            case "creepers" -> respond(client, countType(client, EntityType.CREEPER, 16.0) + " creepers within 16 blocks.");
            case "skeletons" -> respond(client, countType(client, EntityType.SKELETON, 16.0) + " skeletons within 16 blocks.");
            case "light" -> respond(client, "Light level " + level.getMaxLocalRawBrightness(player.blockPosition()) + ".");
            case "speed" -> {
                var v = player.getDeltaMovement();
                double bps = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
                respond(client, String.format(Locale.ROOT, "Speed %.2f blocks per second.", bps));
            }
            case "weather" -> respond(client, "Weather: " + (level.isThundering() ? "thunder" : level.isRaining() ? "rain" : "clear") + ".");
            case "fps" -> respond(client, "FPS " + client.getFps() + ".");
            case "ping" -> {
                int ping = -1;
                if (client.getConnection() != null) {
                    var info = client.getConnection().getPlayerInfo(player.getUUID());
                    if (info != null) ping = info.getLatency();
                }
                respond(client, ping < 0 ? "Ping unavailable." : "Ping " + ping + " milliseconds.");
            }
            case "held item", "item" -> respond(client, "Holding " + heldName(player) + ".");
            case "target", "target block" -> {
                if (client.hitResult instanceof BlockHitResult hit) {
                    respond(client, "Target: " + level.getBlockState(hit.getBlockPos()).getBlock().getName().getString() + ".");
                } else respond(client, "No block targeted.");
            }
            case "facing", "direction" -> respond(client, "Facing " + pretty(player.getDirection().getName()) + ".");
            case "chunk" -> respond(client, "Chunk " + player.chunkPosition().x + ", " + player.chunkPosition().z + ".");
            case "effects" -> respond(client, player.getActiveEffects().size() + " active effects.");
            case "xp", "experience" -> respond(client, "XP level " + player.experienceLevel + ".");
            case "durability" -> {
                ItemStack held = player.getMainHandItem();
                respond(client, held.isDamageableItem()
                    ? held.getHoverName().getString() + " durability " + durability(held) + "%."
                    : "Held item has no durability.");
            }
            case "torches" -> respond(client, countItem(player, Items.TORCH) + " torches.");
            case "arrows" -> respond(client, (countItem(player, Items.ARROW) + countItem(player, Items.SPECTRAL_ARROW) + countItem(player, Items.TIPPED_ARROW)) + " arrows.");
            case "rockets" -> respond(client, countItem(player, Items.FIREWORK_ROCKET) + " rockets.");
            case "pearls" -> respond(client, countItem(player, Items.ENDER_PEARL) + " ender pearls.");
            case "bed" -> respond(client, hasBed(player) ? "Bed detected in inventory." : "No bed in inventory.");
            case "portal" -> respond(client, SurvivalUtilsClient.lastPortal == null ? "No portal saved."
                : "Last portal at " + SurvivalUtilsClient.lastPortal.getX() + ", " + SurvivalUtilsClient.lastPortal.getY() + ", " + SurvivalUtilsClient.lastPortal.getZ() + ".");
            case "session" -> {
                long sec = Math.max(0L, (System.currentTimeMillis() - SurvivalUtilsClient.sessionStartMillis) / 1000L);
                respond(client, "Session " + (sec / 60L) + " minutes " + (sec % 60L) + " seconds.");
            }
            case "memory" -> {
                Runtime rt = Runtime.getRuntime();
                long used = (rt.totalMemory() - rt.freeMemory()) / 1024L / 1024L;
                long max = rt.maxMemory() / 1024L / 1024L;
                respond(client, "Memory " + used + " of " + max + " megabytes.");
            }
            case "gamemode", "game mode" -> respond(client,
                client.gameMode == null ? "Game mode unavailable." : "Game mode " + pretty(client.gameMode.getPlayerMode().getName()) + ".");
            case "air" -> respond(client, "Air supply " + player.getAirSupply() + ".");
            case "fire" -> respond(client, player.isOnFire() ? "You are on fire." : "Not on fire.");
            case "freeze", "freezing" -> respond(client, player.getTicksFrozen() > 0 ? "Freezing level " + player.getTicksFrozen() + "." : "Not freezing.");
            case "tool", "best tool" -> {
                if (client.hitResult instanceof BlockHitResult hit) {
                    var state = level.getBlockState(hit.getBlockPos());
                    String tool = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pickaxe"
                        : state.is(BlockTags.MINEABLE_WITH_AXE) ? "axe"
                        : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel"
                        : state.is(BlockTags.MINEABLE_WITH_HOE) ? "hoe" : "any tool";
                    respond(client, "Best tool: " + tool + ".");
                } else respond(client, "No block targeted.");
            }
            case "blocks" -> {
                int blocks = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.getItem() instanceof BlockItem) blocks += stack.getCount();
                }
                respond(client, blocks + " building blocks in inventory.");
            }
            case "world day" -> respond(client, "World day " + (level.getDayTime() / 24000L + 1L) + ".");
            case "movement" -> respond(client, player.isSprinting() ? "Sprinting." : player.isShiftKeyDown() ? "Sneaking." : "Walking.");
            case "helmet" -> {
                ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
                respond(client, stack.isEmpty() ? "No helmet." : stack.getHoverName().getString() + (stack.isDamageableItem() ? ", " + durability(stack) + "% durability." : "."));
            }
            case "shield" -> {
                ItemStack shield = findItem(player, Items.SHIELD);
                respond(client, shield.isEmpty() ? "No shield found." : "Shield durability " + durability(shield) + "%.");
            }
            case "elytra" -> {
                ItemStack elytra = findItem(player, Items.ELYTRA);
                respond(client, elytra.isEmpty() ? "No elytra found." : "Elytra durability " + durability(elytra) + "%.");
            }
            case "journey" -> respond(client, Math.round(SurvivalUtilsClient.journeyDistance) + " blocks traveled this session.");
            case "altitude" -> respond(client, "Altitude Y " + player.blockPosition().getY() + ".");
            case "emeralds" -> respond(client, countItem(player, Items.EMERALD) + " emeralds.");
            case "bone meal" -> respond(client, countItem(player, Items.BONE_MEAL) + " bone meal.");
            case "seeds" -> respond(client, countItem(player, Items.WHEAT_SEEDS) + " wheat seeds.");
            case "mic", "microphone" -> respond(client, "Microphone: " + selectedMicDisplay() + ".");
            case "help", "commands" -> respond(client,
                "Try status, dimension, position, health, armor, biome, time, inventory, danger, speed, weather, FPS, ping, target, tool, torches, pearls, session, mic, and more.");
            default -> respond(client, "Command not recognized.");
        }
    }

    private static Feature featureForCommand(String command) {
        return switch (command) {
            case "dimension" -> Feature.DIMENSION_HUD;
            case "coordinates", "position" -> Feature.COORDS_HUD;
            case "health" -> Feature.LOW_HEALTH_ALERT;
            case "food", "hunger" -> Feature.FOOD_HUD;
            case "armor", "helmet" -> Feature.ARMOR_HUD;
            case "biome" -> Feature.BIOME_HUD;
            case "time", "day night" -> Feature.DAY_NIGHT_HUD;
            case "inventory", "free slots" -> Feature.INVENTORY_SPACE;
            case "danger", "hostiles" -> Feature.HOSTILES_NEARBY;
            case "creepers" -> Feature.CREEPER_ALERT;
            case "skeletons" -> Feature.SKELETON_ALERT;
            case "light" -> Feature.LIGHT_WARNING;
            case "speed" -> Feature.SPEED_HUD;
            case "weather" -> Feature.WEATHER_HUD;
            case "fps" -> Feature.FPS_HUD;
            case "ping" -> Feature.PING_HUD;
            case "held item", "item" -> Feature.HELD_ITEM_COUNT;
            case "target", "target block" -> Feature.TARGET_BLOCK_HUD;
            case "facing", "direction" -> Feature.DIRECTION_HUD;
            case "chunk" -> Feature.CHUNK_HUD;
            case "effects" -> Feature.EFFECT_TIMERS;
            case "xp", "experience" -> Feature.XP_HUD;
            case "durability" -> Feature.DURABILITY_PERCENTAGES;
            case "torches" -> Feature.TORCH_COUNT;
            case "arrows" -> Feature.ARROW_COUNT;
            case "rockets" -> Feature.ROCKET_COUNT;
            case "pearls" -> Feature.PEARL_COUNT;
            case "bed" -> Feature.BED_ALERT;
            case "portal" -> Feature.PORTAL_MEMORY;
            case "session" -> Feature.SESSION_TIMER;
            case "memory" -> Feature.MEMORY_USAGE;
            case "gamemode", "game mode" -> Feature.GAMEMODE_HUD;
            case "air" -> Feature.DROWNING_ALERT;
            case "fire" -> Feature.FIRE_ALERT;
            case "freeze", "freezing" -> Feature.FREEZE_ALERT;
            case "tool", "best tool" -> Feature.TOOL_RECOMMENDATION;
            case "blocks" -> Feature.PALETTE_COUNTS;
            case "world day" -> Feature.WORLD_DAY_COUNTER;
            case "movement" -> Feature.SPRINT_STATE;
            case "shield" -> Feature.SHIELD_DURABILITY;
            case "elytra" -> Feature.ELYTRA_DURABILITY;
            case "journey" -> Feature.JOURNEY_STATS;
            case "altitude" -> Feature.ALTITUDE_HINT;
            case "emeralds" -> Feature.EMERALD_COUNT;
            case "bone meal" -> Feature.BONE_MEAL_COUNT;
            case "seeds" -> Feature.SEED_COUNT;
            default -> null;
        };
    }

    private static int countType(Minecraft client, EntityType<?> type, double radius) {
        return client.level.getEntities(client.player, client.player.getBoundingBox().inflate(radius), e -> e.getType() == type).size();
    }

    private static int freeSlots(net.minecraft.world.entity.player.Player player) {
        int free = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) free++;
        }
        return free;
    }

    private static String heldName(net.minecraft.world.entity.player.Player player) {
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() ? "nothing" : held.getHoverName().getString() + " x" + held.getCount();
    }

    private static int countItem(net.minecraft.world.entity.player.Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static ItemStack findItem(net.minecraft.world.entity.player.Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasBed(net.minecraft.world.entity.player.Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof BedItem) return true;
        }
        return false;
    }

    private static int lowestArmor(net.minecraft.world.entity.player.Player player) {
        int lowest = 101;
        boolean any = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            any = true;
            lowest = Math.min(lowest, durability(stack));
        }
        return any ? lowest : -1;
    }

    private static int durability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 100;
        return Math.max(0, Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
    }

    private static void respond(Minecraft client, String message) {
        lastResponse = message;
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("§bVISOR §8// §f" + message), true);
        }
    }

    private static String shortName(String value) {
        if (value == null) return "System Default";
        return value.length() <= 28 ? value : value.substring(0, 25) + "...";
    }

    private static String pretty(String id) {
        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static String powershellScript() {
        return """
Add-Type -AssemblyName System.Speech
$sampleRate = [int]$args[0]
$confidence = [double]::Parse($args[1], [Globalization.CultureInfo]::InvariantCulture)
$recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine
$bits = [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen
$channels = [System.Speech.AudioFormat.AudioChannel]::Mono
$format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo($sampleRate, $bits, $channels)
$recognizer.SetInputToAudioStream([Console]::OpenStandardInput(), $format)
$choices = New-Object System.Speech.Recognition.Choices
$phrases = @(
  'status','dimension','coordinates','position','health','food','hunger','armor','biome','time','day night',
  'inventory','free slots','danger','hostiles','creepers','skeletons','light','speed','weather','fps','ping',
  'held item','item','target','target block','facing','direction','chunk','effects','xp','experience','durability',
  'torches','arrows','rockets','pearls','bed','portal','session','memory','gamemode','game mode','air','fire',
  'freeze','freezing','tool','best tool','blocks','world day','movement','helmet','shield','elytra','journey',
  'altitude','emeralds','bone meal','seeds','mic','microphone','help','commands'
)
$choices.Add([string[]]$phrases)
$builder = New-Object System.Speech.Recognition.GrammarBuilder
$builder.Append($choices)
$grammar = New-Object System.Speech.Recognition.Grammar($builder)
$recognizer.LoadGrammar($grammar)
Write-Output '__READY__'
[Console]::Out.Flush()
while ($true) {
  try {
    $result = $recognizer.Recognize([TimeSpan]::FromMilliseconds(850))
    if ($null -ne $result -and $result.Confidence -ge $confidence) {
      Write-Output ('CMD:' + $result.Text)
      [Console]::Out.Flush()
    }
  } catch {
    Start-Sleep -Milliseconds 80
  }
}
""";
    }
}
