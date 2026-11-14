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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Converts Skript's chat components to the BungeeCord chat API that Crucible ships with Bukkit 1.7.10
 * ({@code Player.Spigot#sendMessage(BaseComponent...)}), so click and hover events work.
 * <p>
 * Only use this class when {@link ChatSender#hasBungeeChat()} is true: the classes don't exist on other servers.
 * Crucible's copy of the API is old: it has no insertions, no fonts, no {@code show_entity} hover and no
 * {@code change_page} click; those are left out.
 */
public final class BungeeConverter {
	
	private BungeeConverter() {}
	
	public static BaseComponent convert(MessageComponent origin) {
		TextComponent base = new TextComponent(origin.text);
		base.setBold(origin.bold);
		base.setItalic(origin.italic);
		base.setUnderlined(origin.underlined);
		base.setStrikethrough(origin.strikethrough);
		base.setObfuscated(origin.obfuscated);
		if (origin.color != null) {
			ChatColor color = convert(origin.color);
			if (color != null)
				base.setColor(color);
		}
		
		MessageComponent.ClickEvent click = origin.clickEvent;
		if (click != null) {
			ClickEvent.Action action = valueOf(ClickEvent.Action.class, click.action.spigotName);
			if (action != null)
				base.setClickEvent(new ClickEvent(action, click.value));
		}
		MessageComponent.HoverEvent hover = origin.hoverEvent;
		if (hover != null) {
			HoverEvent.Action action = valueOf(HoverEvent.Action.class, hover.action.spigotName);
			if (action != null) {
				BaseComponent[] value = action == HoverEvent.Action.SHOW_TEXT
						? convert(ChatMessages.parse(hover.value)) // Parse colors in the hover text
						: new BaseComponent[] {new TextComponent(hover.value)};
				base.setHoverEvent(new HoverEvent(action, value));
			}
		}
		return base;
	}
	
	public static BaseComponent[] convert(List<MessageComponent> origins) {
		BaseComponent[] bases = new BaseComponent[origins.size()];
		for (int i = 0; i < bases.length; i++)
			bases[i] = convert(origins.get(i));
		return bases;
	}
	
	public static BaseComponent[] convert(MessageComponent[] origins) {
		BaseComponent[] bases = new BaseComponent[origins.length];
		for (int i = 0; i < bases.length; i++)
			bases[i] = convert(origins[i]);
		return bases;
	}
	
	/**
	 * Sends components to a player as a JSON chat message, or as legacy text to other receivers.
	 */
	public static void send(CommandSender receiver, List<MessageComponent> components) {
		if (receiver instanceof Player)
			((Player) receiver).spigot().sendMessage(convert(components));
		else
			receiver.sendMessage(ChatSender.toLegacy(components));
	}
	
	public static void send(Collection<? extends CommandSender> receivers, List<MessageComponent> components) {
		BaseComponent[] converted = null;
		String legacy = null;
		for (CommandSender receiver : receivers) {
			if (receiver instanceof Player) {
				if (converted == null)
					converted = convert(components);
				((Player) receiver).spigot().sendMessage(converted);
			} else {
				if (legacy == null)
					legacy = ChatSender.toLegacy(components);
				receiver.sendMessage(legacy);
			}
		}
	}
	
	private static ChatColor convert(org.bukkit.ChatColor color) {
		return valueOf(ChatColor.class, color.name());
	}
	
	private static <E extends Enum<E>> E valueOf(Class<E> type, String name) {
		try {
			return Enum.valueOf(type, name.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
