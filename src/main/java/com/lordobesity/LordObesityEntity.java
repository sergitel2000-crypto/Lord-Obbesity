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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
            SUMMON = 6, SUCK = 7, ROAR = 8, SINK = 9, EMERGE = 10, TALK = 11, SCREAM = 12, DANCE = 13;
    public static final int EMERGE_TICKS = 90;
    public static final int DANCE_TICKS = 90;
    // ---- fases ----
    public static final int MERCHANT = 0, ANGRY = 1, SCARE = 2, MEGA = 3, MINI = 4, CHASER = 5;

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
    private boolean fulfilled = false;   // ya le has dado lo de hoy
    private int streak = 0;              // dias seguidos que le has dado de comer
    private String lastItemId = "";      // lo que pidio ayer (hoy pide otra cosa)
    @Nullable
    private UUID lastPlayer;
    private int scareKind = 0;          // 0 = lejos, 1 = a tu espalda, 4 = acechador (se mueve cuando no lo miras)
    private int stareTicks = 0;
    private boolean screamSent = false;
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

    /** Fases que luchan cuerpo a cuerpo (incluye a los mini). */
    public boolean isFighter() {
        int s = getStage();
        return s == ANGRY || s == MEGA || s == MINI || s == CHASER;
    }

    public float getModelScale() {
        int s = getStage();
        return s == MEGA ? 1.9f : (s == MINI ? 0.4f : (s == CHASER ? 0.85f : 1.0f));
    }

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
        if (st == EMERGE) return (1.0f - smooth(t / (float) EMERGE_TICKS)) * full;
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
            case MINI -> { hp = 24; spd = 0.36; dmg = 5; armor = 0; kb = 0; }
            case CHASER -> { hp = 100; spd = 0.28; dmg = 0; armor = 0; kb = 1; }
            default -> { hp = 100; spd = 0.23; dmg = 0; armor = 0; kb = 0.5; }
        }
        setAttr(Attributes.MAX_HEALTH, hp);
        setAttr(Attributes.MOVEMENT_SPEED, spd);
        setAttr(Attributes.ATTACK_DAMAGE, dmg);
        setAttr(Attributes.ARMOR, armor);
        setAttr(Attributes.KNOCKBACK_RESISTANCE, kb);
        if (getStage() == CHASER) setAttr(Attributes.FOLLOW_RANGE, 120.0);
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

    public void initScare(int kind) {
        setStage(SCARE);
        applyStageStats(true);
        this.scareKind = kind;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    /** El perseguidor de la pesadilla: no se le puede herir; si te alcanza, se acaba el sueno. */
    public void initChaser(@Nullable LivingEntity target) {
        setStage(CHASER);
        applyStageStats(true);
        if (target != null) setTarget(target);
        particles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0, getZ(), 40, 0.6, 1.0, 0.6, 0.1);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.ROAR.get(), SoundSource.HOSTILE, 4.0f, 0.7f);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.RUMBLE.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
    }

    /** Mini Lord Obesity invocado por el Devorador. */
    public void initMini(@Nullable LivingEntity target) {
        setStage(MINI);
        applyStageStats(true);
        if (target != null) setTarget(target);
        particles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.4, getZ(), 24, 0.35, 0.4, 0.35, 0.06);
        particles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 14, 0.3, 0.3, 0.3, 0.03);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.CHITTER.get(), SoundSource.HOSTILE, 0.7f, 0.9f + random.nextFloat() * 0.3f);
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
            @Override public boolean canUse() { return isFighter() && getState() == IDLE && super.canUse(); }
            @Override public boolean canContinueToUse() { return isFighter() && getState() == IDLE && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override public boolean canUse() { return getState() == IDLE && getStage() != SCARE && super.canUse(); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return isFighter() && super.canUse(); }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, true) {
            @Override public boolean canUse() { return isFighter() && super.canUse(); }
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
        if (stage == SCARE || stage == CHASER) return false;
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
            for (LordObesityEntity m : level().getEntitiesOfClass(LordObesityEntity.class, getBoundingBox().inflate(90), x -> x.getStage() == MINI)) {
                m.particles(ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                m.discard();
            }
            double sc = getModelScale();
            particles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2.0 * sc, getZ(), 3, 1.5, 1.0, 1.5, 0);
            particles(ParticleTypes.SOUL, getX(), getY() + 2.0 * sc, getZ(), 80, 1.5, 2.0, 1.5, 0.08);
            ring(ParticleTypes.CLOUD, 48, getY() + 0.3, 0.9);
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
    }

    // ================= Tick =================
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (getStage() == SCARE) tickScare();
        } else if ((getStage() == SCARE || getStage() == CHASER) && getState() == SCREAM && !screamSent) {
            // cliente: lanza el screamer al jugador que tiene al lado
            Boolean ok = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ScreamerClient.trigger(getX(), getY(), getZ()));
            if (ok != null && ok) screamSent = true;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        int st = getState();
        int t = this.tickCount - this.animStart;
        if (st == EMERGE || st == SINK || st == ROAR || st == DANCE) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (st == EMERGE) {
                if (t == 1) {
                    snd(ModSounds.RUMBLE, 3.0f, 1.0f);
                    snd(ModSounds.EVIL_LAUGH, 2.5f, isMega() ? 0.8f : 1.0f);
                }
                if (t % 4 == 0) groundParticles(6);
                if (t % 3 == 0) {
                    double sc = getModelScale();
                    for (int j = 0; j < 3; j++) {
                        double a = t * 0.3 + j * Math.PI * 2 / 3;
                        particles(ParticleTypes.SOUL, getX() + Math.cos(a) * 1.6 * sc, getY() + (t % 24) * 0.12 * sc, getZ() + Math.sin(a) * 1.6 * sc, 1, 0, 0.03, 0, 0.0);
                    }
                    particles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.1, getZ(), 3, 1.0 * sc, 0.05, 1.0 * sc, 0.02);
                }
                if (t == EMERGE_TICKS - 10) {
                    ring(ParticleTypes.CLOUD, 40, getY() + 0.2, 0.9);
                    snd(ModSounds.BOOM, 1.8f, 1.1f);
                }
                if (t >= EMERGE_TICKS) { setState(getStage() == MERCHANT ? DANCE : IDLE); if (isMega()) cooldown = 40; }
            } else if (st == ROAR) {
                LivingEntity tg = getTarget();
                if (tg != null) this.getLookControl().setLookAt(tg, 30, 30);
                if (t == 20) {
                    particles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 3.0 * getModelScale(), getZ(), 12, 1.5, 0.5, 1.5, 0.0);
                    particles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2.2 * getModelScale(), getZ(), 1, 0, 0, 0, 0);
                    ring(ParticleTypes.CLOUD, 36, getY() + 0.3, 0.8);
                    ring(ParticleTypes.SOUL_FIRE_FLAME, 36, getY() + 0.3, 0.5);
                    snd(ModSounds.EVIL_LAUGH, 2.0f, 0.9f);
                }
                if (t >= 50) setState(IDLE);
            } else if (st == DANCE) {
                if (t == 1) {
                    snd(ModSounds.HONK, 2.0f, 1.0f);
                    particles(ParticleTypes.NOTE, getX(), getY() + 4.2, getZ(), 12, 1.2, 0.8, 1.2, 1.0);
                }
                if (t % 10 == 0) {                                   // al ritmo del baile
                    particles(ParticleTypes.NOTE, getX(), getY() + 4.0, getZ(), 2, 1.5, 0.6, 1.5, 1.0);
                    if (t % 20 == 0) snd(ModSounds.BOING, 0.5f, t % 40 == 0 ? 1.15f : 1.0f);
                }
                if (t >= DANCE_TICKS) setState(IDLE);
            } else {
                if (t % 3 == 0) groundParticles(8);
                if (t >= 70) finishSink();
            }
            return;
        }

        switch (getStage()) {
            case MERCHANT -> tickMerchant();
            case ANGRY, MEGA -> tickCombat();
            case MINI -> tickMini();
            case CHASER -> tickChaser();
            default -> { }
        }
    }

    // ================= MERCADER (una peticion nueva cada dia) =================
    private void tickMerchant() {
        int t = this.tickCount - this.animStart;
        if (getState() == TALK && t >= 40) setState(IDLE);
        if (leaving) return;

        Player near = level().getNearestPlayer(this, 48);
        if (near == null) return;
        long today = currentClock();

        if (requestClock < 0) {                 // primera peticion, justo tras el ritual
            newDailyRequest(true);
            return;
        }
        if (today > requestClock) {             // ha amanecido un dia nuevo
            if (!fulfilled) {
                Player who = lastPlayer != null ? level().getPlayerByUUID(lastPlayer) : null;
                becomeAngry(who != null ? who : near);
            } else {
                newDailyRequest(false);
            }
            return;
        }
        if (!fulfilled && this.tickCount % 2400 == 0) {
            say("Hmm... ¿y mi antojo de hoy? " + describeRequests());
        }
    }

    private long currentClock() {
        long clock = level().dimensionType().natural() ? level().getDayTime() : level().getGameTime();
        return clock / 24000L;
    }

    private static String idOf(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.toString();
    }

    /** Elige un objeto distinto al de ayer y lo pide para hoy. Se mantiene hasta el amanecer. */
    private void newDailyRequest(boolean first) {
        List<Item> options = new ArrayList<>(List.of(
                Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_MUTTON,
                Items.COOKED_SALMON, Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.CAKE,
                Items.PUMPKIN_PIE, Items.COOKIE, Items.BREAD, Items.HONEY_BOTTLE, Items.BAKED_POTATO,
                Items.APPLE, Items.MELON_SLICE, Items.BEETROOT_SOUP, Items.RABBIT_STEW,
                Items.MUSHROOM_STEW, Items.SWEET_BERRIES));
        final String yesterday = lastItemId;
        options.removeIf(i -> idOf(i).equals(yesterday));
        Item it = options.get(random.nextInt(options.size()));

        int amount;
        int extra = Math.min(streak / 2, 6);
        if (it == Items.CAKE || it == Items.GOLDEN_APPLE) amount = 1 + random.nextInt(2) + Math.min(streak / 4, 3);
        else if (it == Items.BEETROOT_SOUP || it == Items.RABBIT_STEW || it == Items.MUSHROOM_STEW) amount = 2 + random.nextInt(3);
        else if (it == Items.HONEY_BOTTLE || it == Items.GOLDEN_CARROT) amount = 3 + random.nextInt(4) + extra;
        else if (it == Items.COOKIE) amount = 16 + random.nextInt(17) + extra;
        else if (it == Items.PUMPKIN_PIE) amount = 4 + random.nextInt(5) + extra;
        else amount = 8 + random.nextInt(9) + extra;

        requests.clear();
        Request r = new Request();
        r.item = it; r.total = amount; r.left = amount;
        requests.add(r);
        lastItemId = idOf(it);
        fulfilled = false;

        long today = currentClock();
        long tod = (level().dimensionType().natural() ? level().getDayTime() : level().getGameTime()) % 24000L;
        // si te lo invocan ya de noche, tienes hasta el amanecer del dia siguiente
        requestClock = (first && tod > 12000L) ? today + 1 : today;

        setState(TALK);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HONK.get(), SoundSource.NEUTRAL, 1.5f, 1.0f);
        if (first) {
            say("Hoho... ¡hola, humano! Me voy a quedar por aquí. Cada día tendré un antojo distinto. "
                    + "Hoy quiero: " + describeRequests() + ". Tráemelo antes de que amanezca, o ya sabes...");
        } else {
            say("¡Buenos días! Hoy me apetece: " + describeRequests() + ". Antes de mañana, ¿eh?"
                    + (streak >= 3 ? " (Llevas " + streak + " días portándote bien.)" : ""));
        }
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
        if (getStage() != MERCHANT || getState() == EMERGE || getState() == SINK || getState() == DANCE || leaving) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        lastPlayer = player.getUUID();
        if (fulfilled) {
            setState(TALK);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AMBIENT.get(), SoundSource.NEUTRAL, 1.5f, 1.1f);
            say("Hoy ya he comido, gracias. Mañana tendré otro antojo... Vuelve entonces.");
            return InteractionResult.SUCCESS;
        }

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
        boolean done = !requests.isEmpty() && requests.stream().allMatch(q -> q.left <= 0);
        setState(TALK);
        if (done) {
            fulfilled = true;
            streak++;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HONK.get(), SoundSource.NEUTRAL, 1.5f, 1.2f);
            say("¡Ñam ñam ñam! ¡Delicioso! Toma tu recompensa. Mañana tendré otro antojo, no te olvides.");
            giveReward();
        } else if (gave) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AMBIENT.get(), SoundSource.NEUTRAL, 1.5f, 1.2f);
            say("Mmm, gracias. Aún me falta: " + describeRequests());
        } else {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.AMBIENT.get(), SoundSource.NEUTRAL, 1.5f, 0.9f);
            say("Hoy quiero: " + describeRequests() + ". Antes de que amanezca...");
        }
        return InteractionResult.SUCCESS;
    }

    /** Recompensa diaria; mejora con la racha de dias seguidos. */
    private void giveReward() {
        int s = streak;
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + random.nextInt(3) + Math.min(s, 6)));
        this.spawnAtLocation(new ItemStack(Items.EMERALD, 8 + random.nextInt(9) + Math.min(s * 2, 16)));
        this.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(2)));
        if (s % 3 == 0) this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        if (s % 7 == 0) this.spawnAtLocation(new ItemStack(Items.NETHERITE_INGOT, 1));
        if (level() instanceof ServerLevel sl) {
            ExperienceOrb.award(sl, this.position(), 60 + s * 10);
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
                    long wait = (5L + random.nextInt(6)) * 1200L;    // 5 a 10 minutos
                    HauntData.get(sl.getServer()).add(target, now + wait, now + 400L + random.nextInt(600));
                }
            }
        }
        this.discard();
    }

    // ================= SUSTO =================
    private void vanishScare(boolean laugh) {
        particles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 50, 1.0, 1.5, 1.0, 0.06);
        particles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 20, 0.6, 1.0, 0.6, 0.05);
        if (laugh) level().playSound(null, getX(), getY(), getZ(), ModSounds.EVIL_LAUGH.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        this.discard();
    }

    private void triggerScream(Player p) {
        scareTriggered = true;
        scareHitTick = scareTicks;
        setState(SCREAM);
        level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SCREAM.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
        level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.STING.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
    }

    /** Se queda helado si lo miras y avanza hacia ti cuando apartas la vista. Si te alcanza, screamer. */
    private void tickStalker(Player p) {
        double dist = this.distanceTo(p);
        Vec3 look = p.getViewVector(1.0f);
        Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
        boolean watched = look.dot(to) > 0.5 && p.hasLineOfSight(this);
        if (watched) {
            if (scareTicks % 25 == 0) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE,
                        Math.max(0.8f, (float) (4.0 - dist * 0.08)), 0.8f);
            }
        } else {
            Vec3 dir = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
            if (dir.lengthSqr() > 1.0E-4) {
                dir = dir.normalize();
                double sp = dist < 14.0 ? 0.42 : 0.30;
                double nx = getX() + dir.x * sp, nz = getZ() + dir.z * sp;
                double ny = p.getY();
                net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(nx, p.getY(), nz);
                if (level().hasChunkAt(bp) && level().canSeeSky(p.blockPosition())) {
                    ny = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            (int) Math.floor(nx), (int) Math.floor(nz));
                }
                this.setPos(nx, ny, nz);
                if (scareTicks % 9 == 0) {
                    level().playSound(null, getX(), getY(), getZ(), ModSounds.SCRAPE.get(), SoundSource.HOSTILE, 1.8f, 0.8f);
                }
            }
        }
        if (dist < 3.4) triggerScream(p);
        else if (scareTicks > 1500) vanishScare(false);
    }

    private void tickScare() {
        scareTicks++;
        Player p = level().getNearestPlayer(this, 90);
        if (p == null) {
            if (scareTicks > 600) this.discard();
            return;
        }
        // siempre te mira
        this.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        this.setDeltaMovement(Vec3.ZERO);

        if (!scareTriggered) {
            if (scareKind == 4) {
                tickStalker(p);
                return;
            }
            if (scareTicks % 90 == 30) {
                level().playSound(null, getX(), getY(), getZ(),
                        scareKind == 1 ? ModSounds.BREATH.get() : ModSounds.WHISPER.get(),
                        SoundSource.HOSTILE, scareKind == 1 ? 1.8f : 3.0f, 0.8f);
            }
            if (scareTicks % 40 == 0) particles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 6, 0.5, 0.1, 0.5, 0.01);

            Vec3 look = p.getViewVector(1.0f);
            Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
            boolean seen = look.dot(to) > (scareKind == 1 ? 0.55 : 0.96) && p.hasLineOfSight(this);
            if (seen) stareTicks++;
            else if (stareTicks > 0 && scareTicks % 2 == 0) stareTicks--;

            if (stareTicks >= (scareKind == 1 ? 3 : 24)) triggerScream(p);
            else if (scareTicks > 2400) vanishScare(false);
            return;
        }

        // screamer en marcha: pegado a tu cara, temblando
        int dt = scareTicks - scareHitTick;
        Vec3 look = p.getViewVector(1.0f);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        double dist = 2.4 - Math.min(dt, 10) * 0.02;
        this.setPos(p.getX() + flat.x * dist + (random.nextDouble() - 0.5) * 0.14, p.getY(),
                p.getZ() + flat.z * dist + (random.nextDouble() - 0.5) * 0.14);
        if (dt % 4 == 0) particles(ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 8, 0.6, 1.0, 0.6, 0.02);
        if (dt >= 26) vanishScare(true);
    }

    // ================= COMBATE =================
    private int phase() {
        float f = this.getHealth() / this.getMaxHealth();
        if (isMega()) return f < 0.33f ? 3 : (f < 0.66f ? 2 : 1);
        return f < 0.5f ? 2 : 1;
    }

    private void tickCombat() {
        LivingEntity target = this.getTarget();
        if (isMega() && this.tickCount % 3 == 0) {            // aura tenebrosa
            double a = random.nextDouble() * Math.PI * 2;
            particles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * 2.0, getY() + random.nextDouble() * 6.5, getZ() + Math.sin(a) * 2.0, 1, 0, 0.05, 0, 0.01);
        }
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
            if (pick == SUMMON && level().getEntitiesOfClass(LordObesityEntity.class, getBoundingBox().inflate(50), x -> x.getStage() == MINI && x.isAlive()).size() >= 8) pick = BEAM;
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
        double sc = getModelScale();

        switch (s) {
            case SLAM -> {
                lookAtTarget(target, 30);
                double rad = (ph >= 2 ? 12.0 : 8.0) * rm;
                if (t == 1) snd(ModSounds.ROAR, 2.0f, 1.0f);
                if (t < 25 && t % 2 == 0) {                         // energia que se acumula sobre las manos
                    double a = t * 0.5, h = (3.2 + t * 0.04) * sc;
                    particles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.3 * sc, getY() + h, getZ() + Math.sin(a) * 1.3 * sc, 1, 0, 0, 0, 0.0);
                    particles(ParticleTypes.END_ROD, getX() - Math.cos(a) * 1.3 * sc, getY() + h, getZ() - Math.sin(a) * 1.3 * sc, 1, 0, 0, 0, 0.0);
                }
                if (t == 3 || t == 14) circle(ParticleTypes.SOUL_FIRE_FLAME, rad, 44, getY() + 0.1);   // aviso del area
                if (t == 27) shockwave(rad, (ph >= 2 ? 12f : 8f) * dm, 1.2, true);
                if (t >= 40) endAttack(ph);
            }
            case BURP -> {
                lookAtTarget(target, 30);
                if (t == 2) snd(ModSounds.INHALE, 2.0f, 1.3f);
                if (t < 30 && t % 3 == 0) {
                    particles(ParticleTypes.SNEEZE, getX(), getY() + 1.4 * sc, getZ(), 3, 0.8 * sc, 0.4 * sc, 0.8 * sc, 0.0);
                }
                if (t == 30) burp(rm, dm);
                if (t >= 50) endAttack(ph);
            }
            case CHARGE -> {
                if (t < 20) {
                    lookAtTarget(target, 60);
                    if (t == 1) snd(ModSounds.WHISTLE, 2.0f, 0.8f);
                    if (t % 3 == 0) groundParticles(3);
                    if (t % 5 == 0) circle(ParticleTypes.FLAME, 1.6 * sc, 16, getY() + 0.1);
                } else if (t < 60) {
                    if (t == 20) {
                        Vec3 dir = target != null ? target.position().subtract(this.position()) : this.getLookAngle();
                        dashDir = new Vec3(dir.x, 0, dir.z).normalize();
                        snd(ModSounds.WHOOSH, 2.5f, 0.8f);
                        ring(ParticleTypes.CLOUD, 28, getY() + 0.2, 0.7);
                    }
                    float yaw = (float) (Mth.atan2(dashDir.z, dashDir.x) * (180.0 / Math.PI)) - 90.0f;
                    this.setYRot(yaw);
                    this.yBodyRot = yaw;
                    this.yHeadRot = yaw;
                    double speed = isMega() ? 1.15 : (ph >= 2 ? 1.0 : 0.85);
                    this.setDeltaMovement(dashDir.x * speed, this.getDeltaMovement().y, dashDir.z * speed);
                    particles(ParticleTypes.CLOUD, getX() - dashDir.x * 1.2 * sc, getY() + 0.3, getZ() - dashDir.z * 1.2 * sc, 4, 0.5 * sc, 0.15, 0.5 * sc, 0.02);
                    particles(ParticleTypes.FLAME, getX() - dashDir.x * 1.0 * sc, getY() + 0.2, getZ() - dashDir.z * 1.0 * sc, 2, 0.4 * sc, 0.1, 0.4 * sc, 0.02);
                    particles(ParticleTypes.SMOKE, getX() - dashDir.x * 1.6 * sc, getY() + 0.5, getZ() - dashDir.z * 1.6 * sc, 2, 0.5 * sc, 0.3, 0.5 * sc, 0.02);
                    chargeHit(dm);
                    if (this.horizontalCollision && t > 26) {
                        snd(ModSounds.SLAM, 1.8f, 0.8f);
                        particles(ParticleTypes.EXPLOSION, getX() + dashDir.x * 1.5 * sc, getY() + 1.0, getZ() + dashDir.z * 1.5 * sc, 3, 0.5, 0.5, 0.5, 0);
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
                    if (t % 3 == 0) groundParticles(3);
                } else if (t == 15) {
                    snd(ModSounds.BOING, 2.5f, 0.8f);
                    snd(ModSounds.WHOOSH, 2.0f, 0.7f);
                    ring(ParticleTypes.CLOUD, 36, getY() + 0.2, 0.9);
                    particles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 4, 0.8 * sc, 0.1, 0.8 * sc, 0);
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
                if (t > 15 && t < 100 && !this.onGround()) {          // estela en el aire
                    particles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 6, 0.5 * sc, 0.2, 0.5 * sc, 0.02);
                    particles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 4, 0.6 * sc, 0.1, 0.6 * sc, 0.01);
                }
            }
            case BEAM -> {
                if (t < 20) {
                    lookAtTarget(target, 40);
                    if (t == 1) snd(ModSounds.INHALE, 2.0f, 0.9f);
                    Vec3 look = this.getViewVector(1.0f);
                    Vec3 m = this.getEyePosition().add(look.scale(1.5 * sc));
                    double k = 1.0 - t / 20.0;
                    particles(ParticleTypes.END_ROD, m.x + (random.nextDouble() - 0.5) * 4.0 * sc * k,
                            m.y + (random.nextDouble() - 0.5) * 4.0 * sc * k, m.z + (random.nextDouble() - 0.5) * 4.0 * sc * k, 1, 0, 0, 0, 0.0);
                    if (t == 19) particles(ParticleTypes.FLASH, m.x, m.y, m.z, 1, 0, 0, 0, 0);
                } else if (t < 62) {
                    lookAtTarget(target, 5);
                    beamTick(t, rm, dm);
                } else if (t >= 72) {
                    endAttack(ph);
                }
            }
            case SUMMON -> {
                lookAtTarget(target, 30);
                if (t == 1) snd(ModSounds.EVIL_LAUGH, 2.5f, 0.8f);
                if (t < 30 && t % 2 == 0) {                          // runas que suben desde el suelo
                    circle(ParticleTypes.SOUL_FIRE_FLAME, (4.2 - t * 0.07) * sc, 24, getY() + 0.1 + t * 0.06 * sc);
                }
                if (t == 25) summonMinions(target);
                if (t >= 60) endAttack(ph);
            }
            case SUCK -> {
                lookAtTarget(target, 30);
                if (t == 1) snd(ModSounds.INHALE, 3.0f, 0.8f);
                if (t >= 15 && t < 55) suckTick(t, rm);
                if (t == 58) {
                    snd(ModSounds.BURP, 2.5f, 0.6f);
                    particles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2.0 * sc, getZ(), 1, 0, 0, 0, 0);
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
                particles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.5, 0.4, 0.4);
                particles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    /** Onda de choque alrededor del jefe (aplastamiento, aterrizaje, estallido). */
    private void shockwave(double r, float dmg, double kb, boolean slow) {
        snd(ModSounds.SLAM, 2.0f, 1.0f);
        snd(ModSounds.BOOM, 0.7f, isMega() ? 0.9f : 1.2f);
        AABB box = getBoundingBox().inflate(r, 2.0, r);
        for (Player p : level().getEntitiesOfClass(Player.class, box, this::validVictim)) {
            p.hurt(damageSources().mobAttack(this), dmg);
            Vec3 dir = flatDir(position(), p.position());
            p.push(dir.x * kb, 0.8, dir.z * kb);
            p.hurtMarked = true;
            if (slow) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        double y = getY() + 0.15;
        ring(ParticleTypes.CLOUD, 44, y, 0.55);
        ring(ParticleTypes.CLOUD, 44, y, 0.9);
        ring(ParticleTypes.POOF, 56, y + 0.1, 1.3);
        ring(ParticleTypes.FIREWORK, 36, y + 0.2, 0.55);
        ring(ParticleTypes.SOUL_FIRE_FLAME, 36, y + 0.1, 0.4);
        particles(ParticleTypes.END_ROD, getX(), y + 0.6, getZ(), 50, 0.5, 0.2, 0.5, 0.4);
        BlockState below = level().getBlockState(blockPosition().below());
        if (!below.isAir()) {
            particles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), y, getZ(), 120, r * 0.4, 0.15, r * 0.4, 0.4);
        }
        particles(ParticleTypes.EXPLOSION, getX(), y + 0.2, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
        if (isMega()) particles(ParticleTypes.SONIC_BOOM, getX(), y + 1.0, getZ(), 1, 0, 0, 0, 0);
    }

    /** Eructo en cono: hambre + nauseas + empuje, con una nube verde que se abre. */
    private void burp(double rm, float dm) {
        double sc = getModelScale();
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
        Vec3 o = new Vec3(getX() + flat.x * 1.5 * sc, getY() + 2.5 * sc, getZ() + flat.z * 1.5 * sc);
        for (double d = 0.5; d < 11.0 * rm; d += 1.0) {
            double spread = 0.12 * d + 0.2;
            particles(ParticleTypes.SNEEZE, o.x + flat.x * d, o.y - d * 0.04, o.z + flat.z * d, 3, spread, spread * 0.6, spread, 0.02);
            if (d % 2 < 1.0) particles(ParticleTypes.HAPPY_VILLAGER, o.x + flat.x * d, o.y, o.z + flat.z * d, 1, spread, spread * 0.5, spread, 0.0);
        }
        particles(ParticleTypes.COMPOSTER, o.x + flat.x * 4, o.y, o.z + flat.z * 4, 14, 1.5, 0.6, 1.5, 0.05);
        particles(ParticleTypes.END_ROD, o.x, o.y, o.z, 12, 0.3, 0.3, 0.3, 0.3);
    }

    /** Rayo continuo de eructo: nucleo luminoso, helice de fuego de alma y chispas en el impacto. */
    private void beamTick(int t, double rm, float dm) {
        double sc = getModelScale();
        Vec3 look = this.getViewVector(1.0f);
        Vec3 origin = this.getEyePosition().add(look.scale(1.5 * sc));
        double len = 18.0 * rm;
        BlockHitResult hit = level().clip(new ClipContext(origin, origin.add(look.scale(len)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) len = Math.max(1.0, hit.getLocation().distanceTo(origin));

        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = look.cross(up).normalize();
        Vec3 v = look.cross(u).normalize();
        for (double d = 0.5; d < len; d += 1.0) {
            Vec3 c = origin.add(look.scale(d));
            particles(ParticleTypes.END_ROD, c.x, c.y, c.z, 1, 0.03, 0.03, 0.03, 0.0);
            particles(ParticleTypes.SNEEZE, c.x, c.y, c.z, 1, 0.3, 0.3, 0.3, 0.01);
            double ph = t * 0.5 + d * 1.1;
            double rad = 0.65 * (0.7 + 0.3 * Math.sin(d * 0.3));
            for (int k = 0; k < 2; k++) {
                double a = ph + k * Math.PI;
                Vec3 h = c.add(u.scale(Math.cos(a) * rad)).add(v.scale(Math.sin(a) * rad));
                particles(ParticleTypes.SOUL_FIRE_FLAME, h.x, h.y, h.z, 1, 0, 0, 0, 0.0);
            }
        }
        Vec3 end = origin.add(look.scale(len));
        particles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 8, 0.4, 0.4, 0.4, 0.3);
        if (t % 4 == 0) particles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.3, 0.3, 0.3, 0);
        if (t == 20 || t == 50) snd(ModSounds.BEAM, 2.0f, 1.0f);
        if (t % 12 == 0) snd(ModSounds.BURP, 1.2f, 1.4f);

        if (t % 5 == 0) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(18.0 * rm), this::validVictim)) {
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

    /** Inhala: arrastra a los jugadores hacia su boca con remolinos de particulas. */
    private void suckTick(int t, double rm) {
        double sc = getModelScale();
        double r = 26.0 * rm;
        Vec3 mouth = this.position().add(0, 2.3 * sc, 0);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), this::validVictim)) {
            Vec3 dir = mouth.subtract(p.position().add(0, p.getBbHeight() * 0.5, 0));
            if (dir.length() < 1.5) continue;
            Vec3 n = dir.normalize();
            p.push(n.x * 0.11, n.y * 0.05, n.z * 0.11);
            p.hurtMarked = true;
            if (t % 10 == 0 && p.distanceTo(this) < 5.0f * sc) {
                p.hurt(damageSources().mobAttack(this), 2.0f);
            }
        }
        for (int arm = 0; arm < 4; arm++) {                       // remolino que entra por la boca
            for (int st = 0; st < 10; st++) {
                double rad = (14.0 * rm) * (1.0 - st / 10.0) + 1.5;
                double a = t * 0.25 + arm * Math.PI / 2 + st * 0.35;
                double h = (2.3 * sc) * (st / 10.0) + 0.3;
                particles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * rad, getY() + h, getZ() + Math.sin(a) * rad, 1, 0, 0, 0, 0.0);
            }
        }
        if (t % 2 == 0) particles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 3, 0.4, 0.4, 0.4, 0.05);
    }

    /** Invoca mini Lord Obesity (ojos rojos, rapidos y chillones). */
    private void summonMinions(@Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        double sc = getModelScale();
        int existing = sl.getEntitiesOfClass(LordObesityEntity.class, getBoundingBox().inflate(50),
                x -> x.getStage() == MINI && x.isAlive()).size();
        int toSpawn = Math.min(5, 9 - existing);
        snd(ModSounds.CHITTER, 2.5f, 0.8f);
        for (int i = 0; i < toSpawn; i++) {
            LordObesityEntity m = ModEntities.LORD_OBESITY.get().create(sl);
            if (m == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            double dist = 3.0 * sc + 1.0 + random.nextDouble() * 2.0;
            m.moveTo(getX() + Math.cos(a) * dist, getY(), getZ() + Math.sin(a) * dist, random.nextFloat() * 360f, 0f);
            m.initMini(target);
            sl.addFreshEntity(m);
        }
        ring(ParticleTypes.SOUL_FIRE_FLAME, 40, getY() + 0.2, 0.6);
        ring(ParticleTypes.SMOKE, 40, getY() + 0.3, 0.4);
    }

    // ================= Perseguidor =================
    private void tickChaser() {
        if (this.tickCount % 100 == 0) {                      // cada vez mas rapido
            setAttr(Attributes.MOVEMENT_SPEED, Math.min(0.40, 0.28 + 0.012 * (this.tickCount / 100)));
        }
        if (this.tickCount % 10 == 0) {
            LivingEntity tg = getTarget();
            if (tg == null || !tg.isAlive()) {
                Player n = level().getNearestPlayer(this, 150);
                if (n != null) { setTarget(n); tg = n; }
            }
            if (tg != null) this.getNavigation().moveTo(tg, 1.0);
        }
        if (this.tickCount % 40 == 0) particles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 3, 0.5, 0.8, 0.5, 0.02);
        if (this.tickCount % 90 == 45) level().playSound(null, getX(), getY(), getZ(), ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 2.5f, 0.8f);
    }

    // ================= Mini =================
    private void tickMini() {
        if (this.tickCount % 4 == 0) particles(ParticleTypes.SOUL, getX(), getY() + 0.5, getZ(), 1, 0.2, 0.2, 0.2, 0.01);
        if (this.tickCount > 100 && this.tickCount % 40 == 0) {
            boolean boss = !level().getEntitiesOfClass(LordObesityEntity.class, getBoundingBox().inflate(70),
                    x -> x.getStage() == MEGA && x.isAlive()).isEmpty();
            if (!boss || this.tickCount > 1200) {
                particles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                this.discard();
            }
        }
    }

    // ================= Particulas utiles =================
    /** Anillo que se expande hacia fuera desde el jefe. */
    private void ring(ParticleOptions p, int count, double y, double speed) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            sl.sendParticles(p, getX(), y, getZ(), 0, Math.cos(a), 0.03, Math.sin(a), speed);
        }
    }

    /** Circulo fijo de particulas (aviso de area). */
    private void circle(ParticleOptions p, double radius, int count, double y) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            sl.sendParticles(p, getX() + Math.cos(a) * radius, y, getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0.0);
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
        tag.putBoolean("Fulfilled", fulfilled);
        tag.putInt("Streak", streak);
        tag.putString("LastItem", lastItemId);
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
        fulfilled = tag.getBoolean("Fulfilled");
        streak = tag.getInt("Streak");
        lastItemId = tag.getString("LastItem");
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

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ================= Sonidos propios =================
    @Override protected SoundEvent getAmbientSound() {
        int st = getStage();
        if (st == SCARE) return null;
        if (st == CHASER) return ModSounds.EVIL_LAUGH.get();
        return st == MINI ? ModSounds.CHITTER.get() : ModSounds.AMBIENT.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return getStage() == MINI ? ModSounds.MINI_HURT.get() : ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return getStage() == MINI ? ModSounds.MINI_DEATH.get() : ModSounds.DEATH.get(); }
    @Override protected float getSoundVolume() { return getStage() == MINI ? 0.45f : 1.0f; }
    @Override public int getAmbientSoundInterval() { return 260; }
    @Override public float getVoicePitch() { return isMega() ? 0.7f : 1.0f; }
}
