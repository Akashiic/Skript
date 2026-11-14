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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;

/**
 * Access to Forge's registries on Forge/Bukkit hybrids (Crucible, Thermos, KCauldron), through reflection only:
 * this class links to nothing outside the Bukkit API, so it can be loaded anywhere (it just returns nothing
 * when the server doesn't run Forge).
 * <p>
 * At runtime Minecraft classes have their MCP names but SRG member names (e.g. {@code func_82833_r}); FML and
 * CraftBukkit classes are not obfuscated. Every lookup tries the SRG name first and the MCP name second
 * (development environments), and is resolved once.
 */
public final class ForgeRegistry {

	private ForgeRegistry() {}

	/**
	 * A registry name, e.g. {@code netherlicious:CrimsonStem}.
	 */
	public static final class RegistryName {
		public final String modId, name;

		RegistryName(String modId, String name) {
			this.modId = modId;
			this.name = name;
		}

		@Override
		public String toString() {
			return modId + ":" + name;
		}
	}

	/**
	 * An entity type registered by a mod.
	 */
	public static final class ModEntity {
		/** Name of the Bukkit EntityType Crucible created for it, e.g. {@code etfuturum-polarbear} */
		public final String bukkitName;
		public final String modId;
		/** Name the mod registered it with, e.g. {@code polar_bear} */
		public final String registryName;
		/** Translated name if the server has it, e.g. {@code Polar Bear} */
		@Nullable
		public final String translatedName;
		public final Class<?> entityClass;

		ModEntity(String bukkitName, String modId, String registryName, @Nullable String translatedName, Class<?> entityClass) {
			this.bukkitName = bukkitName;
			this.modId = modId;
			this.registryName = registryName;
			this.translatedName = translatedName;
			this.entityClass = entityClass;
		}

		@Nullable
		private Class<? extends Entity> bukkitClass;
		
		/**
		 * @return The most specific Bukkit interface the wrapper of this entity implements (e.g. Animals), so that
		 * Skript knows whether it's a living entity, a projectile...
		 */
		public Class<? extends Entity> getBukkitClass() {
			Class<? extends Entity> c = bukkitClass;
			if (c == null)
				bukkitClass = c = findBukkitClass(entityClass);
			return c;
		}
		
		@Override
		public String toString() {
			return modId + ":" + registryName + " (" + bukkitName + ")";
		}
	}
	
	@SuppressWarnings("unchecked")
	static Class<? extends Entity> findBukkitClass(Class<?> nmsClass) {
		// Crucible picks the CraftBukkit wrapper class for a Minecraft entity class
		Class<?> craftEntity = findClass(craftBukkitPackage() + ".entity.CraftEntity");
		Class<?> nmsEntity = findClass("net.minecraft.entity.Entity");
		if (craftEntity != null && nmsEntity != null && nmsEntity.isAssignableFrom(nmsClass)) {
			Object wrapper = invoke(findMethod(craftEntity, new Class<?>[] {Class.class}, "getEntityClass"), null, nmsClass);
			if (wrapper instanceof Class) {
				Class<? extends Entity> best = Entity.class;
				for (Class<?> i : allInterfaces((Class<?>) wrapper)) {
					if (Entity.class.isAssignableFrom(i) && i.getName().startsWith("org.bukkit.entity.") && best.isAssignableFrom(i))
						best = (Class<? extends Entity>) i;
				}
				return best;
			}
		}
		Class<?> livingBase = findClass("net.minecraft.entity.EntityLivingBase");
		if (livingBase != null && livingBase.isAssignableFrom(nmsClass))
			return org.bukkit.entity.LivingEntity.class;
		Class<?> projectile = findClass("net.minecraft.entity.IProjectile");
		if (projectile != null && projectile.isAssignableFrom(nmsClass))
			return org.bukkit.entity.Projectile.class;
		return Entity.class;
	}
	
	private static java.util.Set<Class<?>> allInterfaces(Class<?> c) {
		java.util.Set<Class<?>> set = new java.util.LinkedHashSet<>();
		for (Class<?> cl = c; cl != null; cl = cl.getSuperclass()) {
			for (Class<?> i : cl.getInterfaces())
				addInterface(i, set);
		}
		return set;
	}
	
	private static void addInterface(Class<?> i, java.util.Set<Class<?>> set) {
		if (set.add(i)) {
			for (Class<?> s : i.getInterfaces())
				addInterface(s, set);
		}
	}

	@Nullable
	private static Class<?> findClass(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
	}

