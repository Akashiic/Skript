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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent;
import com.akashic.kunftig.api.bukkit.fishing.KunftigFishingBukkit;
import com.akashic.kunftig.api.bukkit.fishing.ModernFishHook;
import com.akashic.kunftig.api.bukkit.persistence.KunftigItems;
import com.akashic.kunftig.api.bukkit.persistence.NamespacedKey;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataContainer;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataType;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.slot.Slot;

/**
 * Shared code of the Kunftig hook: item persistent data and fishing hooks.
 * <p>
 * Everything in {@code hooks.kunftig} is only loaded when the Kunftig mod's API is on the server
 * (see {@link ch.njol.skript.hooks.ModHooks}). The API can still be "not available" (the mod disabled a feature),
 * so each feature checks at runtime and warns once.
 */
public final class KunftigSupport {

	private KunftigSupport() {}

	/**
	 * Namespace of keys written without one, e.g. {@code persistent data "coins" of tool} is {@code skript:coins}.
	 */
	public static final String DEFAULT_NAMESPACE = "skript";

	private static final Set<String> warned = new HashSet<>();

	/**
	 * Logs a warning once per server run.
	 */
	public static void warnOnce(String id, String message) {
		if (warned.add(id))
			Skript.warning(message);
	}

	// ---------------------------------------------------------------- keys

	/**
	 * @param key {@code namespace:key} or {@code key} (namespace {@value #DEFAULT_NAMESPACE}). Lowercased; spaces
	 *            become underscores.
	 * @return The key, or null (and a warning) if it is not a valid key.
	 */
	@Nullable
	public static NamespacedKey parseKey(@Nullable String key) {
		if (key == null)
			return null;
		String normalized = key.trim().toLowerCase(Locale.ENGLISH).replace(' ', '_');
		NamespacedKey parsed;
		if (normalized.indexOf(':') == -1) {
			try {
				parsed = new NamespacedKey(DEFAULT_NAMESPACE, normalized);
			} catch (IllegalArgumentException e) {
				parsed = null;
			}
		} else {
			parsed = NamespacedKey.fromString(normalized);
		}
		if (parsed == null)
			warnOnce("key:" + key, "'" + key + "' is not a valid persistent data key: use letters, digits, '.', '_', '-' and '/', "
					+ "optionally prefixed by a namespace like 'myplugin:'");
		return parsed;
	}

	// ---------------------------------------------------------------- item data

	/**
	 * @return Whether item persistent data works on this server (warns once if not).
	 */
	public static boolean itemDataAvailable() {
		if (KunftigItems.isAvailable())
			return true;
		warnOnce("itemdata", "Kunftig's item persistent data is not available on this server (is the Kunftig mod enabled?)");
		return false;
	}

	private static boolean isEmpty(@Nullable ItemStack stack) {
		return stack == null || stack.getType() == Material.AIR;
	}

	/**
	 * The persistent data of an item, for reading. Items that can't hold data have none.
	 */
	@Nullable
	public static PersistentDataContainer read(Object holder) {
		ItemStack stack;
		if (holder instanceof Slot) {
			stack = ((Slot) holder).getItem();
		} else if (holder instanceof ItemType) {
			List<ItemData> types = ((ItemType) holder).getTypes();
			stack = types.isEmpty() ? null : types.get(0).getStack();
		} else if (holder instanceof ItemStack) {
			stack = (ItemStack) holder;
		} else {
			return null;
		}
		if (isEmpty(stack) || !KunftigItems.canHoldData(stack))
			return null;
		return KunftigItems.getData(stack);
	}

