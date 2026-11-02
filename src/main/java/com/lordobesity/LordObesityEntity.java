package com.lordobesity;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Lord Obesity. Una sola entidad con cuatro "fases de vida":
 * MERCHANT (pide objetos), ANGRY (combate normal), SCARE (susto inofensivo), MEGA (devorador).
 */
public class LordObesityEntity extends Monster {
    // ---- estados de animacion / ataque ----
    public static final int IDLE = 0, SLAM = 1, CHARGE = 2, BURP = 3, JUMP = 4, BEAM = 5,
            SUMMON = 6, SUCK = 7, ROAR = 8, SINK = 9, EMERGE = 10, TALK = 11;
    // ---- fases ----
    public static final int MERCHANT = 0, ANGRY = 1, SCARE = 2, MEGA = 3;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(LordObesityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(LordObesityEntity.class, EntityDataSerializers.INT);

    public static class Request {
        public Item item;
        public int total;
        public int left;
    }

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.lordobesity.lord_obesity"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    /** tickCount en el que empezo el estado actual (lo usa el modelo para animar). */
    public int animStart;
    private int cooldown = 80;
    private int lastPhase = 1;
    private Vec3 dashDir = Vec3.ZERO;
    private final Set<Integer> chargeHit = new HashSet<>();
    private final List<Request> requests = new ArrayList<>();
    private long requestClock = -1L;
    private boolean leaving = false;
    @Nullable
    private UUID lastPlayer;
    private int scareTicks = 0;
    private boolean scareTriggered = false;
    private int scareHitTick = 0;

    public LordObesityEntity(EntityType<LordObesityEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // ================= Datos sincronizados =================
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, IDLE);
        this.entityData.define(STAGE, MERCHANT);
    }

    public int getState() { return this.entityData.get(STATE); }

    public void setState(int s) {
        this.entityData.set(STATE, s);
        this.animStart = this.tickCount;
    }

    public int getStage() { return this.entityData.get(STAGE); }

    public void setStage(int s) {
        this.entityData.set(STAGE, s);
        this.xpReward = (s == MEGA) ? 500 : 0;
        this.refreshDimensions();
        updateBar();
    }

    public boolean isCombat() {
        int s = getStage();
        return s == ANGRY || s == MEGA;
    }

    public boolean isBossFight() {
        return isCombat() && isAlive() && getState() != SINK;
    }

    public boolean isMega() { return getStage() == MEGA; }

    public float getModelScale() { return getStage() == MEGA ? 1.9f : 1.0f; }

    private float dmgMul() { return isMega() ? 1.8f : 1.0f; }

