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
package ch.njol.skript.entity;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.util.coll.CollectionUtils;

/**
 * @author Peter Güttinger
 */
public class ThrownPotionData extends EntityData<ThrownPotion> {
	static {
		EntityData.register(ThrownPotionData.class, "thrown potion", ThrownPotion.class, "thrown potion");
	}
	
	private static final Adjective m_adjective = new Adjective("entities.thrown potion.adjective");
	// Minecraft 1.7.10: all potions (drinkable and splash) are Material.POTION, distinguished by data value
	private static final ItemType POTION = new ItemType(Material.POTION);
	private static final short SPLASH_BIT = 0x4000;
	
	@Nullable
	private ItemType[] types;
	
	@Override
	protected boolean init(Literal<?>[] exprs, int matchedPattern, ParseResult parseResult) {
		if (exprs.length > 0 && exprs[0] != null) {
			return (types = Converters.convert((ItemType[]) exprs[0].getAll(), ItemType.class, t -> {
				if (!POTION.isSupertypeOf(t))
					return null;
				return t;
			})).length != 0; // no error message - other things can be thrown as well
		} else {
			types = new ItemType[]{POTION.clone()};
		}
		return true;
	}
	
	@Override
	protected boolean init(@Nullable Class<? extends ThrownPotion> c, @Nullable ThrownPotion e) {
		if (e != null) {
			ItemStack i = e.getItem();
			types = new ItemType[] {new ItemType(i)};
		}
		return true;
	}
	
	@Override
	protected boolean match(ThrownPotion entity) {
		if (types != null) {
			for (ItemType t : types) {
				if (t.isOfType(entity.getItem()))
					return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public @Nullable ThrownPotion spawn(Location loc, @Nullable Consumer<ThrownPotion> consumer) {
		ItemType t = CollectionUtils.getRandom(types);
		assert t != null;
		ItemStack i = t.getRandom();
		if (i == null)
			return null;

		ThrownPotion potion = loc.getWorld().spawn(loc, ThrownPotion.class);
		potion.setItem(toSplash(i));
		if (consumer != null)
			consumer.accept(potion);
		return potion;
	}

	@Override
	public void set(ThrownPotion entity) {
		if (types != null) {
			ItemType t = CollectionUtils.getRandom(types);
			assert t != null;
			ItemStack i = t.getRandom();
			if (i == null)
				return; // Missing item, can't make thrown potion of it
			entity.setItem(toSplash(i));
		}
	}

	/**
	 * Thrown potions need the splash bit set in the data value on 1.7.10.
	 */
	@SuppressWarnings("deprecation")
	private static ItemStack toSplash(ItemStack item) {
		ItemStack splash = item.clone();
		splash.setDurability((short) (splash.getDurability() | SPLASH_BIT));
		return splash;
	}
	
	@Override
	public Class<? extends ThrownPotion> getType() {
		return ThrownPotion.class;
	}
	
	@Override
	public EntityData getSuperType() {
		return new ThrownPotionData();
	}
	
	@Override
	public boolean isSupertypeOf(EntityData<?> e) {
		if (!(e instanceof ThrownPotionData))
			return false;
		ThrownPotionData d = (ThrownPotionData) e;
		if (types != null) {
			return d.types != null && ItemType.isSubset(types, d.types);
		}
		return true;
	}
	
	@Override
	public String toString(int flags) {
		ItemType[] types = this.types;
		if (types == null)
			return super.toString(flags);
		StringBuilder b = new StringBuilder();
		b.append(Noun.getArticleWithSpace(types[0].getTypes().get(0).getGender(), flags));
		b.append(m_adjective.toString(types[0].getTypes().get(0).getGender(), flags));
		b.append(" ");
		b.append(Classes.toString(types, flags & Language.NO_ARTICLE_MASK, false));
		return "" + b.toString();
	}
	
	//		return ItemType.serialize(types);
	@Override
	@Deprecated
	protected boolean deserialize(String s) {
		throw new UnsupportedOperationException("old serialization is no longer supported");
//		if (s.isEmpty())
//			return true;
//		types = ItemType.deserialize(s);
//		return types != null;
	}
	
	@Override
	protected boolean equals_i(EntityData<?> obj) {
		if (!(obj instanceof ThrownPotionData))
			return false;
		return Arrays.equals(types, ((ThrownPotionData) obj).types);
	}
	
	@Override
	protected int hashCode_i() {
		return Arrays.hashCode(types);
	}
	
}
