# PistonEventProbe

Temporary Paper/Leaf 1.21.11 diagnostic plugin for Polar support.

It verifies:

- whether `BlockPistonExtendEvent` is fired;
- whether another listener cancels it between LOWEST and MONITOR;
- which plugins are registered for the event;
- whether the player's world name equals the piston's world name;
- whether the player is within 16 blocks of the piston block;
- whether the player is within 16 blocks of any block moved by the piston;
- the player's location and velocity during the event and one tick later.

## Build

Requires Java 21.

### IntelliJ / Maven (easiest)

Open the folder containing `pom.xml` in IntelliJ. In the Maven tool window, run `Lifecycle > package`.

The output JAR will be `target/PistonEventProbe-1.0.0.jar`.

### Gradle

The Gradle build files are also included, but no Gradle wrapper is bundled. With Gradle installed, run `gradle build`. The output JAR will be in `build/libs/`.

## Install and test

1. Put the built JAR in the server's `plugins` folder.
2. Fully restart the server. Do not use `/reload`.
3. Run:

```text
/pistonprobe listeners
/pistonprobe start 10
```

4. Reproduce the slime-piston launch while the affected player is standing in the normal test position.
5. Copy the lines containing `[PistonEventProbe]`, `[LISTENERS]`, or `[PROBE event=` from `logs/latest.log` and send them to Polar.
6. Remove the plugin after testing.

## Interpreting the important fields

- No `[PROBE event=...]` lines: the Bukkit event did not reach this plugin, or the wrong server/plugin JAR is being tested.
- `called=true`: the event definitely fired.
- `cancelled=false` at LOWEST but `cancelled=true` at MONITOR: another plugin cancelled it.
- Polar absent from `/pistonprobe listeners`: Polar did not register a listener for this event on this server.
- `worldNameEquals=false`: Polar's stated world-name filter fails.
- `within16OfPiston=false`: Polar's stated distance filter fails.
- `within16OfPiston=false` but `within16OfMovedBlock=true`: the player is near the slime/moved blocks but too far from the piston itself.
- `describedPolarFilterPass=true`: both conditions Polar described pass for that player.
