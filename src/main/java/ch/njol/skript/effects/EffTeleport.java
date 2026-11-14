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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.eclipse.jdt.annotation.Nullable;

@Name("Teleport")
@Description({"Teleport an entity to a specific location. ",
		"On Minecraft 1.7.10 teleporting is never delayed; the 'force' keyword is accepted but has no effect."})
@Examples({"teleport the player to {homes.%player%}",
		"teleport the attacker to the victim"})
@Since("1.0")
public class EffTeleport extends Effect {

	static {
		Skript.registerEffect(EffTeleport.class, "[(1¦force)] teleport %entities% (to|%direction%) %location%");
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<Entity> entities;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<Location> location;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = (Expression<Entity>) exprs[0];
		location = Direction.combine((Expression<? extends Direction>) exprs[1], (Expression<? extends Location>) exprs[2]);
		return true;
	}
	
	@Nullable
	@Override
	protected TriggerItem walk(Event e) {
		debug(e, true);

		TriggerItem next = getNext();

		boolean delayed = Delay.isDelayed(e);
		
		Location loc = location.getSingle(e);
		if (loc == null)
			return next;

		Entity[] entityArray = entities.getArray(e); // We have to fetch this before possible async execution to avoid async local variable access.
		if (entityArray.length == 0)
			return next;

		if (!delayed) {
			if (e instanceof PlayerRespawnEvent && entityArray.length == 1 && entityArray[0].equals(((PlayerRespawnEvent) e).getPlayer())) {
				((PlayerRespawnEvent) e).setRespawnLocation(loc);
				return next;
			}

			if (e instanceof PlayerMoveEvent && entityArray.length == 1 && entityArray[0].equals(((PlayerMoveEvent) e).getPlayer())) {
				((PlayerMoveEvent) e).setTo(loc);
				return next;
			}
		}

		// 1.7.10 has no async chunk loading, so teleports are always synchronous ('force' is kept for compatibility)
		for (Entity entity : entityArray) {
			entity.teleport(loc);
		}
		return next;
	}

	@Override
	protected void execute(Event e) {
		// Nothing needs to happen here, we're executing in walk
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "teleport " + entities.toString(e, debug) + " to " + location.toString(e, debug);
	}

}
