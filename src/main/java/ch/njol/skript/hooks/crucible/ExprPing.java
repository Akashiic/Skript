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

import org.bukkit.entity.Player;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;

@Name("Ping")
@Description("Pings of players in milliseconds, as the server knows them (Crucible's Player.Spigot#getPing()). "
		+ "They will almost certainly be different from what an ICMP ping would give.")
@Examples({"command /ping <player=%player%>:",
		"\ttrigger:",
		"\t\tsend \"%arg-1%'s ping is %arg-1's ping%\""})
@Since("2.2-dev36, 2.6.3-crucible (Crucible)")
@RequiredPlugins("Crucible")
public class ExprPing extends SimplePropertyExpression<Player, Long> {
	
	static {
		register(ExprPing.class, Long.class, "ping", "players");
	}
	
	@Override
	public Long convert(Player player) {
		return (long) player.spigot().getPing();
	}
	
	@Override
	public Class<? extends Long> getReturnType() {
		return Long.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "ping";
	}
	
}
