package com.mraof.minestuck.world.lands;

import com.mraof.minestuck.world.biome.LandBiomeHolder;
import com.mraof.minestuck.world.biome.LandWrapperBiome;
import com.mraof.minestuck.world.lands.gen.LandGenSettings;
import com.mraof.minestuck.world.lands.structure.IGateStructure;
import com.mraof.minestuck.world.lands.structure.blocks.StructureBlockRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public interface ILandAspect<A extends ILandAspect> extends IForgeRegistryEntry<A>
{
	/**
	 * Returns a list of strings used in giving a land a random name.
	 */
	String[] getNames();
	
	IGateStructure getGateStructure();
	
	boolean canBePickedAtRandom();
	
	ResourceLocation getGroup();
	
	void registerBlocks(StructureBlockRegistry registry);
	
	default void setBiomeSettings(LandBiomeHolder settings)
	{}
	
	default void setGenSettings(LandGenSettings settings)
	{}
	
	default void setBiomeGenSettings(LandWrapperBiome biome, StructureBlockRegistry blockRegistry)
	{}
}
