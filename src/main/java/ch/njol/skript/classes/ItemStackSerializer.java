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
package ch.njol.skript.classes;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import ch.njol.skript.aliases.ItemData;
import ch.njol.yggdrasil.Fields;

/**
 * Item stacks in variables: Bukkit's serialized form (as {@link ConfigurationSerializer}, so old variables load the
 * same) plus the data of {@link ItemData.SerializerExtension}s, which 1.7.10's ItemMeta doesn't keep (e.g. Kunftig's
 * persistent data container).
 */
public class ItemStackSerializer extends ConfigurationSerializer<ItemStack> {
	
	@Override
	public Fields serialize(ItemStack stack) throws NotSerializableException {
		Fields fields = super.serialize(stack);
		ItemData.saveExtensions(stack, fields);
		return fields;
	}
	
	@Override
	protected ItemStack deserialize(Fields fields) throws StreamCorruptedException {
		Map<String, String> extensionData = ItemData.takeExtensions(fields);
		ItemStack stack = super.deserialize(fields);
		return ItemData.applyExtensions(stack, extensionData);
	}
	
}
