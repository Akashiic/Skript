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
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.block.BlockCompat;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A block that gets all data from a BlockState, and either reflects changes on the BlockState
 * or delays them to the real block by 1 tick depending on which constructor is used.
 */
@SuppressWarnings("deprecation")
public class BlockStateBlock implements Block {

	final BlockState state;
	private final boolean delayChanges;

	public BlockStateBlock(BlockState state) {
		this(state, false);
	}

	public BlockStateBlock(BlockState state, boolean delayChanges) {
		assert state != null;
		this.state = state;
		this.delayChanges = delayChanges;
	}

	private void later(Runnable r) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), r);
	}

	@Override
	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		state.setMetadata(metadataKey, newMetadataValue);
	}

	@Override
	public List<MetadataValue> getMetadata(String metadataKey) {
		return state.getMetadata(metadataKey);
	}

	@Override
	public boolean hasMetadata(String metadataKey) {
		return state.hasMetadata(metadataKey);
	}

	@Override
	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		state.removeMetadata(metadataKey, owningPlugin);
	}

	@Override
	public byte getData() {
		return state.getRawData();
	}

	@Override
	public void setData(byte data) {
		setData(data, true);
	}

	@Override
	public void setData(final byte data, final boolean applyPhysics) {
		if (delayChanges) {
			later(() -> state.getBlock().setData(data, applyPhysics));
		} else {
			state.setRawData(data);
		}
	}

	@Override
	public Block getRelative(int modX, int modY, int modZ) {
		return state.getBlock().getRelative(modX, modY, modZ);
	}

	@Override
	public Block getRelative(BlockFace face) {
		return state.getBlock().getRelative(face);
	}

	@Override
	public Block getRelative(BlockFace face, int distance) {
		return state.getBlock().getRelative(face, distance);
	}

	@Override
	public Material getType() {
		return state.getType();
	}

	@Override
	public int getTypeId() {
		return state.getTypeId();
	}

	@Override
	public byte getLightLevel() {
		return state.getLightLevel();
	}

	@Override
	public byte getLightFromSky() {
		return state.getBlock().getLightFromSky();
	}

	@Override
	public byte getLightFromBlocks() {
		return state.getBlock().getLightFromBlocks();
	}

	@Override
	public World getWorld() {
		return state.getWorld();
	}

	@Override
	public int getX() {
		return state.getX();
	}

	@Override
	public int getY() {
		return state.getY();
	}

	@Override
	public int getZ() {
		return state.getZ();
	}

	@Override
	public Location getLocation() {
		return state.getLocation();
	}

	@Override
	@Nullable
	public Location getLocation(@Nullable Location loc) {
		if (loc != null) {
			loc.setWorld(getWorld());
			loc.setX(getX());
			loc.setY(getY());
			loc.setZ(getZ());
			loc.setPitch(0);
			loc.setYaw(0);
		}
		return loc;
	}

	@Override
	public Chunk getChunk() {
		return state.getChunk();
	}

	@Override
	public void setType(final Material type) {
		if (delayChanges) {
			later(() -> state.getBlock().setType(type));
		} else {
			state.setType(type);
		}
	}

	@Override
	public boolean setTypeId(int type) {
		return setTypeId(type, true);
	}

	@Override
	public boolean setTypeId(final int type, final boolean applyPhysics) {
		if (delayChanges) {
			later(() -> state.getBlock().setTypeId(type, applyPhysics));
			return true;
		} else {
			return state.setTypeId(type);
		}
	}

	@Override
	public boolean setTypeIdAndData(final int type, final byte data, final boolean applyPhysics) {
		if (delayChanges) {
			later(() -> state.getBlock().setTypeIdAndData(type, data, applyPhysics));
			return true;
		} else {
			boolean changed = state.setTypeId(type);
			state.setRawData(data);
			return changed;
		}
	}

	@Nullable
	@Override
	public BlockFace getFace(Block block) {
		return state.getBlock().getFace(block);
	}

	@Override
	public BlockState getState() {
		return state;
	}

	@Override
	public Biome getBiome() {
		return state.getBlock().getBiome();
	}

	@Override
	public void setBiome(Biome bio) {
		state.getBlock().setBiome(bio);
	}

	@Override
	public boolean isBlockPowered() {
		return state.getBlock().isBlockPowered();
	}

	@Override
	public boolean isBlockIndirectlyPowered() {
		return state.getBlock().isBlockIndirectlyPowered();
	}

	@Override
	public boolean isBlockFacePowered(BlockFace face) {
		return state.getBlock().isBlockFacePowered(face);
	}

	@Override
	public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
		return state.getBlock().isBlockFaceIndirectlyPowered(face);
	}

	@Override
	public int getBlockPower(BlockFace face) {
		return state.getBlock().getBlockPower(face);
	}

	@Override
	public int getBlockPower() {
		return state.getBlock().getBlockPower();
	}

	@Override
	public boolean isEmpty() {
		Material type = getType();
		assert type != null;
		return BlockCompat.INSTANCE.isEmpty(type);
	}

	@Override
	public boolean isLiquid() {
		Material type = getType();
		assert type != null;
		return BlockCompat.INSTANCE.isLiquid(type);
	}

	@Override
	public double getTemperature() {
		return state.getBlock().getTemperature();
	}

	@Override
	public double getHumidity() {
		return state.getBlock().getHumidity();
	}

	@Override
	public PistonMoveReaction getPistonMoveReaction() {
		return state.getBlock().getPistonMoveReaction();
	}

	@Override
	public boolean breakNaturally() {
		if (delayChanges) {
			later(() -> state.getBlock().breakNaturally());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean breakNaturally(@Nullable final ItemStack tool) {
		if (delayChanges) {
			later(() -> state.getBlock().breakNaturally(tool));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Collection<ItemStack> getDrops() {
		return state.getBlock().getDrops();
	}

	@Override
	public Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
		return state.getBlock().getDrops(tool);
	}

	@Override
	public String toString() {
		return "BlockStateBlock{" + getType() + ":" + getData() + " at " + getX() + "," + getY() + "," + getZ() + "}";
	}

}
