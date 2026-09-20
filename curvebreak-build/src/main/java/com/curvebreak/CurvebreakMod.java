package com.curvebreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

import com.curvebreak.command.CurveCommands;
import com.curvebreak.block.ModBlocks;
import com.curvebreak.item.ModItems;

public final class CurvebreakMod implements ModInitializer {
    public static final String MOD_ID = "curvebreak";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        CurveCommands.register();
        LOGGER.info("Curvebreak initialized: 20 Curve-linked dimensions online.");
    }
}
