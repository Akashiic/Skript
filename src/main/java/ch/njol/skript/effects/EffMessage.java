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

import java.util.List;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.chat.MessageComponent;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprColoured;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.util.chat.ChatSender;
import ch.njol.util.Kleenean;

@Name("Message")
@Description({"Sends a message to the given player. Only styles written",
		"in given string or in <a href=expressions.html#ExprColoured>formatted expressions</a> will be parsed.",
		"On Minecraft 1.7.10, click and hover events (e.g. <code>&lt;link:...&gt;</code>, <code>&lt;tooltip:...&gt;</code>) need Crucible, "
		+ "which sends them as JSON chat; on other servers they are sent through /tellraw if available, otherwise dropped."})
@Examples({"message \"A wild %player% appeared!\"",
		"message \"This message is a distraction. Mwahaha!\"",
		"send \"Your kill streak is %{kill streak::%uuid of player%}%.\" to player",
		"if the targeted entity exists:",
		"\tmessage \"You're currently looking at a %type of the targeted entity%!\"",
		"on chat:",
		"\tcancel event",
		"\tsend \"[%player%] >> %message%\" to all players"})
@Since("1.0, 2.2-dev26 (advanced features), 2.5.2 (optional sender), 2.6 (sending objects)")
public class EffMessage extends Effect {
	
	static {
		Skript.registerEffect(EffMessage.class, "(message|send [message[s]]) %objects% [to %commandsenders%]");
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?>[] messages;

	/**
	 * Used for {@link EffMessage#toString(Event, boolean)}
	 */
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?> messageExpr;

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<CommandSender> recipients;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		messageExpr = LiteralUtils.defendExpression(exprs[0]);

		messages = messageExpr instanceof ExpressionList ?
			((ExpressionList<?>) messageExpr).getExpressions() : new Expression[] {messageExpr};
		recipients = (Expression<CommandSender>) exprs[1];
		return LiteralUtils.canInitSafely(messageExpr);
	}

	@Override
	protected void execute(Event e) {
		CommandSender[] commandSenders = recipients.getArray(e);

		for (Expression<?> message : getMessages()) {

			Object[] messageArray = null;
			List<MessageComponent> components = null;

			for (CommandSender receiver : commandSenders) {
				if (receiver instanceof Player && message instanceof VariableString) {
					// Process formatting that is safe
					if (components == null)
						components = ((VariableString) message).getMessageComponents(e);
					// Click and hover events are sent as JSON chat when the server supports it (Crucible)
					ChatSender.send(receiver, components);
					continue;
				}

				if (messageArray == null)
					messageArray = message.getArray(e);

				if (receiver instanceof Player) {
					if (message instanceof ExprColoured && ((ExprColoured) message).isUnsafeFormat()) { // Manually marked as trusted
						for (Object object : messageArray)
							ChatSender.send(receiver, ChatMessages.parse((String) object));
					} else { // It is just a string, no idea if it comes from a trusted source -> don't parse anything
						for (Object object : messageArray)
							ChatSender.send(receiver, ChatMessages.fromParsedString(toString(object)));
					}
				} else { // Not a player, send plain text with legacy formatting
					for (Object object : messageArray)
						receiver.sendMessage(toString(object));
				}
			}
		}
	}

	/**
	 * Converts Skript's message components to a legacy formatted (§-coded) string.
	 * Click and hover events are dropped.
	 */
	static String toLegacyText(List<MessageComponent> components) {
		return ChatSender.toLegacy(components);
	}

	private Expression<?>[] getMessages() {
		if (messageExpr instanceof ExpressionList && !messageExpr.getAnd()) {
			return new Expression[] {CollectionUtils.getRandom(messages)};
		}
		return messages;
	}

	private String toString(Object object) {
		return object instanceof String ? (String) object : Classes.toString(object);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "send " + messageExpr.toString(e, debug) + " to " + recipients.toString(e, debug);
	}
	
}
