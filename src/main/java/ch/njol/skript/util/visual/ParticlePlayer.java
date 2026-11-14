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
package ch.njol.skript.util.visual;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Plays particles that Bukkit 1.7.10 has no API for. Minecraft 1.7.10 has no {@code org.bukkit.Particle}, so particle
 * kinds come from hooks (e.g. the Kunftig mod's particles, see {@link VisualEffects#registerParticles}); this keeps
 * {@link VisualEffect} free of references to optional classes.
 */
public interface ParticlePlayer {

	/**
	 * @param particle The particle constant given to {@link VisualEffects#registerParticles}.
	 * @param data Data of the effect (a color, an item type, a number...), or null.
	 * @param location Where to play it.
	 * @param players Who sees it, or null for every player within {@code radius}.
	 * @param count Number of particles. 0 is one particle moving with {@code offset * speed}.
	 * @param speed The particle's speed ("extra" in Bukkit's API).
	 * @param radius Radius in blocks when {@code players} is null.
	 */
	void play(Enum<?> particle, @Nullable Object data, Location location, @Nullable Player[] players, int count,
			float offsetX, float offsetY, float offsetZ, float speed, int radius);

}
