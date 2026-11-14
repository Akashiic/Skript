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

import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.fishing.KunftigFishEvent;
import com.akashic.kunftig.api.bukkit.fishing.ModernFishHook;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Fishing Hook Values (Kunftig)")
@Description({"Values of the fishing hook in a fishing event, with Minecraft 1.21.1's fishing API from the Kunftig mod. "
		+ "They work in the normal fishing event and in the fishing state events (lured, bite, failed attempt, reel in).",
		"Times: the minimum and maximum wait time (before a fish appears; the Lure enchantment shortens it), "
		+ "the minimum and maximum lure time (how long the fish swims to the bobber), the current wait time, the bite time "
		+ "(how long until the fish bites) and the bite window (how long the player has to reel in once it bites).",
		"Numbers: the minimum and maximum lure angle (in degrees) and the maximum distance of the hook from its angler.",
		"Booleans: whether the Lure enchantment applies, whether rain and the open sky change the wait, lava fishing, "
		+ "the reel lock (nothing can reel the hook in, e.g. during a minigame), whether the catch flies toward the angler "
		+ "and whether treasure requires open water."})
@Examples({"on fish:",
		"\tset min fishing wait time to 3 seconds",
		"\tset max fishing wait time to 10 seconds",
		"\tset fishing sky influence to false",
		"on fishing bite:",
		"\tset fishing bite window to 5 seconds",
		"\tset fishing reel lock to true # e.g. while a minigame runs"})
@RequiredPlugins("Kunftig mod (server), fishing enabled in config/kunftig.cfg")
@Since("2.6.3-crucible")
public class ExprKunftigFishing extends SimpleExpression<Object> {
	
	private enum Property {
		MIN_WAIT(Timespan.class), MAX_WAIT(Timespan.class), MIN_LURE(Timespan.class), MAX_LURE(Timespan.class),
		BITE_TIME(Timespan.class), WAIT_TIME(Timespan.class), BITE_WINDOW(Timespan.class),
		MIN_ANGLE(Number.class), MAX_ANGLE(Number.class), MAX_DISTANCE(Number.class),
		APPLY_LURE(Boolean.class), RAIN(Boolean.class), SKY(Boolean.class), LAVA(Boolean.class), REEL_LOCK(Boolean.class),
		CATCH_AIM(Boolean.class), TREASURE_OPEN_WATER(Boolean.class);
		
		final Class<?> type;
		
		Property(Class<?> type) {
			this.type = type;
		}
	}
	
