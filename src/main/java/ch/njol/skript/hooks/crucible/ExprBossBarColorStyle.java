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

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Boss Bar Color and Style")
@Description({"The color (pink, blue, red, green, yellow, purple or white) and the style (solid, 6, 10, 12 or 20 segments) of a boss bar.",
		"On Crucible this needs the NecroTempus mod."})
@Examples({"set boss bar color of {_bar} to red",
		"set boss bar style of {_bar} to 10 segments",
		"reset boss bar color of {_bar} # pink"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class ExprBossBarColorStyle extends SimplePropertyExpression<BossBar, Object> {
	
	static {
		register(ExprBossBarColorStyle.class, Object.class, "boss[ ]bar (colo[u]r|1¦style)", "bossbars");
	}
	
	private boolean style;
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		style = parseResult.mark == 1;
		return super.init(exprs, matchedPattern, isDelayed, parseResult);
	}
	
	@Override
	@Nullable
	public Object convert(BossBar bar) {
		try {
			return style ? bar.getStyle() : bar.getColor();
		} catch (UnsupportedOperationException e) {
			NecroTempus.warnMissing("Boss bars", null);
			return null;
		}
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.RESET)
			return CollectionUtils.array(style ? BarStyle.class : BarColor.class);
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		Object value = delta == null ? (style ? BarStyle.SOLID : BarColor.PINK) : delta[0];
		for (BossBar bar : getExpr().getArray(e)) {
			try {
				if (style)
					bar.setStyle((BarStyle) value);
				else
					bar.setColor((BarColor) value);
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Boss bars", null);
				return;
			}
		}
	}
	
	@Override
	public Class<?> getReturnType() {
		return style ? BarStyle.class : BarColor.class;
	}
	
	@Override
	protected String getPropertyName() {
		return style ? "boss bar style" : "boss bar color";
	}
	
}
