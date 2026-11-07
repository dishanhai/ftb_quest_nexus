package com.dishanhai.ftb_quest_nexus.client.glass;

/** Author: dishanhai. Pure rules; safe to share between platform builds and tests. */
public final class GlassUiPolicy {
    private GlassUiPolicy() {}

    public static int scaleAlpha(int argb, double opacity) {
        double scale = Double.isFinite(opacity) ? Math.max(0, Math.min(1, opacity)) : 1;
        return ((int) Math.round((argb >>> 24) * scale) << 24) | (argb & 0xFFFFFF);
    }

    public static boolean isQuestScreenClass(String name) {
        return name != null && (name.startsWith("dev.ftb.mods.ftbquests.client.gui.")
                || name.equals("com.dishanhai.ftb_quest_nexus.client.FtbqChoiceRewardScreen"));
    }

    public static boolean isLibraryScreenClass(String name) {
        return name != null && name.startsWith("dev.ftb.mods.ftblibrary.");
    }
}
