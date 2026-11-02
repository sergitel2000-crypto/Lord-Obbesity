package com.lordobesity;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Baston de Lord Obesity.
 *  - Golpe: mucho dano.
 *  - Clic derecho: eructo letal en cono.
 *  - Agachado + clic derecho: gancho para desplazarte.
 */
public class LordObesityCaneItem extends Item {
    public LordObesityCaneItem(Properties props) {
        super(props);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 8.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (player.isShiftKeyDown()) {
            hook(level, player);
            player.getCooldowns().addCooldown(this, 20);
        } else {
            burp(level, player);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Eructo letal: dano + veneno + empuje a todo lo que este delante. */
    private void burp(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 14.0;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BURP.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand))) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range || d < 0.1) continue;
            if (to.normalize().dot(look) < 0.8) continue;
            t.hurt(level.damageSources().playerAttack(player), 14.0f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            t.push(look.x * 1.5, 0.4, look.z * 1.5);
            t.hurtMarked = true;
        }
        if (level instanceof ServerLevel sl) {
            for (double d = 1.5; d < range; d += 1.2) {
                sl.sendParticles(ParticleTypes.SNEEZE, eye.x + look.x * d, eye.y + look.y * d - 0.2, eye.z + look.z * d,
                        3, 0.25, 0.25, 0.25, 0.01);
            }
        }
    }

    /** Gancho: se agarra al bloque al que apuntas y te arrastra hasta el. */
    private void hook(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(40.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.literal("No hay nada a lo que agarrarse"), true);
            return;
        }
        Vec3 loc = hit.getLocation();
        CompoundTag d = player.getPersistentData();
        d.putDouble("lo_hx", loc.x);
        d.putDouble("lo_hy", loc.y);
        d.putDouble("lo_hz", loc.z);
        d.putInt("lo_ht", 40);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.3f);
    }
}
