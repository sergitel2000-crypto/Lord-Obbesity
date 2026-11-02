package com.lordobesity;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Máscara de Lord Obesity: se pone en la cabeza y te transforma en él. */
public class LordObesityMaskItem extends Item implements Equipable {
    private static final UUID HP_ID = UUID.fromString("5b1d2c10-7a4e-4c3f-9d11-0a1b2c3d4e01");
    private static final UUID ARMOR_ID = UUID.fromString("5b1d2c10-7a4e-4c3f-9d11-0a1b2c3d4e02");
    private static final UUID DMG_ID = UUID.fromString("5b1d2c10-7a4e-4c3f-9d11-0a1b2c3d4e03");
    private static final UUID KB_ID = UUID.fromString("5b1d2c10-7a4e-4c3f-9d11-0a1b2c3d4e04");
    private static final UUID SPD_ID = UUID.fromString("5b1d2c10-7a4e-4c3f-9d11-0a1b2c3d4e05");

    public LordObesityMaskItem(Properties props) {
        super(props);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD) return super.getDefaultAttributeModifiers(slot);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.MAX_HEALTH, new AttributeModifier(HP_ID, "Lord Obesity health", 20.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ARMOR, new AttributeModifier(ARMOR_ID, "Lord Obesity armor", 4.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DMG_ID, "Lord Obesity strength", 4.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KB_ID, "Lord Obesity weight", 0.5, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPD_ID, "Lord Obesity slowness", -0.15, AttributeModifier.Operation.MULTIPLY_BASE));
        return b.build();
    }
}
