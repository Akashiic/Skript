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
package ch.njol.skript.hooks.kunftig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.hologram.Hologram;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

/**
 * Holograms of the Kunftig mod: floating text in the world, drawn like name tags by clients with Kunftig
 * (other players don't see them). They don't survive a restart: create them again in an 'on load' event.
 */
public final class KunftigHolograms {
	
	private KunftigHolograms() {}
	
	@Nullable
	static Hologram lastCreated;
	
	static {
		Classes.registerClass(new ClassInfo<>(Hologram.class, "hologram")
				.user("holograms?")
				.name("Hologram (Kunftig)")
				.description("Floating text in the world from the Kunftig mod (up to 16 lines), seen by players with Kunftig "
						+ "on their client. Holograms don't survive a restart.")
				.usage("")
				.examples("create a hologram with lines \"&6Lucky Amulet\" and \"&a+25%% luck\" at location above target block",
						"set {amulet} to the last created hologram",
						"set hologram lines of {amulet} to \"&6Lucky Amulet\" and \"&a+50%% luck\"",
						"remove hologram {amulet}")
				.since("2.6.3-crucible")
				.requiredPlugins("Kunftig mod")
				.parser(new Parser<Hologram>() {
					@Override
					@Nullable
					public Hologram parse(String s, ParseContext context) {
						return null;
					}
					
					@Override
					public boolean canParse(ParseContext context) {
						return false;
					}
					
					@Override
					public String toString(Hologram hologram, int flags) {
						List<String> lines = hologram.getLines();
						return "hologram " + (lines.isEmpty() ? "#" + hologram.getId() : "\"" + lines.get(0) + "\"");
					}
					
					@Override
					public String toVariableNameString(Hologram hologram) {
						return "hologram:" + hologram.getId();
					}
				}));
	}
	
	static boolean available() {
		if (com.akashic.kunftig.api.bukkit.hologram.KunftigHolograms.isAvailable())
			return true;
		KunftigSupport.warnOnce("holograms", "Kunftig holograms are not available on this server (is the Kunftig mod enabled?)");
		return false;
	}
	
	@Name("Create Hologram (Kunftig)")
	@Description({"Creates floating text at a location (the center of the top line; the others go down 0.27 block each), "
			+ "seen by players with Kunftig on their client within the view range (32 blocks by default, 1 to 128). "
			+ "Get it with 'last created hologram'. Holograms don't survive a restart."})
	@Examples({"on load:",
			"\tcreate a hologram with lines \"&6&lWelcome\" and \"&7to the server\" at location(0, 70, 0, world \"world\")",
			"\tset {welcome hologram} to the last created hologram"})
	@RequiredPlugins("Kunftig mod (and a Kunftig client to see it)")
	@Since("2.6.3-crucible")
	public static class EffCreate extends Effect {
		static {
			Skript.registerEffect(EffCreate.class,
					"(create|spawn) [a] [new] hologram [with [the] (text|line[s])] %strings% at %location% [with [a] (view|render) (range|distance) [of] %-number%]");
		}
		
		@SuppressWarnings("NotNullFieldNotInitialized")
		private Expression<String> lines;
		@SuppressWarnings("NotNullFieldNotInitialized")
		private Expression<Location> location;
		@Nullable
		private Expression<Number> range;
		
		@Override
		@SuppressWarnings("unchecked")
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			lines = (Expression<String>) exprs[0];
			location = (Expression<Location>) exprs[1];
			range = (Expression<Number>) exprs[2];
			return true;
		}
		
		@Override
		protected void execute(Event e) {
			Location loc = location.getSingle(e);
			if (loc == null || loc.getWorld() == null || !available())
				return;
			List<String> text = new ArrayList<>(Arrays.asList(lines.getArray(e)));
			Expression<Number> range = this.range;
			Number viewRange = range == null ? null : range.getSingle(e);
			try {
				lastCreated = viewRange == null
						? com.akashic.kunftig.api.bukkit.hologram.KunftigHolograms.create(loc, text)
						: com.akashic.kunftig.api.bukkit.hologram.KunftigHolograms.create(loc, text, Math.max(1, Math.min(128, viewRange.doubleValue())));
			} catch (IllegalArgumentException ex) {
				KunftigSupport.warnOnce("hologram-create", "Could not create a hologram: " + ex.getMessage());
			}
		}
		
