package com.survivalutils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BionicScreen extends Screen {
    private final Screen parent;
    private EditBox input;

    public BionicScreen(Screen parent) {
        super(Component.literal("Bionic Assistant"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(500, this.width - 40);
        int x = (this.width - w) / 2;
        int y = this.height - 58;

        input = new EditBox(this.font, x, y, w - 84, 20, Component.literal("Ask Bionic"));
        input.setMaxLength(300);
        input.setHint(Component.literal("Ask about your inventory, armor, surroundings, or Minecraft..."));
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.literal("SEND"), b -> send())
            .bounds(x + w - 78, y, 78, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(this.width / 2 - 35, this.height - 30, 70, 20)
            .build());

        setInitialFocus(input);
    }

    private void send() {
        if (this.minecraft == null || input == null) return;
        String q = input.getValue();
        if (q == null || q.isBlank()) return;
        BionicAssistant.ask(this.minecraft, q);
        input.setValue("");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int panelW = Math.min(560, this.width - 50);
        int panelX = (this.width - panelW) / 2;
        int panelY = 36;
        int panelH = Math.max(110, this.height - 110);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xB5121B22);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF35E8FF);
        graphics.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0xFF35E8FF);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelH, 0xFF35E8FF);
        graphics.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, 0xFF35E8FF);

        graphics.drawCenteredString(this.font,
            Component.literal("§b§lBIONIC §7// §fSURVIVAL ASSISTANT"),
            this.width / 2, 12, 0xFFFFFFFF);

        graphics.drawString(this.font,
            Component.literal("§7STATUS: " + statusColor() + BionicAssistant.status
                + " §8| §7MODEL: §f" + shorten(BionicAssistant.modelName, 42)),
            panelX + 10, panelY + 10, 0xFFFFFFFF, false);

        int y = panelY + 32;
        graphics.drawString(this.font, Component.literal("§bYOU"), panelX + 10, y, 0xFFFFFFFF, false);
        y += 13;
        y = drawWrapped(graphics, BionicAssistant.lastQuestion, panelX + 10, y, panelW - 20, 0xFFD8F8FF, 4);

        y += 9;
        graphics.drawString(this.font, Component.literal("§bBIONIC"), panelX + 10, y, 0xFFFFFFFF, false);
        y += 13;
        drawWrapped(graphics, BionicAssistant.lastAnswer, panelX + 10, y, panelW - 20, 0xFFFFFFFF, 9);
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color, int maxLines) {
        if (text == null) return y;
        var lines = this.font.split(Component.literal(text), width);
        int shown = Math.min(maxLines, lines.size());
        for (int i = 0; i < shown; i++) {
            graphics.drawString(this.font, lines.get(i), x, y, color, false);
            y += this.font.lineHeight + 2;
        }
        return y;
    }

    private static String statusColor() {
        return switch (BionicAssistant.status) {
            case "ONLINE" -> "§a";
            case "THINKING" -> "§e";
            default -> "§c";
        };
    }

    private static String shorten(String value, int max) {
        if (value == null) return "unknown";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 3)) + "...";
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
