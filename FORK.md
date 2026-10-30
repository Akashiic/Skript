# Akashiic/Skript fork

This fork tracks **Skript 2.2-dev33** (upstream commit `09f1b0a94dcbdfe79af55df6c0c0b7bd802b9482`),
the version we run on our Crucible (Forge 1.7.10) server. It is not based on the current
Skript 2.x releases. The old 2.17.1 history is still reachable in this branch's history.

## Differences from upstream 2.2-dev33

- The built-in updater is disabled (`Updater.start()` only marks the build as custom). It never
  contacts GitHub and never downloads a jar.
- The build uses Gradle 8 and compiles Java 8 bytecode (`--release 8`) with `javac`, instead of
  the original Gradle 4.2 + ECJ setup. Three source files were converted from ISO-8859-1 to UTF-8
  and two generic-inference spots were made explicit so they compile on modern JDKs.

## Building

Requires a JDK 17 or newer (the output still targets Java 8).

```bash
./gradlew jar
```

The plugin is written to `build/libs/Skript.jar`. Unit tests run with `./gradlew test`.
