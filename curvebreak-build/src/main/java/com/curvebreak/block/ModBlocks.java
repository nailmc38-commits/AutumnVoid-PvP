package com.curvebreak.block;

import java.util.function.Function;

import com.curvebreak.CurvebreakMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block CURVE_WORKBENCH = register(
        "curve_workbench",
        CraftingTableBlock::new,
        BlockBehaviour.Properties.of().strength(4.0F).requiresCorrectToolForDrops()
    );

    public static final Block RIFT_ANCHOR = register(
        "rift_anchor",
        Block::new,
        BlockBehaviour.Properties.of().strength(5.0F).lightLevel(state -> 8)
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        Block block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return block;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(CURVE_WORKBENCH.asItem());
            entries.accept(RIFT_ANCHOR.asItem());
        });
    }
}
