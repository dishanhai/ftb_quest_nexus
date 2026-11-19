package com.dishanhai.ftb_quest_nexus.compat.jade;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlock;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade discovers this entry point only when installed. */
@WailaPlugin
public final class NexusJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SubmitterInfoProvider.INSTANCE, SubmitterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SubmitterInfoProvider.INSTANCE, SubmitterBlock.class);
    }
}
