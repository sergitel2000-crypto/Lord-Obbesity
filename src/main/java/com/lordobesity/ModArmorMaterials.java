package com.lordobesity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Material del traje de Lord Obesity (sombrero + chaqueta). */
public enum ModArmorMaterials implements ArmorMaterial {
    SUIT;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        int base = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        return base * 28;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 5;
            case BOOTS -> 2;
        };
    }

    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
    @Override public String getName() { return LordObesityMod.MODID + ":suit"; }
    @Override public float getToughness() { return 1.5f; }
    @Override public float getKnockbackResistance() { return 0.1f; }
}
