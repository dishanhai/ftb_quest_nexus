# Ars Nouveau Source objective (NeoForge 1.21.1)

In the FTB Quests editor, add **Ars Nouveau Source** and set the target in
Source units. This is stored Source, not the player's spell mana or fluid mB.

- Without **Consume Source**, clicking the quest task checks the combined fill
  of carried ordinary Source Jars in inventory and offhand. It does not
  drain them; the complete target must be present at the time of the check.
- With **Consume Source**, clicking the task debits only the remaining target
  from the player's carried ordinary Source Jars. Partial deliveries count.
  Splitting a stack uses a free main inventory slot; without one, only a
  uniform amount per jar can be deducted, leaving any undistributable
  remainder for a later submission. No jars are dropped or destroyed.
- With Ars Énergistique (`arseng`), the powered AE Submitter can also
  deliver a consuming task directly from stored Source, using the real
  `ars_nouveau:source` AE key with no unit conversion. The submitter's
  normal team binding and AE machine action source still apply. Manual
  collection never drains a placed or ME Source Jar, and never touches
  player mana or Creative Source Jars.

The task saves and synchronizes its target and consume mode even when
optional integrations are absent on 1.21.1. It does not award Source, add a
Checker stock catalog category, or add detection for 26.1.2 without verified
provider releases.

Upstream: [Ars Nouveau 1.21.x][ars] and [Ars Énergistique 2.1.1-beta][arseng].
[ars]: https://github.com/baileyholl/Ars-Nouveau/tree/1.21.x
[arseng]: https://github.com/62832/ArsEnergistique/releases/tag/2.1.1-beta
