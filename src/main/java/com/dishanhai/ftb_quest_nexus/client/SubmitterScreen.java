package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassPalette;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.GuiMotion;
import com.dishanhai.sh_better_ui.client.gui.scaled.Spring;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.Util;

/** Native Forge presentation of the original submitter panel. Author: dishanhai. */
public final class SubmitterScreen extends AbstractContainerScreen<SubmitterMenu> {
    private static final String GUI = "gui.ftb_quest_nexus.submitter.";
    private Component hoveredText;

    public SubmitterScreen(SubmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 232;
        imageHeight = 194;
    }

    @Override
    protected void init() {
        super.init();
        GlassBlur.warmUp();
        String[] keys = {"bind", "clear", "toast", "claim"};
        for (int i = 0; i < keys.length; i++) {
            final int action = i;
            Component message = Component.translatable(GUI + keys[i]);
            AbstractButton button = new AbstractButton(leftPos + 10 + i * 54, topPos + 170, 50, 16, message) {
                private final Spring hover = new Spring(Spring.HOVER);
                @Override
                public void onPress() {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
                    }
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    GlassTheme theme = GlassTheme.GLASS;
                    long now = System.currentTimeMillis();
                    hover.setTarget(isHoveredOrFocused() ? 1f : 0f, now);
                    hover.update(now);
                    float progress = GuiMotion.clamp01(hover.value());
                    theme.button(graphics, font, getX(), getY(), width, height, getMessage().getString(),
                            GuiMotion.lerpColor(GlassPalette.BUTTON_EDGE, GlassPalette.ACCENT, progress),
                            GuiMotion.lerpColor(GlassPalette.BUTTON_FILL, GlassPalette.BUTTON_FILL_HOVER, progress),
                            GlassPalette.TEXT);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput output) {
                    defaultButtonNarrationText(output);
                }
            };
            button.setTooltip(Tooltip.create(message));
            addRenderableWidget(button);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.submitter == null || menu.submitter.isRemoved()) return;
        GlassTheme theme = GlassTheme.GLASS;
        theme.rootShadow(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.backdrop(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.box(graphics, leftPos, topPos, imageWidth, imageHeight, GlassPalette.ROOT_EDGE, theme.panelBg);
        SubmitterBlockEntity be = menu.submitter;
        row(graphics, mouseX, mouseY, 10, 8, 212, title, GlassPalette.TEXT_HEAD);
        row(graphics, mouseX, mouseY, 10, 26, 212,
                Component.translatable(GUI + (be.isOnline() ? "online" : "offline")), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 42, 212,
                Component.translatable(GUI + "status", be.getStatusComponent()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 60, 212,
                Component.translatable(GUI + "uuid", identifier(be.getSubmitterIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 78, 212,
                Component.translatable(GUI + "team", identifier(be.getTeamIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 96, 212,
                Component.translatable(GUI + "player", identifier(be.getRewardPlayerIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 116, 68,
                Component.translatable(GUI + "queue", be.getQueueSize()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 82, 116, 68,
                Component.translatable(GUI + "submitted", be.getLastSubmitted()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 154, 116, 68,
                Component.translatable(GUI + "rewards", be.getLastClaimedRewards()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 134, 212,
                Component.translatable(GUI + (be.isSuppressCompletionToast() ? "toast.suppressed" : "toast.normal")),
                GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 150, 212,
                Component.translatable(GUI + (be.isClaimOnlyQueuedRewards() ? "claim.queued" : "claim.all")),
                GlassPalette.TEXT);
    }

    private static Component identifier(String text) {
        return text == null || text.isBlank() ? Component.translatable(GUI + "unbound") : Component.literal(text);
    }

    private void row(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width,
                     Component text, int color) {
        int px = leftPos + x;
        int py = topPos + y;
        boolean hovered = mouseX >= px && mouseX < px + width && mouseY >= py && mouseY < py + 12;
        String rendered = text.getString();
        int textWidth = font.width(rendered);
        if (textWidth > width - 4 && hovered) {
            // Smoothly pan the full value while the tooltip provides a stationary copy.
            int overflow = textWidth - (width - 4);
            double seconds = Util.getMillis() / 1000.0;
            double period = Math.max(overflow * 0.04, 3.0);
            double position = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
            graphics.enableScissor(px, py, px + width - 4, py + 12);
            graphics.drawString(font, rendered, px - (int) (position * overflow), py + 2, color, false);
            graphics.disableScissor();
        } else {
            if (textWidth > width - 4) {
                rendered = font.plainSubstrByWidth(rendered, width - 4 - font.width("…")) + "…";
            }
            graphics.drawString(font, rendered, px, py + 2, color, false);
        }
        if (hovered) hoveredText = text;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are part of the original fixed panel; there is no player inventory.
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.submitter == null || menu.submitter.isRemoved()) onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredText = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredText != null) graphics.renderTooltip(font, hoveredText, mouseX, mouseY);
    }
}
