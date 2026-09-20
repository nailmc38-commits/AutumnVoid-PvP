package com.curvebreak.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.curvebreak.CurvebreakMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public final class ModItems {
    private ModItems() {}

    public static final List<Item> ALL = new ArrayList<>();

    public static final Item SCRAP = basic("scrap");
    public static final Item STABILIZER = basic("stabilizer");
    public static final Item QUANTUM_RESIDUE = rare("quantum_residue", Rarity.UNCOMMON);
    public static final Item ALIEN_CRYSTAL = rare("alien_crystal", Rarity.RARE);
    public static final Item RARE_ARTIFACT = rare("rare_artifact", Rarity.EPIC);
    public static final Item CURVE_FRAGMENT = rare("curve_fragment", Rarity.EPIC);
    public static final Item MACHINE_CORE = rare("machine_core", Rarity.UNCOMMON);
    public static final Item FROST_CORE = rare("frost_core", Rarity.UNCOMMON);
    public static final Item AETHER_SHARD = rare("aether_shard", Rarity.UNCOMMON);
    public static final Item NULL_SHARD = rare("null_shard", Rarity.RARE);
    public static final Item RIFT_HEART = rare("rift_heart", Rarity.EPIC);

    public static final Item BASIC_CARTRIDGE = basic("basic_cartridge");
    public static final Item REINFORCED_CARTRIDGE = rare("reinforced_cartridge", Rarity.UNCOMMON);
    public static final Item QUANTUM_CARTRIDGE = rare("quantum_cartridge", Rarity.RARE);
    public static final Item SINGULARITY_CARTRIDGE = rare("singularity_cartridge", Rarity.EPIC);

    public static final Item PORTAL_PROJECTOR_MK1 = traveler("portal_projector_mk1", 1, 180);
    public static final Item PORTAL_PROJECTOR_MK2 = traveler("portal_projector_mk2", 2, 260);
    public static final Item PORTAL_PROJECTOR_MK3 = traveler("portal_projector_mk3", 3, 380);
    public static final Item RIFT_PROJECTOR = traveler("rift_projector", 4, 520);
    public static final Item QUANTUM_PROJECTOR = traveler("quantum_projector", 5, 700);
    public static final Item EDGE_PROJECTOR = traveler("edge_projector", 6, 900);
    public static final Item CURVEBREAK_PROJECTOR = traveler("curvebreak_projector", 7, 1200);

    public static final Item ENVIRONMENT_MODULE = rare("environment_module", Rarity.UNCOMMON);
    public static final Item PRESSURE_MODULE = rare("pressure_module", Rarity.UNCOMMON);
    public static final Item THERMAL_MODULE = rare("thermal_module", Rarity.UNCOMMON);
    public static final Item GRAVITY_MODULE = rare("gravity_module", Rarity.RARE);
    public static final Item PHASE_MODULE = rare("phase_module", Rarity.EPIC);

    private static Item basic(String name) {
        return register(name, Item::new, new Item.Properties());
    }

    private static Item rare(String name, Rarity rarity) {
        return register(name, Item::new, new Item.Properties().rarity(rarity));
    }

    private static Item traveler(String name, int tier, int durability) {
        return register(name, p -> new CurveTravelerItem(tier, p),
                new Item.Properties().stacksTo(1).durability(durability).rarity(tier >= 6 ? Rarity.EPIC : Rarity.RARE));
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        ALL.add(item);
        return item;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(SCRAP);
            entries.accept(STABILIZER);
            entries.accept(QUANTUM_RESIDUE);
            entries.accept(ALIEN_CRYSTAL);
            entries.accept(RARE_ARTIFACT);
            entries.accept(CURVE_FRAGMENT);
            entries.accept(MACHINE_CORE);
            entries.accept(FROST_CORE);
            entries.accept(AETHER_SHARD);
            entries.accept(NULL_SHARD);
            entries.accept(RIFT_HEART);
            entries.accept(BASIC_CARTRIDGE);
            entries.accept(REINFORCED_CARTRIDGE);
            entries.accept(QUANTUM_CARTRIDGE);
            entries.accept(SINGULARITY_CARTRIDGE);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(PORTAL_PROJECTOR_MK1);
            entries.accept(PORTAL_PROJECTOR_MK2);
            entries.accept(PORTAL_PROJECTOR_MK3);
            entries.accept(RIFT_PROJECTOR);
            entries.accept(QUANTUM_PROJECTOR);
            entries.accept(EDGE_PROJECTOR);
            entries.accept(CURVEBREAK_PROJECTOR);
            entries.accept(ENVIRONMENT_MODULE);
            entries.accept(PRESSURE_MODULE);
            entries.accept(THERMAL_MODULE);
            entries.accept(GRAVITY_MODULE);
            entries.accept(PHASE_MODULE);
        });
    }
}
