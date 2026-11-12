package com.dishanhai.ftb_quest_nexus.config;

/** Author: dishanhai. Server settings shared by the config file and administrator command. */
public enum ServerOption {
    DEFAULT_MATCHING("defaults", "matching", false),
    DEFAULT_QUEUED_REWARDS("defaults", "queuedRewards", true),
    DEFAULT_COMPLETION_TOAST("defaults", "completionToast", true),
    AUTO_SUBMIT("rules", "autoSubmit", true),
    AUTO_CHECK("rules", "autoCheck", true),
    NETWORK_REWARDS("rules", "networkRewards", true);

    final String section;
    final String key;
    final boolean defaultValue;

    ServerOption(String section, String key, boolean defaultValue) {
        this.section = section;
        this.key = key;
        this.defaultValue = defaultValue;
    }

    String translationKey() {
        return "config.ftb_quest_nexus.server." + section + "." + key;
    }
}
