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
package ch.njol.skript.util.chat;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Sends Skript's chat components on Minecraft 1.7.10, which has no Bungee chat API.
 * <p>
 * Plain messages (colours and styles only) are sent as legacy section-sign text.
 * Messages that use click/hover events or insertions are sent to players as raw JSON
 * through Vanilla's {@code /tellraw}, which 1.7.10 supports.
 */
public final class ChatSender {

	private ChatSender() {}

	/**
	 * Converts components to legacy (section sign) text. Click and hover events are dropped.
	 */
	public static String toLegacy(List<MessageComponent> components) {
		StringBuilder sb = new StringBuilder();
		for (MessageComponent c : components)
			appendLegacy(sb, c);
		return sb.toString();
	}

	public static String toLegacy(MessageComponent... components) {
		return toLegacy(Arrays.asList(components));
	}

	private static void appendLegacy(StringBuilder sb, MessageComponent c) {
		if (c.reset)
			sb.append(ChatColor.RESET);
		if (c.color != null)
			sb.append(c.color);
		if (c.bold)
			sb.append(ChatColor.BOLD);
		if (c.italic)
			sb.append(ChatColor.ITALIC);
		if (c.underlined)
			sb.append(ChatColor.UNDERLINE);
		if (c.strikethrough)
			sb.append(ChatColor.STRIKETHROUGH);
		if (c.obfuscated)
			sb.append(ChatColor.MAGIC);
		sb.append(c.text);
	}

	/**
	 * @return Whether the components need JSON chat to be displayed properly.
	 */
	public static boolean needsJson(List<MessageComponent> components) {
		for (MessageComponent c : components) {
			if (c.clickEvent != null || c.hoverEvent != null || c.insertion != null)
				return true;
		}
		return false;
	}

	public static void send(CommandSender receiver, MessageComponent... components) {
		send(receiver, Arrays.asList(components));
	}

	public static void send(CommandSender receiver, List<MessageComponent> components) {
		if (receiver instanceof Player && needsJson(components)) {
			sendJson((Player) receiver, ChatMessages.toJson(components));
		} else {
			receiver.sendMessage(toLegacy(components));
		}
	}

	public static void send(Collection<? extends CommandSender> receivers, List<MessageComponent> components) {
		if (needsJson(components)) {
			String json = ChatMessages.toJson(components);
			String legacy = toLegacy(components);
			for (CommandSender receiver : receivers) {
				if (receiver instanceof Player)
					sendJson((Player) receiver, json);
				else
					receiver.sendMessage(legacy);
			}
		} else {
			String legacy = toLegacy(components);
			for (CommandSender receiver : receivers)
				receiver.sendMessage(legacy);
		}
	}

	/**
	 * Sends a raw JSON chat message to a player using Vanilla's /tellraw command.
	 */
	public static void sendJson(final Player player, final String json) {
		Runnable task = () -> {
			if (player.isOnline())
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + json);
		};
		if (Bukkit.isPrimaryThread())
			task.run();
		else
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), task);
	}

}