	@Nullable
	private static Method findMethod(@Nullable Class<?> c, Class<?>[] params, String... names) {
		if (c == null)
			return null;
		for (String name : names) {
			for (Class<?> cl = c; cl != null; cl = cl.getSuperclass()) {
				try {
					Method m = cl.getDeclaredMethod(name, params);
					m.setAccessible(true);
					return m;
				} catch (NoSuchMethodException ignored) {
				} catch (RuntimeException | LinkageError e) { // InaccessibleObjectException
					break;
				}
			}
		}
		return null;
	}

	@Nullable
	private static Object invoke(@Nullable Method m, @Nullable Object target, Object... args) {
		if (m == null)
			return null;
		try {
			return m.invoke(target, args);
		} catch (Throwable e) { // Mods can throw anything for odd metadata etc.
			return null;
		}
	}

	private static String craftBukkitPackage() {
		return Bukkit.getServer().getClass().getPackage().getName(); // org.bukkit.craftbukkit.v1_7_R4
	}

	// --- Translations ---

	private static boolean translationLookupDone;
	@Nullable
	private static Method translateToLocal, canTranslate;

	/**
	 * @param key Translation key, e.g. {@code entity.etfuturum.rabbit.name}.
	 * @return The server's translation, or null if there is none.
	 */
	@Nullable
	public static String translate(String key) {
		if (!translationLookupDone) {
			translationLookupDone = true;
			Class<?> statCollector = findClass("net.minecraft.util.StatCollector");
			translateToLocal = findMethod(statCollector, new Class<?>[] {String.class}, "func_74838_a", "translateToLocal");
			canTranslate = findMethod(statCollector, new Class<?>[] {String.class}, "func_94522_b", "canTranslate");
		}
		if (canTranslate != null && !Boolean.TRUE.equals(invoke(canTranslate, null, key)))
			return null;
		Object result = invoke(translateToLocal, null, key);
		if (!(result instanceof String) || result.equals(key))
			return null;
		return (String) result;
	}

	// --- Items and blocks ---

	private static boolean itemLookupDone;
	@Nullable
	private static Method getItem, getBlock, findIdForItem, findIdForBlock, asNMSCopy, getDisplayName, getUnlocalizedName, getHasSubtypes;
	@Nullable
	private static Field idModId, idName;

	private static void lookupItems() {
		if (itemLookupDone)
			return;
		itemLookupDone = true;
		Class<?> magicNumbers = findClass(craftBukkitPackage() + ".util.CraftMagicNumbers");
		Class<?> craftItemStack = findClass(craftBukkitPackage() + ".inventory.CraftItemStack");
		Class<?> item = findClass("net.minecraft.item.Item");
		Class<?> block = findClass("net.minecraft.block.Block");
		Class<?> nmsStack = findClass("net.minecraft.item.ItemStack");
		Class<?> gameRegistry = findClass("cpw.mods.fml.common.registry.GameRegistry");
		Class<?> uniqueId = findClass("cpw.mods.fml.common.registry.GameRegistry$UniqueIdentifier");
		if (magicNumbers == null || item == null || gameRegistry == null || uniqueId == null)
			return;
		getItem = findMethod(magicNumbers, new Class<?>[] {Material.class}, "getItem");
		getBlock = findMethod(magicNumbers, new Class<?>[] {Material.class}, "getBlock");
		findIdForItem = findMethod(gameRegistry, new Class<?>[] {item}, "findUniqueIdentifierFor");
		if (block != null)
			findIdForBlock = findMethod(gameRegistry, new Class<?>[] {block}, "findUniqueIdentifierFor");
		asNMSCopy = findMethod(craftItemStack, new Class<?>[] {ItemStack.class}, "asNMSCopy");
		getDisplayName = findMethod(nmsStack, new Class<?>[0], "func_82833_r", "getDisplayName");
		getUnlocalizedName = findMethod(nmsStack, new Class<?>[0], "func_77977_a", "getUnlocalizedName");
		getHasSubtypes = findMethod(item, new Class<?>[0], "func_77614_k", "getHasSubtypes");
		try {
			idModId = uniqueId.getField("modId");
			idName = uniqueId.getField("name");
		} catch (NoSuchFieldException e) {
			idModId = idName = null;
		}
	}

