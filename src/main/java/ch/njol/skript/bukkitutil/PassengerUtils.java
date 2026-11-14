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
package ch.njol.skript.bukkitutil;

import org.bukkit.entity.Entity;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Passenger helpers. Minecraft 1.7.10 only supports a single passenger per vehicle.
 */
public abstract class PassengerUtils {

	private PassengerUtils() {}

	public static Entity[] getPassenger(Entity e) {
		Entity passenger = e.getPassenger();
		return passenger == null ? new Entity[0] : new Entity[] {passenger};
	}

	/**
	 * Add the passenger to the vehicle. Any previous passenger is ejected first.
	 * @param vehicle - The entity vehicle
	 * @param passenger - The entity passenger
	 */
	public static void addPassenger(@Nullable Entity vehicle, @Nullable Entity passenger) {
		if (vehicle == null || passenger == null)
			return;
		vehicle.eject();
		vehicle.setPassenger(passenger);
	}

	/**
	 * Remove the passenger from the vehicle.
	 * @param vehicle - The entity vehicle
	 * @param passenger - The entity passenger
	 */
	public static void removePassenger(@Nullable Entity vehicle, @Nullable Entity passenger) {
		if (vehicle == null || passenger == null)
			return;
		if (passenger.equals(vehicle.getPassenger()))
			vehicle.eject();
	}

	/**
	 * @return false, Minecraft 1.7.10 does not support multiple passengers
	 */
	public static boolean hasMultiplePassenger() {
		return false;
	}

}
