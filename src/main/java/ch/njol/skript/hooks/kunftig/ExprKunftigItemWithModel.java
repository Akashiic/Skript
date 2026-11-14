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

import com.akashic.kunftig.api.bukkit.persistence.NamespacedKey;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.util.Kleenean;

@Name("Item with Model (Kunftig)")
@Description("A copy of an item with a Kunftig item model (see the item model expression).")
@Examples({"give player paper with item model \"myserver:coin\"",
		"set slot 4 of player's current inventory to raw fish named \"Tucunaré\" with model \"akashicfishing:tucunare\""})
@RequiredPlugins("Kunftig mod (and a Kunftig client to see it)")
@Since("2.6.3-crucible")
public class ExprKunftigItemWithModel extends PropertyExpression<ItemType, ItemType> {
	
	static {
		Skript.registerExpression(ExprKunftigItemWithModel.class, ItemType.class, ExpressionType.PROPERTY,
				"%itemtypes% with [[a] custom] [item] model [id] %string%");
	}
	
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<String> model;
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setExpr((Expression<? extends ItemType>) exprs[0]);
		model = (Expression<String>) exprs[1];
		return true;
	}
	
	@Override
	protected ItemType[] get(Event e, ItemType[] source) {
		String modelName = model.getSingle(e);
		NamespacedKey key = modelName == null ? null : ExprKunftigItemModel.parseModel(modelName);
		if (key == null || !KunftigSupport.itemDataAvailable())
			return source;
		return get(source, item -> {
			ItemType copy = item.clone();
			KunftigSupport.write(copy, data -> ExprKunftigItemModel.setModel(data, key));
			return copy;
		});
	}
	
	@Override
	public Class<? extends ItemType> getReturnType() {
		return ItemType.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return getExpr().toString(e, debug) + " with item model " + model.toString(e, debug);
	}
	
}
