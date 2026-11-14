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
package ch.njol.skript.hooks.forge;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.hooks.forge.ForgeRegistry.ModEntity;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.localization.Language;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.util.Utils;
import ch.njol.yggdrasil.Fields;

/**
 * Entities added by Forge mods (Et Futurum Requiem, Netherlicious and any other mod) on Crucible.
 * <p>
 * Every mod entity can be written as {@code <mod id> <name>} (e.g. {@code etfuturum polar bear}) and, when no
 * vanilla Skript entity and no mod with a higher priority uses it, by its plain name ({@code polar bear}).
 * Names come from the server's translation of the entity, from the name the mod registered it with, and for
 * Et Futurum Requiem from the names modern Skript uses ({@code armor stand}, {@code zombie villager}...).
 * <p>
 * The patterns in {@code lang/hooks/forge.lang} match any word sequence; {@link #init(Literal[], int, ParseResult)}
 * accepts only known names, so parsing falls through to the other entity types otherwise.
 */
public class ForgeEntityData extends EntityData<Entity> {

	/**
	 * Extra names for entities whose registry/translated name differs from the name modern Skript uses.
	 * Key: {@code modid:registryname}. The first name is the one used to display the entity.
	 */
	private static final Map<String, String[]> EXTRA_NAMES = new HashMap<>();

	/**
	 * Mod entities that are implementation details and should not get a plain name.
	 */
	private static final Set<String> NO_PLAIN_NAME = new HashSet<>(Arrays.asList(
			"etfuturum:new_boat_seat", "etfuturum:boosting_firework_rocket", "etfuturum:fireproof_item",
			"etfuturum:ender_dragon", "etfuturum:new_boat"));

	static {
		// Et Futurum Requiem: names of the entities added after 1.7.10, as Skript calls them
		EXTRA_NAMES.put("etfuturum:wooden_armorstand", new String[] {"armor stand", "armour stand"});
		EXTRA_NAMES.put("etfuturum:villager_zombie", new String[] {"zombie villager"});
		EXTRA_NAMES.put("etfuturum:end_crystal", new String[] {"end crystal"});
		EXTRA_NAMES.put("etfuturum:shulker_candy", new String[] {"shulker bullet"});
		EXTRA_NAMES.put("etfuturum:lingering_effect", new String[] {"area effect cloud", "lingering effect"});
		EXTRA_NAMES.put("etfuturum:lingering_potion", new String[] {"lingering potion"});
		EXTRA_NAMES.put("etfuturum:tipped_arrow", new String[] {"tipped arrow"});
		EXTRA_NAMES.put("etfuturum:spectral_arrow", new String[] {"spectral arrow"});
		EXTRA_NAMES.put("etfuturum:chest_boat", new String[] {"chest boat", "boat with chest", "boat with a chest"});
		EXTRA_NAMES.put("etfuturum:brown_mooshroom", new String[] {"brown mooshroom", "brown mushroom cow"});
		EXTRA_NAMES.put("etfuturum:polar_bear", new String[] {"polar bear"});
		EXTRA_NAMES.put("etfuturum:falling_dripstone", new String[] {"falling dripstone"});
		EXTRA_NAMES.put("etfuturum:snow_golem", new String[] {"etfuturum snow golem"});
	}

	/**
	 * Names entities can only be parsed with if they are a whole word sequence that doesn't start with an age.
	 */
	private static final Pattern VALID_NAME = Pattern.compile("[a-z][a-z -]*[a-z]");

	/** Name (lowercase, singular) -> mod entity */
	private static final Map<String, ModEntity> names = new LinkedHashMap<>();
	/** Bukkit name (lowercase) -> name used to display the entity */
	private static final Map<String, String> displayNames = new HashMap<>();

