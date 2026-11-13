package com.lordobesity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LordObesityMod.MODID);

    public static final RegistryObject<Block> RITUAL_CAULDRON =
            BLOCKS.register("ritual_cauldron", () ->
                    new RitualCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON)));
}
