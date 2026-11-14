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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Boss Bar Players")
@Description({"The players who see a boss bar. Players can be added, removed or set, and all of them can be removed with clear/delete.",
		"On Crucible this needs the NecroTempus mod."})
@Examples({"add player to boss bar players of {_bar}",
		"remove all players from boss bar players of {_bar}",
		"set boss bar players of {_bar} to all players in world \"world\"",
		"clear boss bar players of {_bar}"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class ExprBossBarPlayers extends SimpleExpression<Player> {
	
	static {
		Skript.registerExpression(ExprBossBarPlayers.class, Player.class, ExpressionType.PROPERTY,
				"[the] boss[ ]bar (players|viewers) of %bossbars%",
				"%bossbars%'[s] boss[ ]bar (players|viewers)");
	}
	
	@SuppressWarnings("null")
	private Expression<BossBar> bars;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		bars = (Expression<BossBar>) exprs[0];
		return true;
	}
	
	@Override
	protected Player[] get(Event e) {
		List<Player> players = new ArrayList<>();
		for (BossBar bar : bars.getArray(e)) {
			try {
				for (Player player : bar.getPlayers()) {
					if (!players.contains(player))
						players.add(player);
				}
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Boss bars", null);
			}
		}
		return players.toArray(new Player[0]);
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case ADD:
			case REMOVE:
			case REMOVE_ALL:
			case SET:
				return CollectionUtils.array(Player[].class);
			case DELETE:
			case RESET:
				return CollectionUtils.array();
			default:
				return null;
		}
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		for (BossBar bar : bars.getArray(e)) {
			try {
				switch (mode) {
					case SET:
					case DELETE:
					case RESET:
						bar.removeAll();
						if (mode != ChangeMode.SET)
							break;
						//$FALL-THROUGH$
					case ADD:
						assert delta != null;
						for (Object player : delta)
							bar.addPlayer((Player) player);
						break;
					case REMOVE:
					case REMOVE_ALL:
						assert delta != null;
						for (Object player : delta)
							bar.removePlayer((Player) player);
						break;
					default:
						assert false;
				}
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Boss bars", null);
				return;
			}
		}
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}
	
	@Override
	public Class<? extends Player> getReturnType() {
		return Player.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "boss bar players of " + bars.toString(e, debug);
	}
	
}
