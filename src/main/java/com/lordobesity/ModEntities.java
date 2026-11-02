package com.lordobesity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LordObesityMod.MODID);

    public static final RegistryObject<EntityType<LordObesityEntity>> LORD_OBESITY =
            ENTITIES.register("lord_obesity", () ->
                    EntityType.Builder.of(LordObesityEntity::new, MobCategory.MONSTER)
                            .sized(2.2f, 3.5f)
                            .clientTrackingRange(10)
                            .build("lord_obesity"));
}
