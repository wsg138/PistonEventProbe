# Testing PistonEventProbe

The runnable Gradle project lives in `PistonEventProbe/`. Handwritten tests live with that project under `PistonEventProbe/src/test/java/`; Sentinel or external staging may exercise the built plugin later, but these regression tests remain in this repository.

## Automated coverage

`PistonEventProbeCommandTest` builds the real plugin JAR first, loads that packaged JAR through MockBukkit, enables it, and protects the command/state-machine behavior:

- command permission metadata from the packaged `plugin.yml`;
- permission denial for non-admin senders;
- stopped status by default;
- default `start` count of 10;
- explicit count clamping to 1..100;
- invalid numeric input remaining fail-closed/stopped;
- continuous capture state;
- stop/reset behavior;
- unknown-subcommand usage behavior.

This is behavioral coverage rather than a source-text assertion. JAR loading is intentional: MockBukkit's class-based loader creates a ByteBuddy subclass of the plugin class, while `PistonEventProbePlugin` is `final`. The JAR loader uses MockBukkit's URL plugin classloader instead, so the production class and packaged descriptor can be exercised without changing production modifiers or metadata just for testing.

## Run locally

From the repository root:

```bash
cd PistonEventProbe
./gradlew clean test build --no-daemon
```

On Windows:

```powershell
cd PistonEventProbe
.\gradlew.bat clean test build --no-daemon
```

Run only the command suite with:

```bash
./gradlew test --tests com.enthusia.pistoneventprobe.PistonEventProbeCommandTest
```

The `test` task depends on `jar`, so `build/libs/PistonEventProbe-1.0.0.jar` exists before the MockBukkit suite starts. HTML results are written to `PistonEventProbe/build/reports/tests/test/`; machine-readable XML is under `PistonEventProbe/build/test-results/test/`.

## CI

`.github/workflows/test-hardening.yml` checks out the exact pull-request head, uses Java 21, runs `clean test build`, and uploads the test reports even on failure. A green run proves the packaged plugin JAR loaded and enabled under MockBukkit and that the automated command suite and build passed on that exact PR head.

## What these tests do not prove

MockBukkit is appropriate for the command state machine, but this plugin exists specifically to observe real piston-event behavior. The following remain real-Paper/manual boundaries:

- actual `BlockPistonExtendEvent` ordering at LOWEST and MONITOR;
- cancellation behavior from other installed plugins;
- the exact set/order of registered piston listeners on the production stack;
- real player/world object identity and distance observations;
- next-tick movement/velocity after piston activity;
- behavior under Polar or other third-party mechanics that motivated the probe.

Use the plugin's existing README/probe procedure on a controlled Paper/Leaf server for those. Do not mock those external runtime interactions and present them as production proof.

## Maintenance rule

When the command syntax, permissions, capture modes, count limits, or state transitions change, update the automated tests in the same PR. If a test reveals a production defect, keep the failing regression case and fix the production code in the branch that owns that change rather than weakening the assertion.
