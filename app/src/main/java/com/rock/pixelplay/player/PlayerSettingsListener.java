package com.rock.pixelplay.player;

public interface PlayerSettingsListener {
    void onSettingsChanged(PlayerSettingConfig config);
    void onClose();
}
