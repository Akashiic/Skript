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

import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.akashic.kunftig.api.bukkit.hud.HudElement;
import com.akashic.kunftig.api.bukkit.hud.HudElement.Anchor;
import com.akashic.kunftig.api.bukkit.hud.KunftigHud;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;

@Name("HUD Text and Bars (Kunftig)")
@Description({"Shows text or a progress bar on a player's screen, drawn by clients with the Kunftig mod after the vanilla HUD "
		+ "(other players see nothing). Each element has an id: showing an element with the same id again replaces it, "
		+ "and an id without a namespace is 'skript:id'. Up to 32 elements per player; they don't survive a reconnect.",
		"Position: an anchor of the screen (top left by default) and an offset in GUI pixels. "
		+ "A bar's progress is from 0 to 1. Without a duration the element stays until removed."})
@Examples({"show hud text \"&6&lSTYLISH\" with id \"rank\" to player anchored to the right offset by -12, -30 for 5 seconds",
		"show hud bar 0.65 with id \"stamina\" to player anchored to the bottom offset by 0, -50",
		"remove hud element \"rank\" from player",
		"clear the hud of player"})
@RequiredPlugins("Kunftig mod (and a Kunftig client to see it)")
@Since("2.6.3-crucible")
public class EffKunftigHud extends Effect {
	
	private static final String POSITION = " [anchored [to] [the] (1¦top left|2¦top right|3¦top|4¦left|5¦right|6¦center|7¦bottom left|8¦bottom right|9¦bottom)]"
			+ " [offset by %-number%, %-number%] [(with|at) scale %-number%] [for %-timespan%]";
	
	private static final Anchor[] ANCHORS = {Anchor.TOP_LEFT, Anchor.TOP_LEFT, Anchor.TOP_RIGHT, Anchor.TOP, Anchor.LEFT,
			Anchor.RIGHT, Anchor.CENTER, Anchor.BOTTOM_LEFT, Anchor.BOTTOM_RIGHT, Anchor.BOTTOM};
	
	static {
		Skript.registerEffect(EffKunftigHud.class,
				"show [a] hud text %string% (with|as) id %string% to %players%" + POSITION,
				"show [a] hud bar %number% (with|as) id %string% to %players%" + POSITION,
				"(remove|hide) [the] hud [element] [with id] %string% (from|for) %players%",
				"clear [the] hud of %players%",
				"clear %players%'[s] hud");
	}
	
	private int pattern;
	private int anchor;
	@Nullable
	private Expression<?> content;
	@Nullable
	private Expression<String> id;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<Player> players;
	@Nullable
	private Expression<Number> dx, dy, scale;
	@Nullable
	private Expression<Timespan> duration;
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		pattern = matchedPattern;
		anchor = parseResult.mark;
		switch (matchedPattern) {
			case 0:
			case 1:
				content = exprs[0];
				id = (Expression<String>) exprs[1];
				players = (Expression<Player>) exprs[2];
				dx = (Expression<Number>) exprs[3];
				dy = (Expression<Number>) exprs[4];
				scale = (Expression<Number>) exprs[5];
				duration = (Expression<Timespan>) exprs[6];
				break;
			case 2:
				id = (Expression<String>) exprs[0];
				players = (Expression<Player>) exprs[1];
				break;
			default:
				players = (Expression<Player>) exprs[0];
		}
		return true;
	}
	
	/**
	 * HUD ids are 'namespace:path' (lowercase); a plain id uses 'skript'.
	 */
	@Nullable
	private static String normalizeId(@Nullable String id) {
		if (id == null)
			return null;
		String normalized = id.trim().toLowerCase(Locale.ENGLISH).replace(' ', '_');
		if (normalized.indexOf(':') == -1)
			normalized = KunftigSupport.DEFAULT_NAMESPACE + ":" + normalized;
		if (!HudElement.ID.matcher(normalized).matches()) {
			KunftigSupport.warnOnce("hud-id:" + id, "'" + id + "' is not a valid HUD element id (letters, digits, '_', '.', '-' and '/')");
			return null;
		}
		return normalized;
	}
	
	@Override
	protected void execute(Event e) {
		if (!KunftigHud.isAvailable()) {
			KunftigSupport.warnOnce("hud", "Kunftig's HUD is not available on this server (is the Kunftig mod enabled?)");
			return;
		}
		Player[] targets = players.getArray(e);
		if (pattern >= 3) {
			for (Player p : targets)
				KunftigHud.clear(p);
			return;
		}
		Expression<String> idExpr = id;
		String elementId = normalizeId(idExpr == null ? null : idExpr.getSingle(e));
		if (elementId == null)
			return;
		if (pattern == 2) {
			for (Player p : targets)
				KunftigHud.remove(p, elementId);
			return;
		}
		
		HudElement.Builder builder = HudElement.builder(elementId).anchor(ANCHORS[anchor]);
		Expression<?> content = this.content;
		Object value = content == null ? null : content.getSingle(e);
		if (value == null)
			return;
		if (pattern == 0) {
			builder.text((String) value);
		} else {
			float progress = (float) Math.max(0, Math.min(1, ((Number) value).doubleValue()));
			builder.bar(progress, 80, 0xFFFFFFFF, 0x80000000);
		}
		Number x = dx == null ? null : dx.getSingle(e), y = dy == null ? null : dy.getSingle(e);
		if (x != null && y != null)
			builder.offset(x.intValue(), y.intValue());
		Number s = scale == null ? null : scale.getSingle(e);
		if (s != null && s.floatValue() > 0)
			builder.scale(s.floatValue());
		Timespan time = duration == null ? null : duration.getSingle(e);
		if (time != null) {
			int ticks = (int) Math.min(Integer.MAX_VALUE, time.getTicks_i());
			builder.duration(ticks, Math.min(20, ticks / 4)); // fade out during the last quarter, at most a second
		}
		HudElement element;
		try {
			element = builder.build();
		} catch (IllegalArgumentException | IllegalStateException ex) {
			KunftigSupport.warnOnce("hud-build:" + elementId, "Could not show HUD element " + elementId + ": " + ex.getMessage());
			return;
		}
		for (Player p : targets)
			KunftigHud.show(p, element);
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		switch (pattern) {
			case 0:
			case 1:
				Expression<?> content = this.content;
				Expression<String> idExpr = id;
				return "show hud " + (pattern == 0 ? "text " : "bar ") + (content == null ? "" : content.toString(e, debug))
						+ " with id " + (idExpr == null ? "" : idExpr.toString(e, debug)) + " to " + players.toString(e, debug);
			case 2:
				Expression<String> removed = id;
				return "remove hud element " + (removed == null ? "" : removed.toString(e, debug)) + " from " + players.toString(e, debug);
			default:
				return "clear the hud of " + players.toString(e, debug);
		}
	}
	
}
