package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import appeng.api.stacks.AEKey;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Author: dishanhai. References to the optional Mekanism and Applied Mekanistics APIs. */
final class ChemicalStorage {
    private ChemicalStorage() {}

    static AEKey key(ResourceLocation id) {
        var holder = find(id);
        return holder == null ? null : MekanismKey.of(new ChemicalStack(holder, 1));
    }

    static Component name(ResourceLocation id) {
        var holder = find(id);
        return holder == null ? Component.translatable(ChemicalTask.LANG + ".missing", id.toString())
                : holder.value().getTextComponent();
    }

    static Icon icon(ResourceLocation id) {
        var holder = find(id);
        if (holder == null) return Icon.empty();
        Chemical chemical = holder.value();
        return Icon.getIcon(chemical.getIcon()).withTint(Color4I.rgb(chemical.getTint()));
    }

    private static Holder.Reference<Chemical> find(ResourceLocation id) {
        if (id.equals(MekanismAPI.EMPTY_CHEMICAL_KEY.location())) return null;
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(id).orElse(null);
    }
}
