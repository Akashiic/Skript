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
import java.util.List;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.persistence.NamespacedKey;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataContainer;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Persistent Data of Items (Kunftig)")
@Description({"Values stored on an item that stay on it wherever it goes: inventories, chests, dropped on the ground, "
		+ "across restarts. Like the PersistentDataContainer of Minecraft 1.14+, with the same layout on the item.",
		"A key is either 'namespace:key' (to share data with a plugin, e.g. \"akashicfishing:species\") or just 'key', "
		+ "which is 'skript:key'. Values can be texts, numbers and booleans; adding and removing numbers works too.",
		"Change the data of a slot (e.g. the player's tool, a slot of an inventory) or of an item in a variable: "
		+ "the change is written back. Items kept in Skript variables lose their persistent data on a restart; "
		+ "the items themselves (in inventories, chests, the world) keep it."})
@Examples({"set persistent data \"coins\" of player's tool to 10",
		"add 5 to persistent data \"coins\" of player's tool",
		"if persistent data \"akashicfishing:species\" of player's tool is \"tucunare\":",
		"delete persistent data \"coins\" of player's tool",
		"send \"%persistent data keys of player's tool%\""})
@RequiredPlugins("Kunftig mod")
@Since("2.6.3-crucible")
public class ExprKunftigPersistentData extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprKunftigPersistentData.class, Object.class, ExpressionType.PROPERTY,
				"[the] persistent [item] (data|tag|value)[s] %strings% of %itemtypes/itemstacks/slots%",
				"%itemtypes/itemstacks/slots%'[s] persistent [item] (data|tag|value)[s] %strings%",
				"[all [[of] the]|the] persistent [item] (data|tag) keys of %itemtypes/itemstacks/slots%",
				"%itemtypes/itemstacks/slots%'[s] persistent [item] (data|tag) keys");
	}

	@Nullable
	private Expression<String> keys;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?> holders;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		switch (matchedPattern) {
			case 0:
				keys = (Expression<String>) exprs[0];
				holders = exprs[1];
				break;
			case 1:
				holders = exprs[0];
				keys = (Expression<String>) exprs[1];
				break;
			default:
				holders = exprs[0];
		}
		return true;
	}

	@Override
	@Nullable
	protected Object[] get(Event e) {
		if (!KunftigSupport.itemDataAvailable())
			return new Object[0];
		List<Object> values = new ArrayList<>();
		Expression<String> keys = this.keys;
		String[] keyNames = keys == null ? null : keys.getArray(e);
		for (Object holder : holders.getArray(e)) {
			PersistentDataContainer data = KunftigSupport.read(holder);
			if (data == null)
				continue;
			if (keyNames == null) { // All keys
				for (NamespacedKey key : data.getKeys())
					values.add(key.toString());
				continue;
			}
			for (String keyName : keyNames) {
				NamespacedKey key = KunftigSupport.parseKey(keyName);
				if (key == null)
					continue;
				Object value = KunftigSupport.get(data, key);
				if (value != null)
					values.add(value);
			}
		}
		return values.toArray();
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (keys == null) // Keys can only be deleted one by one
			return null;
		switch (mode) {
			case SET:
				return CollectionUtils.array(String.class, Number.class, Boolean.class);
			case ADD:
			case REMOVE:
				return CollectionUtils.array(Number.class);
			case DELETE:
			case RESET:
				return CollectionUtils.array();
			default:
				return null;
		}
	}

	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		Expression<String> keys = this.keys;
		if (keys == null || !KunftigSupport.itemDataAvailable())
			return;
		List<NamespacedKey> parsedKeys = new ArrayList<>();
		for (String keyName : keys.getArray(e)) {
			NamespacedKey key = KunftigSupport.parseKey(keyName);
			if (key != null)
				parsedKeys.add(key);
		}
		if (parsedKeys.isEmpty())
			return;
		Object value = delta == null || delta.length == 0 ? null : delta[0];
		if (value == null && (mode == ChangeMode.SET || mode == ChangeMode.ADD || mode == ChangeMode.REMOVE))
			return;

		for (Object holder : holders.getArray(e)) {
			KunftigSupport.write(holder, data -> {
				for (NamespacedKey key : parsedKeys) {
					switch (mode) {
						case SET:
							assert value != null;
							KunftigSupport.set(data, key, value);
							break;
						case ADD:
						case REMOVE:
							assert value != null;
							Object old = KunftigSupport.get(data, key);
							double base = old instanceof Number ? ((Number) old).doubleValue() : 0;
							double amount = ((Number) value).doubleValue();
							KunftigSupport.set(data, key, mode == ChangeMode.ADD ? base + amount : base - amount);
							break;
						case DELETE:
						case RESET:
							data.remove(key);
							break;
						default:
							assert false;
					}
				}
			});
		}
	}

	@Override
	public boolean isSingle() {
		Expression<String> keys = this.keys;
		return keys != null && keys.isSingle() && holders.isSingle();
	}

	@Override
	public Class<?> getReturnType() {
		return keys == null ? String.class : Object.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		Expression<String> keys = this.keys;
		if (keys == null)
			return "persistent data keys of " + holders.toString(e, debug);
		return "persistent data " + keys.toString(e, debug) + " of " + holders.toString(e, debug);
	}

}
