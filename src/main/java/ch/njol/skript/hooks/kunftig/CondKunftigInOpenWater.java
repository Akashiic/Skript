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

import com.akashic.kunftig.api.bukkit.fishing.ModernFishHook;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;

@Name("Is In Open Water (Kunftig)")
@Description({"Whether a fishing hook is in open water (Minecraft 1.16+): enough water around it with nothing but air above. "
		+ "With Kunftig's modern profile, treasure can only be caught in open water."})
@Examples({"on fishing bite:", "\tif fishing hook is in open water:", "\t\tsend \"Good spot!\" to player"})
@RequiredPlugins("Kunftig mod (server)")
@Since("2.6.3-crucible")
public class CondKunftigInOpenWater extends PropertyCondition<Entity> {
	
	static {
		register(CondKunftigInOpenWater.class, "in open water[s]", "entities");
	}
	
	@Override
	public boolean check(Entity entity) {
		ModernFishHook hook = KunftigSupport.hookOf(entity);
		if (hook == null)
			return false;
		return hook.isInOpenWater();
	}
	
	@Override
	protected String getPropertyName() {
		return "in open water";
	}
	
}
