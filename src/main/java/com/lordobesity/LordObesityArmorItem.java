package com.lordobesity;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.UUID;
import java.util.function.Consumer;

/** Sombrero (casco) y chaqueta (peto) de Lord Obesity: se equipan, dan armadura y vida extra. */
public class LordObesityArmorItem extends ArmorItem {
    private static final UUID HAT_HP = UUID.fromString("7c2e1a10-3b4d-4e5f-8a91-1b2c3d4e5f01");
    private static final UUID JACKET_HP = UUID.fromString("7c2e1a10-3b4d-4e5f-8a91-1b2c3d4e5f02");
    private static final UUID JACKET_LUCK = UUID.fromString("7c2e1a10-3b4d-4e5f-8a91-1b2c3d4e5f03");

    public LordObesityArmorItem(ArmorItem.Type type, Properties props) {
        super(ModArmorMaterials.SUIT, type, props);
    }

    private boolean isHat() {
        return this.getType() == ArmorItem.Type.HELMET;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return LordObesityMod.MODID + ":textures/models/armor/" + (isHat() ? "lord_hat" : "lord_jacket") + ".png";
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != this.getEquipmentSlot()) return base;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(base);
        if (isHat()) {
            b.put(Attributes.MAX_HEALTH, new AttributeModifier(HAT_HP, "Lord Obesity hat", 2.0, AttributeModifier.Operation.ADDITION));
        } else {
            b.put(Attributes.MAX_HEALTH, new AttributeModifier(JACKET_HP, "Lord Obesity jacket", 4.0, AttributeModifier.Operation.ADDITION));
            b.put(Attributes.LUCK, new AttributeModifier(JACKET_LUCK, "Lord Obesity jacket luck", 1.0, AttributeModifier.Operation.ADDITION));
        }
        return b.build();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot == EquipmentSlot.HEAD) return ClientArmor.hatModel();
                return original;   // la chaqueta usa el modelo de peto normal con nuestra textura
            }
        });
    }
}
