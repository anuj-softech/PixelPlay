package com.rock.pixelplay.player;

public class PlayerSettingConfig {
    public boolean nightMode;
    public float playbackSpeed;
    public boolean aiSubtitles;

    public PlayerSettingConfig(boolean nightMode, float playbackSpeed, boolean aiSubtitles) {
        this.nightMode = nightMode;
        this.playbackSpeed = playbackSpeed;
        this.aiSubtitles = aiSubtitles;
    }
}
