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

import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.persistence.KunftigItems;
import com.akashic.kunftig.api.bukkit.persistence.NamespacedKey;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataContainer;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ch.njol.skript.aliases.ItemData;

/**
 * Keeps Kunftig's persistent data (which includes the item model) on items stored in Skript variables.
 * 1.7.10's ItemMeta doesn't keep extra NBT, so without this the data would be lost on restart.
 * <p>
 * Saved as JSON: {@code {"ns:key": {"t": "s|l|d|i|h|f|b", "v": value}, ...}}. Arrays and nested containers
 * can't be saved this way and are skipped.
 */
public final class KunftigItemVariables implements ItemData.SerializerExtension {

	static {
		ItemData.registerSerializerExtension("kunftig", new KunftigItemVariables());
	}

	private KunftigItemVariables() {}

	@Override
	@Nullable
	public String save(ItemStack stack) {
		if (!KunftigItems.isAvailable() || !KunftigItems.canHoldData(stack))
			return null;
		PersistentDataContainer data = KunftigItems.getData(stack);
		if (data.isEmpty())
			return null;
		JsonObject json = new JsonObject();
		for (NamespacedKey key : data.getKeys()) {
			JsonObject entry = new JsonObject();
			if (data.has(key, PersistentDataType.STRING)) {
				entry.addProperty("t", "s");
				entry.addProperty("v", data.get(key, PersistentDataType.STRING));
			} else if (data.has(key, PersistentDataType.LONG)) {
				entry.addProperty("t", "l");
				entry.addProperty("v", data.get(key, PersistentDataType.LONG));
			} else if (data.has(key, PersistentDataType.DOUBLE)) {
				entry.addProperty("t", "d");
				entry.addProperty("v", data.get(key, PersistentDataType.DOUBLE));
			} else if (data.has(key, PersistentDataType.INTEGER)) {
				entry.addProperty("t", "i");
				entry.addProperty("v", data.get(key, PersistentDataType.INTEGER));
			} else if (data.has(key, PersistentDataType.SHORT)) {
				entry.addProperty("t", "h");
				entry.addProperty("v", data.get(key, PersistentDataType.SHORT));
			} else if (data.has(key, PersistentDataType.FLOAT)) {
				entry.addProperty("t", "f");
				entry.addProperty("v", data.get(key, PersistentDataType.FLOAT));
			} else if (data.has(key, PersistentDataType.BYTE)) {
				entry.addProperty("t", "b");
				entry.addProperty("v", data.get(key, PersistentDataType.BYTE));
			} else {
				continue;
			}
			json.add(key.toString(), entry);
		}
		return json.size() == 0 ? null : json.toString();
	}

	@Override
	@SuppressWarnings("deprecation") // JsonParser#parse keeps working with the gson versions servers ship
	public ItemStack load(ItemStack stack, String saved) {
		if (!KunftigItems.isAvailable())
			return stack;
		ItemStack result = KunftigItems.asDataCapable(stack);
		if (!KunftigItems.canHoldData(result))
			return result;
		PersistentDataContainer data = KunftigItems.getData(result);
		JsonObject json = new JsonParser().parse(saved).getAsJsonObject();
		for (Map.Entry<String, JsonElement> e : json.entrySet()) {
			NamespacedKey key = NamespacedKey.fromString(e.getKey());
			if (key == null || !e.getValue().isJsonObject())
				continue;
			JsonObject entry = e.getValue().getAsJsonObject();
			JsonElement value = entry.get("v");
			if (value == null || !entry.has("t"))
				continue;
			switch (entry.get("t").getAsString()) {
				case "s":
					data.set(key, PersistentDataType.STRING, value.getAsString());
					break;
				case "l":
					data.set(key, PersistentDataType.LONG, value.getAsLong());
					break;
				case "d":
					data.set(key, PersistentDataType.DOUBLE, value.getAsDouble());
					break;
				case "i":
					data.set(key, PersistentDataType.INTEGER, value.getAsInt());
					break;
				case "h":
					data.set(key, PersistentDataType.SHORT, value.getAsShort());
					break;
				case "f":
					data.set(key, PersistentDataType.FLOAT, value.getAsFloat());
					break;
				case "b":
					data.set(key, PersistentDataType.BYTE, value.getAsByte());
					break;
				default:
					break;
			}
		}
		return result;
	}

}
