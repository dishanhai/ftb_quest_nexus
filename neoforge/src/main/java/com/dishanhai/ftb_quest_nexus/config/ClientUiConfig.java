package com.dishanhai.ftb_quest_nexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Author: dishanhai. Client configuration has no references to client-only classes. */
public final class ClientUiConfig {
    public enum Mode { ORIGINAL, GLASS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.IntValue BLUR_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("config.ftb_quest_nexus.ftbq_ui").push("ftbqUi");
        MODE = builder.translation("config.ftb_quest_nexus.ftbq_ui.mode")
                .defineEnum("mode", Mode.ORIGINAL);
        OPACITY = builder.translation("config.ftb_quest_nexus.ftbq_ui.opacity")
                .defineInRange("opacity", 1.0, 0.0, 1.0);
        BLUR_RADIUS = builder.translation("config.ftb_quest_nexus.ftbq_ui.blur_radius")
                .defineInRange("blurRadius", 8, 0, 16);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientUiConfig() {}
}
