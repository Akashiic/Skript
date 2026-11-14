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
import ch.njol.skript.util.chat.BungeeConverter;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.util.Kleenean;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;

@Name("Action Bar")
@Description({"Sends an action bar message to the given player(s).",
		"On Crucible this needs the NecroTempus mod on the server and on the clients. Without it nothing is sent "
		+ "(action bars are often sent every second, so falling back to chat would flood it)."})
@Examples("send action bar \"Hello player!\" to player")
@Since("2.3, 2.6.3-crucible (Crucible)")
@RequiredPlugins("Crucible + NecroTempus")
public class EffActionBar extends Effect {
	
	static {
		Skript.registerEffect(EffActionBar.class, "send [the] action[ ]bar [with text] %string% [to %players%]");
	}
	
	@SuppressWarnings("null")
	private Expression<String> message;
	@SuppressWarnings("null")
	private Expression<Player> recipients;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		message = (Expression<String>) exprs[0];
		recipients = (Expression<Player>) exprs[1];
		return true;
	}
	
	@Override
	protected void execute(Event e) {
		String msg = message.getSingle(e);
		if (msg == null)
			return;
		BaseComponent[] components = BungeeConverter.convert(ChatMessages.parseToArray(msg));
		for (Player player : recipients.getArray(e)) {
			try {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
			} catch (UnsupportedOperationException ex) {
				NecroTempus.warnMissing("Action bars", "nothing is sent");
				return;
			}
		}
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "send action bar " + message.toString(e, debug) + " to " + recipients.toString(e, debug);
	}
	
}
