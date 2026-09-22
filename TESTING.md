# Testing PistonEventProbe

The runnable Gradle project lives in `PistonEventProbe/`. Handwritten tests live with that project under `PistonEventProbe/src/test/java/`; Sentinel or external staging may exercise the built plugin later, but these regression tests remain in this repository.

## Automated coverage

`PistonEventProbeCommandTest` loads and enables the real `PistonEventProbePlugin` class in MockBukkit and protects the command/state-machine behavior:

- command permission metadata;
- permission denial for non-admin senders;
- stopped status by default;
- default `start` count of 10;
- explicit count clamping to 1..100;
- invalid numeric input remaining fail-closed/stopped;
- continuous capture state;
- stop/reset behavior;
- unknown-subcommand usage behavior.

This is behavioral coverage rather than a source-text assertion: the real plugin class is enabled and its real command executor is exercised through a mocked Bukkit server. MockBukkit currently rejects the production descriptor's patch-level `api-version: 1.21.11`, so the test uses an equivalent in-memory descriptor with `api-version: 1.21`, the same command name, and the same permission. The production `plugin.yml` is not changed or weakened; descriptor compatibility with real Paper remains a runtime/build boundary.

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

HTML results are written to `PistonEventProbe/build/reports/tests/test/`; machine-readable XML is under `PistonEventProbe/build/test-results/test/`.

## CI

`.github/workflows/test-hardening.yml` checks out the exact pull-request head, uses Java 21, runs `clean test build`, and uploads the test reports even on failure. A green run proves the automated MockBukkit suite and build passed on that exact PR head.

## What these tests do not prove

MockBukkit is appropriate for the command state machine, but this plugin exists specifically to observe real piston-event behavior. The following remain real-Paper/manual boundaries:

- production `plugin.yml` acceptance, including the patch-level Paper API version;
- actual `BlockPistonExtendEvent` ordering at LOWEST and MONITOR;
- cancellation behavior from other installed plugins;
- the exact set/order of registered piston listeners on the production stack;
- real player/world object identity and distance observations;
- next-tick movement/velocity after piston activity;
- behavior under Polar or other third-party mechanics that motivated the probe.

Use the plugin's existing README/probe procedure on a controlled Paper/Leaf server for those. Do not mock those external runtime interactions and present them as production proof.

## Maintenance rule

When the command syntax, permissions, capture modes, count limits, or state transitions change, update the automated tests in the same PR. If a test reveals a production defect, keep the failing regression case and fix the production code in the branch that owns that change rather than weakening the assertion.
