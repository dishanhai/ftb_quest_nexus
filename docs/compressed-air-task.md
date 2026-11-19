# Compressed-air quest (NeoForge 1.21.1)

In the FTB Quests editor, add **Compressed air**. Configure an amount in
mL (1–2,147,483,647 per task) and choose whether the task consumes air. This is air quantity, not
pressure in bar, a liquid stack, or Forge Energy.

For manual submission, keep a PneumaticCraft air canister (ordinary,
reinforced or reinforced array) or an Applied Pneumatics portable air cell
in the main inventory or offhand and click the task. The task does not
drain pneumatic tools, armor, or arbitrary blocks. Non-consuming mode
checks whether the currently held total meets the goal in one submission.
Consuming mode can submit a partial amount, and progress reflects only the
confirmed debit from each container. Incompatible containers are left
unchanged.

Applied Pneumatics also enables consuming submission via the existing
FTBQN ME submitter. It extracts its actual AirKey quantity without
multiplying by the display, crafting-operation or storage-byte factors.
Normal team and quest progression rules apply.

PneumaticCraft and Applied Pneumatics remain optional. The quest type is
still readable without the relevant mods; no unsupported measurement is
counted. No release compatibility with NeoForge 26.1.2 is claimed.

Sources: [PneumaticCraft air API](https://github.com/TeamPneumatic/pnc-repressurized/tree/1.21/src/api/java/me/desht/pneumaticcraft/api),
[Applied Pneumatics source](https://github.com/Frostbite-time/AppliedPneumatics/tree/1.21.1),
[official Applied Pneumatics downloads](https://modrinth.com/mod/applied-pneumatics).
