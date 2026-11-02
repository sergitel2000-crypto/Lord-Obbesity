package com.lordobesity;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LordObesityMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.LORD_OBESITY.get(), LordObesityEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(ModItems.LORD_OBESITY_SPAWN_EGG);
        }
        if (e.getTabKey() == CreativeModeTabs.COMBAT) {
            e.accept(ModItems.LORD_OBESITY_MASK);
            e.accept(ModItems.LORD_OBESITY_CANE);
            e.accept(ModItems.LORD_OBESITY_HAT);
            e.accept(ModItems.LORD_OBESITY_JACKET);
        }
    }
}
