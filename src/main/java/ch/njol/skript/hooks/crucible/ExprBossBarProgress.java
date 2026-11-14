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
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;

@Name("Boss Bar Progress")
@Description({"How full a boss bar is, as a percentage from 0 (empty) to 100 (full).",
		"On Crucible this needs the NecroTempus mod."})
@Examples({"set boss bar progress of {_bar} to 50",
		"remove 10 from boss bar progress of {_bar}",
		"set boss bar progress of {_bar} to 100 * health of {_boss} / max health of {_boss}"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class ExprBossBarProgress extends SimplePropertyExpression<BossBar, Number> {
	
	static {
		register(ExprBossBarProgress.class, Number.class, "boss[ ]bar progress", "bossbars");
	}
	
	/**
	 * @param percent A percentage, clamped to 0-100.
	 * @return The progress Bukkit expects (0-1).
	 */
	static double toProgress(double percent) {
		if (Double.isNaN(percent))
			return 0;
		return Math.max(0, Math.min(100, percent)) / 100;
	}
	
	@Override
	@Nullable
	public Number convert(BossBar bar) {
		try {
			return bar.getProgress() * 100;
		} catch (UnsupportedOperationException e) {
			NecroTempus.warnMissing("Boss bars", null);
			return null;
		}
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case ADD:
			case REMOVE:
			case DELETE:
			case RESET:
				return CollectionUtils.array(Number.class);
			default:
				return null;
		}
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		double value = delta == null ? 0 : ((Number) delta[0]).doubleValue();
		for (BossBar bar : getExpr().getArray(e)) {
			try {
				double percent;
				switch (mode) {
					case ADD:
						percent = bar.getProgress() * 100 + value;
						break;
					case REMOVE:
						percent = bar.getProgress() * 100 - value;
						break;
					case DELETE:
						percent = 0;
						break;
					case RESET:
						percent = 100;
						break;
					default:
						percent = value;
				}
				bar.setProgress(toProgress(percent));
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Boss bars", null);
				return;
			}
		}
	}
	
	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "boss bar progress";
	}
	
}
