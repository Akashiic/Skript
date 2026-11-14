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

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

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

@Name("Boss Bar Visibility")
@Description({"Shows or hides boss bars. With players, they are added to or removed from the boss bar; without, the boss bar is "
		+ "made visible or invisible for everyone who has it. Removing a boss bar hides it from all its players for good.",
		"On Crucible this needs the NecroTempus mod."})
@Examples({"show boss bar {_bar} to player",
		"hide boss bar {_bar} from all players",
		"hide boss bar {_bar}",
		"remove boss bar {_bar}"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class EffBossBarVisibility extends Effect {
	
	static {
		Skript.registerEffect(EffBossBarVisibility.class,
				"(show|reveal) [the] boss[ ]bar[s] %bossbars% [(to|for) %-players%]",
				"hide [the] boss[ ]bar[s] %bossbars% [(from|for) %-players%]",
				"(remove|destroy) [the] boss[ ]bar[s] %bossbars%");
	}
	
	private static final int SHOW = 0, HIDE = 1, REMOVE = 2;
	
	@SuppressWarnings("null")
	private Expression<BossBar> bars;
	@Nullable
	private Expression<Player> players;
	private int action;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		action = matchedPattern;
		bars = (Expression<BossBar>) exprs[0];
		players = action == REMOVE ? null : (Expression<Player>) exprs[1];
		return true;
	}
	
	@Override
	protected void execute(Event e) {
		Player[] players = this.players != null ? this.players.getArray(e) : null;
		for (BossBar bar : bars.getArray(e)) {
			try {
				switch (action) {
					case SHOW:
						if (players != null) {
							for (Player player : players)
								bar.addPlayer(player);
						}
						bar.setVisible(true);
						break;
					case HIDE:
						if (players != null) {
							for (Player player : players)
								bar.removePlayer(player);
						} else {
							bar.setVisible(false);
						}
						break;
					case REMOVE:
						bar.setVisible(false);
						bar.removeAll();
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
	public String toString(@Nullable Event e, boolean debug) {
		String bars = this.bars.toString(e, debug);
		switch (action) {
			case SHOW:
				return "show boss bar " + bars + (players != null ? " to " + players.toString(e, debug) : "");
			case HIDE:
				return "hide boss bar " + bars + (players != null ? " from " + players.toString(e, debug) : "");
			default:
				return "remove boss bar " + bars;
		}
	}
	
}
