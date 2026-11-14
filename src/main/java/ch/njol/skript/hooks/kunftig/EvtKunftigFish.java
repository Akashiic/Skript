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

import java.util.Arrays;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent;
import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent.State;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumSerializer;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.EnumUtils;
import ch.njol.skript.util.Getter;

/**
 * The fishing states that Minecraft 1.7.10 doesn't have (Minecraft 1.21.1's lured, bite, failed attempt and reel in),
 * from the Kunftig mod's fishing engine. Bukkit's own states (fishing, caught fish, caught entity, in ground) stay in
 * the normal {@code on fish} event.
 */
public class EvtKunftigFish extends SkriptEvent {
	
	static {
		EnumUtils<State> states = new EnumUtils<>(State.class, "kunftig fish states");
		Classes.registerClass(new ClassInfo<>(State.class, "kunftigfishstate")
				.user("(kunftig )?fish(ing)? ?states?")
				.name("Fishing State (Kunftig)")
				.description("A fishing state of Minecraft 1.21.1, from the Kunftig mod: lured (a fish swims toward the bobber), "
						+ "bite (the bobber dips: reeling in now catches it), failed attempt (the player was too slow) "
						+ "and reel in (reeled in with nothing on the hook).")
				.usage(states.getAllNames())
				.examples("on fishing state of bite:", "if event-fishing state is lured:")
				.since("2.6.3-crucible")
				.requiredPlugins("Kunftig mod")
				.parser(new Parser<State>() {
					@Override
					@Nullable
					public State parse(String input, ParseContext context) {
						return states.parse(input);
					}
					
					@Override
					public String toString(State state, int flags) {
						return states.toString(state, flags);
					}
					
					@Override
					public String toVariableNameString(State state) {
						return state.name();
					}
				})
				.serializer(new EnumSerializer<>(State.class)));
		
		Skript.registerEvent("Fishing State (Kunftig)", EvtKunftigFish.class, KunftigFishEvent.class,
				"[player] fish[ing] (1¦lure[d]|1¦approach|2¦bit(e|ing)|3¦fail[ed] [attempt]|3¦escape|4¦reel[ing] in)",
				"[player] fish[ing] state [change] [of %-kunftigfishstates%]")
			.description("Called on the fishing states of Minecraft 1.21.1 that 1.7.10 doesn't have, from the Kunftig mod's fishing engine:",
					"lured: a fish appeared and swims toward the bobber (cancel: it doesn't come and the hook waits again);",
					"bite: the fish bites and reeling in now catches it (cancel: no bite, the hook waits again);",
					"failed attempt: the bite window ran out, the fish got away (can't be cancelled);",
					"reel in: the player reeled in with nothing on the hook (cancel: the hook stays in the water).",
					"Bukkit's own states (cast, caught fish, caught entity, in ground) are in the normal fishing event. "
					+ "The fishing expressions (wait time, lure time, bite window...) work in both.")
			.examples("on fishing bite:",
					"\tset fishing bite window to 3 seconds",
					"\tsend \"Reel in now!\" to player",
					"on fishing failed attempt:",
					"\tsend \"It got away...\" to player")
			.requiredPlugins("Kunftig mod (server)")
			.since("2.6.3-crucible");
		
		EventValues.registerEventValue(KunftigFishEvent.class, State.class, new Getter<State, KunftigFishEvent>() {
			@Override
			public State get(KunftigFishEvent e) {
				return e.getState();
			}
		}, 0);
		EventValues.registerEventValue(KunftigFishEvent.class, Entity.class, new Getter<Entity, KunftigFishEvent>() {
			@Override
			@Nullable
			public Entity get(KunftigFishEvent e) {
				return e.getHookEntity();
			}
		}, 0);
	}
	
	private static final State[] BY_MARK = {null, State.LURED, State.BITE, State.FAILED_ATTEMPT, State.REEL_IN};
	
	@Nullable
	private State[] states;
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		if (matchedPattern == 0) {
			states = new State[] {BY_MARK[parseResult.mark]};
		} else if (args[0] != null) {
			states = ((Literal<State>) args[0]).getAll();
		}
		return true;
	}
	
	@Override
	public boolean check(Event e) {
		State[] states = this.states;
		return states == null || Arrays.asList(states).contains(((KunftigFishEvent) e).getState());
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		State[] states = this.states;
		return "fishing state" + (states == null ? "" : " of " + Arrays.toString(states));
	}
	
}