	static {
		Skript.registerExpression(ExprKunftigFishing.class, Object.class, ExpressionType.SIMPLE,
				"[the] min[imum] fish[ing] wait[ing] time",
				"[the] max[imum] fish[ing] wait[ing] time",
				"[the] min[imum] fish[ing] lure time",
				"[the] max[imum] fish[ing] lure time",
				"[the] fish[ing] bit(e|ing) [wait] time",
				"[the] [current] fish[ing] wait[ing] time",
				"[the] fish[ing] bite window",
				"[the] min[imum] fish[ing] (lure|approach)[ing] angle",
				"[the] max[imum] fish[ing] (lure|approach)[ing] angle",
				"[the] max[imum] fish[ing] (distance|range)",
				"[the] fish[ing] lure [enchantment] bonus",
				"[the] fish[ing] rain influence",
				"[the] fish[ing] sky influence",
				"[the] lava fishing",
				"[the] fish[ing] (reel|line) lock",
				"[the] fish[ing] catch aim[ing]",
				"[the] fish[ing] treasure requires open water");
	}
	
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Property property;
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(PlayerFishEvent.class, KunftigFishEvent.class)) {
			Skript.error("The fishing hook values can only be used in a fishing event");
			return false;
		}
		property = Property.values()[matchedPattern];
		return true;
	}
	
	@Override
	@Nullable
	protected Object[] get(Event e) {
		ModernFishHook hook = KunftigSupport.hookOf(e);
		if (hook == null)
			return null;
		Object value;
		switch (property) {
			case MIN_WAIT: value = Timespan.fromTicks_i(hook.getMinWaitTime()); break;
			case MAX_WAIT: value = Timespan.fromTicks_i(hook.getMaxWaitTime()); break;
			case MIN_LURE: value = Timespan.fromTicks_i(hook.getMinLureTime()); break;
			case MAX_LURE: value = Timespan.fromTicks_i(hook.getMaxLureTime()); break;
			case BITE_TIME: value = Timespan.fromTicks_i(Math.max(hook.getTimeUntilBite(), 0)); break;
			case WAIT_TIME: value = Timespan.fromTicks_i(Math.max(hook.getWaitTime(), 0)); break;
			case BITE_WINDOW: value = Timespan.fromTicks_i(Math.max(hook.getBiteWindow(), 0)); break;
			case MIN_ANGLE: value = hook.getMinLureAngle(); break;
			case MAX_ANGLE: value = hook.getMaxLureAngle(); break;
			case MAX_DISTANCE: value = hook.getMaxDistance(); break;
			case APPLY_LURE: value = hook.getApplyLure(); break;
			case RAIN: value = hook.isRainInfluenced(); break;
			case SKY: value = hook.isSkyInfluenced(); break;
			case LAVA: value = hook.isLavaFishing(); break;
			case REEL_LOCK: value = hook.isReelLocked(); break;
			case CATCH_AIM: value = hook.isCatchAimedAtAngler(); break;
			case TREASURE_OPEN_WATER: value = hook.getTreasureRequiresOpenWater(); break;
			default: throw new IllegalStateException();
		}
		Object[] one = (Object[]) java.lang.reflect.Array.newInstance(property.type, 1);
		one[0] = value;
		return one;
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET)
			return CollectionUtils.array(property.type);
		if ((mode == ChangeMode.ADD || mode == ChangeMode.REMOVE) && property.type != Boolean.class)
			return CollectionUtils.array(property.type);
		return null;
	}
	
	@Override
	public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
		ModernFishHook hook = KunftigSupport.hookOf(e);
		if (hook == null || delta == null || delta.length == 0 || delta[0] == null)
			return;
		Object value = delta[0];
		try {
			if (property.type == Boolean.class) {
				setBoolean(hook, (Boolean) value);
				return;
			}
			double current = property.type == Timespan.class ? ((Timespan) get(e)[0]).getTicks_i() : ((Number) get(e)[0]).doubleValue();
			double amount = value instanceof Timespan ? ((Timespan) value).getTicks_i() : ((Number) value).doubleValue();
			double result = mode == ChangeMode.ADD ? current + amount : mode == ChangeMode.REMOVE ? current - amount : amount;
			setNumber(hook, result);
		} catch (IllegalArgumentException ex) { // Same validation as 1.21.1's FishHook (e.g. min > max)
			KunftigSupport.warnOnce("fishing-value:" + property, "Can't set the fishing " + property.name().toLowerCase().replace('_', ' ')
					+ ": " + ex.getMessage());
		}
	}
	
	private void setNumber(ModernFishHook hook, double value) {
		int ticks = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(value)));
		switch (property) {
			case MIN_WAIT: hook.setMinWaitTime(ticks); break;
			case MAX_WAIT: hook.setMaxWaitTime(ticks); break;
			case MIN_LURE: hook.setMinLureTime(ticks); break;
			case MAX_LURE: hook.setMaxLureTime(ticks); break;
			case BITE_TIME: hook.setTimeUntilBite(ticks); break;
			case WAIT_TIME: hook.setWaitTime(ticks); break;
			case BITE_WINDOW: hook.setBiteWindow(ticks); break;
			case MIN_ANGLE: hook.setMinLureAngle((float) value); break;
			case MAX_ANGLE: hook.setMaxLureAngle((float) value); break;
			case MAX_DISTANCE: hook.setMaxDistance(value); break;
			default: throw new IllegalStateException();
		}
	}
	
	private void setBoolean(ModernFishHook hook, boolean value) {
		switch (property) {
			case APPLY_LURE: hook.setApplyLure(value); break;
			case RAIN: hook.setRainInfluenced(value); break;
			case SKY: hook.setSkyInfluenced(value); break;
			case LAVA: hook.setLavaFishing(value); break;
			case REEL_LOCK: hook.setReelLocked(value); break;
			case CATCH_AIM: hook.setCatchAimedAtAngler(value); break;
			case TREASURE_OPEN_WATER: hook.setTreasureRequiresOpenWater(value); break;
			default: throw new IllegalStateException();
		}
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<?> getReturnType() {
		return property.type;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "fishing " + property.name().toLowerCase().replace('_', ' ');
	}
	
}
