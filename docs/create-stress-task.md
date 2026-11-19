# Create network stress objective (NeoForge 1.21.1)

In the FTB Quests editor, add **Create network stress** and select Network
capacity, Applied load, or Available capacity (capacity minus load). Set the
minimum in whole stress units (SU). The sample is not rounded up.

Connect a Create Stressometer to the target network, aim at it in-world, then
open the quest book and click the task. The tooltip also shows
`/ftbqn stress check <hex-task-id>` as a direct world-side alternative. A
rotating, loaded, ticking, connected and non-overloaded network with Create
stress enabled must meet the threshold in one reading. Zero load is allowed
for capacity/headroom checks; a full (not overloaded) network has zero
headroom. A check does not sum prior readings or extract anything. Normal FTB
team, quest dependency and sequence gates apply.

Applied Kinetics Energy Providers are Create kinetic blocks, not a separate
stress store. Connect the Stressometer to the same Create network as the
Energy Provider. This task verifies that network's SU, not the AE energy
produced by an individual Energy Provider.

The objective is not a resource or a reward. It does not use the AE submitter,
FTB Task Screen pipes or automatic inventory/login submission. The task ID
remains readable on NeoForge 1.21.1 without optional Create installed, but
cannot be completed there. NeoForge 26.1.2 has no official compatible Create
or Applied Kinetics version in the checked releases; this task is not
registered in that build. Do not move quest data containing this task to a
26.1.2 pack expecting it to load or complete.

Sources: [Create 1.21.1 Stressometer][gauge] and [Applied Kinetics 1.21.1 Energy Provider][energy].

[gauge]: https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/kinetics/gauge/StressGaugeBlockEntity.java
[energy]: https://github.com/Forsteri123/CreateAppliedKinetics/blob/forge/1.21.1/src/main/java/com/forsteri/createappliedkinetics/content/energyProvider/EnergyProviderBlockEntity.java

## Verification

`./gradlew -p neoforge test` covers metric boundaries and malformed, stalled, disconnected,
dirty and overloaded readings. The optional `probesRunningCreateNetwork`
GameTest places a creative motor and Stressometer. With the Applied Kinetics
add-on present it also exercises a spinning Energy Provider connected to the
same shaft and a real AE2 Energy Acceptor. Add the official Create 6.0.10 and
Applied Kinetics 1.5.6 NeoForge 1.21.1 jars to the development runtime
classpath to exercise the mod-present branch; the normal package does not
include either jar.
