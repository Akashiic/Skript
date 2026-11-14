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
import ch.njol.util.Kleenean;

@Name("Message")
@Description({"Sends a message to the given player. Only styles written",
		"in given string or in <a href=expressions.html#ExprColoured>formatted expressions</a> will be parsed.",
		"On Minecraft 1.7.10 all formatting is sent as legacy color codes; click and hover events are not supported."})
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
			String formattedMessage = null;

			for (CommandSender receiver : commandSenders) {
				if (receiver instanceof Player && message instanceof VariableString) {
					if (formattedMessage == null)
						formattedMessage = toLegacyText(((VariableString) message).getMessageComponents(e));
				} else {
					if (messageArray == null)
						messageArray = message.getArray(e);
				}

				if (receiver instanceof Player) {
					// 1.7.10 has no chat component API, so all formatting is converted to legacy color codes.
					// Click and hover events are not available.
					if (message instanceof VariableString) { // Process formatting that is safe
						receiver.sendMessage(formattedMessage);
					} else if (message instanceof ExprColoured && ((ExprColoured) message).isUnsafeFormat()) { // Manually marked as trusted
						for (Object object : messageArray) {
							receiver.sendMessage(toLegacyText(ChatMessages.parse((String) object)));
						}
					} else { // It is just a string, no idea if it comes from a trusted source -> don't parse anything
						for (Object object : messageArray) {
							receiver.sendMessage(toLegacyText(ChatMessages.fromParsedString(toString(object))));
						}
					}
				} else { // Not a player, send plain text with legacy formatting
					for (Object object : messageArray) {
						receiver.sendMessage(toString(object));
					}
				}
			}
		}
	}

	/**
	 * Converts Skript's message components to a legacy formatted (§-coded) string, since Minecraft 1.7.10
	 * servers have no chat component API. Each component starts from default formatting, like siblings
	 * in a JSON chat message do.
	 */
	static String toLegacyText(List<MessageComponent> components) {
		StringBuilder sb = new StringBuilder();
		for (MessageComponent component : components) {
			if (component.text.isEmpty())
				continue;
			if (component.color != null)
				sb.append(component.color.toString());
			else if (sb.length() > 0)
				sb.append(org.bukkit.ChatColor.RESET);
			if (component.bold)
				sb.append(org.bukkit.ChatColor.BOLD);
			if (component.italic)
				sb.append(org.bukkit.ChatColor.ITALIC);
			if (component.underlined)
				sb.append(org.bukkit.ChatColor.UNDERLINE);
			if (component.strikethrough)
				sb.append(org.bukkit.ChatColor.STRIKETHROUGH);
			if (component.obfuscated)
				sb.append(org.bukkit.ChatColor.MAGIC);
			sb.append(component.text);
		}
		return sb.toString();
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
