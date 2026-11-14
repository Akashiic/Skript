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
import ch.njol.skript.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Sends Skript's chat components on Minecraft 1.7.10.
 * <p>
 * Plain messages (colours and styles only) are sent as legacy section-sign text. Messages with click/hover events
 * are sent to players as JSON chat: through the BungeeCord chat API on Crucible ({@link BungeeConverter}), otherwise
 * through Vanilla's {@code /tellraw} when the server has it, otherwise as legacy text without the events.
 */
public final class ChatSender {

	private ChatSender() {}

	private static final boolean BUNGEE_CHAT = Skript.classExists("net.md_5.bungee.api.chat.BaseComponent")
			&& Skript.methodExists(Player.class, "spigot");

	/**
	 * @return Whether the server has the BungeeCord chat API (Crucible), i.e. {@link BungeeConverter} can be used.
	 */
	public static boolean hasBungeeChat() {
		return BUNGEE_CHAT;
	}

	/**
	 * Converts components to legacy (section sign) text. Click and hover events are dropped.
	 * Each component starts from default formatting, like siblings of a JSON chat message do.
	 */
	public static String toLegacy(List<MessageComponent> components) {
		StringBuilder sb = new StringBuilder();
		for (MessageComponent c : components) {
			if (c.text.isEmpty())
				continue;
			if (c.color != null)
				sb.append(c.color);
			else if (c.reset || sb.length() > 0)
				sb.append(ChatColor.RESET);
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
		return sb.toString();
	}

	public static String toLegacy(MessageComponent... components) {
		return toLegacy(Arrays.asList(components));
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
			if (BUNGEE_CHAT) {
				BungeeConverter.send(receiver, components);
				return;
			}
			if (canUseTellraw()) {
				sendJson((Player) receiver, ChatMessages.toJson(components));
				return;
			}
		}
		receiver.sendMessage(toLegacy(components));
	}

	public static void send(Collection<? extends CommandSender> receivers, List<MessageComponent> components) {
		if (needsJson(components)) {
			if (BUNGEE_CHAT) {
				BungeeConverter.send(receivers, components);
				return;
			}
			if (canUseTellraw()) {
				String json = ChatMessages.toJson(components);
				String legacy = toLegacy(components);
				for (CommandSender receiver : receivers) {
					if (receiver instanceof Player)
						sendJson((Player) receiver, json);
					else
						receiver.sendMessage(legacy);
				}
				return;
			}
		}
		String legacy = toLegacy(components);
		for (CommandSender receiver : receivers)
			receiver.sendMessage(legacy);
	}

	/**
	 * Whether /tellraw can be dispatched through Bukkit. Forge hybrids register Vanilla and
	 * modded commands in Bukkit's command map, plain CraftBukkit 1.7.10 might not.
	 * If it can't, messages are sent as legacy text (without click/hover events).
	 */
	private static boolean canUseTellraw() {
		SimpleCommandMap commandMap = Commands.getCommandMap();
		return commandMap != null && commandMap.getCommand("tellraw") != null;
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
