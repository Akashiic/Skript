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

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.persistence.KunftigItems;
import com.akashic.kunftig.api.bukkit.persistence.NamespacedKey;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataContainer;
import com.akashic.kunftig.api.bukkit.persistence.PersistentDataType;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;

@Name("Item Model (Kunftig)")
@Description({"The model (icon) of an item on clients with the Kunftig mod: the CustomModelData of Minecraft 1.14+ for 1.7.10, "
		+ "chosen by an id like 'namespace:name' that the resource pack lists in assets/<namespace>/kunftig/item_models.json. "
		+ "An id without a namespace uses 'skript'.",
		"Clients without Kunftig, or without the icon in their resource pack, see the item's normal icon. Works best on items "
		+ "with a simple icon (not blocks, potions or leather armor)."})
@Examples({"set item model of player's tool to \"akashicfishing:tucunare\"",
		"delete item model of player's tool",
		"if item model of player's tool is \"akashicfishing:tucunare\":"})
@RequiredPlugins("Kunftig mod (and a Kunftig client to see it)")
@Since("2.6.3-crucible")
public class ExprKunftigItemModel extends SimplePropertyExpression<Object, String> {
	
	static {
		register(ExprKunftigItemModel.class, String.class, "[custom] item model [id]", "itemtypes/itemstacks/slots");
	}
	
	/**
	 * Where Kunftig keeps the model id (same as {@link KunftigItems#MODEL_KEY}).
	 */
	static final NamespacedKey MODEL_KEY = new NamespacedKey("kunftig", "model");
	
	@Override
	@Nullable
	public String convert(Object holder) {
		if (!KunftigSupport.itemDataAvailable())
			return null;
		PersistentDataContainer data = KunftigSupport.read(holder);
		return data == null ? null : data.get(MODEL_KEY, PersistentDataType.STRING);
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET)
			return CollectionUtils.array(String.class);
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		if (!KunftigSupport.itemDataAvailable())
			return;
		NamespacedKey model = null;
		if (mode == ChangeMode.SET) {
			if (delta == null || delta.length == 0)
				return;
			model = parseModel((String) delta[0]);
			if (model == null)
				return;
		}
		NamespacedKey finalModel = model;
		for (Object holder : getExpr().getArray(e))
			KunftigSupport.write(holder, data -> setModel(data, finalModel));
	}
	
	/**
	 * @return The model id, or null (with a warning) if it isn't valid.
	 */
	@Nullable
	static NamespacedKey parseModel(String model) {
		return KunftigSupport.parseKey(model);
	}
	
	static void setModel(PersistentDataContainer data, @Nullable NamespacedKey model) {
		if (model == null)
			data.remove(MODEL_KEY);
		else
			data.set(MODEL_KEY, PersistentDataType.STRING, model.toString());
	}
	
	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "item model";
	}
	
}
