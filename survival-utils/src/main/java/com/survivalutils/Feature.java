package com.survivalutils;

public enum Feature {
    DAY_NIGHT_HUD(Category.HUD, "Day / Night Timer", "Time until sunset or sunrise.", true),
    COORDS_HUD(Category.HUD, "Coordinates", "Compact XYZ display.", true),
    BIOME_HUD(Category.HUD, "Biome", "Shows your current biome.", true),
    ARMOR_HUD(Category.HUD, "Armor Durability", "Shows equipped armor durability.", true),
    EFFECT_TIMERS(Category.HUD, "Potion Timers", "Shows active effect timers.", true),
    FOOD_HUD(Category.HUD, "Food Details", "Shows food and saturation info.", false),
    XP_HUD(Category.HUD, "XP Details", "Shows level and XP progress.", false),
    FPS_HUD(Category.HUD, "FPS", "Shows current FPS.", false),
    PING_HUD(Category.HUD, "Ping", "Shows multiplayer latency.", false),
    SPEED_HUD(Category.HUD, "Movement Speed", "Shows horizontal speed.", false),
    DIRECTION_HUD(Category.HUD, "Direction", "Shows cardinal facing.", false),
    WEATHER_HUD(Category.HUD, "Weather", "Shows clear/rain/thunder.", false),
    MOON_PHASE_HUD(Category.HUD, "Moon Phase", "Shows the current moon phase.", false),
    CHUNK_HUD(Category.HUD, "Chunk Coordinates", "Shows current chunk coordinates.", false),
    TARGET_BLOCK_HUD(Category.HUD, "Target Block", "Shows the block you are looking at.", false),
    HELD_ITEM_COUNT(Category.HUD, "Held Item Count", "Shows total count of held item in inventory.", false),
    DIMENSION_HUD(Category.HUD, "Dimension", "Shows current dimension.", false),
    SESSION_TIMER(Category.HUD, "Session Timer", "Shows time in the current world session.", false),
    HELMET_VISOR(Category.HUD, "Helmet Visor", "Adds a holographic suit-style visor when a helmet is equipped.", true),
    VOICE_COMMANDS(Category.HUD, "Voice Commands", "Listens for fixed local voice commands through your Windows microphone.", true),

    DURABILITY_WARNINGS(Category.INVENTORY, "Durability Warnings", "Warns when held or equipped gear is low.", true),
    TOTEM_ALERT(Category.INVENTORY, "Totem Alert", "Warns when no totem is in your inventory.", false),
    INVENTORY_SPACE(Category.INVENTORY, "Inventory Space", "Shows free inventory slots.", false),
    INVENTORY_FULL_ALERT(Category.INVENTORY, "Inventory Full Alert", "Warns when inventory fills up.", true),
    HOTBAR_COUNTS(Category.INVENTORY, "Hotbar Counts", "Shows stack counts for hotbar items.", false),
    FOOD_COUNT(Category.INVENTORY, "Food Count", "Counts edible items.", false),
    TORCH_COUNT(Category.INVENTORY, "Torch Count", "Counts torches.", false),
    ARROW_COUNT(Category.INVENTORY, "Arrow Count", "Counts arrows.", false),
    ROCKET_COUNT(Category.INVENTORY, "Rocket Count", "Counts firework rockets.", false),
    PEARL_COUNT(Category.INVENTORY, "Pearl Count", "Counts ender pearls.", false),
    BED_ALERT(Category.INVENTORY, "Bed Check", "Warns when night approaches and no bed is carried.", false),
    WATER_BUCKET_ALERT(Category.INVENTORY, "Water Bucket Check", "Warns when no water bucket is carried.", false),
    AUTO_REFILL_HOTBAR(Category.INVENTORY, "Auto Refill Hotbar", "Refills an emptied hotbar stack from inventory.", false),
    INVENTORY_ORGANIZER(Category.INVENTORY, "Inventory Organizer", "Keeps similar stacks grouped when possible.", false),
    MENDING_REMINDER(Category.INVENTORY, "Mending Reminder", "Highlights damaged Mending gear while holding XP.", false),

