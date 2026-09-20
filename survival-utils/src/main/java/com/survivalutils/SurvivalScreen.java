package com.survivalutils;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SurvivalScreen extends Screen {
    private final Screen parent;
    private Feature.Category category = Feature.Category.HUD;
    private int scroll = 0;
    private int visibleRows = 8;

    public SurvivalScreen(Screen parent) {
        super(Component.literal("Survival Utils"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        visibleRows = Math.max(5, Math.min(11, (this.height - 112) / 23));

        int topY = 25;
        int controlW = Math.max(92, Math.min(135, (this.width - 150) / 5));
        int startX = 112;

        addRenderableWidget(Button.builder(Component.literal("§bMIC §8| §fSELECT"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(new MicSelectScreen(this));
        }).bounds(startX, topY, controlW, 18).build());

        addRenderableWidget(Button.builder(hudSideLabel(), b -> {
            SurvivalUtilsClient.CONFIG.toggleHudSide();
            b.setMessage(hudSideLabel());
        }).bounds(startX + controlW + 4, topY, controlW, 18).build());

        addRenderableWidget(Button.builder(linesLabel(), b -> {
            SurvivalUtilsClient.CONFIG.cycleMaxHudLines();
            b.setMessage(linesLabel());
        }).bounds(startX + (controlW + 4) * 2, topY, controlW, 18).build());

        addRenderableWidget(Button.builder(autoVoiceLabel(), b -> {
            SurvivalUtilsClient.CONFIG.toggleVoiceAutoStart();
            b.setMessage(autoVoiceLabel());
        }).bounds(startX, topY + 21, controlW, 18).build());

        addRenderableWidget(Button.builder(voiceChipLabel(), b -> {
            SurvivalUtilsClient.CONFIG.toggleVoiceChip();
            b.setMessage(voiceChipLabel());
        }).bounds(startX + controlW + 4, topY + 21, controlW, 18).build());

        addRenderableWidget(Button.builder(sensitivityLabel(), b -> {
            SurvivalUtilsClient.CONFIG.cycleVoiceConfidence();
            b.setMessage(sensitivityLabel());
        }).bounds(startX + (controlW + 4) * 2, topY + 21, controlW, 18).build());

        int catY = 72;
        for (Feature.Category cat : Feature.Category.values()) {
            Feature.Category target = cat;
            String label = (cat == category ? "§b▶ §f" : "§8• §7") + cat.title;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                category = target;
                scroll = 0;
                rebuild();
            }).bounds(9, catY, 94, 20).build());
            catY += 22;
        }

        List<Feature> features = features();
        int maxScroll = Math.max(0, features.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int listX = 112;
        int listY = 72;
        int listW = Math.max(210, this.width - listX - 22);

        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= features.size()) break;

            Feature feature = features.get(index);
            addRenderableWidget(Button.builder(modeLabel(feature), b -> {
                SurvivalUtilsClient.CONFIG.cycleMode(feature);

                if (feature == Feature.VOICE_COMMANDS && !SurvivalUtilsClient.CONFIG.isEnabled(feature)) {
                    VoiceCommands.stop();
                }

                b.setMessage(modeLabel(feature));
            }).bounds(listX, listY + i * 23, listW, 20).build());
        }

        int bottomY = Math.min(this.height - 28, listY + visibleRows * 23 + 4);
        addRenderableWidget(Button.builder(Component.literal("§bDONE"), b -> onClose())
            .bounds(this.width - 78, bottomY, 68, 20).build());
    }

    private List<Feature> features() {
        return Arrays.stream(Feature.values()).filter(f -> f.category == category).toList();
    }

    private static Component modeLabel(Feature feature) {
        DisplayMode mode = SurvivalUtilsClient.CONFIG.mode(feature);
        String color = switch (mode) {
            case OFF -> "§8";
            case HUD -> "§b";
            case VOICE -> "§d";
            case BOTH -> "§a";
        };
        String suffix = SurvivalConfig.supportsVoice(feature) ? "" : " §8[visual]";
        return Component.literal(color + "[" + mode.label + "] §f" + feature.title + suffix);
    }

    private static Component hudSideLabel() {
        return Component.literal("§7HUD §8| §f" + SurvivalUtilsClient.CONFIG.hudSide());
    }

    private static Component linesLabel() {
        return Component.literal("§7LINES §8| §f" + SurvivalUtilsClient.CONFIG.maxHudLines());
    }

    private static Component autoVoiceLabel() {
        return Component.literal("§7AUTO MIC §8| " + (SurvivalUtilsClient.CONFIG.voiceAutoStart() ? "§aON" : "§cOFF"));
    }

    private static Component voiceChipLabel() {
        return Component.literal("§7VOICE CHIP §8| " + (SurvivalUtilsClient.CONFIG.voiceChip() ? "§aON" : "§cOFF"));
    }

    private static Component sensitivityLabel() {
        int pct = (int) Math.round(SurvivalUtilsClient.CONFIG.voiceConfidence() * 100.0);
        return Component.literal("§7VOICE SENS §8| §f" + pct + "%");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<Feature> features = features();
        int maxScroll = Math.max(0, features.size() - visibleRows);
        int old = scroll;

        if (verticalAmount > 0) scroll = Math.max(0, scroll - 1);
        if (verticalAmount < 0) scroll = Math.min(maxScroll, scroll + 1);

        if (old != scroll) {
            rebuild();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, this.width, 21, 0xAA0C151B);
        graphics.fill(0, 20, this.width, 21, 0xFF35E8FF);

        graphics.drawString(this.font,
            Component.literal("§b§lSURVIVAL UTILS §8// §fVISOR CONTROL"),
            10, 7, 0xFFFFFFFF, false);

        graphics.drawString(this.font,
            Component.literal("§7" + SurvivalUtilsClient.CONFIG.enabledCount() + "/" + Feature.values().length + " active"),
            this.width - 72, 7, 0xFFB8DCE3, false);

        graphics.drawString(this.font,
            Component.literal("§7" + category.title + " §8• §fScroll wheel to browse"),
            112, 62, 0xFF9DB4BC, false);

        List<Feature> features = features();
        if (features.size() > visibleRows) {
            int trackX = this.width - 7;
            int top = 72;
            int height = visibleRows * 23 - 3;
            graphics.fill(trackX, top, trackX + 2, top + height, 0x5535E8FF);

            int maxScroll = Math.max(1, features.size() - visibleRows);
            int thumbH = Math.max(16, height * visibleRows / features.size());
            int thumbY = top + (height - thumbH) * scroll / maxScroll;
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFF35E8FF);
        }

        int footerY = this.height - 12;
        graphics.drawCenteredString(this.font,
            Component.literal("§8Click feature: §7OFF §8→ §bHUD §8→ §dVOICE §8→ §aBOTH   §8|   F8 mic toggle • F9 menu"),
            this.width / 2, footerY, 0xFFA7C2CA);
    }

    @Override
    public void onClose() {
        SurvivalUtilsClient.CONFIG.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
