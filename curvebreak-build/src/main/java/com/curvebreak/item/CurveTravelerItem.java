package com.curvebreak.item;

import java.util.Set;

import com.curvebreak.CurvebreakMod;
import com.curvebreak.dimension.CurveDimension;
import com.curvebreak.dimension.CurveDimensions;
import com.curvebreak.progression.CurvePlayerState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class CurveTravelerItem extends Item {
    private final int tier;

    public CurveTravelerItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            cycleDestination(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        CurveDimension destination = CurveDimensions.find(CurvePlayerState.selected(player))
            .orElse(CurveDimensions.ALL.getFirst());

        if (destination.requiredTier() > tier) {
            player.displayClientMessage(
                Component.literal("Projector tier too low for " + destination.displayName())
                    .withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResult.FAIL;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(CurvebreakMod.MOD_ID, destination.id())
        );
        ServerLevel target = serverPlayer.getServer().getLevel(dimensionKey);
        if (target == null) {
            player.displayClientMessage(
                Component.literal("Curve destination unavailable: " + destination.displayName())
                    .withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResult.FAIL;
        }

        var spawn = target.getSharedSpawnPos();
        boolean moved = serverPlayer.teleportTo(
            target,
            spawn.getX() + 0.5D,
            Math.max(spawn.getY() + 1.0D, 80.0D),
            spawn.getZ() + 0.5D,
            Set.<Relative>of(),
            serverPlayer.getYRot(),
            serverPlayer.getXRot(),
            true
        );

        if (!moved) {
            player.displayClientMessage(Component.literal("The Curve failed to stabilize.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (CurvePlayerState.markVisited(player, destination.id())) {
            int reward = 150 * destination.requiredTier();
            CurvePlayerState.addCredits(player, reward);
            player.sendSystemMessage(Component.literal("Discovery reward: +" + reward + " Credits")
                .withStyle(ChatFormatting.GOLD));
        }

        player.displayClientMessage(
            Component.literal("Arrived: " + destination.displayName() + " • Stability " + destination.stability() + "%")
                .withStyle(ChatFormatting.AQUA),
            true
        );
        return InteractionResult.SUCCESS;
    }

    private void cycleDestination(ServerPlayer player) {
        var unlocked = CurveDimensions.unlockedForTier(tier);
        String current = CurvePlayerState.selected(player);
        int currentIndex = 0;
        for (int i = 0; i < unlocked.size(); i++) {
            if (unlocked.get(i).id().equals(current)) {
                currentIndex = i;
                break;
            }
        }

        CurveDimension next = unlocked.get((currentIndex + 1) % unlocked.size());
        CurvePlayerState.selected(player, next.id());
        player.displayClientMessage(
            Component.literal("Selected: " + next.displayName() + " • Stability " + next.stability() + "%")
                .withStyle(ChatFormatting.LIGHT_PURPLE),
            true
        );
    }
}
