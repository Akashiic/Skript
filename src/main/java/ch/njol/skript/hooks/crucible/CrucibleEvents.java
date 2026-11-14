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

import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;

/**
 * Events Crucible backports from Spigot: mounting, dismounting and item damage.
 */
public final class CrucibleEvents {
	
	private CrucibleEvents() {}
	
	static {
		Skript.registerEvent("Entity Mount", SimpleEvent.class, EntityMountEvent.class, "mount[ing]")
				.description("Called when an entity starts riding another (a player mounting a horse, a mob getting in a minecart...).",
						"The rider is the event-entity; the entity being ridden is <a href='expressions.html#ExprMount'>the mount</a>.")
				.examples("on mount:",
						"\tif the mount is a pig:",
						"\t\tcancel event")
				.since("2.2-dev13b, 2.6.3-crucible (Crucible)")
				.requiredPlugins("Crucible");
		Skript.registerEvent("Entity Dismount", SimpleEvent.class, EntityDismountEvent.class, "dismount[ing]")
				.description("Called when an entity stops riding another. It can't be cancelled.",
						"The rider is the event-entity; the entity that was ridden is <a href='expressions.html#ExprMount'>the dismounted entity</a>.")
				.examples("on dismount:",
						"\tsend \"You got off a %dismounted entity%\" to event-entity")
				.since("2.2-dev13b, 2.6.3-crucible (Crucible)")
				.requiredPlugins("Crucible");
		Skript.registerEvent("Item Damage", SimpleEvent.class, PlayerItemDamageEvent.class, "item damag(e|ing)")
				.description("Called when an item of a player loses durability. Most tools are damaged by using them; armor is damaged when the wearer takes damage.",
						"The durability lost is <a href='expressions.html#ExprItemDamageAmount'>the item damage</a>.")
				.examples("on item damage:",
						"\tif event-item is a diamond sword:",
						"\t\tcancel event")
				.since("2.5, 2.6.3-crucible (Crucible)")
				.requiredPlugins("Crucible");
		
		EventValues.registerEventValue(PlayerItemDamageEvent.class, ItemStack.class, new Getter<ItemStack, PlayerItemDamageEvent>() {
			@Override
			@Nullable
			public ItemStack get(PlayerItemDamageEvent event) {
				return event.getItem();
			}
		}, 0);
	}
	
}
