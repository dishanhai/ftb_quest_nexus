package com.dishanhai.ftb_quest_nexus.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Author: dishanhai. Only client appearance; never changes server quest rules. */
public final class ClientUiConfig {
    public enum Mode { ORIGINAL, GLASS }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Mode> MODE;
    public static final ForgeConfigSpec.DoubleValue OPACITY;
    public static final ForgeConfigSpec.IntValue BLUR_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.translation("config.ftb_quest_nexus.ftbq_ui").push("ftbqUi");
        MODE = builder.translation("config.ftb_quest_nexus.ftbq_ui.mode").defineEnum("mode", Mode.ORIGINAL);
        OPACITY = builder.translation("config.ftb_quest_nexus.ftbq_ui.opacity").defineInRange("opacity", 1.0, 0.0, 1.0);
        BLUR_RADIUS = builder.translation("config.ftb_quest_nexus.ftbq_ui.blur_radius").defineInRange("blurRadius", 8, 0, 16);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientUiConfig() {}
}
