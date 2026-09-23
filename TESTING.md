# Testing PistonEventProbe

The runnable Gradle project lives in `PistonEventProbe/`. Handwritten tests live with that project under `PistonEventProbe/src/test/java/`; Sentinel or external staging may exercise the built plugin later, but these regression tests remain in this repository.

## Automated coverage

`PistonEventProbeCommandTest` exercises the real `PistonEventProbePlugin.onCommand(...)` implementation directly with JUnit + Mockito. It protects:

- permission denial for non-admin senders;
- stopped status for both empty arguments and explicit `status`;
- default `start` count of 10;
- lower/upper count clamping to 1..100;
- invalid numeric input remaining fail-closed/stopped;
- continuous capture state;
- `start` correctly leaving continuous mode;
- stop/reset behavior;
- unknown-subcommand usage behavior.

The test uses a Mockito instance with real method calls and initializes only the private in-memory event map needed by `stop`. Bukkit interfaces (`CommandSender` and `Command`) are mocked. This keeps the assertions focused on the actual command/state implementation without pretending a mocked server proves production plugin loading.

`PluginDescriptorContractTest` separately parses the production `plugin.yml` from the test runtime classpath and locks the plugin name, main class, Paper API version, command permission, and permission default.

### Why this repo does not use MockBukkit plugin loading

The test campaign tried both MockBukkit's class loader and JAR loader. The class loader cannot load the plugin because `PistonEventProbePlugin` is final and MockBukkit creates a ByteBuddy subclass. The JAR loader also fails before `onEnable` with `JavaPlugin requires to be created by a valid classloader` for this Paper/MockBukkit combination.

Those are harness limitations, not evidence of a production defect. Production modifiers and metadata were intentionally left unchanged rather than weakening the plugin to satisfy MockBukkit.

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

Run only the descriptor contract with:

```bash
./gradlew test --tests com.enthusia.pistoneventprobe.PluginDescriptorContractTest
```

HTML results are written to `PistonEventProbe/build/reports/tests/test/`; machine-readable XML is under `PistonEventProbe/build/test-results/test/`.

## CI

`.github/workflows/test-hardening.yml` checks out the exact pull-request head, uses Java 21, runs `clean test build`, and uploads the test reports even on failure. A green run proves the direct command-state suite, production descriptor contract, and Gradle build passed on that exact PR head.

## What these tests do not prove

This plugin exists specifically to observe real piston-event behavior. The following remain real-Paper/manual boundaries:

- successful enable/disable on the target Paper/Leaf build;
- actual `BlockPistonExtendEvent` ordering at LOWEST and MONITOR;
- cancellation behavior from other installed plugins;
- the exact set/order of registered piston listeners on the production stack;
- real player/world object identity and distance observations;
- next-tick movement/velocity after piston activity;
- behavior under Polar or other third-party mechanics that motivated the probe.

Use the plugin's existing README/probe procedure on a controlled Paper/Leaf server for those. Do not mock those external runtime interactions and present them as production proof.

## Maintenance rule

When command syntax, permissions, capture modes, count limits, state transitions, or plugin metadata change, update the relevant automated tests in the same PR. If a test reveals a production defect, keep the failing regression case and fix the production code in the branch that owns that change rather than weakening the assertion.