    WAYPOINTS(Category.EXPLORATION, "Waypoints", "Local waypoint HUD support.", false),
    DEATH_WAYPOINT(Category.EXPLORATION, "Death Waypoint", "Remembers your last death position.", true),
    PORTAL_MEMORY(Category.EXPLORATION, "Portal Memory", "Remembers recent Nether portal coordinates.", false),
    JOURNEY_STATS(Category.EXPLORATION, "Journey Stats", "Tracks distance traveled this session.", false),
    ALTITUDE_HINT(Category.EXPLORATION, "Altitude Hint", "Useful Y-level hints for your dimension.", false),
    NETHER_COORD_CONVERTER(Category.EXPLORATION, "Nether Coordinate Converter", "Shows Overworld/Nether coordinate conversion.", false),
    COMPASS_HOME(Category.EXPLORATION, "Spawn Direction", "Shows direction and distance to world spawn.", false),
    DISTANCE_FROM_SPAWN(Category.EXPLORATION, "Distance From Spawn", "Shows horizontal distance from spawn.", false),
    LIGHT_WARNING(Category.EXPLORATION, "Low Light Warning", "Warns about dark blocks around you.", false),
    SAFE_SLEEP_ALERT(Category.EXPLORATION, "Sleep Reminder", "Reminds you when sleeping becomes useful.", false),
    WORLD_DAY_COUNTER(Category.EXPLORATION, "World Day Counter", "Shows the current world day.", false),
    CHUNK_BORDER_DISTANCE(Category.EXPLORATION, "Chunk Edge Distance", "Shows distance to the nearest chunk edge.", false),

    TOOL_RECOMMENDATION(Category.MINING, "Tool Recommendation", "Suggests the appropriate tool for targeted blocks.", false),
    PICKAXE_DURABILITY(Category.MINING, "Pickaxe Durability", "Dedicated pickaxe durability display.", false),
    ORE_SESSION_TRACKER(Category.MINING, "Ore Session Tracker", "Tracks ore blocks you mine this session.", false),
    DIAMOND_Y_HINT(Category.MINING, "Diamond Y Hint", "Shows how close you are to common diamond mining depth.", false),
    ANCIENT_DEBRIS_Y_HINT(Category.MINING, "Ancient Debris Y Hint", "Shows how close you are to common debris mining depth.", false),
    MINING_SESSION_TIMER(Category.MINING, "Mining Timer", "Tracks time spent below ground.", false),
    TORCH_INTERVAL(Category.MINING, "Torch Spacing Hint", "Reminds you about lighting while tunneling.", false),
    INVENTORY_ORE_SUMMARY(Category.MINING, "Ore Inventory Summary", "Counts common ores and raw materials.", false),
    FORTUNE_REMINDER(Category.MINING, "Fortune Reminder", "Warns when mining valuable ore without Fortune.", false),
    SILK_TOUCH_REMINDER(Category.MINING, "Silk Touch Reminder", "Shows when Silk Touch would preserve a block.", false),

    CROP_READY_COUNT(Category.FARMING, "Crop Ready Count", "Counts mature nearby crops.", false),
    BONE_MEAL_COUNT(Category.FARMING, "Bone Meal Count", "Counts bone meal in inventory.", false),
    SEED_COUNT(Category.FARMING, "Seed Count", "Counts common crop seeds.", false),
    HOE_DURABILITY(Category.FARMING, "Hoe Durability", "Dedicated hoe durability display.", false),
    HARVEST_REMINDER(Category.FARMING, "Harvest Reminder", "Alerts when many nearby crops are mature.", false),
    COMPOSTER_REMINDER(Category.FARMING, "Composter Reminder", "Shows compostable item count.", false),
    FARM_LIGHT_WARNING(Category.FARMING, "Farm Light Warning", "Warns when your crop area is unusually dark.", false),
    ANIMAL_FOOD_HELPER(Category.FARMING, "Animal Food Helper", "Shows common breeding food you are carrying.", false),

