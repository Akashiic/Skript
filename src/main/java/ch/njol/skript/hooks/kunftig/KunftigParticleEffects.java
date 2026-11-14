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
package ch.njol.skript.hooks.kunftig;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.particle.KunftigParticles;
import com.akashic.kunftig.api.bukkit.particle.ParticleBuilder;
import com.akashic.kunftig.api.particle.KunftigParticle;
import com.akashic.kunftig.api.particle.KunftigParticle.DataKind;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.visual.ParticlePlayer;
import ch.njol.skript.util.visual.VisualEffects;

/**
 * Adds the Kunftig mod's particles to Skript's visual effects, so the usual effect works with them:
 * <pre>
 * play 10 end rod with offset of 0.3, 0.5, 0.3 and speed 0.01 at player
 * play 20 orange dust at location of player
 * play 5 soul fire flame on target block to player
 * </pre>
 * Kunftig sends them to every player nearby (Crucible itself can only send particles to one player). Modern
 * particles (end rod, soul fire flame, cherry leaves...) are drawn by clients with Kunftig and Et Futurum Requiem;
 * everyone else sees the closest 1.7.10 particle. Names and patterns are in {@code lang/hooks/kunftig.lang}.
 */
public final class KunftigParticleEffects implements ParticlePlayer {

	static {
		for (KunftigParticle particle : KunftigParticle.values()) // The pattern's expression (color, item, note...) is the data
			VisualEffects.registerDataSupplier(KunftigParticle.class.getSimpleName() + "." + particle.name(), (raw, location) -> raw);
		VisualEffects.registerParticles(KunftigParticle.values(), new KunftigParticleEffects());
	}

	@Override
	public void play(Enum<?> constant, @Nullable Object data, Location location, @Nullable Player[] players, int count,
			float offsetX, float offsetY, float offsetZ, float speed, int radius) {
		if (!KunftigParticles.isAvailable()) {
			KunftigSupport.warnOnce("particles", "Kunftig particles are not available on this server (is the Kunftig mod enabled?)");
			return;
		}
		KunftigParticle particle = (KunftigParticle) constant;
		ParticleBuilder builder = KunftigParticles.builder(particle)
			.location(location)
			.count(Math.max(count, 0))
			.offset(offsetX, offsetY, offsetZ)
			.extra(speed);

		DataKind kind = particle.dataKind();
		if (kind == DataKind.COLOR && data instanceof Color) {
			builder.color(((Color) data).asBukkitColor().asRGB());
		} else if (kind == DataKind.ITEM || kind == DataKind.BLOCK) {
			int[] ids = ids(data);
			if (kind == DataKind.ITEM)
				builder.item(ids[0], ids[1]);
			else
				builder.block(ids[0], Math.min(ids[1], 15));
		} else if (particle == KunftigParticle.NOTE && data instanceof Number) {
			// As in modern Bukkit: one particle, offset X is the note color (0-24 / 24), speed 1
			builder.count(0).offset(((Number) data).doubleValue() / 24.0, 0, 0).extra(1);
		}

		if (players != null)
			builder.receivers(Arrays.asList(players));
		else if (radius > 0)
			builder.receivers(radius);

		try {
			builder.spawn();
		} catch (IllegalStateException | IllegalArgumentException e) {
			KunftigSupport.warnOnce("particle:" + particle, "Could not play " + particle + ": " + e.getMessage());
		}
	}

	/**
	 * Numeric id and data value of an item type, stone if none was given (1.7.10 particles use numeric ids).
	 */
	@SuppressWarnings("deprecation")
	private static int[] ids(@Nullable Object data) {
		if (data instanceof ItemType) {
			ItemStack stack = ((ItemType) data).getRandom();
			if (stack != null && stack.getTypeId() > 0)
				return new int[] {stack.getTypeId(), Math.max(stack.getDurability(), 0)};
		}
		return new int[] {1, 0};
	}
	

}