		@Override
		public String toString(@Nullable Event e, boolean debug) {
			return "create a hologram with lines " + lines.toString(e, debug) + " at " + location.toString(e, debug);
		}
	}
	
	@Name("Last Created Hologram (Kunftig)")
	@Description("The hologram created most recently with the create hologram effect.")
	@Examples({"create a hologram with text \"Shop\" at location above target block", "set {shop} to last created hologram"})
	@RequiredPlugins("Kunftig mod")
	@Since("2.6.3-crucible")
	public static class ExprLastCreated extends SimpleExpression<Hologram> {
		static {
			Skript.registerExpression(ExprLastCreated.class, Hologram.class, ExpressionType.SIMPLE, "[the] last[ly] (created|spawned) hologram");
		}
		
		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			return true;
		}
		
		@Override
		@Nullable
		protected Hologram[] get(Event e) {
			Hologram hologram = lastCreated;
			return hologram == null ? new Hologram[0] : new Hologram[] {hologram};
		}
		
		@Override
		public boolean isSingle() {
			return true;
		}
		
		@Override
		public Class<? extends Hologram> getReturnType() {
			return Hologram.class;
		}
		
		@Override
		public String toString(@Nullable Event e, boolean debug) {
			return "the last created hologram";
		}
	}
	
	@Name("Remove Hologram (Kunftig)")
	@Description("Removes holograms for everyone.")
	@Examples("remove hologram {shop}")
	@RequiredPlugins("Kunftig mod")
	@Since("2.6.3-crucible")
	public static class EffRemove extends Effect {
		static {
			Skript.registerEffect(EffRemove.class, "(remove|destroy) [the] hologram[s] %holograms%");
		}
		
		@SuppressWarnings("NotNullFieldNotInitialized")
		private Expression<Hologram> holograms;
		
		@Override
		@SuppressWarnings("unchecked")
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			holograms = (Expression<Hologram>) exprs[0];
			return true;
		}
		
		@Override
		protected void execute(Event e) {
			for (Hologram hologram : holograms.getArray(e)) {
				if (!hologram.isRemoved())
					hologram.remove();
			}
		}
		
		@Override
		public String toString(@Nullable Event e, boolean debug) {
			return "remove hologram " + holograms.toString(e, debug);
		}
	}
	
	@Name("Hologram Lines (Kunftig)")
	@Description("The text of a hologram, one line each (up to 16 lines of up to 256 characters). Changing it updates it for everyone who sees it.")
	@Examples("set hologram lines of {shop} to \"&6Shop\" and \"&7Open now\"")
	@RequiredPlugins("Kunftig mod")
	@Since("2.6.3-crucible")
	public static class ExprLines extends SimpleExpression<String> {
		static {
			Skript.registerExpression(ExprLines.class, String.class, ExpressionType.PROPERTY,
					"[the] hologram (lines|text) of %holograms%", "%holograms%'[s] hologram (lines|text)");
		}
		
		@SuppressWarnings("NotNullFieldNotInitialized")
		private Expression<Hologram> holograms;
		
		@Override
		@SuppressWarnings("unchecked")
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			holograms = (Expression<Hologram>) exprs[0];
			return true;
		}
		
		@Override
		@Nullable
		protected String[] get(Event e) {
			List<String> lines = new ArrayList<>();
			for (Hologram hologram : holograms.getArray(e))
				lines.addAll(hologram.getLines());
			return lines.toArray(new String[0]);
		}
		
		@Override
		@Nullable
		public Class<?>[] acceptChange(ChangeMode mode) {
			if (mode == ChangeMode.SET || mode == ChangeMode.ADD || mode == ChangeMode.DELETE || mode == ChangeMode.RESET)
				return CollectionUtils.array(String[].class);
			return null;
		}
		
		@Override
		public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
			for (Hologram hologram : holograms.getArray(e)) {
				if (hologram.isRemoved())
					continue;
				List<String> lines = mode == ChangeMode.ADD ? new ArrayList<>(hologram.getLines()) : new ArrayList<>();
				if (delta != null) {
					for (Object line : delta)
						lines.add((String) line);
				}
				try {
					hologram.setLines(lines);
				} catch (IllegalArgumentException ex) {
					KunftigSupport.warnOnce("hologram-lines", "Can't set the lines of a hologram: " + ex.getMessage());
				}
			}
		}
		
		@Override
		public boolean isSingle() {
			return false;
		}
		
		@Override
		public Class<? extends String> getReturnType() {
			return String.class;
		}
		
		@Override
		public String toString(@Nullable Event e, boolean debug) {
			return "hologram lines of " + holograms.toString(e, debug);
		}
	}
	
	@Name("Hologram Location (Kunftig)")
	@Description("Where a hologram is (the center of its top line). Setting it moves the hologram, even to another world.")
	@Examples("set hologram location of {shop} to location above player")
	@RequiredPlugins("Kunftig mod")
	@Since("2.6.3-crucible")
	public static class ExprLocation extends SimplePropertyExpression<Hologram, Location> {
		static {
			register(ExprLocation.class, Location.class, "hologram (location|position)", "holograms");
		}
		
		@Override
		@Nullable
		public Location convert(Hologram hologram) {
			return hologram.getLocation();
		}
		
		@Override
		@Nullable
		public Class<?>[] acceptChange(ChangeMode mode) {
			return mode == ChangeMode.SET ? CollectionUtils.array(Location.class) : null;
		}
		
		@Override
		public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
			if (delta == null || delta.length == 0 || !(delta[0] instanceof Location))
				return;
			Location location = (Location) delta[0];
			if (location.getWorld() == null)
				return;
			for (Hologram hologram : getExpr().getArray(e)) {
				if (!hologram.isRemoved())
					hologram.teleport(location);
			}
		}
		
		@Override
		public Class<? extends Location> getReturnType() {
			return Location.class;
		}
		
		@Override
		protected String getPropertyName() {
			return "hologram location";
		}
	}
	
}
