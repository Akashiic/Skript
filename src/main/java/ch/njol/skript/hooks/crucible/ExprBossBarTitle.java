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

@Name("Boss Bar Title")
@Description("The title of a boss bar. On Crucible this needs the NecroTempus mod.")
@Examples({"set boss bar title of {_bar} to \"Phase 2\"",
		"send \"%boss bar title of {_bar}%\""})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class ExprBossBarTitle extends SimplePropertyExpression<BossBar, String> {
	
	static {
		register(ExprBossBarTitle.class, String.class, "boss[ ]bar (title|name)", "bossbars");
	}
	
	@Override
	@Nullable
	public String convert(BossBar bar) {
		try {
			return bar.getTitle();
		} catch (UnsupportedOperationException e) {
			NecroTempus.warnMissing("Boss bars", null);
			return null;
		}
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET)
			return CollectionUtils.array(String.class);
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		String title = delta == null ? "" : (String) delta[0];
		for (BossBar bar : getExpr().getArray(e)) {
			try {
				bar.setTitle(title);
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Boss bars", null);
				return;
			}
		}
	}
	
	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	protected String getPropertyName() {
		return "boss bar title";
	}
	
}
