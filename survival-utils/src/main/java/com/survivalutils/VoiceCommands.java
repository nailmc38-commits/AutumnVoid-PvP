package com.survivalutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

public final class VoiceCommands {
    private VoiceCommands() {}

    public static volatile String status = "OFF";
    public static volatile String lastHeard = "Say 'help' for commands.";
    public static volatile String lastResponse = "Voice commands are off.";
    public static volatile String microphone = "Windows default microphone";

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Process process;
    private static Thread readerThread;

    public static boolean isRunning() {
        return running.get();
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
            Path script = FabricLoader.getInstance().getConfigDir().resolve("survival-utils-voice.ps1");
            Files.createDirectories(script.getParent());
            Files.writeString(script, powershellScript(), StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", script.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            running.set(true);
            status = "STARTING";
            lastResponse = "Connecting to your default microphone...";

            readerThread = Thread.ofVirtual().name("survival-utils-voice").start(() -> readLoop(process));
        } catch (Throwable t) {
            running.set(false);
            status = "ERROR";
            lastResponse = "Could not start Windows speech recognition.";
        }
    }

    public static synchronized void stop() {
        running.set(false);
        status = "OFF";
        lastResponse = "Voice commands disabled.";
        if (process != null) {
            process.destroyForcibly();
            process = null;
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
                    lastResponse = "Microphone ready. Say 'help' for commands.";
                    continue;
                }

                if (value.startsWith("__MIC__:")) {
                    microphone = value.substring("__MIC__:".length()).trim();
                    continue;
                }

                if (value.startsWith("CMD:")) {
                    String command = value.substring(4).trim().toLowerCase(Locale.ROOT);
                    lastHeard = command;
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) {
                        client.execute(() -> handle(client, command));
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (running.get()) {
                running.set(false);
                status = "ERROR";
                lastResponse = "Microphone listener stopped.";
            }
        }
    }

    private static void handle(Minecraft client, String command) {
        if (client.player == null || client.level == null) {
            respond(client, "World is not ready.");
            return;
        }

        var player = client.player;
        var level = client.level;

        switch (command) {
            case "status" -> {
                int armor = lowestArmor(player);
                respond(client, String.format(Locale.ROOT,
                    "Health %.1f of %.1f. Hunger %d of 20. Armor %s. XP level %d.",
                    player.getHealth(), player.getMaxHealth(),
                    player.getFoodData().getFoodLevel(),
                    armor < 0 ? "none" : armor + " percent lowest",
                    player.experienceLevel));
            }
            case "dimension" -> respond(client,
                "Dimension: " + pretty(level.dimension().identifier().getPath()) + ".");
            case "coordinates", "position" -> respond(client, String.format(Locale.ROOT,
                "Coordinates: X %.0f, Y %.0f, Z %.0f.",
                player.getX(), player.getY(), player.getZ()));
            case "health" -> respond(client, String.format(Locale.ROOT,
                "Health: %.1f of %.1f. Hunger: %d of 20.",
                player.getHealth(), player.getMaxHealth(), player.getFoodData().getFoodLevel()));
            case "armor" -> {
                int lowest = lowestArmor(player);
                respond(client, lowest < 0 ? "No armor equipped." : "Lowest armor durability is " + lowest + " percent.");
            }
            case "biome" -> {
                String biome = level.getBiome(player.blockPosition()).unwrapKey()
                    .map(k -> pretty(k.identifier().getPath()))
                    .orElse("Unknown");
                respond(client, "Biome: " + biome + ".");
            }
            case "time" -> {
                long t = Math.floorMod(level.getDayTime(), 24000L);
                boolean day = t < 13000L;
                long remain = day ? 13000L - t : 24000L - t;
                long seconds = remain / 20L;
                respond(client, (day ? "Daytime. " : "Nighttime. ") + (seconds / 60L) + " minutes " + (seconds % 60L) + " seconds remaining.");
            }
            case "inventory" -> {
                int free = 0;
                int occupied = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) free++;
                    else occupied++;
                }
                ItemStack held = player.getMainHandItem();
                respond(client, "Inventory has " + occupied + " occupied slots and " + free + " empty slots. Held item: "
                    + (held.isEmpty() ? "nothing" : held.getHoverName().getString()) + ".");
            }
            case "danger" -> {
                var hostiles = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0));
                double nearest = hostiles.stream().mapToDouble(player::distanceTo).min().orElse(-1.0);
                if (hostiles.isEmpty()) respond(client, "No hostile mobs detected within 16 blocks.");
                else respond(client, hostiles.size() + " hostile mobs within 16 blocks. Nearest is about " + Math.round(nearest) + " blocks away.");
            }
            case "light" -> respond(client,
                "Local light level: " + level.getMaxLocalRawBrightness(player.blockPosition()) + ".");
            case "help", "commands" -> respond(client,
                "Commands: status, dimension, coordinates, health, armor, biome, time, inventory, danger, light.");
            default -> respond(client, "Command not recognized.");
        }
    }

    private static void respond(Minecraft client, String message) {
        lastResponse = message;
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("§bVISOR §8// §f" + message), true);
        }
    }

    private static int lowestArmor(net.minecraft.world.entity.player.Player player) {
        int lowest = 101;
        boolean any = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            any = true;
            int pct = Math.max(0, Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
            lowest = Math.min(lowest, pct);
        }
        return any ? lowest : -1;
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
$recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine
$recognizer.SetInputToDefaultAudioDevice()
$choices = New-Object System.Speech.Recognition.Choices
$phrases = @(
  'status',
  'dimension',
  'coordinates',
  'position',
  'health',
  'armor',
  'biome',
  'time',
  'inventory',
  'danger',
  'light',
  'help',
  'commands'
)
$choices.Add([string[]]$phrases)
$builder = New-Object System.Speech.Recognition.GrammarBuilder
$builder.Append($choices)
$grammar = New-Object System.Speech.Recognition.Grammar($builder)
$recognizer.LoadGrammar($grammar)
Write-Output '__MIC__:Windows default microphone'
Write-Output '__READY__'
[Console]::Out.Flush()
while ($true) {
  try {
    $result = $recognizer.Recognize([TimeSpan]::FromMilliseconds(900))
    if ($null -ne $result -and $result.Confidence -ge 0.58) {
      Write-Output ('CMD:' + $result.Text)
      [Console]::Out.Flush()
    }
  } catch {
    Start-Sleep -Milliseconds 100
  }
}
""";
    }
}
