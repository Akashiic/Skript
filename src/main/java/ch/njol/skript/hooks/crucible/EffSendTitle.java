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
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;

@Name("Title - Send")
@Description({
	"Sends a title/subtitle to the given player(s) with optional fade in/stay/fade out times.",
	"",
	"On Crucible this needs the NecroTempus mod on the server and on the clients; without it the title is sent as a chat message."
})
@Examples({
	"send title \"Competition Started\" with subtitle \"Have fun, Stay safe!\" to player for 5 seconds",
	"send title \"Hi %player%\" to player",
	"send title \"Loot Drop\" with subtitle \"starts in 3 minutes\" to all players",
	"send title \"Hello %player%!\" with subtitle \"Welcome to our server\" to player for 5 seconds with fadein 1 second and fade out 1 second",
	"send subtitle \"Party!\" to all players"
})
@Since("2.3, 2.6.3-crucible (Crucible)")
@RequiredPlugins("Crucible + NecroTempus")
public class EffSendTitle extends Effect {
	
	// Minecraft's defaults, in ticks
	private static final int DEFAULT_FADE_IN = 10, DEFAULT_STAY = 70, DEFAULT_FADE_OUT = 20;
	
	static {
		Skript.registerEffect(EffSendTitle.class,
				"send title %string% [with subtitle %-string%] [to %players%] [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]",
				"send subtitle %string% [to %players%] [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]");
	}
	
	@Nullable
	private Expression<String> title;
	@Nullable
	private Expression<String> subtitle;
	@SuppressWarnings("null")
	private Expression<Player> recipients;
	@Nullable
	private Expression<Timespan> fadeIn, stay, fadeOut;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		title = matchedPattern == 0 ? (Expression<String>) exprs[0] : null;
		subtitle = (Expression<String>) exprs[1 - matchedPattern];
		recipients = (Expression<Player>) exprs[2 - matchedPattern];
		stay = (Expression<Timespan>) exprs[3 - matchedPattern];
		fadeIn = (Expression<Timespan>) exprs[4 - matchedPattern];
		fadeOut = (Expression<Timespan>) exprs[5 - matchedPattern];
		return true;
	}
	
	@Override
	protected void execute(Event e) {
		String title = this.title != null ? this.title.getSingle(e) : null;
		String subtitle = this.subtitle != null ? this.subtitle.getSingle(e) : null;
		if (title == null && subtitle == null)
			return;
		int fadeIn = ticks(this.fadeIn, e, DEFAULT_FADE_IN);
		int stay = ticks(this.stay, e, DEFAULT_STAY);
		int fadeOut = ticks(this.fadeOut, e, DEFAULT_FADE_OUT);
		
		for (Player player : recipients.getArray(e)) {
			try {
				player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Titles", "they are sent as chat messages instead");
				if (title != null)
					player.sendMessage(title);
				if (subtitle != null)
					player.sendMessage(subtitle);
			}
		}
	}
	
	private static int ticks(@Nullable Expression<Timespan> expr, Event e, int defaultTicks) {
		if (expr == null)
			return defaultTicks;
		Timespan timespan = expr.getSingle(e);
		return timespan == null ? defaultTicks : (int) timespan.getTicks_i();
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		StringBuilder sb = new StringBuilder("send ");
		if (title != null) {
			sb.append("title ").append(title.toString(e, debug));
			if (subtitle != null)
				sb.append(" with subtitle ").append(subtitle.toString(e, debug));
		} else if (subtitle != null) {
			sb.append("subtitle ").append(subtitle.toString(e, debug));
		}
		sb.append(" to ").append(recipients.toString(e, debug));
		if (stay != null)
			sb.append(" for ").append(stay.toString(e, debug));
		if (fadeIn != null)
			sb.append(" with fade in ").append(fadeIn.toString(e, debug));
		if (fadeOut != null)
			sb.append(" with fade out ").append(fadeOut.toString(e, debug));
		return sb.toString();
	}
	
}
