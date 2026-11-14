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


import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("deprecation")
@Name("Toggle")
@Description("Toggle the state of a block.")
@Examples({"# use arrows to toggle switches, doors, etc.",
		"on projectile hit:",
		"\tprojectile is arrow",
		"\ttoggle the block at the arrow"})
@Since("1.4")
public class EffToggle extends Effect {
	
	static {
		Skript.registerEffect(EffToggle.class, "(close|turn off|de[-]activate) %blocks%", "(toggle|switch) [[the] state of] %blocks%", "(open|turn on|activate) %blocks%");
	}
	
	@SuppressWarnings("null")
	private Expression<Block> blocks;
	private int toggle;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		blocks = (Expression<Block>) vars[0];
		toggle = matchedPattern - 1;
		return true;
	}
	
	// Block ids and data values (Minecraft 1.7.10 uses numeric ids and data values for block states)
	private final static int MAX_VANILLA_ID = 255;
	private final static byte[] bitFlags = new byte[MAX_VANILLA_ID + 1];
	private final static boolean[] doors = new boolean[MAX_VANILLA_ID + 1];
	static {
		bitFlags[28] = 0x8; // Detector rail
		// Doors
		bitFlags[64] = 0x4; // Oak door (block)
		bitFlags[193] = 0x4; // Spruce door (block)
		bitFlags[194] = 0x4; // Birch door (block)
		bitFlags[195] = 0x4; // Jungle door (block)
		bitFlags[196] = 0x4; // Acacia door (block)
		bitFlags[197] = 0x4; // Dark oak door (block)
		bitFlags[71] = 0x4; // Iron door (block)
		// Redstone stuff
		bitFlags[69] = 0x8; // Lever
		bitFlags[70] = 0x1; // Stone pressure plate
		bitFlags[72] = 0x1; // Wooden pressure plate
		bitFlags[77] = 0x8; // Stone button
		bitFlags[143] = 0x8; // Wooden button
		// Trapdoors
		bitFlags[96] = 0x4; // Wooden trapdoor
		bitFlags[167] = 0x4; // Iron trapdoor
		// Fence gates
		bitFlags[107] = 0x4; // Oak fence gate
		bitFlags[183] = 0x4; // Spruce fence gate
		bitFlags[184] = 0x4; // Birch fence gate
		bitFlags[185] = 0x4; // Jungle fence gate
		bitFlags[186] = 0x4; // Dark oak fence gate
		bitFlags[187] = 0x4; // Acacia fence gate
		
		doors[64] = true; // Oak door (block)
		doors[193] = true; // Spruce door (block)
		doors[194] = true; // Birch door (block)
		doors[195] = true; // Jungle door (block)
		doors[196] = true; // Acacia door (block
		doors[197] = true; // Dark oak door (block)
		doors[71] = true; // Iron door (block)
	}
	
	@Override
	@SuppressWarnings("deprecation")
	protected void execute(final Event e) {
		for (Block b : blocks.getArray(e)) {
			int type = b.getTypeId();
			if (type < 0 || type > MAX_VANILLA_ID) // modded blocks (Forge ids go up to 4095) can't be toggled
				continue;
			
			byte data = b.getData();
			if (doors[type] == true && (data & 0x8) == 0x8) {
				b = b.getRelative(BlockFace.DOWN);
				type = b.getTypeId();
				if (type < 0 || type > MAX_VANILLA_ID || !doors[type])
					continue;
				data = b.getData();
			}
			
			if (toggle == -1)
				b.setData((byte) (data & ~bitFlags[type]));
			else if (toggle == 0)
				b.setData((byte) (data ^ bitFlags[type]));
			else
				b.setData((byte) (data | bitFlags[type]));
		}
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "toggle " + blocks.toString(e, debug);
	}
	
}
