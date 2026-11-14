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
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumSerializer;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.EnumUtils;

/**
 * Types for Crucible's backported Bukkit APIs: boss bars and their colors and styles.
 * Boss bars are displayed by the NecroTempus mod.
 */
public final class CrucibleClasses {
	
	private CrucibleClasses() {}
	
	static final EnumUtils<BarColor> barColors = new EnumUtils<>(BarColor.class, "boss bar colors");
	static final EnumUtils<BarStyle> barStyles = new EnumUtils<>(BarStyle.class, "boss bar styles");
	
	static {
		Classes.registerClass(new ClassInfo<>(BossBar.class, "bossbar")
				.user("boss ?bars?")
				.name("Boss Bar")
				.description("A boss bar shown at the top of the screen, created with the <a href='expressions.html#ExprNewBossBar'>new boss bar</a> expression.",
						"On Crucible boss bars need the NecroTempus mod on the server and on the clients.",
						"Boss bars can't be saved in global variables: they disappear when the server restarts.")
				.usage("")
				.examples("set {_bar} to a new boss bar named \"Dragon\" colored red",
						"add player to boss bar players of {_bar}")
				.since("2.6.3-crucible")
				.parser(new Parser<BossBar>() {
					@Override
					public boolean canParse(ParseContext context) {
						return false;
					}
					
					@Override
					@Nullable
					public BossBar parse(String s, ParseContext context) {
						return null;
					}
					
					@Override
					public String toString(BossBar bar, int flags) {
						return "boss bar \"" + title(bar) + "\"";
					}
					
					@Override
					public String toVariableNameString(BossBar bar) {
						return "bossbar:" + title(bar);
					}
				}));
		
		Classes.registerClass(new ClassInfo<>(BarColor.class, "bossbarcolor")
				.user("boss ?bar ?colou?rs?")
				.name("Boss Bar Color")
				.description("The color of a <a href='#bossbar'>boss bar</a>.")
				.usage(barColors.getAllNames())
				.examples("set boss bar color of {_bar} to red")
				.since("2.6.3-crucible")
				.parser(new Parser<BarColor>() {
					@Override
					@Nullable
					public BarColor parse(String s, ParseContext context) {
						return barColors.parse(s);
					}
					
					@Override
					public String toString(BarColor color, int flags) {
						return barColors.toString(color, flags);
					}
					
					@Override
					public String toVariableNameString(BarColor color) {
						return color.name();
					}
				})
				.serializer(new EnumSerializer<>(BarColor.class)));
		
		Classes.registerClass(new ClassInfo<>(BarStyle.class, "bossbarstyle")
				.user("boss ?bar ?styles?")
				.name("Boss Bar Style")
				.description("The style of a <a href='#bossbar'>boss bar</a>: solid or split in 6, 10, 12 or 20 segments.")
				.usage(barStyles.getAllNames())
				.examples("set boss bar style of {_bar} to 10 segments")
				.since("2.6.3-crucible")
				.parser(new Parser<BarStyle>() {
					@Override
					@Nullable
					public BarStyle parse(String s, ParseContext context) {
						return barStyles.parse(s);
					}
					
					@Override
					public String toString(BarStyle style, int flags) {
						return barStyles.toString(style, flags);
					}
					
					@Override
					public String toVariableNameString(BarStyle style) {
						return style.name();
					}
				})
				.serializer(new EnumSerializer<>(BarStyle.class)));
	}
	
	/**
	 * Gets the title of a boss bar; empty if it can't be read (no NecroTempus).
	 */
	static String title(BossBar bar) {
		try {
			String title = bar.getTitle();
			return title == null ? "" : title;
		} catch (UnsupportedOperationException e) {
			return "";
		}
	}
	
}
