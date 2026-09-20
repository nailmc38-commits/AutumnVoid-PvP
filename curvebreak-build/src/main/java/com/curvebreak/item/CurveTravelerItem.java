package com.curvebreak.item;

import com.curvebreak.dimension.CurveDimension;
import com.curvebreak.dimension.CurveDimensions;
import com.curvebreak.progression.CurvePlayerState;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
        serverPlayer.displayClientMessage(
            Component.literal("Curve destination: " + next.displayName() + " • Stability " + next.stability() + "%")
                .withStyle(ChatFormatting.LIGHT_PURPLE),
            true
        );
        return InteractionResult.SUCCESS;
    }
}
