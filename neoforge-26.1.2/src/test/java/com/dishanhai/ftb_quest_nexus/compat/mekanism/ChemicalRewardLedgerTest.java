package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Chemical reward delivery survives refusal, retries, and reloads. */
class ChemicalRewardLedgerTest {
    private static final Identifier HYDROGEN = Identifier.fromNamespaceAndPath("mekanism", "hydrogen");

    @Test
    void partialDeliverySurvivesReloadAndOnlyItsOwnerCanRedeemIt() {
        UUID owner = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        var ledger = new ChemicalRewardLedger();
        var entry = ledger.add(owner, HYDROGEN, 4500);

        assertEquals(0, ledger.deliver(teammate, entry.id(), pending -> fail("Other player can redeem a reward")));
        assertEquals(1500, ledger.deliver(owner, entry.id(), pending -> 1500));
        var reloaded = ChemicalRewardLedger.load(ledger.save());

        assertEquals(3000, reloaded.first(owner).amount());
        assertEquals(HYDROGEN, reloaded.first(owner).chemical());
        assertEquals(0, reloaded.deliver(owner, entry.id(), pending -> 0));
        assertEquals(3000, reloaded.deliver(owner, entry.id(), ChemicalRewardLedger.Pending::amount));
        assertNull(reloaded.first(owner));
        assertEquals(0, reloaded.deliver(owner, entry.id(), pending -> fail("A repeated request duplicated the reward")));
    }

    @Test
    void aPartialPackagingConfirmationCannotBeReplayed() {
        UUID player = UUID.randomUUID();
        var ledger = new ChemicalRewardLedger();
        var first = ledger.add(player, HYDROGEN, 3000);
        var second = ledger.add(player, HYDROGEN, 4000);

        assertEquals(1000, ledger.deliver(player, first.id(), pending -> 1000));
        ledger.invalidateOffer(player, first.id());
        assertEquals(0, ledger.deliver(player, first.id(), pending -> fail("Confirmation replayed")));
        var remaining = ledger.first(player);
        assertNotEquals(first.id(), remaining.id());
        assertEquals(2000, ledger.deliver(player, remaining.id(), ChemicalRewardLedger.Pending::amount));
        assertEquals(second, ledger.first(player));
    }
}
