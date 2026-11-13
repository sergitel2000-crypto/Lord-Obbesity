package com.lordobesity;

import net.minecraft.world.item.ArmorItem;
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
                    new LordObesityArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LORD_OBESITY_JACKET =
            ITEMS.register("lord_obesity_jacket", () ->
                    new LordObesityArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LORD_OBESITY_CANE =
            ITEMS.register("lord_obesity_cane", () ->
                    new LordObesityCaneItem(new Item.Properties().durability(700).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> LORE_BOOK_1 =
            ITEMS.register("lore_book_1", () -> new LoreBookItem(1, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LORE_BOOK_2 =
            ITEMS.register("lore_book_2", () -> new LoreBookItem(2, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LORE_BOOK_3 =
            ITEMS.register("lore_book_3", () -> new LoreBookItem(3, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LORE_BOOK_COMPLETE =
            ITEMS.register("lore_book_complete", () -> new LoreBookItem(0, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
}
