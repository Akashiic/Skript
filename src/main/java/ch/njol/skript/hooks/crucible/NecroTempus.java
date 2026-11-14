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
package ch.njol.skript.hooks.crucible;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.ModHooks;

/**
 * Crucible declares titles, action bars, boss bars and the tab list header/footer, but only implements them when the
 * NecroTempus mod is installed (it overwrites Crucible's methods with mixins). Without it they throw
 * {@link UnsupportedOperationException}: the syntax catches it, falls back where it makes sense and warns once.
 */
public final class NecroTempus {
	
	private NecroTempus() {}
	
	public static final String MOD_ID = "necrotempus";
	
	private static final Set<String> warned = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * @return Whether the NecroTempus mod is installed.
	 */
	public static boolean isInstalled() {
		return ModHooks.isModLoaded(MOD_ID);
	}
	
	/**
	 * Warns once that a feature requires NecroTempus.
	 * @param feature What was attempted, e.g. "Titles".
	 * @param fallback What Skript does instead, e.g. "they are sent as chat messages", or null.
	 */
	public static void warnMissing(String feature, String fallback) {
		if (!warned.add(feature))
			return;
		Skript.warning(feature + " require the NecroTempus mod on Crucible (https://github.com/CrucibleMC/NecroTempus)"
				+ (fallback == null ? "." : "; " + fallback + "."));
	}
	
}
