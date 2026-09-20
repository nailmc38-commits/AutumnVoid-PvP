package com.survivalutils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MicSelectScreen extends Screen {
    private final Screen parent;
    private List<String> microphones = new ArrayList<>();
    private int scroll = 0;
    private String pending;
    private static final int VISIBLE = 8;

    public MicSelectScreen(Screen parent) {
        super(Component.literal("Microphone Selector"));
        this.parent = parent;
        this.pending = VoiceCommands.selectedMicDisplay();
    }

    @Override
    protected void init() {
        microphones = VoiceCommands.microphoneNames();
        if (!microphones.contains(pending)) pending = "System Default";
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int listWidth = Math.min(430, this.width - 70);
        int x = (this.width - listWidth) / 2;
        int y = 54;

        int maxScroll = Math.max(0, microphones.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < VISIBLE; i++) {
            int index = scroll + i;
            if (index >= microphones.size()) break;

            String name = microphones.get(index);
            boolean selected = name.equals(pending);
            String shown = name.length() > 54 ? name.substring(0, 51) + "..." : name;
            Component label = Component.literal((selected ? "§b▶ §f" : "§8• §7") + shown);

            addRenderableWidget(Button.builder(label, b -> {
                pending = name;
                rebuild();
            }).bounds(x, y + i * 23, listWidth, 20).build());
        }

        int bottom = Math.min(this.height - 34, y + VISIBLE * 23 + 8);

        addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            if (scroll > 0) {
                scroll--;
                rebuild();
            }
        }).bounds(x - 27, y, 22, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            if (scroll < maxScroll) {
                scroll++;
                rebuild();
            }
        }).bounds(x - 27, y + 24, 22, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§bUSE MIC"), b -> {
            VoiceCommands.selectMicrophone(pending);
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 105, bottom, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(this.width / 2 + 5, bottom, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, microphones.size() - VISIBLE);
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

        int w = Math.min(500, this.width - 30);
        int x = (this.width - w) / 2;
        graphics.fill(x, 8, x + w, 42, 0xAA101820);
        graphics.fill(x, 8, x + w, 9, 0xFF35E8FF);

        graphics.drawCenteredString(this.font,
            Component.literal("§b§lMICROPHONE ARRAY §8// §fSELECT INPUT"),
            this.width / 2, 14, 0xFFFFFFFF);

        String status = VoiceCommands.isRunning()
            ? "§eVoice listener will stop when you apply a different microphone."
            : "§7Selected: §f" + shorten(pending, 55);
        graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, 29, 0xFFB8DCE3);
    }

    private static String shorten(String text, int max) {
        if (text == null) return "System Default";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
