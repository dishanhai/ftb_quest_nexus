package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.network.ChemicalRewardPackets;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiConfirmation;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiOverlayHost;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiPreferences;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Author: dishanhai. SH-Better-UI confirmation for a server-owned chemical reward. */
public final class ChemicalRewardWarning {
    private static final String LANG = "reward.ftb_quest_nexus.mek_chemical.warning.";
    // The library registers its overlay weakly. Retain it until the modal closes.
    private static UiOverlayHost active;

    private ChemicalRewardWarning() {}

    public static void open(ChemicalRewardPackets.Offer offer) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Component title = Component.translatable(LANG + "title");
        Component chemical = ChemicalTask.nameOf(offer.chemicalId());
        if (!offer.canPackage()) {
            UiToast.show(UiToast.Kind.WARNING, title,
                    Component.translatable(LANG + "cannot_package", chemical, offer.amount()));
            return;
        }

        Screen parent = client.screen;
        UiOverlayHost current = UiOverlayHost.current(parent);
        if (current != null && current == active && current.isModalOpen()) return;
        if (client.getOverlay() != null || parent instanceof ConfirmScreen || current != null
                || parent != null && parent.isDragging()) {
            showReopenNotice(title);
            return;
        }

        if (parent == null) {
            parent = new GameplayHost(title);
            client.setScreen(parent);
            if (client.screen != parent) {
                showReopenNotice(title);
                return;
            }
        }

        var overlay = new UiOverlayHost(parent, UiPreferences.DEFAULT);
        overlay.setModalLifecycle(() -> active = overlay, ignored -> {
            if (active == overlay) active = null;
        });
        var prompt = new UiConfirmation.Prompt(title, Component.translatable(LANG + "details", chemical, offer.amount()),
                Component.translatable(LANG + "package"), Component.translatable(LANG + "later"));
        overlay.showDialog(prompt,
                () -> ClientPacketDistributor.sendToServer(new ChemicalRewardPackets.Selection(offer.deliveryId())));
        if (!overlay.isModalOpen()) showReopenNotice(title);
    }

    private static void showReopenNotice(Component title) {
        UiToast.show(UiToast.Kind.WARNING, title, Component.translatable(LANG + "reopen"));
    }

    private static final class GameplayHost extends Screen {
        private GameplayHost(Component title) {
            super(title);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void tick() {
            UiOverlayHost overlay = UiOverlayHost.current(this);
            if (overlay == null || !overlay.isModalOpen()) onClose();
        }
    }
}
