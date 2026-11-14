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

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

@Name("Fishing Hook")
@Description("The fishing hook (bobber) of a fishing event or of a fishing state event (lured, bite, failed attempt, reel in).")
@Examples({"on fishing bite:", "\tplay 10 end rod with offset of 0.2, 0.2, 0.2 at fishing hook"})
@Since("2.6.3-crucible")
public class ExprKunftigFishingHook extends SimpleExpression<Entity> {
	
	static {
		Skript.registerExpression(ExprKunftigFishingHook.class, Entity.class, ExpressionType.SIMPLE, "[the] fish[ing] (hook|bobber)");
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(PlayerFishEvent.class, KunftigFishEvent.class)) {
			Skript.error("The fishing hook can only be used in a fishing event");
			return false;
		}
		return true;
	}
	
	@Override
	@Nullable
	protected Entity[] get(Event e) {
		Entity hook = null;
		if (e instanceof KunftigFishEvent)
			hook = ((KunftigFishEvent) e).getHookEntity();
		else if (e instanceof PlayerFishEvent)
			hook = ((PlayerFishEvent) e).getHook();
		return hook == null ? new Entity[0] : new Entity[] {hook};
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "the fishing hook";
	}
	
}
