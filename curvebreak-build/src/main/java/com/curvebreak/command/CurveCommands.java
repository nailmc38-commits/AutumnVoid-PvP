package com.curvebreak.command;

import com.curvebreak.dimension.CurveDimensions;
import com.curvebreak.item.ModItems;
import com.curvebreak.progression.CurvePlayerState;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CurveCommands {
    private CurveCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("curve")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Curve destinations: " + String.join(", ", CurveDimensions.ALL.stream().map(d -> d.displayName()).toList())
                        ).withStyle(ChatFormatting.AQUA), false);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String selected = CurvePlayerState.selected(player);
                        int credits = CurvePlayerState.credits(player);
                        int visited = CurvePlayerState.visitedCount(player);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Selected: " + selected + " | Credits: " + credits + " | Discovered: " + visited + "/20"
                        ), false);
                        return 1;
                    }))
                .then(Commands.literal("jobs")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Jobs: Explorer (discover dimensions), Salvager (collect Scrap), Researcher (craft modules), Surveyor (reach unstable worlds)."
                        ).withStyle(ChatFormatting.GREEN), false);
                        return 1;
                    }))
                .then(Commands.literal("research")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Research progression is built into projector tiers: MK-I → MK-II → MK-III → Rift → Quantum → Edge → Curvebreak."
                        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                        return 1;
                    }))
                .then(Commands.literal("shop")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Module Shop — environment: 300, pressure: 450, thermal: 450, gravity: 750, phase: 1200 Credits. Use /curve buy <name>."
                        ).withStyle(ChatFormatting.GOLD), false);
                        return 1;
                    }))
                .then(Commands.literal("buy")
                    .then(Commands.argument("module", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String s : new String[]{"environment", "pressure", "thermal", "gravity", "phase"}) builder.suggest(s);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String module = StringArgumentType.getString(ctx, "module");
                            Item item;
                            int price;
                            switch (module) {
                                case "environment" -> { item = ModItems.ENVIRONMENT_MODULE; price = 300; }
                                case "pressure" -> { item = ModItems.PRESSURE_MODULE; price = 450; }
                                case "thermal" -> { item = ModItems.THERMAL_MODULE; price = 450; }
                                case "gravity" -> { item = ModItems.GRAVITY_MODULE; price = 750; }
                                case "phase" -> { item = ModItems.PHASE_MODULE; price = 1200; }
                                default -> {
                                    ctx.getSource().sendFailure(Component.literal("Unknown module."));
                                    return 0;
                                }
                            }
                            if (!CurvePlayerState.spendCredits(player, price)) {
                                ctx.getSource().sendFailure(Component.literal("Not enough Credits."));
                                return 0;
                            }
                            if (!player.getInventory().add(new ItemStack(item))) {
                                player.drop(new ItemStack(item), false);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Purchased " + module + " module for " + price + " Credits."), false);
                            return 1;
                        })))
                .then(Commands.literal("select")
                    .then(Commands.argument("dimension", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (var dim : CurveDimensions.ALL) builder.suggest(dim.id());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "dimension");
                            var dim = CurveDimensions.find(id);
                            if (dim.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("Unknown Curve dimension: " + id));
                                return 0;
                            }
                            CurvePlayerState.selected(player, dim.get().id());
                            ctx.getSource().sendSuccess(() -> Component.literal("Portal destination set to " + dim.get().displayName()), false);
                            return 1;
                        })))
            )
        );
    }
}
