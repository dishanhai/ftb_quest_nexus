package com.dishanhai.ftb_quest_nexus.config;

import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/** Author: dishanhai. Server configuration with NeoForge's world override support. */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    private static final Map<ServerOption, ModConfigSpec.BooleanValue> VALUES = new EnumMap<>(ServerOption.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (ServerOption option : ServerOption.values()) {
            VALUES.put(option, builder.translation(option.translationKey())
                    .define(option.section + "." + option.key, option.defaultValue));
        }
        SPEC = builder.build();
    }

    private ServerConfig() {}

    public static boolean get(ServerOption option) {
        return SPEC.isLoaded() ? VALUES.get(option).get() : option.defaultValue;
    }

    static boolean save(ServerOption option, boolean value) {
        if (!SPEC.isLoaded()) return false;
        var setting = VALUES.get(option);
        boolean previous = setting.get();
        if (previous == value) return true;
        try {
            setting.set(value);
            SPEC.save();
            return true;
        } catch (WritingException error) {
            try {
                setting.set(previous);
            } catch (WritingException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            LogUtils.getLogger().warn("Could not save {}", option.key, error);
            return false;
        } finally {
            SPEC.afterReload();
        }
    }
}
