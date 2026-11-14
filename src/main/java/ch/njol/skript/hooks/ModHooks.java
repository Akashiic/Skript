/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.hooks;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;

/**
 * Loads the syntax that uses APIs from the Forge/Bukkit hybrid (Crucible) and from mods. Unlike {@link Hook}s,
 * these don't depend on a Bukkit plugin: each package is only loaded when what it needs exists at runtime, so
 * the same Skript jar keeps working on a 1.7.10 server without them.
 * <ul>
 * <li>{@code hooks.crucible}: APIs Crucible backports to Bukkit 1.7.10 (titles, action bar, boss bars, tab list,
 * JSON chat, particles, Spigot APIs). Titles, action bar, boss bars and the tab list need the NecroTempus mod.</li>
 * <li>{@code hooks.forge}: entities and items of Forge mods (Et Futurum Requiem, Netherlicious and any other mod).</li>
 * <li>{@code hooks.kunftig}: the Kunftig mod's plugin API.</li>
 * </ul>
 * Class names in these packages must not end with "Hook", or Skript's plugin hook loader would pick them up.
 */
public final class ModHooks {

	private ModHooks() {}

	private static final List<String> loaded = new ArrayList<>();

	@Nullable
	private static Method isModLoaded;
	private static boolean isModLoadedLookupDone;

	/**
	 * @return Whether this server runs Forge (Crucible, Thermos, KCauldron...).
	 */
	public static boolean isForge() {
		return Skript.classExists("cpw.mods.fml.common.Loader");
	}

	/**
	 * @param modId Forge mod id, e.g. {@code etfuturum}.
	 * @return Whether the mod is loaded. Always false when the server doesn't run Forge.
	 */
	public static boolean isModLoaded(String modId) {
		if (!isModLoadedLookupDone) {
			isModLoadedLookupDone = true;
			try {
				isModLoaded = Class.forName("cpw.mods.fml.common.Loader").getMethod("isModLoaded", String.class);
			} catch (ClassNotFoundException | NoSuchMethodException e) {
				isModLoaded = null;
			}
		}
		Method method = isModLoaded;
		if (method == null)
			return false;
		try {
			return (Boolean) method.invoke(null, modId.trim());
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	/**
	 * @return Whether the server has Crucible's backported Bukkit APIs.
	 */
	public static boolean hasCrucibleApi() {
		return Skript.methodExists(Player.class, "sendTitle", String.class, String.class)
				&& Skript.classExists("net.md_5.bungee.api.chat.BaseComponent");
	}

	/**
	 * @return Whether Kunftig's plugin API is available (the Kunftig mod is installed).
	 */
	public static boolean hasKunftig() {
		return Skript.classExists("com.akashic.kunftig.api.KunftigApi");
	}

	/**
	 * @return Names of the hook packages that were loaded.
	 */
	public static List<String> getLoaded() {
		return loaded;
	}

	/**
	 * Loads the hook packages whose requirements are met. Called by Skript while syntax can be registered.
	 */
	public static void load() {
		if (hasCrucibleApi())
			load("crucible", "Crucible APIs");
		if (isForge())
			load("forge", "Forge mods");
		if (hasKunftig())
			load("kunftig", "Kunftig");
	}

	private static void load(String pkg, String name) {
		try {
			Skript.getAddonInstance().loadClasses("ch.njol.skript.hooks." + pkg);
			loaded.add(name);
		} catch (IOException | LinkageError e) {
			Skript.exception(e, "Could not load the " + name + " support");
		}
	}

}
