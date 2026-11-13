package com.lordobesity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/** Reparte sombrero, chaqueta y baston por los cofres de estructuras. */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class LootEvents {
    private static final Set<ResourceLocation> TABLES = Set.of(
            new ResourceLocation("minecraft", "chests/simple_dungeon"),
            new ResourceLocation("minecraft", "chests/abandoned_mineshaft"),
            new ResourceLocation("minecraft", "chests/desert_pyramid"),
            new ResourceLocation("minecraft", "chests/jungle_temple"),
            new ResourceLocation("minecraft", "chests/stronghold_corridor"),
            new ResourceLocation("minecraft", "chests/stronghold_crossing"),
            new ResourceLocation("minecraft", "chests/woodland_mansion"),
            new ResourceLocation("minecraft", "chests/buried_treasure"),
            new ResourceLocation("minecraft", "chests/shipwreck_treasure"),
            new ResourceLocation("minecraft", "chests/pillager_outpost"),
            new ResourceLocation("minecraft", "chests/ancient_city"),
            new ResourceLocation("minecraft", "chests/end_city_treasure"),
            new ResourceLocation("minecraft", "chests/bastion_treasure"),
            new ResourceLocation("minecraft", "chests/ruined_portal"));

    private static LootPool pool(Item item, float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(item))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .build();
    }

    @SubscribeEvent
    public static void onLoot(LootTableLoadEvent e) {
        if (!TABLES.contains(e.getName())) return;
        e.getTable().addPool(pool(ModItems.LORD_OBESITY_HAT.get(), 0.15f));
        e.getTable().addPool(pool(ModItems.LORD_OBESITY_JACKET.get(), 0.15f));
        e.getTable().addPool(pool(ModItems.LORD_OBESITY_CANE.get(), 0.15f));
    }
}
