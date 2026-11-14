package ch.njol.skript.bukkitutil;

import org.bukkit.block.Biome;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.util.EnumUtils;

/**
 * Biome names. Minecraft 1.7.10 biome names are in the 'biomes' section of the language file;
 * biomes added by mods (Crucible) get generated names.
 */
public abstract class BiomeMappings {
	
	private final static EnumUtils<Biome> util = new EnumUtils<>(Biome.class, "biomes");
	
	public static @Nullable Biome parse(final String name) {
		return util.parse(name);
	}
	
	public static String toString(final Biome biome, final int flags) {
		return util.toString(biome, flags);
	}
	
	public static String getAllNames() {
		return util.getAllNames();
	}
}
