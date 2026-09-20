package com.survivalutils;

public enum DisplayMode {
    OFF("OFF"),
    HUD("HUD"),
    VOICE("VOICE"),
    BOTH("BOTH");

    public final String label;

    DisplayMode(String label) {
        this.label = label;
    }

    public DisplayMode next(boolean voiceSupported) {
        if (!voiceSupported) {
            return this == OFF ? HUD : OFF;
        }
        return switch (this) {
            case OFF -> HUD;
            case HUD -> VOICE;
            case VOICE -> BOTH;
            case BOTH -> OFF;
        };
    }

    public boolean hud() {
        return this == HUD || this == BOTH;
    }

    public boolean voice() {
        return this == VOICE || this == BOTH;
    }

    public boolean active() {
        return this != OFF;
    }
}
