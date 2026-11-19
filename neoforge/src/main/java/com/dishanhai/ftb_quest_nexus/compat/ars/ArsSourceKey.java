package com.dishanhai.ftb_quest_nexus.compat.ars;

import appeng.api.stacks.AEKey;
import net.neoforged.fml.ModList;

/** Optional Ars Énergistique singleton; never converts AE amounts to crafting units. */
final class ArsSourceKey {
    private static class OptionalKey {
        static final AEKey KEY = resolve();
        private static AEKey resolve() {
            try {
                return (AEKey) Class.forName("gripe._90.arseng.me.key.SourceKey").getField("KEY").get(null);
            } catch (ReflectiveOperationException | LinkageError | ClassCastException incompatibility) {
                return null;
            }
        }
    }
    static AEKey key() { return ModList.get().isLoaded("arseng") ? OptionalKey.KEY : null; }
    private ArsSourceKey() {}
}
