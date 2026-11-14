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

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
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

@Name("New Boss Bar")
@Description({"Creates a new boss bar. It isn't shown to anyone until players are added to it with the "
		+ "<a href='#ExprBossBarPlayers'>boss bar players</a> expression or the <a href='effects.html#EffBossBarVisibility'>show boss bar</a> effect.",
		"The progress is a percentage from 0 to 100 (100 by default). The default color is pink and the default style is solid.",
		"On Crucible this needs the NecroTempus mod; without it the expression returns nothing."})
@Examples({"set {_bar} to a new boss bar named \"The Dragon\" colored red with style 10 segments",
		"show boss bar {_bar} to all players",
		"set boss bar progress of {_bar} to 50"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class ExprNewBossBar extends SimpleExpression<BossBar> {
	
	static {
		Skript.registerExpression(ExprNewBossBar.class, BossBar.class, ExpressionType.COMBINED,
				"[a] new boss[ ]bar [(named|with title) %-string%] [colo[u]red %-bossbarcolor%] [with style %-bossbarstyle%] [with progress %-number%]");
	}
	
	@Nullable
	private Expression<String> title;
	@Nullable
	private Expression<BarColor> color;
	@Nullable
	private Expression<BarStyle> style;
	@Nullable
	private Expression<Number> progress;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		title = (Expression<String>) exprs[0];
		color = (Expression<BarColor>) exprs[1];
		style = (Expression<BarStyle>) exprs[2];
		progress = (Expression<Number>) exprs[3];
		return true;
	}
	
	@Override
	@Nullable
	protected BossBar[] get(Event e) {
		String title = this.title != null ? this.title.getSingle(e) : null;
		BarColor color = this.color != null ? this.color.getSingle(e) : null;
		BarStyle style = this.style != null ? this.style.getSingle(e) : null;
		Number progress = this.progress != null ? this.progress.getSingle(e) : null;
		BossBar bar;
		try {
			bar = Bukkit.createBossBar(title == null ? "" : title,
					color == null ? BarColor.PINK : color, style == null ? BarStyle.SOLID : style);
			if (bar == null)
				return new BossBar[0];
			bar.setProgress(ExprBossBarProgress.toProgress(progress == null ? 100 : progress.doubleValue()));
		} catch (UnsupportedOperationException ex) {
			NecroTempus.warnMissing("Boss bars", null);
			return new BossBar[0];
		}
		return new BossBar[] {bar};
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends BossBar> getReturnType() {
		return BossBar.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "new boss bar" + (title != null ? " named " + title.toString(e, debug) : "")
				+ (color != null ? " colored " + color.toString(e, debug) : "")
				+ (style != null ? " with style " + style.toString(e, debug) : "")
				+ (progress != null ? " with progress " + progress.toString(e, debug) : "");
	}
	
}
