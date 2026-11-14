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

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.SkriptColor;
import ch.njol.skript.variables.Variables;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffectType;
import org.eclipse.jdt.annotation.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class VisualEffects {


	private static final Map<String, Consumer<VisualEffectType>> effectTypeModifiers = new HashMap<>();
	private static SyntaxElementInfo<VisualEffect> elementInfo;
	private static VisualEffectType[] visualEffectTypes;

	static {
		Variables.yggdrasil.registerSingleClass(VisualEffectType.class, "VisualEffect.NewType");
		Variables.yggdrasil.registerSingleClass(Effect.class, "Bukkit_Effect");
		Variables.yggdrasil.registerSingleClass(EntityEffect.class, "Bukkit_EntityEffect");
	}

	@Nullable
	public static VisualEffect parse(String s) {
		if (elementInfo == null)
			return null;
		return SkriptParser.parseStatic(
			Noun.stripIndefiniteArticle(s), new SingleItemIterator<>(elementInfo), null);
	}

	public static VisualEffectType get(int i) {
		return visualEffectTypes[i];
	}

	public static String getAllNames() {
		List<Noun> names = new ArrayList<>();
		for (VisualEffectType visualEffectType : visualEffectTypes) {
			names.add(visualEffectType.getName());
		}
		return StringUtils.join(names, ", ");
	}

	/**
	 * Particles registered by hooks with {@link #registerParticles}, with the player of each.
	 */
	private static final Map<Enum<?>, ParticlePlayer> hookParticles = new LinkedHashMap<>();
	
	/**
	 * Adds particle kinds to Skript's visual effects, e.g. {@code play 10 end rod at player}. Each constant needs
	 * {@code visual effects.<enum class simple name>.<constant>.pattern} and {@code .name} in the language file;
	 * constants without them are skipped. A pattern may start with one expression, whose value is the effect's data
	 * (register a data supplier for it with {@link #registerDataSupplier}, which must be done before this).
	 * Must be called while Skript accepts registrations.
	 * 
	 * @param particles Particle constants.
	 * @param player What plays them.
	 */
	public static void registerParticles(Enum<?>[] particles, ParticlePlayer player) {
		for (Enum<?> particle : particles)
			hookParticles.put(particle, player);
		if (visualEffectTypes != null) // Types were generated already: add these
			generateTypes();
	}
	
	private static void generateTypes() {
		List<VisualEffectType> types = new ArrayList<>();
		Stream.of(Effect.class, EntityEffect.class)
			.map(Class::getEnumConstants)
			.flatMap(Arrays::stream)
			.map(VisualEffectType::of)
			.filter(Objects::nonNull)
			.forEach(types::add);
		for (Map.Entry<Enum<?>, ParticlePlayer> entry : hookParticles.entrySet()) {
			VisualEffectType type = VisualEffectType.of(entry.getKey(), entry.getValue());
			if (type != null)
				types.add(type);
		}

		for (VisualEffectType type : types) {
			String id = type.getId();
			if (effectTypeModifiers.containsKey(id))
				effectTypeModifiers.get(id).accept(type);
		}

		visualEffectTypes = types.toArray(new VisualEffectType[0]);
		String[] patterns = new String[visualEffectTypes.length];
		for (int i = 0; i < visualEffectTypes.length; i++) {
			patterns[i] = visualEffectTypes[i].getPattern();
		}
		elementInfo = new SyntaxElementInfo<>(patterns, VisualEffect.class, VisualEffect.class.getName());
	}

	/**
	 * @param id Id of a visual effect type: {@code <enum class simple name>.<constant>}, e.g. {@code Effect.SMOKE}.
	 */
	public static void registerColorable(String id) {
		effectTypeModifiers.put(id, VisualEffectType::setColorable);
	}

	/**
	 * Sets how the value of the expression in an effect's pattern becomes the data the effect is played with.
	 * @param id Id of a visual effect type: {@code <enum class simple name>.<constant>}, e.g. {@code Effect.SMOKE}.
	 */
	public static void registerDataSupplier(String id, BiFunction<Object, Location, Object> dataSupplier) {
		Consumer<VisualEffectType> consumer = type -> type.withData(dataSupplier);
		if (effectTypeModifiers.containsKey(id)) {
			consumer = effectTypeModifiers.get(id).andThen(consumer);
		}
		effectTypeModifiers.put(id, consumer);
	}

	static {
		Language.addListener(() -> {
			if (visualEffectTypes != null) // Already registered
				return;
			// Data suppliers
			registerDataSupplier("Effect.POTION_BREAK", (raw, location) -> {
				// Minecraft 1.7.10 uses Potion (not PotionEffect) as data for this effect
				PotionType potionType = raw == null ? null : PotionType.getByEffect((PotionEffectType) raw);
				return new Potion(potionType == null ? PotionType.SPEED : potionType);
			});
			registerDataSupplier("Effect.SMOKE", (raw, location) -> {
				if (raw == null)
					return BlockFace.SELF;
				return Direction.getFacing(((Direction) raw).getDirection(location), false);
			});

			generateTypes();
		});
	}

}
