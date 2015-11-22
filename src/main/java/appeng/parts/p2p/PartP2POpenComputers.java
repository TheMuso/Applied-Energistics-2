/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.parts.p2p;


import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import li.cil.oc.api.API;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.core.AELog;
import appeng.core.settings.TickRates;
import appeng.hooks.TickHandler;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.me.GridAccessException;
import appeng.transformer.annotations.Integration.Interface;
import appeng.transformer.annotations.Integration.InterfaceList;
import appeng.util.IWorldCallable;


@InterfaceList( value = { @Interface( iface = "li.cil.oc.api.network.Environment", iname = IntegrationType.OpenComputers ), @Interface( iface = "li.cil.oc.api.network.SidedEnvironment", iname = IntegrationType.OpenComputers ) } )
public final class PartP2POpenComputers extends PartP2PTunnel<PartP2POpenComputers> implements IGridTickable, Environment, SidedEnvironment
{
	@Nullable
	private final Node node;

	private final IWorldCallable<Void> updateCallback;

	public PartP2POpenComputers( final ItemStack is )
	{
		super( is );

		if( !IntegrationRegistry.INSTANCE.isEnabled( IntegrationType.OpenComputers ) )
		{
			throw new RuntimeException( "OpenComputers is not installed!" );
		}

		// Avoid NPE when called in pre-init phase (part population).
		if( API.network != null )
		{
			this.node = Network.newNode( this, Visibility.None ).create();
		}
		else
		{
			this.node = null; // to satisfy final
		}

		this.updateCallback = new UpdateCallback();
	}

	@Override
	@SideOnly( Side.CLIENT )
	public IIcon getTypeTexture()
	{
		return Items.get( "adapter" ).block().getBlockTextureFromSide( 2 );
	}

	@Override
	public void removeFromWorld()
	{
		super.removeFromWorld();
		if( this.node != null )
		{
			this.node.remove();
		}
	}

	@Override
	public void onTunnelNetworkChange()
	{
		super.onTunnelNetworkChange();
		try
		{
			this.getProxy().getTick().wakeDevice( this.getProxy().getNode() );
		}
		catch( final GridAccessException e )
		{
			// ignore
		}
	}

	@Override
	public void readFromNBT( final NBTTagCompound data )
	{
		super.readFromNBT( data );
		if( this.node != null )
		{
			this.node.load( data );
		}
	}

	@Override
	public void writeToNBT( final NBTTagCompound data )
	{
		super.writeToNBT( data );
		if( this.node != null )
		{
			this.node.save( data );
		}
	}

	@Override
	public TickingRequest getTickingRequest( final IGridNode node )
	{
		return new TickingRequest( TickRates.OpenComputersTunnel.getMin(), TickRates.OpenComputersTunnel.getMax(), true, false );
	}

	@Override
	public TickRateModulation tickingRequest( final IGridNode node, final int ticksSinceLastCall )
	{
		try
		{
			if( !this.getProxy().getPath().isNetworkBooting() )
			{
				if( this.node() != null ) // Client side doesn't have nodes.
				{
					TickHandler.INSTANCE.addCallable( this.getTile().getWorldObj(), this.updateCallback );
				}

				return TickRateModulation.SLEEP;
			}
		}
		catch( final GridAccessException e )
		{
			// ignore
		}

		return TickRateModulation.IDLE;
	}

	private void updateConnections()
	{
		if( this.getProxy().isPowered() && this.getProxy().isActive() )
		{
			// Make sure we're connected to existing OC nodes in the world.
			Network.joinOrCreateNetwork( this.getTile() );

			if( this.isOutput() )
			{
				if( this.getInput() != null && this.node != null )
				{
					Network.joinOrCreateNetwork( this.getInput().getTile() );
					this.node.connect( this.getInput().node() );
				}
			}
			else
			{
				try
				{
					for( final PartP2POpenComputers output : this.getOutputs() )
					{
						if( this.node != null )
						{
							Network.joinOrCreateNetwork( output.getTile() );
							this.node.connect( output.node() );
						}
					}
				}
				catch( final GridAccessException e )
				{
					AELog.debug( e );
				}
			}
		}
		else if( this.node != null )
		{
			this.node.remove();
		}
	}

	@Nullable
	@Override
	public Node node()
	{
		return this.node;
	}

	@Override
	public void onConnect( final Node node )
	{
	}

	@Override
	public void onDisconnect( final Node node )
	{
	}

	@Override
	public void onMessage( final Message message )
	{
	}

	@Nullable
	@Override
	public Node sidedNode( final ForgeDirection side )
	{
		return side == this.getSide() ? this.node : null;
	}

	@Override
	public boolean canConnect( final ForgeDirection side )
	{
		return side == this.getSide();
	}

	private final class UpdateCallback implements IWorldCallable<Void>
	{
		@Nullable
		@Override
		public Void call( final World world ) throws Exception
		{
			PartP2POpenComputers.this.updateConnections();

			return null;
		}
	}
}