	/**
	 * @param material A material.
	 * @return The Forge registry name of its item (or block), e.g. {@code netherlicious:CrimsonStem}, or null.
	 */
	@Nullable
	public static RegistryName getRegistryName(Material material) {
		lookupItems();
		if (idModId == null || idName == null)
			return null;
		Object id = null;
		Object item = invoke(getItem, null, material);
		if (item != null)
			id = invoke(findIdForItem, null, item);
		if (id == null) {
			Object block = invoke(getBlock, null, material);
			if (block != null)
				id = invoke(findIdForBlock, null, block);
		}
		if (id == null)
			return null;
		try {
			Object modId = idModId.get(id), name = idName.get(id);
			if (modId instanceof String && name instanceof String)
				return new RegistryName((String) modId, (String) name);
		} catch (IllegalAccessException ignored) {}
		return null;
	}

	/**
	 * @param material A material.
	 * @return Whether its item uses metadata for subtypes (e.g. colors or wood types).
	 */
	public static boolean hasSubtypes(Material material) {
		lookupItems();
		Object item = invoke(getItem, null, material);
		return item != null && Boolean.TRUE.equals(invoke(getHasSubtypes, item));
	}

	/**
	 * @param material A material.
	 * @param data Data value (metadata).
	 * @return The name the server gives this item stack, e.g. {@code Crimson Planks}, or null if it has none
	 * (untranslated names are turned into words from their translation key).
	 */
	@Nullable
	public static String getDisplayName(Material material, short data) {
		lookupItems();
		Object nms = invoke(asNMSCopy, null, new ItemStack(material, 1, data));
		if (nms == null)
			return null;
		Object name = invoke(getDisplayName, nms);
		if (name instanceof String && !((String) name).isEmpty() && !((String) name).contains(".name") && !((String) name).startsWith("tile.") && !((String) name).startsWith("item."))
			return (String) name;
		// Untranslated: use the translation key, e.g. tile.netherlicious.crimson_planks -> crimson planks
		Object key = invoke(getUnlocalizedName, nms);
		if (!(key instanceof String))
			return null;
		String k = (String) key;
		if (k.endsWith(".name"))
			k = k.substring(0, k.length() - 5);
		int dot = k.lastIndexOf('.');
		k = dot == -1 ? k : k.substring(dot + 1);
		return k.isEmpty() || k.equals("null") ? null : k;
	}

	// --- Entities ---

	@Nullable
	private static volatile List<ModEntity> modEntities;
	private static final Map<String, ModEntity> byBukkitName = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ModEntity> byClass = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Method> getHandleMethods = new ConcurrentHashMap<>();

	/**
	 * @return The entity types registered by mods (Forge entities Crucible made a Bukkit EntityType for).
	 */
	@SuppressWarnings("unchecked")
	public static List<ModEntity> getModEntities() {
		List<ModEntity> list = modEntities;
		if (list != null)
			return list;
		list = new ArrayList<>();
		Class<?> entityRegistry = findClass("cpw.mods.fml.common.registry.EntityRegistry");
		if (entityRegistry != null) {
			Map<Class<?>, String> typeMap = Collections.emptyMap();
			try {
				// Cauldron/Crucible addition: entity class -> Bukkit EntityType name ("modid-name")
				typeMap = new LinkedHashMap<>((Map<Class<?>, String>) entityRegistry.getField("entityTypeMap").get(null));
			} catch (ReflectiveOperationException | RuntimeException e) {
				if (Skript.debug())
					Skript.exception(e, "Could not read Forge's entity types");
			}
			Object registry = invoke(findMethod(entityRegistry, new Class<?>[0], "instance"), null);
			Method lookupModSpawn = findMethod(entityRegistry, new Class<?>[] {Class.class, boolean.class}, "lookupModSpawn");
			for (Map.Entry<Class<?>, String> entry : typeMap.entrySet()) {
				Class<?> cls = entry.getKey();
				String bukkitName = entry.getValue();
				if (cls == null || bukkitName == null)
					continue;
				String modId = null, registryName = null;
				Object registration = registry == null ? null : invoke(lookupModSpawn, registry, cls, false);
				if (registration != null) {
					Object name = invoke(findMethod(registration.getClass(), new Class<?>[0], "getEntityName"), registration);
					Object container = invoke(findMethod(registration.getClass(), new Class<?>[0], "getContainer"), registration);
					Object id = container == null ? null : invoke(findMethod(container.getClass(), new Class<?>[0], "getModId"), container);
					if (name instanceof String && id instanceof String) {
						registryName = (String) name;
						modId = (String) id;
					}
				}
				if (modId == null || registryName == null) { // Globally registered entity: only Crucible's name is known
					int dash = bukkitName.indexOf('-');
					modId = dash == -1 ? "unknown" : bukkitName.substring(0, dash);
					registryName = dash == -1 ? bukkitName : bukkitName.substring(dash + 1);
				}
				String translated = translate("entity." + modId + "." + registryName + ".name");
				if (translated == null)
					translated = translate("entity." + registryName + ".name");
				ModEntity modEntity = new ModEntity(bukkitName, modId, registryName, translated, cls);
				list.add(modEntity);
				byBukkitName.put(bukkitName.toLowerCase(Locale.ENGLISH), modEntity);
				byClass.put(cls, modEntity);
			}
		}
		list = Collections.unmodifiableList(list);
		modEntities = list;
		return list;
	}

