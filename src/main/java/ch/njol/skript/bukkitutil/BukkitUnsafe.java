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
package ch.njol.skript.bukkitutil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ch.njol.util.EnumTypeAdapter;
import ch.njol.skript.Skript;
import ch.njol.skript.util.Version;

/**
 * Contains helpers for Bukkit's not so safe stuff.
 */
@SuppressWarnings("deprecation")
public class BukkitUnsafe {
	
	/**
	 * Bukkit's UnsafeValues allows us to do stuff that would otherwise
	 * require NMS. It has existed for a long time, too, so 1.9 support is
	 * not particularly hard to achieve.
	 * 
	 * UnsafeValues' existence and behavior is not guaranteed across future versions.
	 */
	@Nullable
	private static final UnsafeValues unsafe = Bukkit.getUnsafe();
	
	/**
	 * 1.9 Spigot has some "fun" bugs.
	 */
	private static final boolean knownNullPtr = !Skript.isRunningMinecraft(1, 11);
	
	static {
		if (unsafe == null)
			throw new Error("UnsafeValues are not available.");
	}
	
	/**
	 * Vanilla material names to Bukkit materials.
	 */
	@Nullable
	private static Map<String,Material> materialMap;
	
	/**
	 * Whether a material map exists for this exact Minecraft version.
	 */
	private static boolean preferMaterialMap = true;
	
	/**
	 * Maps pre 1.12 ids to materials for variable conversions.
	 */
	@Nullable
	private static Map<Integer,Material> idMappings;
	
	public static void initialize() {
		try {
			Version version = Skript.getMinecraftVersion();
			boolean mapExists = loadMaterialMap("materials/" + version.getMajor() + "." +  version.getMinor() + ".json");
			if (!mapExists) {
				loadMaterialMap("materials/1.9.json"); // Closest mappings we have
				preferMaterialMap = false;
				Skript.debug("Material mappings for " + version + " are not available, using UnsafeValues and 1.9 mappings.");
			}
		} catch (IOException e) {
			Skript.exception(e, "Failed to load material mappings. Aliases may not work properly.");
		}
	}
	
	/**
	 * Gets a material from an id. Minecraft 1.7.10 / Crucible accepts:
	 * <ul>
	 * <li>Vanilla ids, e.g. {@code minecraft:stone}</li>
	 * <li>Forge registry names of modded items/blocks, e.g. {@code IC2:itemCable}</li>
	 * <li>Bukkit material names, including the ones Crucible generates for mods, e.g. {@code IC2_ITEMCABLE}</li>
	 * <li>Numeric ids, e.g. {@code 4096}</li>
	 * </ul>
	 * @param id Id of material.
	 * @return Material or null if the server doesn't know it.
	 */
	@Nullable
	@SuppressWarnings("deprecation")
	public static Material getMaterialFromMinecraftId(String id) {
		boolean vanilla = id.startsWith("minecraft:");
		
		// If we have correct material map, prefer using it
		if (vanilla && preferMaterialMap && materialMap != null) {
			Material type = materialMap.get(id.substring(10));
			if (type != null)
				return type;
		}
		
		// Item registry lookup, works for both Vanilla and modded items
		if (id.indexOf(':') != -1) {
			Material type = null;
			try {
				assert unsafe != null;
				type = unsafe.getMaterialFromInternalName(id);
			} catch (Throwable e) {
				// Only spit out an error once unless debugging
				if (!unsafeValuesErrored || Skript.debug()) {
					Skript.exception(e, "UnsafeValues failed to get material from id " + id);
					unsafeValuesErrored = true;
				}
			}
			if (type != null && (type != Material.AIR || id.equals("minecraft:air")))
				return type;
			// If there is no item form, UnsafeValues won't work, so rely on material mappings
			if (vanilla && materialMap != null) {
				type = materialMap.get(id.substring(10));
				if (type != null)
					return type;
			}
			if (vanilla) // Bukkit name might still match the Vanilla name
				return Material.matchMaterial(id.substring(10));
			return null;
		}
		
		// Numeric id
		if (!id.isEmpty() && id.chars().allMatch(Character::isDigit)) {
			try {
				return Material.getMaterial(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
		// Bukkit material name (Crucible adds materials for modded items and blocks)
		return Material.matchMaterial(id);
	}
	
	/**
	 * We only spit one exception (unless debugging) from UnsafeValues.
	 */
	private static boolean unsafeValuesErrored;
	
	private static boolean loadMaterialMap(String name) throws IOException {
		try (InputStream is = Skript.getInstance().getResource(name)) {
			if (is == null) { // No mappings for this Minecraft version
				return false;
			}
			String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
			
			Type type = new TypeToken<Map<String,Material>>(){}.getType();
			materialMap = new GsonBuilder().
				registerTypeAdapterFactory(EnumTypeAdapter.factory)
				.create().fromJson(data, type);
		}
		
		return true;
	}
	
	public static void modifyItemStack(ItemStack stack, String arguments) {
		if (unsafe == null)
			throw new IllegalStateException("modifyItemStack could not be performed as UnsafeValues are not available.");
		assert unsafe != null;
		try {
			unsafe.modifyItemStack(stack, arguments);
		} catch (NullPointerException e) {
			if (knownNullPtr) { // Probably known Spigot bug
				// So we continue doing whatever we were doing and hope it works
				Skript.warning("Item " + stack.getType() + arguments + " failed modifyItemStack. This is a bug on old Spigot versions.");
			} else { // Not known null pointer, don't just swallow
				throw e;
			}
		}
	}
	
	private static void initIdMappings() {
		try (InputStream is = Skript.getInstance().getResource("materials/ids.json")) {
			if (is == null) {
				throw new AssertionError("missing id mappings");
			}
			String data = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
			
			Type type = new TypeToken<Map<Integer,String>>(){}.getType();
			Map<Integer, String> rawMappings = new GsonBuilder().
				registerTypeAdapterFactory(EnumTypeAdapter.factory)
				.create().fromJson(data, type);
			
			// Process raw mappings
			Map<Integer, Material> parsed = new HashMap<>(rawMappings.size());
			for (Map.Entry<Integer, String> entry : rawMappings.entrySet()) {
				Material material = Material.getMaterial(entry.getValue());
				if (material != null) // Some materials in the table don't exist on 1.7.10
					parsed.put(entry.getKey(), material);
			}
			idMappings = parsed;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	@Nullable
	public static Material getMaterialFromId(int id) {
		if (idMappings == null) {
			initIdMappings();
		}
		assert idMappings != null;
		return idMappings.get(id);
	}
}
