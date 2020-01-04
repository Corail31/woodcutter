package ovh.corail.woodcutter.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.common.ToolType;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class WoodcutterBlock extends Block {
    private static final TranslationTextComponent TRANSLATION = new TranslationTextComponent("container.corail_woodcutter.woodcutter");
    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.makeCuboidShape(0d, 0d, 0d, 16d, 9d, 16d);

    public WoodcutterBlock() {
        super(Properties.create(Material.WOOD).hardnessAndResistance(3.5f).harvestTool(ToolType.AXE).harvestLevel(0));
        setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote) {
            player.openContainer(state.getContainer(worldIn, pos));
            //player.addStat(Stats.INTERACT_WITH_STONECUTTER);
        }
        return true;
    }

    @Override
    @Nullable
    public INamedContainerProvider getContainer(BlockState state, World worldIn, BlockPos pos) {
        return new SimpleNamedContainerProvider((id, playerInventory, player) -> new WoodcutterContainer(id, playerInventory, IWorldPosCallable.of(worldIn, pos)), TRANSLATION);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }

    @Override
    public boolean func_220074_n(BlockState state) {
        return true;
    }

    @Override
    public boolean isSolid(BlockState state) {
        return true;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this));
        return drops;
    }
}
