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

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;

@Name("Boss Bar Is Visible")
@Description("Checks whether a boss bar is visible to its players. On Crucible this needs the NecroTempus mod.")
@Examples({"if {_bar} is visible:",
		"\thide boss bar {_bar}"})
@Since("2.6.3-crucible")
@RequiredPlugins("Crucible + NecroTempus")
public class CondBossBarVisible extends PropertyCondition<BossBar> {
	
	static {
		register(CondBossBarVisible.class, "visible", "bossbars");
	}
	
	@Override
	public boolean check(BossBar bar) {
		try {
			return bar.isVisible();
		} catch (UnsupportedOperationException e) {
			NecroTempus.warnMissing("Boss bars", null);
			return false;
		}
	}
	
	@Override
	protected String getPropertyName() {
		return "visible";
	}
	
}
