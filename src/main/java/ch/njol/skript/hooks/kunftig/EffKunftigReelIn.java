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

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

@Name("Reel In Fishing Hook (Kunftig)")
@Description({"Reels a fishing hook in as if its angler did: catches the fish if it's biting, pulls the hooked entity, "
		+ "and damages the rod unless told not to (Paper's FishHook#retrieve). 'pull the hooked entity' only pulls the "
		+ "entity attached to the hook toward the angler."})
@Examples({"on fishing bite:", "\treel in fishing hook without damaging the rod"})
@RequiredPlugins("Kunftig mod (server)")
@Since("2.6.3-crucible")
public class EffKunftigReelIn extends Effect {
	
	static {
		Skript.registerEffect(EffKunftigReelIn.class,
				"(reel|pull) in %entities% [(1¦without damag(e|ing) [the] [fishing] rod)]",
				"pull [the] hooked entit(y|ies) of %entities%");
	}
	
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<Entity> hooks;
	private boolean damageRod;
	private boolean pullOnly;
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		hooks = (Expression<Entity>) exprs[0];
		damageRod = parseResult.mark != 1;
		pullOnly = matchedPattern == 1;
		return true;
	}
	
	@Override
	protected void execute(Event e) {
		for (Entity entity : hooks.getArray(e)) {
			ModernFishHook hook = KunftigSupport.hookOf(entity);
			if (hook == null)
				continue;
			if (pullOnly)
				hook.pullHookedEntity();
			else
				hook.retrieve(damageRod);
		}
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (pullOnly)
			return "pull the hooked entity of " + hooks.toString(e, debug);
		return "reel in " + hooks.toString(e, debug) + (damageRod ? "" : " without damaging the rod");
	}
	
}
