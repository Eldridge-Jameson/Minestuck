package com.mraof.minestuck.block;

import com.mraof.minestuck.tileentity.TileEntityPunchDesignix;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BlockPunchDesignix extends BlockLargeMachine
{
	
	public static final Map<EnumFacing, VoxelShape> LEG_SHAPE = createRotatedShapes(0, 0, 0, 16, 16, 12);
	public static final Map<EnumFacing, VoxelShape> SLOT_SHAPE = createRotatedShapes(1, 0, 0, 16, 7, 7);
	public static final Map<EnumFacing, VoxelShape> KEYBOARD_SHAPE = createRotatedShapes(0, 0, 0, 15, 7, 12);
	
	protected final Map<EnumFacing, VoxelShape> shape;
	protected final BlockPos mainPos;
	
	public BlockPunchDesignix(Properties properties, Map<EnumFacing, VoxelShape> shape, BlockPos pos)
	{
		super(properties);
		this.shape = shape;
		this.mainPos = pos;
	}
	
	@Override
	public VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos)
	{
		return shape.get(state.get(FACING));
	}
	
	@Override
	public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (worldIn.isRemote)
			return true;
		BlockPos mainPos = getMainPos(state, pos);
		TileEntity te = worldIn.getTileEntity(mainPos);
		if (te instanceof TileEntityPunchDesignix)
			((TileEntityPunchDesignix) te).onRightClick((EntityPlayerMP) player, state);
		return true;
	}
	
	@Override
	public void onReplaced(IBlockState state, World worldIn, BlockPos pos, IBlockState newState, boolean isMoving)
	{
		BlockPos mainPos = getMainPos(state, pos);
		TileEntity te = worldIn.getTileEntity(mainPos);
		if(te instanceof TileEntityPunchDesignix)
		{
			TileEntityPunchDesignix designix = (TileEntityPunchDesignix) te;
			designix.broken = true;
			if(hasTileEntity(state))
				designix.dropItem(true);
		}
		
		super.onReplaced(state, worldIn, pos, newState, isMoving);
	}
	
	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
	{
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityPunchDesignix)
			((TileEntityPunchDesignix) te).checkStates();
	}
	
    /**
     *returns the block position of the "Main" block
     *aka the block with the TileEntity for the machine
     *@param state the state of the block
     *@param pos the position the block
     */
	public BlockPos getMainPos(IBlockState state, BlockPos pos)
	{
		EnumFacing facing = state.get(FACING);
		Rotation rotation = rotationFromFacing(facing);
		
		return pos.add(this.mainPos.rotate(rotation));
	}
	
	public static class Slot extends BlockPunchDesignix
	{
		public static final BooleanProperty HAS_CARD = MinestuckProperties.HAS_CARD;
		
		public Slot(Properties properties, Map<EnumFacing, VoxelShape> shape)
		{
			super(properties, shape, new BlockPos(0, 0, 0));
		}
		
		@Override
		protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder)
		{
			super.fillStateContainer(builder);
			builder.add(HAS_CARD);
		}
		
		@Override
		public boolean hasTileEntity(IBlockState state)
		{
			return true;
		}
		
		@Nullable
		@Override
		public TileEntity createTileEntity(IBlockState state, IBlockReader world)
		{
			return new TileEntityPunchDesignix();
		}
	}
}