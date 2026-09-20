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
    private int page = 0;
    private static final int PAGE_SIZE = 12;

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

        int categories = Feature.Category.values().length;
        int tabWidth = Math.max(62, Math.min(92, (this.width - 20) / categories));
        int totalWidth = tabWidth * categories;
        int startX = (this.width - totalWidth) / 2;
        int x = startX;

        for (Feature.Category cat : Feature.Category.values()) {
            Feature.Category target = cat;
            String label = (cat == category ? "§b" : "§7") + cat.title;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                category = target;
                page = 0;
                rebuild();
            }).bounds(x, 24, tabWidth - 2, 20).build());
            x += tabWidth;
        }

        List<Feature> features = Arrays.stream(Feature.values())
            .filter(f -> f.category == category)
            .toList();

        int maxPage = Math.max(0, (features.size() - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;

        int first = page * PAGE_SIZE;
        int last = Math.min(features.size(), first + PAGE_SIZE);

        int buttonWidth = Math.min(250, Math.max(180, this.width / 2 - 30));
        int left = this.width / 2 - buttonWidth - 6;
        int right = this.width / 2 + 6;
        int y = 58;

        for (int i = first; i < last; i++) {
            Feature feature = features.get(i);
            int local = i - first;
            int bx = (local % 2 == 0) ? left : right;
            int by = y + (local / 2) * 24;
            addRenderableWidget(Button.builder(labelFor(feature), b -> {
                SurvivalUtilsClient.CONFIG.toggle(feature);
                b.setMessage(labelFor(feature));
            }).bounds(bx, by, buttonWidth, 20).build());
        }

        int bottom = this.height - 34;
        addRenderableWidget(Button.builder(Component.literal("◀ Prev"), b -> {
            if (page > 0) {
                page--;
                rebuild();
            }
        }).bounds(this.width / 2 - 150, bottom, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next ▶"), b -> {
            if (page < maxPage) {
                page++;
                rebuild();
            }
        }).bounds(this.width / 2 + 80, bottom, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(this.width / 2 - 35, bottom, 70, 20).build());
    }

    private static Component labelFor(Feature feature) {
        boolean on = SurvivalUtilsClient.CONFIG.isEnabled(feature);
        return Component.literal((on ? "§aON §8| §f" : "§cOFF §8| §7") + feature.title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font,
            Component.literal("§b§lSurvival Utils §7• §f" + SurvivalUtilsClient.CONFIG.enabledCount()
                + "§7/§f" + Feature.values().length + " enabled"),
            this.width / 2, 7, 0xFFFFFFFF);

        List<Feature> features = Arrays.stream(Feature.values()).filter(f -> f.category == category).toList();
        int maxPage = Math.max(1, (features.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        graphics.drawCenteredString(this.font,
            Component.literal("§7" + category.title + " • Page " + (page + 1) + "/" + maxPage + " • F9 opens this menu"),
            this.width / 2, 47, 0xFFB0B0B0);
    }

    @Override
    public void onClose() {
        SurvivalUtilsClient.CONFIG.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
