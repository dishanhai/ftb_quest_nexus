package com.dishanhai.ftb_quest_nexus.checker;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.network.CheckerFluxPackets.Option;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import sonar.fluxnetworks.api.network.SecurityLevel;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkData;
import sonar.fluxnetworks.common.device.TileFluxPoint;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Author: dishanhai. Registers the checker's position as a Flux consumer without placing another block. */
final class CheckerFluxDevice extends TileFluxPoint {
    private final CheckerBlockEntity checker;
    private boolean retired;

    CheckerFluxDevice(CheckerBlockEntity checker) {
        super(checker.getBlockPos(), checker.getBlockState());
        this.checker = checker;
        setLevel(checker.getLevel());
        setOwnerUUID(checker.boundPlayerId());
        getTransferHandler().updateSideTransfer(Direction.DOWN, checker, false);
    }

    static List<Option> options(ServerPlayer player, int maxOptions) {
        return FluxNetworkData.getAllNetworks().stream()
                .filter(network -> network.isValid() && (network.canPlayerAccess(player)
                        || network.getSecurityLevel() == SecurityLevel.ENCRYPTED))
                .sorted(Comparator.comparing(FluxNetwork::getNetworkName).thenComparingInt(FluxNetwork::getNetworkID))
                .limit(maxOptions)
                .map(network -> new Option(network.getNetworkID(), network.getNetworkName()))
                .toList();
    }

    static boolean canSelect(CheckerBlockEntity checker, ServerPlayer player, int id, String password) {
        if (id == -1) return true;
        FluxNetwork network = FluxNetworkData.getNetwork(id);
        return network.isValid() && (network.canPlayerAccess(player)
                || network.getSecurityLevel() == SecurityLevel.ENCRYPTED && network.canPlayerAccess(player, password))
                && (id == checker.fluxNetworkId() || isPositionAvailable(checker, network));
    }

    static Runnable restore(CheckerBlockEntity checker, int id) {
        FluxNetwork network = FluxNetworkData.getNetwork(id);
        if (!network.isValid()) {
            checker.setFluxNetworkId(-1);
            return null;
        }
        if (!isPositionAvailable(checker, network)) return null;
        CheckerFluxDevice device = new CheckerFluxDevice(checker);
        return device.connect(network) ? device::unload : null;
    }

    private static boolean isPositionAvailable(CheckerBlockEntity checker, FluxNetwork network) {
        var connection = network.getConnectionByPos(GlobalPos.of(checker.getLevel().dimension(), checker.getBlockPos()));
        return connection == null || !connection.isChunkLoaded();
    }

    void unload() {
        retired = true;
        FluxNetwork network = getNetwork();
        getTransferHandler().updateSideTransfer(Direction.DOWN, null, false);
        if (!network.isValid() || network.getLogicalDevices(FluxNetwork.ANY).contains(this)) {
            network.enqueueConnectionRemoval(this, false);
            setRemoved();
        } else {
            // Flux handles pending additions before its end-of-tick transfers.
            Consumer<TickEvent.ServerTickEvent> cleanup = new Consumer<>() {
                @Override
                public void accept(TickEvent.ServerTickEvent event) {
                    if (event.phase != TickEvent.Phase.END) return;
                    MinecraftForge.EVENT_BUS.unregister(this);
                    network.enqueueConnectionRemoval(CheckerFluxDevice.this, false);
                    CheckerFluxDevice.this.setRemoved();
                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, TickEvent.ServerTickEvent.class, cleanup);
        }
    }

    @Override
    public boolean connect(FluxNetwork network) {
        if (retired || !super.connect(network)) return false;
        checker.setFluxNetworkId(getNetworkID());
        return true;
    }

    @Override
    public ItemStack getDisplayStack() {
        return new ItemStack(NexusContent.CHECKER_ITEM.get());
    }
}