    private double rangeMul() { return isMega() ? 1.6 : 1.0; }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) this.animStart = this.tickCount;
        if (STAGE.equals(key)) this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getModelScale());
    }

    /** Profundidad (en bloques) a la que esta enterrado; sirve para emerger / hundirse. */
    public float getBuryDepth(float partialTick) {
        int st = getState();
        float t = (this.tickCount + partialTick) - this.animStart;
        float full = 4.4f * getModelScale();
        if (st == EMERGE) return (1.0f - smooth(t / 70f)) * full;
        if (st == SINK) return smooth(t / 70f) * full;
        return 0f;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    // ================= Fases / estadisticas =================
    private void setAttr(Attribute attr, double value) {
        AttributeInstance a = this.getAttribute(attr);
        if (a != null) a.setBaseValue(value);
    }

    public void applyStageStats(boolean heal) {
        double hp, spd, dmg, armor, kb;
        switch (getStage()) {
            case ANGRY -> { hp = 400; spd = 0.23; dmg = 12; armor = 8; kb = 0.9; }
            case MEGA -> { hp = 1000; spd = 0.27; dmg = 22; armor = 15; kb = 1.0; }
            case SCARE -> { hp = 20; spd = 0.23; dmg = 0; armor = 0; kb = 0; }
            default -> { hp = 100; spd = 0.23; dmg = 0; armor = 0; kb = 0.5; }
        }
        setAttr(Attributes.MAX_HEALTH, hp);
        setAttr(Attributes.MOVEMENT_SPEED, spd);
        setAttr(Attributes.ATTACK_DAMAGE, dmg);
        setAttr(Attributes.ARMOR, armor);
        setAttr(Attributes.KNOCKBACK_RESISTANCE, kb);
        if (heal) this.setHealth(this.getMaxHealth());
    }

    /** Prepara la entidad recien creada: fase + animacion de aparicion. */
    public void initStage(int stage, @Nullable UUID owner) {
        setStage(stage);
        applyStageStats(true);
        this.lastPlayer = owner;
        setState(EMERGE);
        cooldown = 60;
    }

    public void initScare() {
        setStage(SCARE);
        applyStageStats(true);
        this.setNoAi(true);
    }

    private void updateBar() {
        bossEvent.setVisible(isCombat());
        bossEvent.setColor(isMega() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW);
        bossEvent.setName(isMega()
                ? Component.literal("Lord Obesity, el Devorador")
                : Component.translatable("entity.lordobesity.lord_obesity"));
    }

    // ================= Objetivos / goals =================
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override public boolean canUse() { return isCombat() && getState() == IDLE && super.canUse(); }
            @Override public boolean canContinueToUse() { return isCombat() && getState() == IDLE && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override public boolean canUse() { return getState() == IDLE && getStage() != SCARE && super.canUse(); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return isCombat() && super.canUse(); }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, true) {
            @Override public boolean canUse() { return isCombat() && super.canUse(); }
        });
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
        }
        return hit;
    }

    // ================= Dano / muerte =================
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        int stage = getStage();
        int st = getState();
        if (stage == SCARE) return false;
        if (st == EMERGE || st == SINK || st == ROAR) return false;
        if (src.getEntity() instanceof ServerPlayer sp) lastPlayer = sp.getUUID();
        if (stage == MERCHANT) {
            if (src.getEntity() instanceof Player p && !level().isClientSide) becomeAngry(p);
            return false;
        }
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        // En su forma enfadada no muere: se hunde bajo tierra y vuelve mas fuerte.
        if (!level().isClientSide && getStage() == ANGRY && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.setHealth(1.0f);
            startSink(false);
            return;
        }
        if (!level().isClientSide && getStage() == MEGA) {
            say("Glub... mi... ultimo... bocado...");
        }
        super.die(src);
    }

    @Override
    public boolean causeFallDamage(float dist, float mul, DamageSource src) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        if (getStage() != MEGA) return;
        this.spawnAtLocation(new ItemStack(ModItems.LORD_OBESITY_MASK.get()));
        this.spawnAtLocation(new ItemStack(ModItems.LORD_OBESITY_CANE.get()));
        this.spawnAtLocation(new ItemStack(Items.COOKED_PORKCHOP, 16 + random.nextInt(17)));
        this.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 4 + random.nextInt(5)));
        this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1 + random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 6 + random.nextInt(7)));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_INGOT, 1 + random.nextInt(2)));
    }

    // ================= Tick =================
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getStage() == SCARE) tickScare();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        int st = getState();
        int t = this.tickCount - this.animStart;
        if (st == EMERGE || st == SINK || st == ROAR) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (st == EMERGE) {
                if (t % 4 == 0) groundParticles(6);
                if (t >= 70) { setState(IDLE); if (isMega()) cooldown = 40; }
            } else if (st == ROAR) {
                LivingEntity tg = getTarget();
                if (tg != null) this.getLookControl().setLookAt(tg, 30, 30);
                if (t == 20) {
                    particles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 3.0 * getModelScale(), getZ(), 12, 1.5, 0.5, 1.5, 0.0);
                }
                if (t >= 50) setState(IDLE);
            } else {
                if (t % 3 == 0) groundParticles(8);
                if (t >= 70) finishSink();
            }
            return;
        }

        switch (getStage()) {
            case MERCHANT -> tickMerchant();
            case ANGRY, MEGA -> tickCombat();
            default -> { }
        }
    }

    // ================= MERCADER =================
    private void tickMerchant() {
        int t = this.tickCount - this.animStart;
        if (getState() == TALK && t >= 40) setState(IDLE);
        if (leaving) return;
        if (requests.isEmpty()) generateRequests();

        Player near = level().getNearestPlayer(this, 48);
        if (requestClock >= 0 && near != null && currentClock() > requestClock) {
            becomeAngry(lastPlayer != null && level().getPlayerByUUID(lastPlayer) != null
                    ? level().getPlayerByUUID(lastPlayer) : near);
            return;
        }
        if (this.tickCount % 2400 == 0 && near != null && !requests.isEmpty()) {
            say("Hmm... ¿y mi comida? " + describeRequests());
        }
    }

    private long currentClock() {
        long clock = level().dimensionType().natural() ? level().getDayTime() : level().getGameTime();
        return clock / 24000L;
    }

    private void generateRequests() {
        List<Item> pool = new ArrayList<>(List.of(Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.GOLDEN_APPLE,
                Items.CAKE, Items.PUMPKIN_PIE, Items.COOKIE, Items.BREAD, Items.HONEY_BOTTLE,
                Items.COOKED_CHICKEN, Items.BAKED_POTATO));
        Collections.shuffle(pool, new Random(random.nextLong()));
        int n = 2 + random.nextInt(2);
        for (int i = 0; i < n; i++) {
            Item it = pool.get(i);
            int amount;
            if (it == Items.CAKE || it == Items.GOLDEN_APPLE) amount = 1 + random.nextInt(2);
            else if (it == Items.HONEY_BOTTLE) amount = 2 + random.nextInt(3);
            else if (it == Items.COOKIE) amount = 16 + random.nextInt(17);
            else if (it == Items.PUMPKIN_PIE) amount = 4 + random.nextInt(5);
            else amount = 8 + random.nextInt(9);
            Request r = new Request();
            r.item = it; r.total = amount; r.left = amount;
            requests.add(r);
        }
        requestClock = currentClock();
        setState(TALK);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HONK.get(), SoundSource.NEUTRAL, 1.5f, 1.0f);
        say("Hoho... ¡hola, humano! Tengo muuucha hambre. Tráeme esto antes de que amanezca, o lo vas a lamentar: "
                + describeRequests());
    }

    private String describeRequests() {
        StringBuilder sb = new StringBuilder();
        for (Request r : requests) {
            if (r.left <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.left).append("x ").append(r.item.getDescription().getString());
        }
        return sb.length() == 0 ? "nada" : sb.toString();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (getStage() != MERCHANT || getState() == EMERGE || getState() == SINK || leaving) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        lastPlayer = player.getUUID();
        ItemStack held = player.getItemInHand(hand);
        boolean gave = false;
        for (Request r : requests) {
            if (r.left > 0 && held.is(r.item)) {
                int take = Math.min(r.left, held.getCount());
                if (!player.getAbilities().instabuild) held.shrink(take);
                r.left -= take;
                gave = true;
                break;
            }
        }
        boolean done = !requests.isEmpty() && requests.stream().allMatch(r -> r.left <= 0);
        if (done) {
            say("¡Ñam ñam ñam! ¡Delicioso! Toma tu recompensa... y me largo. ¡Hohoho!");
            giveReward();
            leaving = true;
            setState(SINK);
        } else if (gave) {
            setState(TALK);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AMBIENT.get(), SoundSource.NEUTRAL, 1.5f, 1.2f);
            say("Mmm, gracias. Aún quiero: " + describeRequests());
        } else {
            setState(TALK);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AMBIENT.get(), SoundSource.NEUTRAL, 1.5f, 0.9f);
            say("Quiero: " + describeRequests() + ". Antes de que amanezca...");
        }
        return InteractionResult.SUCCESS;
    }

    private void giveReward() {
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 8 + random.nextInt(5)));
        this.spawnAtLocation(new ItemStack(Items.EMERALD, 24 + random.nextInt(17)));
        this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_INGOT, 1));
        if (level() instanceof ServerLevel sl) {
            ExperienceOrb.award(sl, this.position(), 120);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 40, 1.5, 1.5, 1.5, 0.1);
        }
    }

    public void becomeAngry(@Nullable Player cause) {
        if (getStage() != MERCHANT || level().isClientSide) return;
        setStage(ANGRY);
        applyStageStats(true);
        setState(ROAR);
        cooldown = 60;
        if (cause != null) {
            lastPlayer = cause.getUUID();
            setTarget(cause);
        }
        snd(ModSounds.ROAR, 2.5f, 1.0f);
        say("¡¡NO ME HAS DADO LO QUE QUERÍA!! ¡AHORA VERÁS!");
    }

    // ================= Hundirse y regresar =================
    private void startSink(boolean leave) {
        this.leaving = leave;
        setState(SINK);
        snd(ModSounds.DEATH, 2.0f, 1.0f);
        say("Grrr... esto no ha terminado... Volveré más hambriento y MÁS GRANDE...");
    }

    private void finishSink() {
        if (level() instanceof ServerLevel sl) {
            if (!leaving) {
                UUID target = lastPlayer;
                if (target == null) {
                    Player n = level().getNearestPlayer(this, 64);
                    if (n != null) target = n.getUUID();
                }
                if (target != null) {
                    long now = sl.getGameTime();
                    long wait = (5L + random.nextInt(11)) * 1200L;   // 5 a 15 minutos
                    HauntData.get(sl.getServer()).add(target, now + wait, now + 600L + random.nextInt(900));
                }
            }
        }
        this.discard();
    }

    // ================= SUSTO =================
    private void tickScare() {
        scareTicks++;
        Player p = level().getNearestPlayer(this, 80);
        if (p != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            this.setYBodyRot(this.getYRot());
            this.setYHeadRot(this.getYRot());
            if (!scareTriggered) {
                Vec3 look = p.getViewVector(1.0f);
                Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
                if (look.dot(to) > 0.97 && p.hasLineOfSight(this)) {
                    scareTriggered = true;
                    scareHitTick = scareTicks;
                    level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SCARE.get(), SoundSource.HOSTILE, 1.6f, 1.0f);
                    particles(ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 30, 1.0, 1.5, 1.0, 0.02);
                }
            }
        }
        boolean timeUp = scareTriggered ? (scareTicks - scareHitTick >= 35) : scareTicks >= 900;
        if (timeUp) {
            particles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 40, 1.0, 1.5, 1.0, 0.05);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HONK.get(), SoundSource.HOSTILE, 1.5f, 1.3f);
            this.discard();
        }
    }

    // ================= COMBATE =================
    private int phase() {
        float f = this.getHealth() / this.getMaxHealth();
        if (isMega()) return f < 0.33f ? 3 : (f < 0.66f ? 2 : 1);
        return f < 0.5f ? 2 : 1;
    }

    private void tickCombat() {
        LivingEntity target = this.getTarget();
        int s = getState();
        int ph = phase();
        if (s == IDLE && ph > lastPhase) {
            lastPhase = ph;
            setState(ROAR);
            snd(ModSounds.ROAR, 2.5f, 0.9f);
            say(ph == 3 ? "¡¡YA ESTOY HARTO!!" : "¡Ahora sí que me enfadé!");
            return;
        }
        if (s == IDLE) {
            if (target != null && --cooldown <= 0) startAttack(target, ph);
        } else {
            runAttack(s, target, ph);
        }
    }

    private void startAttack(LivingEntity target, int ph) {
        double d = this.distanceTo(target);
        float r = this.random.nextFloat();
        int pick;
        if (!isMega()) {
            if (d < 9) pick = r < 0.55f ? SLAM : BURP;
            else if (d < 26) pick = CHARGE;
            else { cooldown = 20; return; }
        } else {
            if (d < 10) pick = r < 0.30f ? SLAM : r < 0.55f ? BURP : r < 0.80f ? SUCK : JUMP;
            else if (d < 26) pick = r < 0.25f ? CHARGE : r < 0.50f ? JUMP : r < 0.75f ? BEAM : SUMMON;
            else pick = r < 0.6f ? JUMP : SUMMON;
            if (pick == SUMMON && level().getEntitiesOfClass(Zoglin.class, getBoundingBox().inflate(40)).size() >= 6) pick = BEAM;
        }
        this.getNavigation().stop();
        this.chargeHit.clear();
        setState(pick);
    }

    private void endAttack(int ph) {
        setState(IDLE);
        if (isMega()) cooldown = ph == 3 ? 25 : (ph == 2 ? 45 : 65);
        else cooldown = ph == 2 ? 50 : 90;
    }

    private void lookAtTarget(@Nullable LivingEntity target, float speed) {
        if (target != null) this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), speed, speed);
    }

    private void runAttack(int s, @Nullable LivingEntity target, int ph) {
        int t = this.tickCount - this.animStart;
        this.getNavigation().stop();
        double rm = rangeMul();
        float dm = dmgMul();

        switch (s) {
            case SLAM -> {
                lookAtTarget(target, 30);
                if (t == 1) snd(ModSounds.ROAR, 2.0f, 1.0f);
                if (t == 27) shockwave((ph >= 2 ? 12.0 : 8.0) * rm, (ph >= 2 ? 12f : 8f) * dm, 1.2, true);
                if (t >= 40) endAttack(ph);
            }
            case BURP -> {
                lookAtTarget(target, 30);
                if (t == 30) burp(rm, dm);
                if (t >= 50) endAttack(ph);
            }
            case CHARGE -> {
                if (t < 20) {
                    lookAtTarget(target, 60);
                    if (t == 1) snd(ModSounds.WHISTLE, 2.0f, 0.8f);
                    if (t % 3 == 0) groundParticles(3);
                } else if (t < 60) {
                    if (t == 20) {
                        Vec3 dir = target != null ? target.position().subtract(this.position()) : this.getLookAngle();
                        dashDir = new Vec3(dir.x, 0, dir.z).normalize();
                    }
                    float yaw = (float) (Mth.atan2(dashDir.z, dashDir.x) * (180.0 / Math.PI)) - 90.0f;
                    this.setYRot(yaw);
                    this.yBodyRot = yaw;
                    this.yHeadRot = yaw;
                    double speed = isMega() ? 1.15 : (ph >= 2 ? 1.0 : 0.85);
                    this.setDeltaMovement(dashDir.x * speed, this.getDeltaMovement().y, dashDir.z * speed);
                    chargeHit(dm);
                    if (this.horizontalCollision && t > 26) {
                        snd(ModSounds.SLAM, 1.5f, 0.8f);
                        setState(IDLE);
                        cooldown = 110; // se queda aturdido un rato
                        return;
                    }
                } else {
                    endAttack(ph);
                }
            }
            case JUMP -> {
                if (t < 15) {
                    lookAtTarget(target, 60);
                } else if (t == 15) {
                    snd(ModSounds.BOING, 2.5f, 0.8f);
                    Vec3 to = target != null ? target.position().subtract(this.position()) : this.getLookAngle().scale(10);
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    double dist = flat.length();
                    Vec3 dir = dist < 0.001 ? Vec3.ZERO : flat.normalize();
                    double sp = Math.min(dist * 0.095, 1.7);
                    this.setDeltaMovement(dir.x * sp, 1.25, dir.z * sp);
                    this.hasImpulse = true;
                } else if (t > 20 && this.onGround()) {
                    shockwave(9.0 * rm, 9f * dm, 1.6, true);
                    endAttack(ph);
                } else if (t >= 100) {
                    endAttack(ph);
                }
            }
            case BEAM -> {
                if (t < 20) {
                    lookAtTarget(target, 40);
                    if (t == 1) snd(ModSounds.BURP, 2.0f, 0.7f);
                } else if (t < 62) {
                    lookAtTarget(target, 5);
                    beamTick(t, rm, dm);
                } else if (t >= 72) {
                    endAttack(ph);
                }
            }
            case SUMMON -> {
                lookAtTarget(target, 30);
                if (t == 1) snd(ModSounds.HONK, 2.5f, 0.6f);
                if (t == 25) summonMinions(target);
                if (t >= 60) endAttack(ph);
            }
            case SUCK -> {
                lookAtTarget(target, 30);
                if (t == 1) snd(ModSounds.WHISTLE, 2.5f, 0.5f);
                if (t >= 15 && t < 55) suckTick(t, rm);
                if (t == 58) {
                    snd(ModSounds.BURP, 2.5f, 0.6f);
                    shockwave(9.0 * rm, 9f * dm, 2.0, false);
                }
                if (t >= 70) endAttack(ph);
            }
            default -> { }
        }
    }

    private boolean validVictim(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    private static Vec3 flatDir(Vec3 from, Vec3 to) {
        Vec3 d = new Vec3(to.x - from.x, 0, to.z - from.z);
        return d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
    }

    private void chargeHit(float dm) {
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.3), this::validVictim)) {
            if (chargeHit.add(p.getId())) {
                p.hurt(damageSources().mobAttack(this), 14.0f * dm);
                p.push(dashDir.x * 1.6, 0.6, dashDir.z * 1.6);
                p.hurtMarked = true;
            }
        }
    }

    /** Onda de choque alrededor del jefe (aplastamiento, aterrizaje, estallido). */
    private void shockwave(double r, float dmg, double kb, boolean slow) {
        snd(ModSounds.SLAM, 2.0f, 1.0f);
        AABB box = getBoundingBox().inflate(r, 2.0, r);
        for (Player p : level().getEntitiesOfClass(Player.class, box, this::validVictim)) {
            p.hurt(damageSources().mobAttack(this), dmg);
            Vec3 dir = flatDir(position(), p.position());
            p.push(dir.x * kb, 0.8, dir.z * kb);
            p.hurtMarked = true;
            if (slow) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        particles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 80, r * 0.4, 0.2, r * 0.4, 0.05);
        particles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
    }

    /** Eructo en cono: hambre + nauseas + empuje. */
    private void burp(double rm, float dm) {
        Vec3 look = this.getViewVector(1.0f);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        snd(ModSounds.BURP, 2.5f, 1.0f);
        AABB box = getBoundingBox().inflate(11.0 * rm, 3.0, 11.0 * rm);
        for (Player p : level().getEntitiesOfClass(Player.class, box, this::validVictim)) {
            Vec3 to = p.position().subtract(position());
            Vec3 toFlat = new Vec3(to.x, 0, to.z);
            if (toFlat.length() < 0.001) continue;
            if (flat.dot(toFlat.normalize()) > 0.5) {
                p.hurt(damageSources().mobAttack(this), 6.0f * dm);
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1));
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                p.push(flat.x * 1.3, 0.3, flat.z * 1.3);
                p.hurtMarked = true;
            }
        }
        double h = 2.6 * getModelScale();
        particles(ParticleTypes.SNEEZE, getX() + flat.x * 2.5 * getModelScale(), getY() + h,
                getZ() + flat.z * 2.5 * getModelScale(), 70, 1.4, 0.5, 1.4, 0.12);
    }

    /** Rayo continuo de eructo verde que barre al objetivo. */
    private void beamTick(int t, double rm, float dm) {
        Vec3 look = this.getViewVector(1.0f);
        Vec3 origin = this.getEyePosition().add(look.scale(1.5 * getModelScale()));
        double len = 18.0 * rm;
        for (double d = 0; d < len; d += 1.3) {
            particles(ParticleTypes.SNEEZE, origin.x + look.x * d, origin.y + look.y * d, origin.z + look.z * d,
                    2, 0.2, 0.2, 0.2, 0.01);
        }
        if (t % 12 == 0) snd(ModSounds.BURP, 1.5f, 1.4f);
        if (t % 5 == 0) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(len), this::validVictim)) {
                Vec3 rel = p.getBoundingBox().getCenter().subtract(origin);
                double along = rel.dot(look);
                if (along < 0 || along > len) continue;
                Vec3 perp = rel.subtract(look.scale(along));
                if (perp.length() < 1.9) {
                    p.hurt(damageSources().mobAttack(this), 1.5f * dm);
                    p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
            }
        }
    }

    /** Inhala: arrastra a los jugadores hacia su boca. */
    private void suckTick(int t, double rm) {
        double r = 26.0 * rm;
        Vec3 mouth = this.position().add(0, 2.3 * getModelScale(), 0);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), this::validVictim)) {
            Vec3 dir = mouth.subtract(p.position().add(0, p.getBbHeight() * 0.5, 0));
            if (dir.length() < 1.5) continue;
            p.push(dir.normalize().x * 0.11, dir.normalize().y * 0.05, dir.normalize().z * 0.11);
            p.hurtMarked = true;
            if (t % 10 == 0 && p.distanceTo(this) < 5.0f * getModelScale()) {
                p.hurt(damageSources().mobAttack(this), 2.0f);
            }
        }
        if (t % 2 == 0) {
            particles(ParticleTypes.PORTAL, mouth.x, mouth.y, mouth.z, 20, 6.0, 3.0, 6.0, 1.0);
        }
    }

    private void summonMinions(@Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (sl.getEntitiesOfClass(Zoglin.class, getBoundingBox().inflate(40)).size() >= 6) return;
        for (int i = 0; i < 3; i++) {
            Zoglin z = EntityType.ZOGLIN.create(sl);
            if (z == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            double dist = 3.0 * getModelScale() + 1.5;
            z.moveTo(getX() + Math.cos(a) * dist, getY(), getZ() + Math.sin(a) * dist, random.nextFloat() * 360f, 0f);
            if (target != null) z.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            sl.addFreshEntity(z);
            particles(ParticleTypes.POOF, z.getX(), z.getY() + 0.5, z.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
        }
    }

    // ================= Utilidades =================
    private void particles(ParticleOptions p, double x, double y, double z, int n, double dx, double dy, double dz, double sp) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(p, x, y, z, n, dx, dy, dz, sp);
    }

    private void groundParticles(int n) {
        var state = level().getBlockState(blockPosition().below());
        ParticleOptions opt = state.isAir() ? ParticleTypes.POOF : new BlockParticleOption(ParticleTypes.BLOCK, state);
        double s = getModelScale();
        particles(opt, getX(), getY() + 0.1, getZ(), n * 4, 1.2 * s, 0.1, 1.2 * s, 0.1);
    }

    private void snd(RegistryObject<SoundEvent> sound, float vol, float pitch) {
        float v = vol * (isMega() ? 1.4f : 1.0f);
        float p = pitch * (isMega() ? 0.75f : 1.0f);
        level().playSound(null, getX(), getY(), getZ(), sound.get(), SoundSource.HOSTILE, v, p);
    }

    /** Dialogo: lo ven los jugadores cercanos. */
    private void say(String text) {
        if (!(level() instanceof ServerLevel sl)) return;
        MutableComponent msg = Component.literal("[Lord Obesity] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer sp : sl.players()) {
            if (sp.distanceToSqr(this) < 45.0 * 45.0) sp.sendSystemMessage(msg);
        }
    }

    // ================= Barra de jefe =================
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
        updateBar();
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

    // ================= Guardado =================
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", getStage());
        tag.putLong("RequestClock", requestClock);
        tag.putBoolean("Leaving", leaving);
        if (lastPlayer != null) tag.putUUID("LastPlayer", lastPlayer);
        ListTag list = new ListTag();
        for (Request r : requests) {
            CompoundTag c = new CompoundTag();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(r.item);
            c.putString("id", id == null ? "minecraft:bread" : id.toString());
            c.putInt("total", r.total);
            c.putInt("left", r.left);
            list.add(c);
        }
        tag.put("Requests", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stage")) {
            setStage(tag.getInt("Stage"));
            applyStageStats(!tag.contains("Health"));
        }
        requestClock = tag.contains("RequestClock") ? tag.getLong("RequestClock") : -1L;
        leaving = tag.getBoolean("Leaving");
        if (tag.hasUUID("LastPlayer")) lastPlayer = tag.getUUID("LastPlayer");
        requests.clear();
        ListTag list = tag.getList("Requests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(c.getString("id")));
            if (it == null) continue;
            Request r = new Request();
            r.item = it; r.total = c.getInt("total"); r.left = c.getInt("left");
            requests.add(r);
        }
        if (this.hasCustomName()) bossEvent.setName(this.getDisplayName());
        updateBar();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ================= Sonidos propios =================
    @Override protected SoundEvent getAmbientSound() { return getStage() == SCARE ? null : ModSounds.AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.DEATH.get(); }
    @Override public int getAmbientSoundInterval() { return 260; }
    @Override public float getVoicePitch() { return isMega() ? 0.7f : 1.0f; }
}
