// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Moving is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Moving. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================

package net.smart.moving;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent.*;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.*;
import net.minecraft.entity.player.*;
import net.minecraft.network.*;
import net.smart.moving.config.*;
import net.smart.moving.render.*;
import net.smart.utilities.*;

@Mod(modid = SmartMovingMod.ID, name = SmartMovingMod.NAME, version = SmartMovingMod.VERSION, useMetadata = true/*@MCVERSIONDEP@*/)
public class SmartMovingMod
{
	final static String ID = "smartmoving";
	final static String NAME = "Smart Moving";
	final static String VERSION = "@VERSION@";

	protected static String ModComVersion = "2.4";

	private final boolean isClient;

	private boolean hasRenderer = false;

	public SmartMovingMod()
	{
		isClient = FMLCommonHandler.instance().getSide().isClient();
	}

	@EventHandler
	public void init(FMLPreInitializationEvent event)
	{
		if(isClient)
		{
			hasRenderer = Loader.isModLoaded("RenderPlayerAPI");

			if(hasRenderer)
			{
				Class<?> type = Reflect.LoadClass(SmartMovingMod.class, new Name("net.smart.moving.render.playerapi.SmartMoving"), true);
				Method method = Reflect.GetMethod(type, new Name("register"));
				Reflect.Invoke(method, null);
			}
			else
				net.smart.render.SmartRenderMod.doNotAddRenderer();

			if(!hasRenderer)
				net.smart.render.SmartRenderContext.registerRenderers(RenderPlayer.class);
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		NetworkRegistry.INSTANCE.newEventDrivenChannel(SmartMovingPacketStream.Id).register(this);

		if(isClient)
		{
			net.smart.moving.playerapi.SmartMoving.register();

			SmartMovingServerComm.localUserNameProvider = new LocalUserNameProvider();

			registerGameTicks();

			net.smart.moving.playerapi.SmartMovingFactory.initialize();

			checkForPresentModsAndInitializeOptions();

			SmartMovingContext.initialize();
		}
		else
			SmartMovingServer.initialize(new File("."), FMLCommonHandler.instance().getMinecraftServerInstance().getGameType().getID(), new SmartMovingConfig());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		if(!isClient)
			net.smart.moving.playerapi.SmartMovingServerPlayerBase.registerPlayerBase();
	}

	@SubscribeEvent
	public void tickStart(ClientTickEvent event)
	{
		SmartMovingContext.onTickInGame();
	}

	@SubscribeEvent
	public void onPacketData(ServerCustomPacketEvent event)
	{
		SmartMovingPacketStream.receivePacket(event.getPacket(), SmartMovingServerComm.instance, net.smart.moving.playerapi.SmartMovingServerPlayerBase.getPlayerBase(((NetHandlerPlayServer)event.getHandler()).playerEntity));
	}

	@SubscribeEvent
	public void onPacketData(ClientCustomPacketEvent event)
	{
		SmartMovingPacketStream.receivePacket(event.getPacket(), SmartMovingComm.instance, null);
	}

	public void registerGameTicks()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	public Object getInstance(EntityPlayer entityPlayer)
	{
		return SmartMovingFactory.getInstance(entityPlayer);
	}

	public Object getClient()
	{
		return SmartMovingContext.Client;
	}

	public void checkForPresentModsAndInitializeOptions()
	{
		List<ModContainer> modList = Loader.instance().getActiveModList();
		boolean hasRedPowerWiring = false;
		boolean hasBuildCraftTransport = false;
		boolean hasFiniteLiquid = false;
		boolean hasBetterThanWolves = false;
		boolean hasSinglePlayerCommands = false;
		boolean hasRopesPlus = false;
		boolean hasASGrapplingHook = false;
		boolean hasBetterMisc = false;

		for(int i = 0; i < modList.size(); i++)
		{
			ModContainer mod = modList.get(i);
			String name = mod.getName();

			if(name.contains("RedPowerWiring"))
				hasRedPowerWiring = true;
			else if(name.contains("BuildCraftTransport"))
				hasBuildCraftTransport = true;
			else if(name.contains("Liquid"))
				hasFiniteLiquid = true;
			else if(name.contains("FCBetterThanWolves"))
				hasBetterThanWolves = true;
			else if(name.contains("SinglePlayerCommands"))
				hasSinglePlayerCommands = true;
			else if(name.contains("ASGrapplingHook"))
				hasASGrapplingHook = true;
			else if(name.contains("BetterMisc"))
				hasBetterMisc = true;
		}

		hasRopesPlus = Reflect.CheckClasses(SmartMovingMod.class, SmartMovingInstall.RopesPlusCore);

		SmartMovingOptions.initialize(hasRedPowerWiring, hasBuildCraftTransport, hasFiniteLiquid, hasBetterThanWolves, hasSinglePlayerCommands, hasRopesPlus, hasASGrapplingHook, hasBetterMisc);
	}
}