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
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.fishing.ModernFishHook;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;

@Name("Hooked Entity (Kunftig)")
@Description("The entity a fishing hook is attached to. It can be set (the hook attaches to that entity) or deleted.")
@Examples({"on fish:", "\tif hooked entity of fishing hook is a player:", "\t\tdelete hooked entity of fishing hook"})
@RequiredPlugins("Kunftig mod (server)")
@Since("2.6.3-crucible")
public class ExprKunftigHookedEntity extends SimplePropertyExpression<Entity, Entity> {
	
	static {
		register(ExprKunftigHookedEntity.class, Entity.class, "hook[ed] entity", "entities");
	}
	
	@Override
	@Nullable
	public Entity convert(Entity entity) {
		ModernFishHook hook = KunftigSupport.hookOf(entity);
		return hook == null ? null : hook.getHookedEntity();
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE)
			return CollectionUtils.array(Entity.class);
		return null;
	}
	
	@Override
	public void change(org.bukkit.event.Event e, @Nullable Object[] delta, ChangeMode mode) {
		Entity target = mode == ChangeMode.SET && delta != null && delta.length > 0 ? (Entity) delta[0] : null;
		for (Entity entity : getExpr().getArray(e)) {
			ModernFishHook hook = KunftigSupport.hookOf(entity);
			if (hook != null)
				hook.setHookedEntity(target);
		}
	}
	
	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "hooked entity";
	}
	
}
