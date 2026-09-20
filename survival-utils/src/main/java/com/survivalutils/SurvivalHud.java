package com.survivalutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SurvivalHud {
    private SurvivalHud() {}

    private static final int WHITE = 0xFFF2F2F2;
    private static final int GRAY = 0xFFB8B8B8;
    private static final int YELLOW = 0xFFFFD55A;
    private static final int RED = 0xFFFF5555;
    private static final int AQUA = 0xFF55FFFF;
    private static final int GREEN = 0xFF55FF7A;

    public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null || client.level == null || client.font == null) return;
            if (client.options == null || client.options.hideGui) return;
            if (SurvivalUtilsClient.worldReadyTicks < 40) return;

            List<Line> lines = collect(client);
            if (lines == null) return;

            int max = SurvivalUtilsClient.CONFIG.maxHudLines();
            int shown = Math.min(max, lines.size());
            boolean right = "RIGHT".equals(SurvivalUtilsClient.CONFIG.hudSide());

            int y = 6;
            for (int i = 0; i < shown; i++) {
                Line line = lines.get(i);
                if (line == null || line.text == null) continue;
                int x = right ? graphics.guiWidth() - 6 - client.font.width(line.text) : 6;
                graphics.drawString(client.font, line.text, x, y, line.color, true);
                y += client.font.lineHeight;
            }

            if (lines.size() > max) {
                String more = "+" + (lines.size() - max) + " more";
                int x = right ? graphics.guiWidth() - 6 - client.font.width(more) : 6;
                graphics.drawString(client.font, more, x, y, GRAY, true);
            }

            renderVisor(graphics, client);
            renderVoice(graphics, client);
        } catch (Throwable t) {
            SurvivalUtilsClient.runtimeFaulted = true;
        }
    }

    private static List<Line> collect(Minecraft client) {
        List<Line> out = new ArrayList<>();
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;
        var level = client.level;
        BlockPos pos = player.blockPosition();

        if (cfg.showHud(Feature.DAY_NIGHT_HUD)) {
            long dayTime = Math.floorMod(level.getDayTime(), 24000L);
            boolean day = dayTime < 13000L;
            long remaining = day ? 13000L - dayTime : 24000L - dayTime;
            out.add(info((day ? "☀ Day" : "☾ Night") + " • " + formatTicks(remaining) + " remaining"));
        }

        if (cfg.showHud(Feature.COORDS_HUD)) {
            out.add(info(String.format(Locale.ROOT, "XYZ %.0f / %.0f / %.0f", player.getX(), player.getY(), player.getZ())));
        }

        if (cfg.showHud(Feature.BIOME_HUD)) {
            String biome = level.getBiome(pos).unwrapKey()
                .map(k -> pretty(k.identifier().getPath()))
                .orElse("Unknown");
            out.add(info("Biome • " + biome));
        }

        if (cfg.showHud(Feature.ARMOR_HUD)) {
            int lowest = lowestArmorPercent(player);
            out.add(new Line("Armor • " + (lowest < 0 ? "none" : lowest + "% lowest"), lowest >= 0 && lowest <= 15 ? RED : WHITE));
        }

        if (cfg.showHud(Feature.EFFECT_TIMERS)) {
            int count = player.getActiveEffects().size();
            if (count > 0) out.add(info("Effects • " + count + " active"));
        }

        if (cfg.showHud(Feature.FOOD_HUD)) {
            out.add(info(String.format(Locale.ROOT, "Food • %d/20 • Sat %.1f",
                player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel())));
        }

        if (cfg.showHud(Feature.XP_HUD)) {
            out.add(info(String.format(Locale.ROOT, "XP • Level %d • %.0f%%",
                player.experienceLevel, player.experienceProgress * 100.0F)));
        }

        if (cfg.showHud(Feature.FPS_HUD)) {
            out.add(info("FPS • " + client.getFps()));
        }

        if (cfg.showHud(Feature.PING_HUD) && client.getConnection() != null) {
            var entry = client.getConnection().getPlayerInfo(player.getUUID());
            if (entry != null) out.add(info("Ping • " + entry.getLatency() + " ms"));
        }

        if (cfg.showHud(Feature.SPEED_HUD)) {
            var velocity = player.getDeltaMovement();
            double bps = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
            out.add(info(String.format(Locale.ROOT, "Speed • %.2f b/s", bps)));
        }

        if (cfg.showHud(Feature.DIRECTION_HUD)) {
            out.add(info("Facing • " + pretty(player.getDirection().getName())));
        }

        if (cfg.showHud(Feature.WEATHER_HUD)) {
            out.add(info("Weather • " + (level.isThundering() ? "Thunder" : level.isRaining() ? "Rain" : "Clear")));
        }

        if (cfg.showHud(Feature.MOON_PHASE_HUD)) {
            out.add(info("Moon phase • " + Math.floorMod(level.getDayTime() / 24000L, 8L)));
        }

        if (cfg.showHud(Feature.CHUNK_HUD)) {
            var chunk = player.chunkPosition();
            out.add(info("Chunk • " + chunk.x + ", " + chunk.z));
        }

        if (cfg.showHud(Feature.TARGET_BLOCK_HUD) && client.hitResult instanceof BlockHitResult hit) {
            BlockState state = level.getBlockState(hit.getBlockPos());
            out.add(info("Target • " + state.getBlock().getName().getString()));
        }

        if (cfg.showHud(Feature.HELD_ITEM_COUNT)) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty()) out.add(info("Held total • " + countItem(player, held.getItem())));
        }

        if (cfg.showHud(Feature.DIMENSION_HUD)) {
            out.add(info("Dimension • " + pretty(level.dimension().identifier().getPath())));
        }

        if (cfg.showHud(Feature.SESSION_TIMER)) {
            long seconds = Math.max(0L, (System.currentTimeMillis() - SurvivalUtilsClient.sessionStartMillis) / 1000L);
            out.add(info("Session • " + formatSeconds(seconds)));
        }

        addInventoryLines(out, client);
        addExplorationLines(out, client);
        addMiningLines(out, client);
        addFarmingLines(out, client);
        addVillagerLines(out, client);
        addBuildingLines(out, client);
        addCombatLines(out, client);
        addMiscLines(out, client);

        return out;
    }

    private static void addInventoryLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;

        if (cfg.showHud(Feature.DURABILITY_WARNINGS)) {
            ItemStack weakest = weakestDamageable(player);
            if (!weakest.isEmpty()) {
                int p = durabilityPercent(weakest);
                if (p <= 15) out.add(warn("⚠ " + weakest.getHoverName().getString() + " durability " + p + "%"));
            }
        }

        if (cfg.showHud(Feature.TOTEM_ALERT) && countItem(player, Items.TOTEM_OF_UNDYING) == 0) {
            out.add(warn("⚠ No Totem"));
        }

        int free = freeSlots(player);
        if (cfg.showHud(Feature.INVENTORY_SPACE)) out.add(info("Inventory • " + free + " free slots"));
        if (cfg.showHud(Feature.INVENTORY_FULL_ALERT) && free == 0) out.add(warn("⚠ Inventory full"));

        if (cfg.showHud(Feature.HOTBAR_COUNTS)) {
            int nonEmpty = 0;
            for (int i = 0; i < 9; i++) if (!player.getInventory().getItem(i).isEmpty()) nonEmpty++;
            out.add(info("Hotbar • " + nonEmpty + "/9 occupied"));
        }

        if (cfg.showHud(Feature.FOOD_COUNT)) {
            int count = countFood(player);
            out.add(info("Food items • " + count));
        }

        if (cfg.showHud(Feature.TORCH_COUNT)) out.add(info("Torches • " + countItem(player, Items.TORCH)));
        if (cfg.showHud(Feature.ARROW_COUNT)) out.add(info("Arrows • " + countArrows(player)));
        if (cfg.showHud(Feature.ROCKET_COUNT)) out.add(info("Rockets • " + countItem(player, Items.FIREWORK_ROCKET)));
        if (cfg.showHud(Feature.PEARL_COUNT)) out.add(info("Pearls • " + countItem(player, Items.ENDER_PEARL)));

        if (cfg.showHud(Feature.BED_ALERT) && isNight(client) && !hasBed(player)) out.add(warn("⚠ Night approaching • no bed carried"));
        if (cfg.showHud(Feature.WATER_BUCKET_ALERT) && countItem(player, Items.WATER_BUCKET) == 0) out.add(new Line("Water bucket • none", YELLOW));

        if (cfg.showHud(Feature.AUTO_REFILL_HOTBAR)) {
            out.add(new Line("Auto refill • armed", GREEN));
        }

        if (cfg.showHud(Feature.INVENTORY_ORGANIZER)) {
            out.add(new Line("Organizer • enabled", GREEN));
        }

        if (cfg.showHud(Feature.MENDING_REMINDER)) {
            ItemStack weak = weakestDamageable(player);
            if (!weak.isEmpty() && durabilityPercent(weak) < 60 && player.experienceLevel > 0) {
                out.add(new Line("Mending check • damaged gear + XP available", AQUA));
            }
        }
    }

    private static void addExplorationLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;
        var level = client.level;

        if (cfg.showHud(Feature.WAYPOINTS)) {
            out.add(new Line("Waypoints • local HUD enabled", AQUA));
        }

        if (cfg.showHud(Feature.DEATH_WAYPOINT) && SurvivalUtilsClient.lastDeath != null) {
            double d = horizontalDistance(player.getX(), player.getZ(), SurvivalUtilsClient.lastDeath.getX(), SurvivalUtilsClient.lastDeath.getZ());
            out.add(new Line("Last death • " + coord(SurvivalUtilsClient.lastDeath) + " • " + Math.round(d) + "m", AQUA));
        }

        if (cfg.showHud(Feature.PORTAL_MEMORY) && SurvivalUtilsClient.lastPortal != null) {
            out.add(new Line("Last portal • " + coord(SurvivalUtilsClient.lastPortal), AQUA));
        }

        if (cfg.showHud(Feature.JOURNEY_STATS)) {
            out.add(info("Journey • " + Math.round(SurvivalUtilsClient.journeyDistance) + " blocks"));
        }

        if (cfg.showHud(Feature.ALTITUDE_HINT)) {
            out.add(info("Altitude • Y " + player.blockPosition().getY() + altitudeHint(client)));
        }

        if (cfg.showHud(Feature.NETHER_COORD_CONVERTER)) {
            boolean nether = level.dimension().identifier().getPath().equals("the_nether");
            double x = nether ? player.getX() * 8.0 : player.getX() / 8.0;
            double z = nether ? player.getZ() * 8.0 : player.getZ() / 8.0;
            out.add(info((nether ? "Overworld" : "Nether") + " link • " + Math.round(x) + ", " + Math.round(z)));
        }

        if (cfg.showHud(Feature.COMPASS_HOME)) {
            out.add(info("Origin • " + directionTo(player.getX(), player.getZ(), 0, 0) + " • " + Math.round(horizontalDistance(player.getX(), player.getZ(), 0, 0)) + "m"));
        }

        if (cfg.showHud(Feature.DISTANCE_FROM_SPAWN)) {
            out.add(info("Distance from origin • " + Math.round(horizontalDistance(player.getX(), player.getZ(), 0, 0)) + "m"));
        }

        int light = level.getMaxLocalRawBrightness(player.blockPosition());
        if (cfg.showHud(Feature.LIGHT_WARNING) && light <= 3) out.add(new Line("Low light • " + light, YELLOW));
        if (cfg.showHud(Feature.SAFE_SLEEP_ALERT) && isNight(client)) out.add(new Line("Sleep window • open", AQUA));
        if (cfg.showHud(Feature.WORLD_DAY_COUNTER)) out.add(info("World day • " + (level.getDayTime() / 24000L + 1L)));

        if (cfg.showHud(Feature.CHUNK_BORDER_DISTANCE)) {
            int lx = Math.floorMod(player.blockPosition().getX(), 16);
            int lz = Math.floorMod(player.blockPosition().getZ(), 16);
            int edge = Math.min(Math.min(lx, 15 - lx), Math.min(lz, 15 - lz));
            out.add(info("Nearest chunk edge • " + edge + " blocks"));
        }
    }

    private static void addMiningLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;
        var level = client.level;

        BlockState target = null;
        if (client.hitResult instanceof BlockHitResult hit) target = level.getBlockState(hit.getBlockPos());

        if (cfg.showHud(Feature.TOOL_RECOMMENDATION) && target != null) {
            String tool = target.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "Pickaxe"
                : target.is(BlockTags.MINEABLE_WITH_AXE) ? "Axe"
                : target.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "Shovel"
                : target.is(BlockTags.MINEABLE_WITH_HOE) ? "Hoe" : "Any";
            out.add(info("Best tool • " + tool));
        }

        if (cfg.showHud(Feature.PICKAXE_DURABILITY)) {
            ItemStack pick = findFirst(player, s -> s.is(ItemTags.PICKAXES));
            if (!pick.isEmpty()) out.add(info("Pickaxe • " + durabilityPercent(pick) + "%"));
        }

        if (cfg.showHud(Feature.ORE_SESSION_TRACKER)) {
            out.add(info("Ore inventory • " + oreTotal(player) + " tracked materials"));
        }

        if (cfg.showHud(Feature.DIAMOND_Y_HINT) && level.dimension().identifier().getPath().equals("overworld")) {
            int y = player.blockPosition().getY();
            out.add(info("Diamond depth • Y " + y + " • target ≈ -59"));
        }

        if (cfg.showHud(Feature.ANCIENT_DEBRIS_Y_HINT) && level.dimension().identifier().getPath().equals("the_nether")) {
            int y = player.blockPosition().getY();
            out.add(info("Debris depth • Y " + y + " • common ≈ 15"));
        }

        if (cfg.showHud(Feature.MINING_SESSION_TIMER) && player.blockPosition().getY() < 40) {
            long seconds = (System.currentTimeMillis() - SurvivalUtilsClient.sessionStartMillis) / 1000L;
            out.add(info("Underground session • " + formatSeconds(seconds)));
        }

        if (cfg.showHud(Feature.TORCH_INTERVAL) && level.getMaxLocalRawBrightness(player.blockPosition()) <= 5) {
            out.add(new Line("Torch hint • light this area", YELLOW));
        }

        if (cfg.showHud(Feature.INVENTORY_ORE_SUMMARY)) {
            out.add(info("Ores • D:" + countItem(player, Items.DIAMOND)
                + " Fe:" + countItem(player, Items.RAW_IRON)
                + " Au:" + countItem(player, Items.RAW_GOLD)
                + " Cu:" + countItem(player, Items.RAW_COPPER)));
        }

        if (cfg.showHud(Feature.FORTUNE_REMINDER) && target != null && isValuableOre(target)) {
            out.add(new Line("Fortune check • valuable ore targeted", AQUA));
        }

        if (cfg.showHud(Feature.SILK_TOUCH_REMINDER) && target != null && target.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            out.add(new Line("Silk Touch check • special block targeted", AQUA));
        }
    }

    private static void addFarmingLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;

        int mature = -1;
        if (cfg.showHud(Feature.CROP_READY_COUNT) || cfg.showHud(Feature.HARVEST_REMINDER)) {
            mature = matureCrops(client, 5);
        }

        if (cfg.showHud(Feature.CROP_READY_COUNT)) out.add(info("Mature crops nearby • " + mature));
        if (cfg.showHud(Feature.BONE_MEAL_COUNT)) out.add(info("Bone meal • " + countItem(player, Items.BONE_MEAL)));
        if (cfg.showHud(Feature.SEED_COUNT)) {
            int seeds = countItem(player, Items.WHEAT_SEEDS) + countItem(player, Items.BEETROOT_SEEDS)
                + countItem(player, Items.MELON_SEEDS) + countItem(player, Items.PUMPKIN_SEEDS);
            out.add(info("Seeds • " + seeds));
        }

        if (cfg.showHud(Feature.HOE_DURABILITY)) {
            ItemStack hoe = findFirst(player, s -> s.getItem() instanceof HoeItem);
            if (!hoe.isEmpty()) out.add(info("Hoe • " + durabilityPercent(hoe) + "%"));
        }

        if (cfg.showHud(Feature.HARVEST_REMINDER) && mature >= 8) out.add(new Line("Harvest ready • " + mature + " crops", GREEN));

        if (cfg.showHud(Feature.COMPOSTER_REMINDER)) {
            int compost = countItem(player, Items.WHEAT_SEEDS) + countItem(player, Items.BEETROOT_SEEDS)
                + countItem(player, Items.KELP) + countItem(player, Items.DRIED_KELP);
            out.add(info("Easy compostables • " + compost));
        }

        if (cfg.showHud(Feature.FARM_LIGHT_WARNING) && client.level.getMaxLocalRawBrightness(player.blockPosition()) < 8) {
            out.add(new Line("Farm light • low", YELLOW));
        }

        if (cfg.showHud(Feature.ANIMAL_FOOD_HELPER)) {
            out.add(info("Breeding food • wheat " + countItem(player, Items.WHEAT)
                + " • carrots " + countItem(player, Items.CARROT)));
        }
    }

    private static void addVillagerLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;
        var box = player.getBoundingBox().inflate(16.0);

        if (cfg.showHud(Feature.VILLAGER_HELPER)) {
            int villagers = countEntities(client, EntityType.VILLAGER, 16.0);
            out.add(info("Villagers within 16m • " + villagers));
        }

        if (cfg.showHud(Feature.EMERALD_COUNT)) out.add(info("Emeralds • " + countItem(player, Items.EMERALD)));

        if (cfg.showHud(Feature.TRADE_ITEM_CHECK)) {
            int trade = countItem(player, Items.PAPER) + countItem(player, Items.STICK)
                + countItem(player, Items.WHEAT) + countItem(player, Items.COAL);
            out.add(info("Common trade items • " + trade));
        }

        if (cfg.showHud(Feature.RAID_WARNING) && player.hasEffect(net.minecraft.world.effect.MobEffects.BAD_OMEN)) {
            out.add(warn("⚠ Raid risk • Bad Omen active"));
        }

        if (cfg.showHud(Feature.HERO_TIMER)) {
            out.add(info("Hero effect • " + (player.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE) ? "active" : "inactive")));
        }
    }

    private static void addBuildingLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;

        if (cfg.showHud(Feature.BLOCK_COUNT)) {
            ItemStack held = player.getMainHandItem();
            out.add(info("Selected stack total • " + (held.isEmpty() ? 0 : countItem(player, held.getItem()))));
        }

        if (cfg.showHud(Feature.PALETTE_COUNTS)) {
            int blocks = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) blocks += stack.getCount();
            }
            out.add(info("Building blocks • " + blocks));
        }

        if (cfg.showHud(Feature.SCAFFOLD_COUNT)) out.add(info("Scaffolding • " + countItem(player, Items.SCAFFOLDING)));

        if (cfg.showHud(Feature.BUILD_HEIGHT)) {
            int y = player.blockPosition().getY();
            out.add(info("Build Y • " + y));
        }

        if (cfg.showHud(Feature.BUILD_LIGHT)) {
            out.add(info("Local light • " + client.level.getMaxLocalRawBrightness(player.blockPosition())));
        }
    }

    private static void addCombatLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;

        if (cfg.showHud(Feature.LOW_HEALTH_ALERT) && player.getHealth() <= 6.0F) {
            out.add(warn("⚠ LOW HEALTH • " + String.format(Locale.ROOT, "%.1f", player.getHealth())));
        }

        if (cfg.showHud(Feature.LOW_HUNGER_ALERT) && player.getFoodData().getFoodLevel() <= 5) {
            out.add(warn("⚠ LOW HUNGER • " + player.getFoodData().getFoodLevel()));
        }

        if (cfg.showHud(Feature.SHIELD_DURABILITY)) {
            addSpecificDurability(out, player, Items.SHIELD, "Shield");
        }

        if (cfg.showHud(Feature.BOW_DURABILITY)) {
            ItemStack bow = findFirst(player, s -> s.is(Items.BOW) || s.is(Items.CROSSBOW));
            if (!bow.isEmpty()) out.add(info("Bow • " + durabilityPercent(bow) + "%"));
        }

        if (cfg.showHud(Feature.ELYTRA_DURABILITY)) {
            addSpecificDurability(out, player, Items.ELYTRA, "Elytra");
        }

        var box = player.getBoundingBox().inflate(16.0);
        if (cfg.showHud(Feature.HOSTILES_NEARBY)) {
            int hostiles = client.level.getEntitiesOfClass(Monster.class, box).size();
            out.add(new Line("Hostiles within 16m • " + hostiles, hostiles > 0 ? YELLOW : WHITE));
        }

        if (cfg.showHud(Feature.CREEPER_ALERT)) {
            double nearest = client.level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(10.0))
                .stream().mapToDouble(player::distanceTo).min().orElse(-1.0);
            if (nearest >= 0) out.add(warn("⚠ Creeper • " + Math.round(nearest) + "m"));
        }

        if (cfg.showHud(Feature.SKELETON_ALERT)) {
            int skeletons = countEntities(client, EntityType.SKELETON, 12.0);
            if (skeletons > 0) out.add(new Line("Skeletons nearby • " + skeletons, YELLOW));
        }

        if (cfg.showHud(Feature.FIRE_ALERT) && player.isOnFire()) out.add(warn("⚠ ON FIRE"));
        if (cfg.showHud(Feature.DROWNING_ALERT) && player.getAirSupply() < 80) out.add(warn("⚠ AIR LOW • " + player.getAirSupply()));
        if (cfg.showHud(Feature.FREEZE_ALERT) && player.getTicksFrozen() > 60) out.add(warn("⚠ FREEZING"));
        if (cfg.showHud(Feature.FALL_ALERT) && player.fallDistance > 6.0F) out.add(warn("⚠ FALL • " + Math.round(player.fallDistance) + " blocks"));
    }

    private static void addMiscLines(List<Line> out, Minecraft client) {
        var cfg = SurvivalUtilsClient.CONFIG;
        var player = client.player;

        if (cfg.showHud(Feature.MEMORY_USAGE)) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024L / 1024L;
            long max = rt.maxMemory() / 1024L / 1024L;
            out.add(info("Memory • " + used + "/" + max + " MB"));
        }

        if (cfg.showHud(Feature.SMART_DASHBOARD)) {
            int warnings = 0;
            if (player.getHealth() <= 6.0F) warnings++;
            if (player.getFoodData().getFoodLevel() <= 5) warnings++;
            if (freeSlots(player) == 0) warnings++;
            if (weakestPercent(player) <= 10) warnings++;
            out.add(new Line("Dashboard • " + (warnings == 0 ? "All good" : warnings + " warning" + (warnings == 1 ? "" : "s")),
                warnings == 0 ? GREEN : YELLOW));
        }

        if (cfg.showHud(Feature.CLOCK_HUD)) {
            out.add(info("Clock • tick " + Math.floorMod(client.level.getDayTime(), 24000L)));
        }

        if (cfg.showHud(Feature.SPRINT_STATE)) {
            out.add(info("Movement • " + (player.isSprinting() ? "Sprinting" : player.isShiftKeyDown() ? "Sneaking" : "Walking")));
        }

        if (cfg.showHud(Feature.GAMEMODE_HUD) && client.gameMode != null) {
            out.add(info("Mode • " + pretty(client.gameMode.getPlayerMode().getName())));
        }

        if (cfg.showHud(Feature.DURABILITY_PERCENTAGES)) {
            ItemStack held = player.getMainHandItem();
            if (held.isDamageableItem()) out.add(info("Held durability • " + durabilityPercent(held) + "%"));
        }

        if (cfg.showHud(Feature.POSITION_COPY_HINT)) {
            out.add(new Line("Pos • " + player.blockPosition().getX() + " " + player.blockPosition().getY() + " " + player.blockPosition().getZ(), AQUA));
        }

        if (cfg.showHud(Feature.DEBUG_MINI)) {
            out.add(info("Mini debug • " + player.chunkPosition().x + "," + player.chunkPosition().z
                + " • light " + client.level.getMaxLocalRawBrightness(player.blockPosition())));
        }
    }

    private static void addSpecificDurability(List<Line> out, net.minecraft.world.entity.player.Player player, Item item, String name) {
        ItemStack stack = findFirst(player, s -> s.is(item));
        if (!stack.isEmpty()) {
            int p = durabilityPercent(stack);
            out.add(new Line(name + " • " + p + "%", p <= 15 ? RED : WHITE));
        }
    }

    private static int lowestArmorPercent(net.minecraft.world.entity.player.Player player) {
        int lowest = 101;
        boolean any = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                any = true;
                lowest = Math.min(lowest, durabilityPercent(stack));
            }
        }
        return any ? lowest : -1;
    }

    private static int durabilityPercent(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 100;
        return Math.max(0, Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
    }

    private static ItemStack weakestDamageable(net.minecraft.world.entity.player.Player player) {
        ItemStack weakest = ItemStack.EMPTY;
        int percent = 101;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int p = durabilityPercent(stack);
                if (p < percent) {
                    percent = p;
                    weakest = stack;
                }
            }
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int p = durabilityPercent(stack);
                if (p < percent) {
                    percent = p;
                    weakest = stack;
                }
            }
        }
        return weakest;
    }

    private static int weakestPercent(net.minecraft.world.entity.player.Player player) {
        ItemStack stack = weakestDamageable(player);
        return stack.isEmpty() ? 100 : durabilityPercent(stack);
    }

    private static int freeSlots(net.minecraft.world.entity.player.Player player) {
        int free = 0;
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) if (player.getInventory().getItem(i).isEmpty()) free++;
        return free;
    }

    private static int countItem(net.minecraft.world.entity.player.Player player, Item item) {
        int total = 0;
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static int countFood(net.minecraft.world.entity.player.Player player) {
        int total = 0;
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.get(DataComponents.FOOD) != null) total += stack.getCount();
        }
        return total;
    }

    private static int countArrows(net.minecraft.world.entity.player.Player player) {
        return countItem(player, Items.ARROW) + countItem(player, Items.SPECTRAL_ARROW) + countItem(player, Items.TIPPED_ARROW);
    }

    private static boolean hasBed(net.minecraft.world.entity.player.Player player) {
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (player.getInventory().getItem(i).getItem() instanceof BedItem) return true;
        }
        return false;
    }

    private static ItemStack findFirst(net.minecraft.world.entity.player.Player player, java.util.function.Predicate<ItemStack> predicate) {
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (predicate.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static int oreTotal(net.minecraft.world.entity.player.Player player) {
        return countItem(player, Items.DIAMOND)
            + countItem(player, Items.RAW_IRON)
            + countItem(player, Items.RAW_GOLD)
            + countItem(player, Items.RAW_COPPER)
            + countItem(player, Items.REDSTONE)
            + countItem(player, Items.LAPIS_LAZULI)
            + countItem(player, Items.EMERALD)
            + countItem(player, Items.COAL)
            + countItem(player, Items.ANCIENT_DEBRIS);
    }

    private static boolean isValuableOre(BlockState state) {
        Item item = state.getBlock().asItem();
        return item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE
            || item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE
            || item == Items.ANCIENT_DEBRIS;
    }

    private static int countEntities(Minecraft client, EntityType<?> type, double radius) {
        return client.level.getEntities(client.player, client.player.getBoundingBox().inflate(radius), e -> e.getType() == type).size();
    }

    private static int matureCrops(Minecraft client, int radius) {
        int count = 0;
        BlockPos center = client.player.blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockState state = client.level.getBlockState(center.offset(x, y, z));
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) count++;
                }
            }
        }
        return count;
    }

    private static String altitudeHint(Minecraft client) {
        int y = client.player.blockPosition().getY();
        String dim = client.level.dimension().identifier().getPath();
        if (dim.equals("overworld") && y <= -45) return " • deep mining";
        if (dim.equals("the_nether") && y >= 8 && y <= 22) return " • debris band";
        if (y >= 150) return " • high altitude";
        return "";
    }

    private static boolean isNight(Minecraft client) {
        long t = Math.floorMod(client.level.getDayTime(), 24000L);
        return t >= 12000L;
    }

    private static String directionTo(double x, double z, double tx, double tz) {
        double dx = tx - x;
        double dz = tz - z;
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) angle += 360;
        if (angle < 22.5 || angle >= 337.5) return "E";
        if (angle < 67.5) return "SE";
        if (angle < 112.5) return "S";
        if (angle < 157.5) return "SW";
        if (angle < 202.5) return "W";
        if (angle < 247.5) return "NW";
        if (angle < 292.5) return "N";
        return "NE";
    }

    private static double horizontalDistance(double x, double z, double tx, double tz) {
        double dx = tx - x;
        double dz = tz - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static String coord(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String formatTicks(long ticks) {
        long seconds = ticks / 20L;
        return formatSeconds(seconds);
    }

    private static String formatSeconds(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        long h = m / 60L;
        m %= 60L;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    private static String pretty(String id) {
        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!b.isEmpty()) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    private static void renderVisor(GuiGraphics graphics, Minecraft client) {
        if (!SurvivalUtilsClient.CONFIG.showHud(Feature.HELMET_VISOR)) return;
        ItemStack helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return;

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int cyan = 0x8835E8FF;
        int dim = 0x3335E8FF;

        graphics.fill(0, 0, w, 1, cyan);
        graphics.fill(0, h - 1, w, h, cyan);
        graphics.fill(0, 0, 1, h, cyan);
        graphics.fill(w - 1, 0, w, h, cyan);

        graphics.fill(8, 8, 44, 10, dim);
        graphics.fill(8, 8, 10, 28, dim);
        graphics.fill(w - 44, 8, w - 8, 10, dim);
        graphics.fill(w - 10, 8, w - 8, 28, dim);

        int armor = lowestArmorPercent(client.player);
        var v = client.player.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
        int hostiles = client.level.getEntitiesOfClass(Monster.class, client.player.getBoundingBox().inflate(16.0)).size();

        String top = String.format(Locale.ROOT,
            "§bVISOR §8| §fHP %.0f §8| §fH %d §8| §fA %s §8| §fSPD %.1f §8| §fHOST %d",
            client.player.getHealth(),
            client.player.getFoodData().getFoodLevel(),
            armor < 0 ? "--" : armor + "%",
            speed,
            hostiles);
        graphics.drawCenteredString(client.font, Component.literal(top), w / 2, 4, WHITE);

        String bottom = "§8XYZ §f" + client.player.blockPosition().getX() + " "
            + client.player.blockPosition().getY() + " " + client.player.blockPosition().getZ()
            + " §8| DIM §f" + pretty(client.level.dimension().identifier().getPath())
            + " §8| L §f" + client.level.getMaxLocalRawBrightness(client.player.blockPosition());
        graphics.drawCenteredString(client.font, Component.literal(bottom), w / 2, h - 10, GRAY);
    }

    private static void renderVoice(GuiGraphics graphics, Minecraft client) {
        if (!SurvivalUtilsClient.CONFIG.showHud(Feature.VOICE_COMMANDS) || !SurvivalUtilsClient.CONFIG.voiceChip()) return;

        int panelW = Math.min(195, Math.max(155, graphics.guiWidth() / 5));
        int x2 = graphics.guiWidth() - 6;
        int x1 = x2 - panelW;
        int y1 = 17;
        int y2 = 62;

        graphics.fill(x1, y1, x2, y2, 0x99101820);
        graphics.fill(x1, y1, x2, y1 + 1, 0xFF35E8FF);
        graphics.fill(x1, y2 - 1, x2, y2, 0xFF35E8FF);
        graphics.fill(x1, y1, x1 + 1, y2, 0xFF35E8FF);
        graphics.fill(x2 - 1, y1, x2, y2, 0xFF35E8FF);

        int statusColor = switch (VoiceCommands.status) {
            case "LISTENING" -> GREEN;
            case "STARTING" -> YELLOW;
            default -> RED;
        };

        graphics.drawString(client.font, "VOICE", x1 + 5, y1 + 4, AQUA, true);
        graphics.drawString(client.font, VoiceCommands.status,
            x2 - 5 - client.font.width(VoiceCommands.status), y1 + 4, statusColor, true);

        String heard = VoiceCommands.lastHeard == null ? "-" : VoiceCommands.lastHeard;
        graphics.drawString(client.font, "§8> §f" + shortenVoice(heard, 20), x1 + 5, y1 + 16, WHITE, false);

        String response = VoiceCommands.lastResponse == null ? "Say 'help'." : VoiceCommands.lastResponse;
        graphics.drawString(client.font, shortenVoice(response, 29), x1 + 5, y1 + 28, GRAY, false);

        graphics.drawString(client.font, "F8", x2 - 18, y2 - 10, 0xFF5A7A85, false);
    }

    private static String shortenVoice(String value, int max) {
        if (value == null) return "-";
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        if (clean.length() <= max) return clean;
        return clean.substring(0, Math.max(1, max - 3)) + "...";
    }

    private static Line info(String text) { return new Line(text, WHITE); }
    private static Line warn(String text) { return new Line(text, RED); }

    private record Line(String text, int color) {}
}
