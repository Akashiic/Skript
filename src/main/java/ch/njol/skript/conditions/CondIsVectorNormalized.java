/**
*  This file is part of Skript.
*
* Skript is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Skript is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Skript.  If not, see <http://www.gnu.org/licenses/>.
*
* Copyright Peter Güttinger, SkriptLang team and contributors
*/
package ch.njol.skript.conditions;
	
	import org.bukkit.util.Vector;
	
	import ch.njol.skript.conditions.base.PropertyCondition;
	import ch.njol.skript.doc.Description;
	import ch.njol.skript.doc.Examples;
	import ch.njol.skript.doc.Name;
	import ch.njol.skript.doc.Since;

@Name("Is Normalized")
@Description("Checks whether a vector is normalized i.e. length of 1")
@Examples("vector of player's location is normalized")
@Since("2.5.1")
public class CondIsVectorNormalized extends PropertyCondition<Vector> {
	
	static {
		register(CondIsVectorNormalized.class, "normalized", "vectors");
	}
	
	@Override
	public boolean check(Vector vector) {
		// Vector#isNormalized doesn't exist on 1.7.10; same epsilon as Bukkit's implementation
		return Math.abs(vector.lengthSquared() - 1) < 0.000001;
	}
	
	@Override
	protected String getPropertyName() {
		return "normalized";
	}
	
}
