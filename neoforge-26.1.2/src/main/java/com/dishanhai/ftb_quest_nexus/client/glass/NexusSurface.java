package com.dishanhai.ftb_quest_nexus.client.glass;

import com.dishanhai.sh_better_ui.client.api.ShBetterUiApi;
import com.dishanhai.sh_better_ui.client.glass.LiquidGlassSurface;
import com.dishanhai.sh_better_ui.client.gui.scaled.RoundedCorners;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiPreferences;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassMaterial;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassOptics;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassVariant;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Author: dishanhai. Draws Nexus panels with the SHBUI 26.1.2 glass pipeline. */
public final class NexusSurface {
    private NexusSurface() {}

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                             int fill, int edge, GlassVariant variant, float opacity, int blurRadius) {
        if (opacity <= 0 || width <= 0 || height <= 0) return;
        var settings = ShBetterUiApi.settings().current();
        var preferences = UiPreferences.configured();
        if (variant != null && blurRadius != 0 && ShBetterUiApi.blur().prepareBackdrop(graphics)) {
            var optics = settings.optics(GlassMaterial.preset(variant).optics());
            if (blurRadius > 0) {
                float radius = blurRadius * settings.glassBlur() / 100f;
                optics = new GlassOptics(optics.transmission(), optics.refraction(), optics.lensing(),
                        optics.scattering(), optics.edgeHighlight(), optics.thickness(), radius);
            }
            if (LiquidGlassSurface.drawPrepared(graphics, x, y, width, height, 12, fill, edge, optics,
                    preferences, opacity)) return;
        }
        if (preferences.highContrast()) fill |= 0xFF000000;
        RoundedCorners.draw(graphics, x, y, width, height, 12, 1,
                GlassUiPolicy.scaleAlpha(edge, opacity), GlassUiPolicy.scaleAlpha(fill, opacity));
    }
}
