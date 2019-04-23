package com.mraof.minestuck.block;

import com.mraof.minestuck.tileentity.TileEntityAlchemiter;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BlockAlchemiter extends BlockLargeMachine
{
	public static final Map<EnumFacing, VoxelShape> FULL_BLOCK_SHAPE = createRotatedShapes(0, 0, 0, 16, 16, 16);
	public static final Map<EnumFacing, VoxelShape> TOTEM_PAD_SHAPE = createRotatedShapes(8, 0, 2, 14, 16, 16);
	public static final Map<EnumFacing, VoxelShape> LOWER_ROD_SHAPE = createRotatedShapes(10, 0, 2, 14, 16, 16);
	public static final Map<EnumFacing, VoxelShape> UPPER_ROD_SHAPE = createRotatedShapes(7, 0, 2, 14, 10, 16);
	
	public static final Map<EnumFacing, BlockFaceShape> SOLID_FACE_SHAPES = createEnumMapping(EnumFacing.class, EnumFacing.values(), enumFacing -> BlockFaceShape.SOLID);
	public static final Map<EnumFacing, BlockFaceShape> SIDE_FACE_SHAPES = createEnumMapping(EnumFacing.class, EnumFacing.values(), enumFacing -> enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.SOUTH ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED);
	public static final Map<EnumFacing, BlockFaceShape> CORNER_FACE_SHAPES = createEnumMapping(EnumFacing.class, EnumFacing.values(), enumFacing -> enumFacing == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED);
	public static final Map<EnumFacing, BlockFaceShape> UNDEFINED_FACE_SHAPES = createEnumMapping(EnumFacing.class, EnumFacing.values(), enumFacing -> BlockFaceShape.UNDEFINED);
	
	protected final Map<EnumFacing, VoxelShape> shape;
	protected final Map<EnumFacing, BlockFaceShape> faceShapes;
	protected final boolean fullCube, recursive, corner;
	protected final BlockPos mainPos;
	
	public BlockAlchemiter(Properties properties, Map<EnumFacing, VoxelShape> shape, Map<EnumFacing, BlockFaceShape> faceShapes, boolean fullCube, boolean recursive, boolean corner, BlockPos mainPos)
	{
		super(properties);
		this.shape = shape;
		this.faceShapes = faceShapes;
		this.fullCube = fullCube;
		this.recursive = recursive;
		this.corner = corner;
		this.mainPos = mainPos;
	}
	
	@Override
	public VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos)
	{
		return shape.get(state.get(FACING));
	}
	
	@Override
	public boolean isFullCube(IBlockState state)
	{
		return fullCube;
	}
	
	@Override
	public BlockFaceShape getBlockFaceShape(IBlockReader worldIn, IBlockState state, BlockPos pos, EnumFacing face)
	{
		Rotation rotation = rotationFromFacing(face);
		return faceShapes.get(rotation.rotate(state.get(FACING)));
	}
	
	@Override
	public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		BlockPos mainPos = getMainPos(state, pos, worldIn);
		TileEntity te = worldIn.getTileEntity(mainPos);
		
		if (te instanceof TileEntityAlchemiter)
		{
			((TileEntityAlchemiter) te).onRightClick(worldIn, player, state);
		}
		
		return true;
	}
	
	@Override
	public void onReplaced(IBlockState state, World worldIn, BlockPos pos, IBlockState newState, boolean isMoving)
	{
		BlockPos mainPos = getMainPos(state, pos, worldIn);
		TileEntity te = worldIn.getTileEntity(mainPos);
		if(te instanceof TileEntityAlchemiter)
		{
			TileEntityAlchemiter alchemiter = (TileEntityAlchemiter) te;
			alchemiter.breakMachine();
			if(mainPos.equals(pos))
				alchemiter.dropItem(true);
		}
		
		super.onReplaced(state, worldIn, pos, newState, isMoving);
	}
	
    /**
     * returns the block position of the "Main" block
     * aka the block with the TileEntity for the machine
     */
	public BlockPos getMainPos(IBlockState state, BlockPos pos, IBlockReader world)
	{
		return getMainPos(state, pos, world, 4);
	}
	
	protected BlockPos getMainPos(IBlockState state, BlockPos pos, IBlockReader world, int count)
	{
		EnumFacing facing = state.get(FACING);
		
		BlockPos newPos = pos.add(mainPos.rotate(rotationFromFacing(facing)));
		
		if(!recursive)
			return newPos;
		else
		{
			IBlockState newState = world.getBlockState(newPos);
			if(count > 0 && newState.getBlock() instanceof BlockAlchemiter && ((BlockAlchemiter) newState.getBlock()).corner
					&& newState.get(FACING).equals(this.corner ? state.get(FACING).rotateY() : state.get(FACING)))
			{
				return ((BlockAlchemiter) newState.getBlock()).getMainPos(state, pos, world, count - 1);
			} else return new BlockPos(0, -1 , 0);
		}
	}
	
	public enum EnumParts implements IStringSerializable
	{
		TOTEM_CORNER(new AxisAlignedBB(0.0D,0.0D,0.0D,1.0D,1.0D,1.0D)),
		TOTEM_PAD(   new AxisAlignedBB(8/16D,0.0D,2/16D,14/16D,1.0D,1.0D),new AxisAlignedBB(0.0D,0.0D,8/16D,14/16D,1.0D,14/16D),
				     new AxisAlignedBB(2/16D,0.0D,0.0D,8/16D,1.0D,14/16D),new AxisAlignedBB(2/16D,0.0D,2/16D,1.0D,1.0D,8/16D)),
		LOWER_ROD(   new AxisAlignedBB(10/16D,0.0D,2/16D,14/16D,1.0D,1.0D),new AxisAlignedBB(0.0D,0.0D,10/16D,14/16D,1.0D,14/16D),
				     new AxisAlignedBB(2/16D,0.0D,0.0D,6/16D,1.0D,14/16D),new AxisAlignedBB(2/16D,0.0D,2/16D,1.0D,1.0D,6/16D)),
		UPPER_ROD(   new AxisAlignedBB(7/16D,0.0D,2/16D,14/16D,10/16D,1.0D),new AxisAlignedBB(0.0D,0.0D,7/16D,14/16D,10/16D,14/16D),
				     new AxisAlignedBB(2/16D,0.0D,0.0D,9/16D,10/16D,14/16D),new AxisAlignedBB(2/16D,0.0D,2/16D,1.0D,10/16D,9/16D)),
		
		SIDE_LEFT(   new AxisAlignedBB(0.0D,0.0D,0.0D,1.0D,1.0D,1.0D)),
		SIDE_RIGHT(  new AxisAlignedBB(0.0D,0.0D,0.0D,1.0D,1.0D,1.0D)),
		CORNER(      new AxisAlignedBB(0.0D,0.0D,0.0D,1.0D,1.0D,1.0D)),
		CENTER_PAD(  new AxisAlignedBB(0.0D,0.0D,0.0D,1.0D,1.0D,1.0D)),
		TOTEM_PAD_DOWEL(new AxisAlignedBB(6.5/16D,0.0D,2/16D,14/16D,1.0D,13/16D),new AxisAlignedBB(3/16D,0.0D,6.5/16D,14/16D,1.0D,14/16D),
				new AxisAlignedBB(2/16D,0.0D,3/16D,9.5/16D,1.0D,14/16D),new AxisAlignedBB(2/16D,0.0D,2/16D,13/16D,1.0D,9.5/16D)),
		TOTEM_PAD_TOTEM(new AxisAlignedBB(6.5/16D,0.0D,2/16D,14/16D,1.0D,13/16D),new AxisAlignedBB(3/16D,0.0D,6.5/16D,14/16D,1.0D,14/16D),
				     new AxisAlignedBB(2/16D,0.0D,3/16D,9.5/16D,1.0D,14/16D),new AxisAlignedBB(2/16D,0.0D,2/16D,13/16D,1.0D,9.5/16D));
		
		private final AxisAlignedBB[] BOUNDING_BOX;
		
		EnumParts(AxisAlignedBB... bb)
		{
			BOUNDING_BOX = bb;
		}
		
		public AxisAlignedBB getBoundingBox(EnumFacing facing)
		{
			return BOUNDING_BOX[facing.getHorizontalIndex() % BOUNDING_BOX.length];
		}
		
		@Override
		public String toString()
		{
			return getName();
		}
		
		@Override
		public String getName()
		{
			return name().toLowerCase();
		}
		
		public boolean isTotemPad()
		{
			return this == TOTEM_PAD || this == TOTEM_PAD_DOWEL || this == TOTEM_PAD_TOTEM;
		}
	}

	public static class Pad extends BlockAlchemiter
	{
		public static final EnumProperty<DowelType> DOWEL = MinestuckProperties.DOWEL_OR_NONE;
		
		public Pad(Properties properties, Map<EnumFacing, VoxelShape> shape, Map<EnumFacing, BlockFaceShape> faceShapes, boolean fullCube)
		{
			super(properties, shape, faceShapes, fullCube, false, false, new BlockPos(0, 0, 0));
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
			return new TileEntityAlchemiter();
		}
		
		@Override
		protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder)
		{
			super.fillStateContainer(builder);
			builder.add(DOWEL);
		}
	}
}