	/**
	 * Changes the persistent data of an item and makes the change stick: slots get the changed item back, item types
	 * (e.g. in variables) are switched to server-backed copies that hold the data, and item stacks are changed in
	 * place when they are server-backed (items of inventories, entities and events on Crucible).
	 *
	 * @return Whether the item could be changed.
	 */
	public static boolean write(Object holder, Consumer<PersistentDataContainer> change) {
		if (holder instanceof Slot) {
			Slot slot = (Slot) holder;
			ItemStack stack = slot.getItem();
			if (isEmpty(stack))
				return false;
			stack = KunftigItems.asDataCapable(stack);
			change.accept(KunftigItems.getData(stack));
			slot.setItem(stack);
			return true;
		} else if (holder instanceof ItemType) {
			ItemType type = (ItemType) holder;
			boolean replaced = false;
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemData data : type.getTypes()) {
				ItemStack stack = data.getStack();
				if (isEmpty(stack)) {
					stacks.add(stack);
					continue;
				}
				ItemStack capable = KunftigItems.asDataCapable(stack);
				if (capable != stack)
					replaced = true;
				change.accept(KunftigItems.getData(capable));
				stacks.add(capable);
			}
			if (replaced) { // Bukkit stacks can't hold data: use the server-backed copies
				ItemType copy = new ItemType();
				for (ItemStack stack : stacks)
					copy.add(new ItemData(stack));
				copy.setAmount(type.getAmount());
				copy.setAll(type.isAll());
				type.setTo(copy);
			}
			return true;
		} else if (holder instanceof ItemStack) {
			ItemStack stack = (ItemStack) holder;
			if (isEmpty(stack))
				return false;
			if (!KunftigItems.canHoldData(stack)) {
				warnOnce("itemstack-copy", "This item can't hold persistent data and can't be replaced from here. "
						+ "Change the persistent data of a slot (e.g. the player's tool) or of an item in a variable instead.");
				return false;
			}
			change.accept(KunftigItems.getData(stack));
			return true;
		}
		return false;
	}

	/**
	 * Reads a value stored by Skript or by a plugin: text, a number or a boolean.
	 */
	@Nullable
	public static Object get(PersistentDataContainer data, NamespacedKey key) {
		if (!data.has(key))
			return null;
		if (data.has(key, PersistentDataType.STRING))
			return data.get(key, PersistentDataType.STRING);
		if (data.has(key, PersistentDataType.LONG))
			return data.get(key, PersistentDataType.LONG);
		if (data.has(key, PersistentDataType.DOUBLE))
			return data.get(key, PersistentDataType.DOUBLE);
		if (data.has(key, PersistentDataType.INTEGER))
			return data.get(key, PersistentDataType.INTEGER).longValue();
		if (data.has(key, PersistentDataType.SHORT))
			return data.get(key, PersistentDataType.SHORT).longValue();
		if (data.has(key, PersistentDataType.FLOAT))
			return data.get(key, PersistentDataType.FLOAT).doubleValue();
		if (data.has(key, PersistentDataType.BYTE)) {
			// Booleans are stored as bytes (PersistentDataType.BOOLEAN), as in modern Bukkit
			byte b = data.get(key, PersistentDataType.BYTE);
			return b == 0 || b == 1 ? Boolean.valueOf(b == 1) : Long.valueOf(b);
		}
		return null; // Arrays and nested containers aren't Skript values
	}

	/**
	 * Stores text as a string, whole numbers as longs, other numbers as doubles and booleans as
	 * {@link PersistentDataType#BOOLEAN}. Anything else is stored as its text.
	 */
	public static void set(PersistentDataContainer data, NamespacedKey key, Object value) {
		if (value instanceof Boolean) {
			data.set(key, PersistentDataType.BOOLEAN, (Boolean) value);
		} else if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			data.set(key, PersistentDataType.LONG, ((Number) value).longValue());
		} else if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < Long.MAX_VALUE)
				data.set(key, PersistentDataType.LONG, (long) d);
			else
				data.set(key, PersistentDataType.DOUBLE, d);
		} else {
			data.set(key, PersistentDataType.STRING, ch.njol.skript.registrations.Classes.toString(value));
		}
	}

	// ---------------------------------------------------------------- fishing

	/**
	 * The fishing hook of a fishing event ({@code on fish}, or one of Kunftig's fishing states), with 1.21.1's API.
	 */
	@Nullable
	public static ModernFishHook hookOf(Event event) {
		if (event instanceof KunftigFishEvent)
			return ((KunftigFishEvent) event).getHook();
		if (event instanceof PlayerFishEvent)
			return hookOf(((PlayerFishEvent) event).getHook());
		return null;
	}

	/**
	 * @return The hook entity with 1.21.1's API, or null if it isn't a fishing hook or Kunftig fishing is off.
	 */
	@Nullable
	public static ModernFishHook hookOf(@Nullable Entity entity) {
		if (!(entity instanceof FishHook))
			return null;
		if (!KunftigFishingBukkit.isAvailable()) {
			warnOnce("fishing", "Kunftig's fishing engine is not available on this server "
					+ "(is the Kunftig mod installed and fishing enabled in config/kunftig.cfg?)");
			return null;
		}
		return KunftigFishingBukkit.of((FishHook) entity);
	}

}
