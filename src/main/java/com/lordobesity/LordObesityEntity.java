package com.lordobesity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class LordObesityEntity extends Monster {
    public static final int IDLE = 0, SLAM = 1, CHARGE = 2, BURP = 3;
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(LordObesityEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.lordobesity.lord_obesity"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    /** tickCount en el que empezó el estado actual (lo usa el modelo para animar). */
    public int animStart;
    private int cooldown = 80;
    private Vec3 dashDir = Vec3.ZERO;
    private final Set<Integer> chargeHit = new HashSet<>();

    public LordObesityEntity(EntityType<LordObesityEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, IDLE);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.animStart = this.tickCount;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) this.animStart = this.tickCount;
    }

    private boolean isFree() {
        return getState() == IDLE;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override public boolean canUse() { return isFree() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isFree() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return isFree() && super.canUse(); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
        }
        return hit;
    }

    // ================= IA de ataques =================
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        boolean phase2 = this.getHealth() < this.getMaxHealth() * 0.5f;
        LivingEntity target = this.getTarget();
        int s = getState();

        if (s == IDLE) {
            if (target != null && --cooldown <= 0) startAttack(target);
        } else {
            runAttack(s, target, phase2);
        }
    }

    private void startAttack(LivingEntity target) {
        double d = this.distanceTo(target);
        int pick;
        if (d < 9) pick = this.random.nextFloat() < 0.55f ? SLAM : BURP;
        else if (d < 26) pick = CHARGE;
        else { cooldown = 20; return; }
        this.getNavigation().stop();
        this.chargeHit.clear();
        setState(pick);
        this.playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.7f);
    }

    private void endAttack(boolean phase2) {
        setState(IDLE);
        cooldown = phase2 ? 50 : 90;
    }

    private void runAttack(int s, LivingEntity target, boolean phase2) {
        int t = this.tickCount - this.animStart;
        this.getNavigation().stop();

        switch (s) {
            case SLAM -> {
                if (target != null) this.getLookControl().setLookAt(target, 30, 30);
                if (t == 27) slam(phase2);
                if (t >= 40) endAttack(phase2);
            }
            case BURP -> {
                if (target != null) this.getLookControl().setLookAt(target, 30, 30);
                if (t == 30) burp();
                if (t >= 50) endAttack(phase2);
            }
            case CHARGE -> {
                if (t < 20) {
                    if (target != null) this.getLookControl().setLookAt(target, 60, 60);
                    if (this.level() instanceof ServerLevel sl && t % 3 == 0) {
                        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 3, 0.6, 0.05, 0.6, 0.01);
                    }
                } else if (t < 55) {
                    if (t == 20) {
                        Vec3 dir = target != null
                                ? target.position().subtract(this.position())
                                : this.getLookAngle();
                        dashDir = new Vec3(dir.x, 0, dir.z).normalize();
                    }
                    float yaw = (float) (Mth.atan2(dashDir.z, dashDir.x) * (180.0 / Math.PI)) - 90.0f;
                    this.setYRot(yaw);
                    this.yBodyRot = yaw;
                    this.yHeadRot = yaw;
                    double speed = phase2 ? 1.0 : 0.85;
                    this.setDeltaMovement(dashDir.x * speed, this.getDeltaMovement().y, dashDir.z * speed);
                    chargeHit();
                    if (this.horizontalCollision && t > 26) {
                        this.playSound(SoundEvents.GENERIC_EXPLODE, 1.2f, 0.7f);
                        setState(IDLE);
                        cooldown = 110; // se queda aturdido un rato
                        return;
                    }
                } else {
                    endAttack(phase2);
                }
            }
            default -> { }
        }
    }

    private boolean validVictim(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    private void chargeHit() {
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.3), this::validVictim)) {
            if (chargeHit.add(p.getId())) {
                p.hurt(damageSources().mobAttack(this), 14.0f);
                p.push(dashDir.x * 1.6, 0.6, dashDir.z * 1.6);
                p.hurtMarked = true;
            }
        }
    }

    /** Aplastamiento en área. */
    private void slam(boolean phase2) {
        double r = phase2 ? 12.0 : 8.0;
        float dmg = phase2 ? 12.0f : 8.0f;
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.5f);
        AABB box = getBoundingBox().inflate(r, 2.0, r);
        for (Player p : level().getEntitiesOfClass(Player.class, box, this::validVictim)) {
            p.hurt(damageSources().mobAttack(this), dmg);
            Vec3 dir = p.position().subtract(position()).normalize();
            p.push(dir.x * 1.2, 0.8, dir.z * 1.2);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 80, r * 0.4, 0.2, r * 0.4, 0.05);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
        }
    }

    /** Eructo en cono: hambre + náuseas + empuje. */
    private void burp() {
        Vec3 look = this.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        this.playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.4f);
        AABB box = getBoundingBox().inflate(11.0, 3.0, 11.0);
        for (Player p : level().getEntitiesOfClass(Player.class, box, this::validVictim)) {
            Vec3 to = p.position().subtract(position());
            Vec3 toFlat = new Vec3(to.x, 0, to.z);
            if (toFlat.length() < 0.001) continue;
            if (flat.dot(toFlat.normalize()) > 0.5) {
                p.hurt(damageSources().mobAttack(this), 6.0f);
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1));
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                p.push(flat.x * 1.3, 0.3, flat.z * 1.3);
                p.hurtMarked = true;
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNEEZE, getX() + flat.x * 2.5, getY() + 2.6, getZ() + flat.z * 2.5,
                    70, 1.4, 0.5, 1.4, 0.12);
        }
    }

    // ================= Barra de jefe =================
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.hasCustomName()) bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
}
