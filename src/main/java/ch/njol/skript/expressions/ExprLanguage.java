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
package ch.njol.skript.expressions;

import java.lang.reflect.Method;

import org.eclipse.jdt.annotation.Nullable;


import org.bukkit.entity.Player;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;

@Name("Language")
@Description({"Currently selected game language of a player. The value of the language is not defined properly.",
			"The vanilla Minecraft client will use lowercase language / country pairs separated by an underscore, but custom resource packs may use any format they wish."})
@Examples({"message player's current language"})
@Since("2.3")
public class ExprLanguage extends SimplePropertyExpression<Player, String> {

	static {
		register(ExprLanguage.class, String.class, "[([currently] selected|current)] [game] (language|locale) [setting]", "players");
	}

	@Nullable
	private static Method spigotMethod, getLocaleMethod;
	private static boolean lookedUp;

	/**
	 * Bukkit 1.7.10 has no locale API. Spigot-based servers may have Player.Spigot#getLocale(),
	 * which is looked up reflectively since Skript compiles against Bukkit.
	 */
	@Override
	@Nullable
	public String convert(Player p) {
		try {
			if (!lookedUp) {
				lookedUp = true;
				Method spigot = p.getClass().getMethod("spigot");
				Method getLocale = spigot.getReturnType().getMethod("getLocale");
				spigotMethod = spigot;
				getLocaleMethod = getLocale;
			}
			if (spigotMethod == null || getLocaleMethod == null)
				return null;
			return (String) getLocaleMethod.invoke(spigotMethod.invoke(p));
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	@Override
	protected String getPropertyName() {
		return "language";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
