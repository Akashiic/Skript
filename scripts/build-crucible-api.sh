#!/usr/bin/env bash
# Builds the Bukkit API that Crucible 1.7.10 actually ships, for compiling Skript:
#
#   scripts/build-crucible-api.sh lib/crucible-bukkit-api.jar
#
# No public Maven repository hosts it. Crucible keeps the Bukkit 1.7.10 API in bukkit/, patches it during its build
# (patches/org/bukkit/*.patch: titles, particles, Player.Spigot, tab list...) and adds classes in src/main/java
# (boss bars, Particle, the BungeeCord chat API, Spigot events). This script does the same, drops the few files that
# depend on Forge/Minecraft internals (the plugin loader, timings, default commands, Material/Enchantment injection),
# compiles everything else and fills the dropped classes with the plain Bukkit 1.7.10 ones from JitPack.
#
# Some methods (titles, action bar, boss bars, tab header/footer) throw UnsupportedOperationException unless the
# NecroTempus mod is installed: it implements them with mixins.
#
# Requires git, curl, patch and a JDK 9+ (javac/jar).
set -euo pipefail

OUT="$(realpath -m "${1:-lib/crucible-bukkit-api.jar}")"
CRUCIBLE_REPO="https://github.com/CrucibleMC/Crucible"
CRUCIBLE_COMMIT="cd1de3dcd8131c5d813ee86f1a502ed9270daca0"
BUKKIT_JAR_URL="https://jitpack.io/com/github/Bukkit/Bukkit/f210234/Bukkit-f210234.jar" # Bukkit 1.7.10-R0.1-SNAPSHOT
MAVEN="https://repo.maven.apache.org/maven2"
DEPS=(
  "org/jetbrains/annotations/24.0.1/annotations-24.0.1.jar"
  "org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"
  "com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
  "com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"
  "it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar"
)

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

download() { # url file
  for attempt in 1 2 3 4 5; do
    if curl -sSfL -o "$2" "$1"; then return 0; fi
    sleep $((attempt * 2))
  done
  echo "Could not download $1" >&2
  exit 1
}

git -C "$WORK" init -q crucible
git -C "$WORK/crucible" remote add origin "$CRUCIBLE_REPO"
git -C "$WORK/crucible" fetch -q --depth 1 origin "$CRUCIBLE_COMMIT"
git -C "$WORK/crucible" checkout -q FETCH_HEAD -- bukkit/src/main/java patches/org/bukkit src/main/java

mkdir -p "$WORK/lib" "$WORK/src" "$WORK/classes" "$WORK/merged"
download "$BUKKIT_JAR_URL" "$WORK/lib/bukkit.jar"
for dep in "${DEPS[@]}"; do download "$MAVEN/$dep" "$WORK/lib/$(basename "$dep")"; done

# 1. Bukkit 1.7.10 as kept by Crucible, with Crucible's patches applied
cp -r "$WORK/crucible/bukkit/src/main/java/." "$WORK/src/"
(cd "$WORK/crucible/patches" && find org/bukkit -name '*.patch' ! -path '*craftbukkit*') | while read -r p; do
  patch -s -p0 --no-backup-if-mismatch -d "$WORK/src" "${p%.patch}" < "$WORK/crucible/patches/$p"
done

# 2. API classes Crucible adds outside bukkit/
C="$WORK/crucible/src/main/java"
(cd "$C" && cp -r --parents org/bukkit/boss org/bukkit/Particle.java org/bukkit/command/ProxiedCommandSender.java \
  org/bukkit/command/MessageCommandSender.java org/bukkit/command/BufferedCommandSender.java org/bukkit/event/player \
  org/spigotmc/event net/md_5 co/aikar "$WORK/src/")

# 3. Drop what depends on Forge/Minecraft internals
sed -i '/^import net.minecraft.world.WorldServer;/d; /public abstract WorldServer getWorldServer();/d' "$WORK/src/org/bukkit/World.java"
rm -f "$WORK/src/org/bukkit/Material.java" "$WORK/src/org/bukkit/enchantments/Enchantment.java" \
  "$WORK/src/org/bukkit/plugin/java/JavaPluginLoader.java" "$WORK/src/org/bukkit/plugin/java/PluginClassLoader.java"
CP="$(find "$WORK/lib" -name '*.jar' | tr '\n' ':')"
for round in 1 2 3 4 5 6 7 8; do
  (cd "$WORK/src" && find . -name '*.java' > ../sources.txt)
  rm -rf "$WORK/classes" && mkdir -p "$WORK/classes"
  if (cd "$WORK/src" && javac --release 8 -nowarn -proc:none -encoding UTF-8 -cp "$CP" -d "$WORK/classes" @../sources.txt > ../javac.log 2>&1); then
    break
  fi
  bad="$(grep ': error:' "$WORK/javac.log" | cut -d: -f1 | sort -u)"
  [ -n "$bad" ] || { cat "$WORK/javac.log" >&2; exit 1; }
  [ "$round" = 8 ] && { echo "Compilation failed" >&2; cat "$WORK/javac.log" >&2; exit 1; }
  (cd "$WORK/src" && rm -f $bad) # internal implementation files (timings, plugin manager, default commands...)
done

# 4. Merge: plain Bukkit 1.7.10 org/bukkit classes, overwritten by Crucible's
(cd "$WORK/merged" && unzip -q "$WORK/lib/bukkit.jar" 'org/bukkit/*')
cp -r "$WORK/classes/." "$WORK/merged/"
mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
(cd "$WORK/merged" && jar cf "$OUT" .)
echo "Wrote $OUT ($(find "$WORK/merged" -name '*.class' | wc -l) classes, Crucible $CRUCIBLE_COMMIT)"
