package com.lordobesity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LordObesityMod.MODID);

    public static final RegistryObject<Item> LORD_OBESITY_SPAWN_EGG =
            ITEMS.register("lord_obesity_spawn_egg", () ->
                    new ForgeSpawnEggItem(ModEntities.LORD_OBESITY, 0xF4C230, 0xEEF0F6, new Item.Properties()));

    public static final RegistryObject<Item> LORD_OBESITY_MASK =
            ITEMS.register("lord_obesity_mask", () ->
                    new LordObesityMaskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LORD_OBESITY_HAT =
            ITEMS.register("lord_obesity_hat", () ->
                    new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LORD_OBESITY_JACKET =
            ITEMS.register("lord_obesity_jacket", () ->
                    new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LORD_OBESITY_CANE =
            ITEMS.register("lord_obesity_cane", () ->
                    new LordObesityCaneItem(new Item.Properties().durability(700).rarity(Rarity.EPIC)));
}