	@Nullable
	public static ModEntity getModEntity(String bukkitName) {
		getModEntities();
		return byBukkitName.get(bukkitName.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * @param entity An entity.
	 * @return The Minecraft entity behind a Bukkit entity, or null.
	 */
	@Nullable
	public static Object getHandle(Entity entity) {
		Method m = getHandleMethods.get(entity.getClass());
		if (m == null) {
			m = findMethod(entity.getClass(), new Class<?>[0], "getHandle");
			if (m == null)
				return null;
			getHandleMethods.put(entity.getClass(), m);
		}
		return invoke(m, entity);
	}

	/**
	 * @param entity An entity.
	 * @return The mod entity type of this entity, or null if it's not a mod entity.
	 */
	@Nullable
	public static ModEntity getModEntity(Entity entity) {
		getModEntities();
		if (byClass.isEmpty())
			return null;
		// Crucible gives living and plain mod entities their mod EntityType...
		org.bukkit.entity.EntityType type = entity.getType();
		if (type != null && type.getName() != null) {
			ModEntity modEntity = byBukkitName.get(type.getName().toLowerCase(Locale.ENGLISH));
			if (modEntity != null)
				return modEntity;
		}
		// ... but mod entities extending vanilla ones (zombies, arrows...) report the vanilla type
		Object handle = getHandle(entity);
		return handle == null ? null : byClass.get(handle.getClass());
	}

	private static boolean spawnLookupDone;
	@Nullable
	private static Method onSpawnWithEgg;

	/**
	 * Lets a freshly spawned mob pick its random variant and equipment, like spawn eggs do
	 * ({@code EntityLiving#onSpawnWithEgg}). Crucible's {@code World#spawnEntity} doesn't do it for mod entities.
	 */
	public static void initializeSpawnedMob(Entity entity) {
		Object handle = getHandle(entity);
		if (handle == null)
			return;
		if (!spawnLookupDone) {
			spawnLookupDone = true;
			Class<?> entityLiving = findClass("net.minecraft.entity.EntityLiving");
			Class<?> livingData = findClass("net.minecraft.entity.IEntityLivingData");
			if (livingData != null)
				onSpawnWithEgg = findMethod(entityLiving, new Class<?>[] {livingData}, "func_110161_a", "onSpawnWithEgg");
		}
		Method m = onSpawnWithEgg;
		if (m != null && m.getDeclaringClass().isInstance(handle))
			invoke(m, handle, new Object[] {null});
	}

	// --- Names ---

	/**
	 * Turns a registry name or a display name into lowercase words: {@code CrimsonStem}, {@code crimson_stem} and
	 * {@code Crimson Stem} all become {@code crimson stem}. Color codes and anything but letters, digits, spaces and
	 * apostrophes are dropped.
	 * @param name Name.
	 * @return Words, possibly empty.
	 */
	public static String toWords(String name) {
		String s = name.replaceAll("§.", "");
		s = s.replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
		s = s.replaceAll("[_.:\\-/]+", " ").toLowerCase(Locale.ENGLISH);
		s = s.replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ").trim();
		return s;
	}

	/**
	 * Mods whose names win when two mods use the same plain name (e.g. "rabbit").
	 */
	private static final Map<String, Integer> MOD_PRIORITY = new HashMap<>();
	static {
		MOD_PRIORITY.put("etfuturum", 0);
		MOD_PRIORITY.put("netherlicious", 1);
	}

	/**
	 * @return A sort key for a mod id: Et Futurum Requiem, then Netherlicious, then the others alphabetically.
	 */
	public static String priorityKey(String modId) {
		Integer p = MOD_PRIORITY.get(modId.toLowerCase(Locale.ENGLISH));
		return (p == null ? 9 : p) + modId.toLowerCase(Locale.ENGLISH);
	}

}
