package com.lordobesity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Fragmento de lore (volumen 1-3) o libro completo (volumen 0). Clic derecho para leer. */
public class LoreBookItem extends Item {
    private final int volume;

    public LoreBookItem(int volume, Properties props) {
        super(props);
        this.volume = volume;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            final int v = this.volume;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LoreClient.open(v));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