    VILLAGER_HELPER(Category.VILLAGERS, "Villager Helper", "Shows nearby villager count and useful context.", false),
    EMERALD_COUNT(Category.VILLAGERS, "Emerald Count", "Counts emeralds.", false),
    TRADE_ITEM_CHECK(Category.VILLAGERS, "Trade Item Check", "Shows common trade materials you carry.", false),
    RAID_WARNING(Category.VILLAGERS, "Raid Warning", "Shows active raid status when nearby.", true),
    HERO_TIMER(Category.VILLAGERS, "Hero Timer", "Shows Hero of the Village effect time.", false),

    BLOCK_COUNT(Category.BUILDING, "Selected Block Count", "Counts the block in your hand.", false),
    PALETTE_COUNTS(Category.BUILDING, "Palette Counts", "Summarizes common building blocks in inventory.", false),
    SCAFFOLD_COUNT(Category.BUILDING, "Scaffolding Count", "Counts scaffolding.", false),
    BUILD_HEIGHT(Category.BUILDING, "Build Height", "Shows current Y and build-limit proximity.", false),
    BUILD_LIGHT(Category.BUILDING, "Build Light", "Shows local brightness while building.", false),

    LOW_HEALTH_ALERT(Category.COMBAT, "Low Health Alert", "Large warning at low health.", true),
    LOW_HUNGER_ALERT(Category.COMBAT, "Low Hunger Alert", "Warns when hunger is low.", true),
    SHIELD_DURABILITY(Category.COMBAT, "Shield Durability", "Warns about a weak shield.", true),
    BOW_DURABILITY(Category.COMBAT, "Bow Durability", "Warns about weak bows/crossbows.", false),
    ELYTRA_DURABILITY(Category.COMBAT, "Elytra Durability", "Warns before your elytra becomes dangerous.", true),
    HOSTILES_NEARBY(Category.COMBAT, "Hostiles Nearby", "Shows nearby hostile mob count.", false),
    CREEPER_ALERT(Category.COMBAT, "Creeper Alert", "Warns about close creepers.", true),
    SKELETON_ALERT(Category.COMBAT, "Skeleton Alert", "Warns about close skeletons.", false),
    FIRE_ALERT(Category.COMBAT, "Fire Alert", "Warns while burning.", true),
    DROWNING_ALERT(Category.COMBAT, "Drowning Alert", "Warns when air is getting low.", true),
    FREEZE_ALERT(Category.COMBAT, "Freeze Alert", "Warns when freezing.", true),
    FALL_ALERT(Category.COMBAT, "Fall Alert", "Warns when falling a dangerous distance.", false),

    MEMORY_USAGE(Category.MISC, "Memory Usage", "Shows Java memory use.", false),
    SMART_DASHBOARD(Category.MISC, "Smart Dashboard", "Combines your most important warnings.", true),
    CLOCK_HUD(Category.MISC, "Clock", "Shows Minecraft clock time.", false),
    SPRINT_STATE(Category.MISC, "Sprint State", "Shows sprinting/sneaking state.", false),
    GAMEMODE_HUD(Category.MISC, "Game Mode", "Shows your current game mode.", false),
    DURABILITY_PERCENTAGES(Category.MISC, "Durability Percentages", "Uses percentages in gear readouts.", true),
    POSITION_COPY_HINT(Category.MISC, "Position Copy Hint", "Shows a compact copy-friendly position line.", false),
    DEBUG_MINI(Category.MISC, "Mini Debug", "Small one-line debug summary.", false);

    public final Category category;
    public final String title;
    public final String description;
    public final boolean defaultEnabled;

    Feature(Category category, String title, String description, boolean defaultEnabled) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.defaultEnabled = defaultEnabled;
    }

    public enum Category {
        HUD("HUD"),
        INVENTORY("Inventory"),
        EXPLORATION("Exploration"),
        MINING("Mining"),
        FARMING("Farming"),
        VILLAGERS("Villagers"),
        BUILDING("Building"),
        COMBAT("Combat"),
        MISC("Misc");

        public final String title;
        Category(String title) { this.title = title; }
    }
}
