package com.mraof.minestuck.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketPlayerAbilities;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Iterator;

public class Teleport
{
	public static boolean teleportEntity(Entity entity, int destinationDimension, ITeleporter teleporter, BlockPos dest)
	{
		return teleportEntity(entity, destinationDimension, teleporter, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
	}
	
	public static boolean teleportEntity(Entity entity, int destinationDimension, ITeleporter teleporter)
	{
		return teleportEntity(entity, destinationDimension, teleporter, entity.posX, entity.posY, entity.posZ);
	}
	
	public static boolean teleportEntity(Entity entity, int destinationDimension, ITeleporter teleporter, double x, double y, double z)
	{
		if(destinationDimension == entity.dimension)
			return localTeleport(entity, teleporter, x, y, z);
		
		if(entity.world.isRemote)
			throw new IllegalArgumentException("Shouldn't do teleporting with a clientside entity.");
		if(!ForgeHooks.onTravelToDimension(entity, destinationDimension))
			return false;
		
		MinecraftServer mcServer = entity.getServer();
		int prevDimension = entity.dimension;
		WorldServer worldFrom = mcServer.getWorld(prevDimension);
		WorldServer worldDest = mcServer.getWorld(destinationDimension);
		
		if(entity instanceof EntityPlayerMP)
		{
			PlayerList playerList = mcServer.getPlayerList();
			EntityPlayerMP player = (EntityPlayerMP) entity;
			
			if(teleporter != null)
			{
				if(teleporter.prepareDestination(new BlockPos(x, y, z), entity, worldFrom))
				{
					teleporter.finalizeDestination(entity, worldFrom, worldDest);
				} else
				{
					return false;
				}
			}
			
			try
			{
				setPortalInvincibilityWithReflection(player);
			} catch(Exception e)
			{
				Debug.warn("Failed to set portal invincibility through reflection. Portal problems might occur. Problem: "+e.getMessage());
			}
			player.dimension = destinationDimension;
			
			SPacketRespawn respawnPacket = new SPacketRespawn(player.dimension, player.world.getDifficulty(), worldDest.getWorldInfo().getTerrainType(), player.interactionManager.getGameType());
			player.connection.sendPacket(respawnPacket);
			playerList.updatePermissionLevel(player);
			worldFrom.removeEntityDangerously(player);
			player.isDead = false;
			
			//player.setPosition(x, y, z);
			
			worldDest.spawnEntity(player);
			worldDest.updateEntityWithOptionalForce(entity, false);
			player.setWorld(worldDest);
			
			playerList.preparePlayer(player, worldFrom);
			
			player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
			player.interactionManager.setWorld(worldDest);
			player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));
			
			playerList.updateTimeAndWeatherForPlayer(player, worldDest);
			playerList.syncPlayerInventory(player);
			Iterator<?> iterator = player.getActivePotionEffects().iterator();
			
			while (iterator.hasNext())
			{
				PotionEffect potioneffect = (PotionEffect)iterator.next();
				player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
			}
			
			FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, prevDimension, destinationDimension);
			
			return true;
		}
		else if (!entity.isDead)
		{
			Entity newEntity = EntityList.newEntity(entity.getClass(), worldDest);
			if(newEntity == null)
				return false;

			NBTTagCompound nbt = new NBTTagCompound();
			entity.dimension = destinationDimension;
			entity.writeToNBT(nbt);
			
			if(teleporter != null)
			{
				if(teleporter.prepareDestination(new BlockPos(x, y, z), entity, worldFrom))
				{
					teleporter.finalizeDestination(entity, worldFrom, worldDest);
				} else
				{
					return false;
				}
			}
			worldDest.updateEntityWithOptionalForce(entity, false);
			
			nbt.removeTag("Dimension");
			newEntity.readFromNBT(nbt);
			newEntity.timeUntilPortal = entity.timeUntilPortal;
			newEntity.setPosition(x, y, z);
						
			boolean flag = newEntity.forceSpawn;
			newEntity.forceSpawn = true;
			worldDest.spawnEntity(newEntity);
			newEntity.forceSpawn = flag;
			worldDest.updateEntityWithOptionalForce(newEntity, false);
			
			entity.world.removeEntity(entity);
			entity.isDead = true;
			worldFrom.resetUpdateEntityTick();
			worldDest.resetUpdateEntityTick();
			
			return true;
		}
		return false;
	}
	
	public static boolean localTeleport(Entity entity, ITeleporter teleporter, double x, double y, double z)
	{
		if(entity.world.isRemote)
			throw new IllegalArgumentException("Shouldn't do teleporting with a clientside entity.");
		if(!ForgeHooks.onTravelToDimension(entity, entity.dimension))
			return false;
		
		if(entity instanceof EntityPlayerMP)
		{
			WorldServer world = (WorldServer) entity.world;
			PlayerList playerList = entity.getServer().getPlayerList();
			EntityPlayerMP player = (EntityPlayerMP) entity;
			try
			{
				setPortalInvincibilityWithReflection(player);
			} catch(Exception e)
			{
				Debug.warn("Failed to set portal invincibility through reflection. Portal problems might occur. Problem: "+e.getMessage());
			}
			
//			SPacketRespawn respawnPacket = new SPacketRespawn(player.dimension, world.getDifficulty(), world.getWorldInfo().getTerrainType(), player.interactionManager.getGameType());
//			player.connection.sendPacket(respawnPacket);
//			playerList.updatePermissionLevel(player);
			
			if(teleporter != null)
			{
				if (teleporter.prepareDestination(new BlockPos(x, y, z), entity, world))
				{
					teleporter.finalizeDestination(entity, world, world);
				} else
				{
					return false;
				}
			}
			player.setPosition(x, y, z);
//			world.updateEntityWithOptionalForce(entity, false);
			
//			playerList.preparePlayer(player, world);
			
			player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
//			player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));
			
			
			return true;
		} else if(!entity.isDead)
		{
			if(teleporter != null)
			{
				if (teleporter.prepareDestination(new BlockPos(x, y, z), entity, (WorldServer) entity.world))
				{
					teleporter.finalizeDestination(entity, (WorldServer) entity.world, (WorldServer) entity.world);
				} else
				{
					return false;
				}
			}
			
			entity.setPosition(x, y, z);
			return true;
		}
		
		return false;
	}
	
	private static Field portalInvincibilityField = null;
	
	private static void setPortalInvincibilityWithReflection(EntityPlayerMP player) throws Exception
	{
		if(portalInvincibilityField == null)
		{
			try
			{
				portalInvincibilityField = ReflectionHelper.findField(EntityPlayerMP.class, "invulnerableDimensionChange", "field_184851_cj");
			} catch (ReflectionHelper.UnableToFindFieldException e)
			{
				Debug.warn("Couldn't find portal invincibility field.");
				return;
			}
		}
		
		portalInvincibilityField.setBoolean(player, true);
	}
	
	public interface ITeleporter
	{
		boolean prepareDestination(BlockPos pos, Entity entity, WorldServer worldserver);
		void finalizeDestination(Entity entity, WorldServer worldserver, WorldServer worldserver1);
	}
	
}