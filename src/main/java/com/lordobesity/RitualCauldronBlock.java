package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.cauldron.CauldronInteraction;

/** Caldero con agua que se va tiñendo de amarillo (TINT 0 = agua normal, 9 = amarillo total). */
public class RitualCauldronBlock extends AbstractCauldronBlock {
    public static final IntegerProperty TINT = IntegerProperty.create("tint", 0, 9);

    public RitualCauldronBlock(Properties props) {
        super(props, CauldronInteraction.EMPTY);
        this.registerDefaultState(this.stateDefinition.any().setValue(TINT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TINT);
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }
}