	static {
		buildNames();
		Class<? extends Entity> entityClass = Entity.class;
		try {
			// So that EntityData.fromEntity() asks this class about mod entities (vanilla entities are matched earlier)
			@SuppressWarnings("unchecked")
			Class<? extends Entity> craftEntity = (Class<? extends Entity>) Class.forName(
					org.bukkit.Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity");
			if (Entity.class.isAssignableFrom(craftEntity))
				entityClass = craftEntity;
		} catch (ClassNotFoundException | LinkageError | RuntimeException ignored) {}
		register(entityClass);
		if (!names.isEmpty())
			Skript.info("Mod entities: " + displayNames.size() + " types (" + names.size() + " names)");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void register(Class<? extends Entity> entityClass) {
		EntityData.register((Class) ForgeEntityData.class, "forge entity", (Class) entityClass, 0,
				"forge entity", "forge entity s", "forge entity es", "forge entity ies");
	}

	private static void addName(String name, ModEntity entity) {
		name = name.toLowerCase(Locale.ENGLISH).trim().replaceAll("\\s+", " ");
		if (!VALID_NAME.matcher(name).matches() || names.containsKey(name))
			return;
		if (startsWithAge(name))
			return;
		// Never take a name from vanilla Skript entities (checked before this class registers its own patterns)
		try (BlockingLogHandler ignored = new BlockingLogHandler().start()) {
			if (EntityData.parse(name) != null)
				return;
		} catch (RuntimeException e) {
			return;
		}
		names.put(name, entity);
		displayNames.putIfAbsent(entity.bukkitName.toLowerCase(Locale.ENGLISH), name);
	}

	private static boolean startsWithAge(String name) {
		return name.matches("(baby|young|adult|grown) .*");
	}

	private static void buildNames() {
		List<ModEntity> entities = new ArrayList<>(ForgeRegistry.getModEntities());
		if (entities.isEmpty())
			return;
		entities.sort((a, b) -> {
			int c = ForgeRegistry.priorityKey(a.modId).compareTo(ForgeRegistry.priorityKey(b.modId));
			return c != 0 ? c : a.registryName.compareToIgnoreCase(b.registryName);
		});

		Map<ModEntity, List<String>> plainNames = new LinkedHashMap<>();
		for (ModEntity entity : entities) {
			String key = (entity.modId + ":" + entity.registryName).toLowerCase(Locale.ENGLISH);
			List<String> candidates = new ArrayList<>();
			String[] extra = EXTRA_NAMES.get(key);
			if (extra != null)
				candidates.addAll(Arrays.asList(extra));
			if (entity.translatedName != null)
				candidates.add(ForgeRegistry.toWords(entity.translatedName));
			candidates.add(entityWords(entity.registryName));
			plainNames.put(entity, NO_PLAIN_NAME.contains(key) ? Collections.<String>emptyList() : candidates);
		}

		// Plain names first, in priority order (Et Futurum Requiem, Netherlicious, then the other mods)...
		for (Map.Entry<ModEntity, List<String>> e : plainNames.entrySet()) {
			for (String name : e.getValue())
				addName(name, e.getKey());
		}
		// ... then the names with the mod id, which are always available
		for (ModEntity entity : entities) {
			String key = (entity.modId + ":" + entity.registryName).toLowerCase(Locale.ENGLISH);
			String modId = ForgeRegistry.toWords(entity.modId);
			String[] extra = EXTRA_NAMES.get(key);
			if (extra != null) {
				for (String name : extra)
					addName(name.startsWith(modId + " ") ? name : modId + " " + name, entity);
			}
			if (entity.translatedName != null)
				addName(modId + " " + ForgeRegistry.toWords(entity.translatedName), entity);
			addName(modId + " " + entityWords(entity.registryName), entity);
			addName(modId + " " + ForgeRegistry.toWords(entity.registryName), entity);
		}
	}
	
	/**
	 * @return Words of an entity's registry name, without the "entity" many mods put in it
	 * ({@code EntityHoglin} -> {@code hoglin}, {@code strider_entity} -> {@code strider}).
	 */
	private static String entityWords(String registryName) {
		String words = ForgeRegistry.toWords(registryName);
		String stripped = words.replaceAll("^(entity|ent) ", "").replaceAll(" entity$", "").trim();
		return stripped.isEmpty() ? words : stripped;
	}

	/**
	 * @return All names mod entities can be written with (lowercase, singular).
	 */
	public static Map<String, ModEntity> getNames() {
		return Collections.unmodifiableMap(names);
	}

	/**
	 * Bukkit name of the mod entity type (EntityType#getName on Crucible), e.g. {@code etfuturum-rabbit}.
	 */
	@Nullable
	private String type;

	public ForgeEntityData() {}

	public ForgeEntityData(ModEntity entity) {
		type = entity.bukkitName;
	}

	@Nullable
	private ModEntity getModEntity() {
		String t = type;
		return t == null ? null : ForgeRegistry.getModEntity(t);
	}

	@Override
	protected boolean init(Literal<?>[] exprs, int matchedPattern, ParseResult parseResult) {
		if (parseResult.regexes.isEmpty())
			return false;
		String name = parseResult.regexes.get(0).group().toLowerCase(Locale.ENGLISH).trim().replaceAll("\\s+", " ");
		if (matchedPattern == 3) // plural 'ies', e.g. "fireflies"
			name = name + "y";
		ModEntity entity = names.get(name);
		if (entity == null)
			return false;
		type = entity.bukkitName;
		this.matchedPattern = 0; // "rabbit" and "rabbits" are the same type
		return true;
	}

	@Override
	protected boolean init(@Nullable Class<? extends Entity> c, @Nullable Entity e) {
		if (e == null)
			return false;
		ModEntity entity = ForgeRegistry.getModEntity(e);
		if (entity == null)
			return false;
		type = entity.bukkitName;
		return true;
	}

	@Override
	public void set(Entity entity) {}

	@Override
	protected boolean match(Entity entity) {
		ModEntity modEntity = ForgeRegistry.getModEntity(entity);
		return modEntity != null && modEntity.bukkitName.equalsIgnoreCase(type);
	}

	@Override
	public Class<? extends Entity> getType() {
		ModEntity entity = getModEntity();
		return entity == null ? Entity.class : entity.getBukkitClass();
	}

	@Override
	public EntityData getSuperType() {
		ForgeEntityData data = new ForgeEntityData();
		data.type = type;
		return data;
	}

	@Override
	public boolean isSupertypeOf(EntityData<?> e) {
		return e instanceof ForgeEntityData && type != null && type.equalsIgnoreCase(((ForgeEntityData) e).type);
	}

	/**
	 * Spawns the mod entity through Crucible, which creates Forge entities for {@code World#spawnEntity}.
	 */
	@Override
	@Nullable
	public Entity spawn(Location loc, @Nullable Consumer<Entity> consumer) {
		String t = type;
		if (t == null)
			return null;
		org.bukkit.entity.EntityType bukkitType = org.bukkit.entity.EntityType.fromName(t);
		if (bukkitType == null)
			return null;
		Entity entity;
		try {
			entity = loc.getWorld().spawnEntity(loc, bukkitType);
		} catch (RuntimeException e) {
			if (Skript.debug())
				Skript.exception(e, "Could not spawn " + t);
			return null;
		}
		if (entity == null)
			return null;
		ForgeRegistry.initializeSpawnedMob(entity);
		if (isBaby().isTrue())
			EntityUtils.setBaby(entity);
		else if (isBaby().isFalse())
			EntityUtils.setAdult(entity);
		if (consumer != null)
			consumer.accept(entity);
		return entity;
	}

	private String getDisplayName() {
		String t = type;
		if (t == null)
			return "mod entity";
		String name = displayNames.get(t.toLowerCase(Locale.ENGLISH));
		return name != null ? name : t;
	}

	@Override
	public String toString(int flags) {
		String name = getDisplayName();
		if (isBaby().isTrue())
			name = "baby " + name;
		else if (isBaby().isFalse())
			name = "adult " + name;
		boolean plural = (flags & Language.F_PLURAL) != 0;
		if (plural)
			name = Utils.toEnglishPlural(name);
		if ((flags & Language.F_DEFINITE_ARTICLE) != 0)
			return "the " + name;
		if ((flags & Language.F_INDEFINITE_ARTICLE) != 0 && !plural)
			return Utils.a(name);
		return name;
	}

	@Override
	protected int hashCode_i() {
		String t = type;
		return t == null ? 0 : t.toLowerCase(Locale.ENGLISH).hashCode();
	}

	@Override
	protected boolean equals_i(EntityData<?> obj) {
		if (!(obj instanceof ForgeEntityData))
			return false;
		String t = type, o = ((ForgeEntityData) obj).type;
		return t == null ? o == null : t.equalsIgnoreCase(o);
	}

	@Override
	public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
		super.deserialize(fields);
		if (getModEntity() == null) // The mod was removed
			throw new StreamCorruptedException("Unknown mod entity " + type);
	}

}
