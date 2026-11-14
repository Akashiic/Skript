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
package ch.njol.skript.hooks.crucible;

import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Item Damage Amount")
@Description("How much durability the item loses in an <a href='events.html#item_damage'>item damage</a> event. It can be changed.")
@Examples({"on item damage:",
		"\tif player has permission \"tools.sturdy\":",
		"\t\tset the item damage to 0"})
@Since("2.6.3-crucible")
@Events("item damage")
@RequiredPlugins("Crucible")
public class ExprItemDamageAmount extends SimpleExpression<Long> {
	
	static {
		Skript.registerExpression(ExprItemDamageAmount.class, Long.class, ExpressionType.SIMPLE, "[the] item damage [amount]");
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(PlayerItemDamageEvent.class)) {
			Skript.error("The item damage can only be used in an item damage event", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
	
	@Override
	@Nullable
	protected Long[] get(Event e) {
		if (!(e instanceof PlayerItemDamageEvent))
			return new Long[0];
		return new Long[] {(long) ((PlayerItemDamageEvent) e).getDamage()};
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (getParser().getHasDelayBefore().isTrue()) {
			Skript.error("Can't change the item damage anymore after the event has already passed");
			return null;
		}
		switch (mode) {
			case SET:
			case ADD:
			case REMOVE:
			case DELETE:
				return CollectionUtils.array(Number.class);
			default:
				return null;
		}
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		if (!(e instanceof PlayerItemDamageEvent))
			return;
		PlayerItemDamageEvent event = (PlayerItemDamageEvent) e;
		int value = delta == null ? 0 : ((Number) delta[0]).intValue();
		switch (mode) {
			case ADD:
				value = event.getDamage() + value;
				break;
			case REMOVE:
				value = event.getDamage() - value;
				break;
			default:
				break;
		}
		event.setDamage(Math.max(0, value));
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends Long> getReturnType() {
		return Long.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "the item damage";
	}
	
}
