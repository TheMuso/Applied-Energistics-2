/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.tile.grid;


import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.tile.AEBaseInvTile;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;


public abstract class AENetworkInvTile extends AEBaseInvTile implements IActionHost, IGridProxyable
{

	private final AENetworkProxy gridProxy = new AENetworkProxy( this, "proxy", this.getItemFromTile( this ), true );

	@TileEvent( TileEventType.WORLD_NBT_READ )
	public void readFromNBT_AENetwork( final NBTTagCompound data )
	{
		this.getGridProxy().readFromNBT( data );
	}

	@TileEvent( TileEventType.WORLD_NBT_WRITE )
	public void writeToNBT_AENetwork( final NBTTagCompound data )
	{
		this.getGridProxy().writeToNBT( data );
	}

	@Override
	public AENetworkProxy getProxy()
	{
		return this.getGridProxy();
	}

	@Override
	public void gridChanged()
	{

	}

	@Override
	public IGridNode getGridNode( final ForgeDirection dir )
	{
		return this.getGridProxy().getNode();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		this.getGridProxy().onChunkUnload();
	}

	@Override
	public void onReady()
	{
		super.onReady();
		this.getGridProxy().onReady();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		this.getGridProxy().invalidate();
	}

	@Override
	public void validate()
	{
		super.validate();
		this.getGridProxy().validate();
	}

	@Override
	public IGridNode getActionableNode()
	{
		return this.getGridProxy().getNode();
	}

	public AENetworkProxy getGridProxy()
	{
		return this.gridProxy;
	}

}
