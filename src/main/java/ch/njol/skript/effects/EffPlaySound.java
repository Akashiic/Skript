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
package ch.njol.skript.effects;

import java.util.Locale;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
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

@Name("Play Sound")
@Description({"Plays a sound at given location for everyone or just for given players, or plays a sound to specified players. " +
		"Both Minecraft sound names and " +
		"<a href=\"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html\">Spigot sound names</a> " +
		"are supported. Playing resource pack sounds are supported too.",
		"",
		"Please note that sound names can get changed in any Minecraft or Spigot version, or even removed from Minecraft itself."})
@Examples({"play sound \"note.pling\"",
		"play sound \"random.orb\" with volume 0.5 to the player",
		"play sound \"custom.music.1\" at {speakerBlock}"})
@Since("2.2-dev28")
public class EffPlaySound extends Effect {

	private static final Pattern SOUND_VALID_PATTERN = Pattern.compile("[a-z0-9\\/:._-]+"); // Minecraft only accepts these characters

	/**
	 * Distance at which a sound with volume 1 can be heard, used for playing custom (string) sounds at a location
	 * since 1.7.10 has no World#playSound(Location, String, float, float).
	 */
	private static final double SOUND_RANGE = 16;
	
	static {
		Skript.registerEffect(EffPlaySound.class,
				"play sound[s] %strings% [(at|with) volume %-number%] " +
						"[(and|at|with) pitch %-number%] at %locations% [(to|for) %-players%]",
				"play sound[s] %strings% [(at|with) volume %-number%] " +
						"[(and|at|with) pitch %-number%] [(to|for) %players%] [(at|from) %-locations%]");
	}

	@SuppressWarnings("null")
	private Expression<String> sounds;
	@Nullable
	private Expression<Number> volume, pitch;
	@Nullable
	private Expression<Location> locations;
	@Nullable
	private Expression<Player> players;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		sounds = (Expression<String>) exprs[0];
		volume = (Expression<Number>) exprs[1];
		pitch = (Expression<Number>) exprs[2];
		if (matchedPattern == 0) {
			locations = (Expression<Location>) exprs[3];
			players = (Expression<Player>) exprs[4];
		} else {
			players = (Expression<Player>) exprs[3];
			locations = (Expression<Location>) exprs[4];
		}
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected void execute(Event e) {
		float volume = 1, pitch = 1;
		if (this.volume != null) {
			Number volumeNumber = this.volume.getSingle(e);
			if (volumeNumber == null)
				return;
			volume = volumeNumber.floatValue();
		}
		if (this.pitch != null) {
			Number pitchNumber = this.pitch.getSingle(e);
			if (pitchNumber == null)
				return;
			pitch = pitchNumber.floatValue();
		}
		if (players != null) {
			if (locations == null) {
				for (Player p : players.getArray(e))
					playSound(p, p.getLocation(), sounds.getArray(e), volume, pitch);
			} else {
				for (Player p : players.getArray(e)) {
					for (Location location : locations.getArray(e))
						playSound(p, location, sounds.getArray(e), volume, pitch);
				}
			}
		} else {
			if (locations != null) {
				for (Location location : locations.getArray(e))
					playSound(location, sounds.getArray(e), volume, pitch);
			}
		}
	}

	@Nullable
	private static Sound getSoundEnum(String sound) {
		try {
			return Sound.valueOf(sound.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static void playSound(Player p, Location location, String[] sounds, float volume, float pitch) {
		for (String sound : sounds) {
			Sound soundEnum = getSoundEnum(sound);
			if (soundEnum == null) {
				sound = sound.toLowerCase(Locale.ENGLISH);
				if (!SOUND_VALID_PATTERN.matcher(sound).matches())
					continue;
				p.playSound(location, sound, volume, pitch);
			} else {
				p.playSound(location, soundEnum, volume, pitch);
			}
		}
	}

	private static void playSound(Location location, String[] sounds, float volume, float pitch) {
		World w = location.getWorld();
		if (w == null)
			return;
		double rangeSquared = Math.pow(SOUND_RANGE * Math.max(volume, 1), 2);
		for (String sound : sounds) {
			Sound soundEnum = getSoundEnum(sound);
			if (soundEnum == null) {
				sound = sound.toLowerCase(Locale.ENGLISH);
				if (!SOUND_VALID_PATTERN.matcher(sound).matches())
					continue;
				for (Player p : w.getPlayers()) {
					if (p.getLocation().distanceSquared(location) <= rangeSquared)
						p.playSound(location, sound, volume, pitch);
				}
			} else {
				w.playSound(location, soundEnum, volume, pitch);
			}
		}
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (locations != null)
			return "play sound " + sounds.toString(e, debug) +
					(volume != null ? " at volume " + volume.toString(e, debug) : "") +
					(pitch != null ? " at pitch " + pitch.toString(e, debug) : "") +
					(locations != null ? " at " + locations.toString(e, debug) : "") +
					(players != null ? " for " + players.toString(e, debug) : "");
		else
			return "play sound " + sounds.toString(e, debug) +
					(volume != null ? " at volume " + volume.toString(e, debug) : "") +
					(pitch != null ? " at pitch " + pitch.toString(e, debug) : "") +
					(players != null ? " to " + players.toString(e, debug) : "");
	}

}
