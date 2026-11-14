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

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import ch.njol.skript.Skript;
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

@Name("Mount")
@Description("The entity being ridden in a <a href='events.html#entity_mount'>mount</a> event, or the entity that was ridden in a "
		+ "<a href='events.html#entity_dismount'>dismount</a> event. The rider is the event-entity.")
@Examples({"on mount:",
		"\tif the mount is a horse:",
		"\t\tsend \"Giddy up!\" to event-entity",
		"on dismount:",
		"\tbroadcast \"%event-entity% got off %the dismounted entity%\""})
@Since("2.6.3-crucible")
@Events({"mount", "dismount"})
@RequiredPlugins("Crucible")
public class ExprMount extends SimpleExpression<Entity> {
	
	static {
		Skript.registerExpression(ExprMount.class, Entity.class, ExpressionType.SIMPLE,
				"[the] (mount|mounted entity|dismounted entity|ridden entity)");
	}
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(EntityMountEvent.class, EntityDismountEvent.class)) {
			Skript.error("The mount can only be used in mount and dismount events", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
	
	@Override
	@Nullable
	protected Entity[] get(Event e) {
		Entity entity = null;
		if (e instanceof EntityMountEvent)
			entity = ((EntityMountEvent) e).getMount();
		else if (e instanceof EntityDismountEvent)
			entity = ((EntityDismountEvent) e).getDismounted();
		return entity == null ? new Entity[0] : new Entity[] {entity};
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
		return "the mount";
	}
	
}
