package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.config.ModConfig
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.ai.TamedPhantomBodyControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomDefendGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomFollowOwnerGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomHeldItemLookGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLeashWanderGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLookAtPlayerGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLookControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomMoveControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomWanderGoal
import com.tamedphantoms.mod.input.PilotInputAccess
import com.tamedphantoms.mod.util.PhantomAcrobatics
import com.tamedphantoms.mod.util.PhantomAngerLogic
import com.tamedphantoms.mod.util.PhantomFlightPace
import com.tamedphantoms.mod.event.ModAdvancements
import com.tamedphantoms.mod.util.PhantomCrawl
import com.tamedphantoms.mod.util.PhantomFlightAttitude
import com.tamedphantoms.mod.util.PhantomScream
import com.tamedphantoms.mod.util.PhantomGroundSkim
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHover
import com.tamedphantoms.mod.util.PhantomShake
import com.tamedphantoms.mod.util.PhantomWetness
import com.tamedphantoms.mod.util.PhantomWingbeat
import com.tamedphantoms.mod.util.PhantomAngerState
import com.tamedphantoms.mod.util.PhantomSeatAssignment
import com.tamedphantoms.mod.util.PhantomTamingLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.neoforged.neoforge.fluids.FluidType
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.Saddleable
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.BodyRotationControl
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.Optional
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Ручной фантом. Наследуется напрямую от ванильного [Phantom] (подробности
 * — в докстринге на companion object и в README).
 */
class TamedPhantomEntity(entityType: EntityType<out TamedPhantomEntity>, level: Level) :
    Phantom(entityType, level),
    Saddleable {

    companion object {
        private val DATA_TAMED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_OWNER: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)
        private val DATA_SITTING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_SADDLED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_DEFENDING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_HEAD_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_RIDE_HEAD: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_SHAKE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_TRACKING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_SCREAM_EYES: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_ACRO_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_ACRO_ROLL: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_ACROBATIC: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_IMMORTAL: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)

        private const val TAG_TAMED = "Tamed"
        private const val TAG_OWNER = "Owner"
        private const val TAG_SITTING = "Sitting"
        private const val TAG_SADDLED = "Saddled"
        private const val TAG_WET_TRACKED = "WetTracked"
        private const val TAG_IN_WATER = "InWater"
        private const val TAG_IN_RAIN = "InRain"
        private const val TAG_SCREAM_COOLDOWN = "ScreamCooldown"
        private const val TAG_IMMORTAL = "Immortal"

        /**
         * Phantom НЕ предоставляет собственный публичный статический
         * createAttributes() — строим билдер на базе общего
         * Mob.createMobAttributes() с явно выставленными значениями
         * ванильного Phantom, подтверждёнными официальной вики: 20 ХП
         * (10 сердец), урон атаки 6, дальность слежения 64 блока.
         */
        const val PET_MAX_HEALTH = 40.0
        const val PET_ATTACK_DAMAGE = 12.0

        @JvmStatic
        fun createAttributes(): AttributeSupplier.Builder = Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, PET_MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, PET_ATTACK_DAMAGE)
            .add(Attributes.FOLLOW_RANGE, 64.0)

        /**
         * Ванильный размер фантома: визуал и хитбокс масштабируются как
         * `1.0 + 0.15 * size`. size=3 даёт примерно ×1.45.
         */
        const val PET_PHANTOM_SIZE = 3

        /** 11 секунд. Ванильный HUD мигает, когда осталось ≤10 с (200 тиков). */
        private const val NIGHT_VISION_DURATION_TICKS = 220

        /** Насколько голова кивает вверх/вниз при наборе и снижении верхом, градусы. */
        private const val RIDE_HEAD_DEGREES = 32f
    }

    init {
        this.moveControl = TamedPhantomMoveControl(this)
        this.lookControl = TamedPhantomLookControl(this)
        this.setPhantomSize(PET_PHANTOM_SIZE)
    }

    private var angerState: PhantomAngerState = PhantomAngerState.NONE
    private var repelCooldown: Int = 0

    /**
     * Состояние клавиш "Взлёт"/"Снижение" ВЛАДЕЛЬЦА, приходит сетевым
     * пакетом от клиента (см. `network/PhantomInputPayload.kt` и
     * `network/ModNetworking.kt`) — сознательно НЕ читается напрямую из
     * полей пассажира (`pilot.isJumping`/`pilot.isShiftKeyDown`), так как
     * при верховой езде "красться" конфликтует со спешиванием в ванильной
     * механике (это и было причиной бага "Ctrl не работает"). Не
     * синхронизируется клиентам — нужно только на сервере, где реально
     * считается движение.
     */
    private var pilotAscending: Boolean = false
    private var pilotDescending: Boolean = false
    private var pilotForward: Float = 0f
    private var pilotStrafe: Float = 0f

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_TAMED, false)
        builder.define(DATA_OWNER, Optional.empty())
        builder.define(DATA_SITTING, false)
        builder.define(DATA_SADDLED, false)
        builder.define(DATA_DEFENDING, false)
        builder.define(DATA_HEAD_PITCH, 0f)
        builder.define(DATA_RIDE_HEAD, 0f)
        builder.define(DATA_SHAKE, 0)
        builder.define(DATA_TRACKING, false)
        builder.define(DATA_SCREAM_EYES, 0)
        builder.define(DATA_ACRO_PITCH, 0f)
        builder.define(DATA_ACRO_ROLL, 0f)
        builder.define(DATA_ACROBATIC, false)
        builder.define(DATA_IMMORTAL, false)
    }

    /**
     * Абсолютный тангаж взгляда головы, градусы. Не путать с [xRot]:
     * тот наклоняет всё тело в полёте.
     */
    var headLookPitch: Float
        get() = this.entityData.get(DATA_HEAD_PITCH)
        set(value) = this.entityData.set(DATA_HEAD_PITCH, value)

    /** Предыдущий и текущий тангаж для плавного кадра. Пишет только клиентский тик. */
    var clientHeadPitchO: Float = 0f
    var clientHeadPitch: Float = 0f

    /** Кивок головы верхом: минус — вверх, плюс — вниз. Пишет сервер по клавишам пилота. */
    var rideHeadPitch: Float
        get() = this.entityData.get(DATA_RIDE_HEAD)
        set(value) = this.entityData.set(DATA_RIDE_HEAD, value)

    var clientRideHeadO: Float = 0f
    var clientRideHead: Float = 0f

    /** Сколько тиков ещё трястись после воды или дождя. */
    var shakeTicks: Int
        get() = this.entityData.get(DATA_SHAKE)
        set(value) = this.entityData.set(DATA_SHAKE, value)

    /** Сколько тиков глаза ещё горят красным от крика. 0 — обычный цвет. */
    var screamEyeTicks: Int
        get() = this.entityData.get(DATA_SCREAM_EYES)
        set(value) = this.entityData.set(DATA_SCREAM_EYES, value)

    private var screamCooldown: Int = 0
    private var screamTicks: Int = 0
    private var screamFears: Boolean = false
    var trackingLook: Boolean
        get() = this.entityData.get(DATA_TRACKING)
        set(value) = this.entityData.set(DATA_TRACKING, value)

    /** Насколько yRot изменился за последний клиентский тик. Для изгиба хвоста. */
    var turnYawDelta: Float = 0f

    private var wetMemory: PhantomWetness.Memory = PhantomWetness.Memory()

    /**
     * 0 — летит, 1 — завис, пятится или снижается без хода вперёд и вбок.
     * У пилота считается по клавишам, у остальных клиентов — по скорости.
     */
    var hoverBlend: Float = 0f

    /** Крен в градусах. Положительный — влево. Сглаживается на клиенте. */
    var bank: Float = 0f
    var bankO: Float = 0f

    /** Поворот головы в сторону крена, градусы. Не больше [PhantomFlightAttitude.HEAD_YAW]. */
    var headBankYaw: Float = 0f
    var headBankYawO: Float = 0f

    /** Тангаж хвоста, градусы. Вверх при снижении, вниз при наборе. */
    var tailPitch: Float = 0f
    var tailPitchO: Float = 0f

    /** 0 — воздух, 1 — глаза под водой. */
    var swimBlend: Float = 0f
    var swimBlendO: Float = 0f

    private var bankTarget: Float = 0f
    private var headYawTarget: Float = 0f

    /**
     * Накопленная фаза взмаха. Скорость зависит от режима полёта.
     * NaN — ещё не тикали на клиенте.
     */
    var wingPhase: Float = Float.NaN

    /** Сглаженные множители взмаха для кадра. Пишет только клиентский тик. */
    var wingFlapRate: Float = 1f
    var wingFlapRateO: Float = 1f
    var wingFlapAmp: Float = 1f
    var wingFlapAmpO: Float = 1f
    var wingDroop: Float = 0f
    var wingDroopO: Float = 0f
    var wingTipHang: Float = 0f
    var wingTipHangO: Float = 0f

    /** 1 — ползёт у земли: без крена и тангажа, крылья шагают. */
    var wingCrawl: Float = 0f
    var wingCrawlO: Float = 0f
    var crawlPhase: Float = 0f
    var crawlPhaseO: Float = 0f

    private var wingGroundBlend: Float = 0f
    var wingTakeoffBlend: Float = 0f
    var wingTakeoffO: Float = 0f
    private var wingGlideBlend: Float = 0f
    private var wingTouchedGround: Boolean = false
    private var crawlLock: Boolean = false
    private var skimLatched: Boolean = false
    private var skimGroundTicks: Int = 0
    private var skimAirTicks: Int = 0
    private var takeoffQueued: Boolean = false
    private var diveEnergy: Double = 0.0
    private var windCooldown: Int = 0
    private var motionSampleX: Double = Double.NaN
    private var motionSampleY: Double = Double.NaN
    private var motionSampleZ: Double = Double.NaN

    fun crawlVisual(partial: Float): Float =
        Mth.lerp(partial, this.wingCrawlO, this.wingCrawl).coerceIn(0f, 1f)

    fun bankVisual(partial: Float): Float =
        Mth.lerp(partial, this.bankO, this.bank) * (1f - this.crawlVisual(partial))

    /** Лишний тангаж сверх позы полёта. Копится, пока держат набор или снижение. */
    var acroPitch: Float = 0f
    var acroPitchO: Float = 0f
    private var acroRoll: Float = 0f
    private var loopProgress: Float = 0f

    /** Летит вперёд или вбок: камера и модель могут крутиться дальше фиксированного наклона. */
    var acrobatic: Boolean = false

    var immortal: Boolean
        get() = this.entityData.get(DATA_IMMORTAL)
        set(value) {
            this.entityData.set(DATA_IMMORTAL, value)
            if (value) this.setGlowingTag(true)
        }

    var boltTicks: Int = 0
    private var boltMotion: Vec3 = Vec3.ZERO

    fun takeBoltMotion(): Vec3? = if (this.boltTicks > 0) this.boltMotion else null

    fun acroPitchVisual(partial: Float): Float =
        Mth.lerp(partial, this.acroPitchO, this.acroPitch) * (1f - this.crawlVisual(partial))

    fun ridePitchVisual(partial: Float): Float {
        val crawl = this.crawlVisual(partial)
        val extra = Mth.lerp(partial, this.acroPitchO, this.acroPitch) * (1f - crawl)
        if (!this.acrobatic) return extra
        val shown = Mth.lerp(partial, this.xRotO, this.xRot) * (1f - crawl)
        return shown + extra
    }

    /** Куда сейчас смотреть. Выставляют цели взгляда и самообороны, крутит [TamedPhantomLookControl]. */
    var glanceTarget: LivingEntity? = null

    var tamed: Boolean
        get() = this.entityData.get(DATA_TAMED)
        set(value) = this.entityData.set(DATA_TAMED, value)

    var ownerUUID: UUID?
        get() = this.entityData.get(DATA_OWNER).orElse(null)
        set(value) = this.entityData.set(DATA_OWNER, Optional.ofNullable(value))

    fun isOrderedToSit(): Boolean = this.entityData.get(DATA_SITTING)

    fun setOrderedToSit(value: Boolean, playEffects: Boolean = true) {
        val changed = value != this.isOrderedToSit()
        this.entityData.set(DATA_SITTING, value)
        this.setNoGravity(!value)
        if (value) {
            this.navigation.stop()
            if (this.isVehicle) {
                this.ejectPassengers()
            }
            this.deltaMovement = Vec3(0.0, this.deltaMovement.y.coerceAtMost(0.0), 0.0)
        }
        if (changed && playEffects) {
            this.spawnGustParticles()
        }
    }

    fun isDefending(): Boolean = this.entityData.get(DATA_DEFENDING)

    private fun setDefending(value: Boolean) {
        this.entityData.set(DATA_DEFENDING, value, true)
    }

    var saddled: Boolean
        get() = this.entityData.get(DATA_SADDLED)
        private set(value) = this.entityData.set(DATA_SADDLED, value)

    fun isOwnedBy(entity: LivingEntity): Boolean = entity.uuid == this.ownerUUID

    /** Вызывается из обработчика сетевого пакета на сервере (только для владельца-пилота). */
    fun setPilotInput(ascending: Boolean, descending: Boolean, forward: Float = 0f, strafe: Float = 0f) {
        this.pilotAscending = ascending
        this.pilotDescending = descending
        this.pilotForward = forward
        this.pilotStrafe = strafe
    }

    // =====================================================================
    // Сохранение/загрузка
    // =====================================================================

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putBoolean(TAG_TAMED, this.tamed)
        tag.putBoolean(TAG_SITTING, this.isOrderedToSit())
        tag.putBoolean(TAG_SADDLED, this.saddled)
        val owner = this.ownerUUID
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner)
        }
        tag.putBoolean(TAG_WET_TRACKED, this.wetMemory.tracked)
        tag.putBoolean(TAG_IN_WATER, this.wetMemory.inWater)
        tag.putBoolean(TAG_IN_RAIN, this.wetMemory.inRain)
        tag.putInt(TAG_SCREAM_COOLDOWN, this.screamCooldown)
        tag.putBoolean(TAG_IMMORTAL, this.immortal)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        this.tamed = tag.getBoolean(TAG_TAMED)
        this.setOrderedToSit(tag.getBoolean(TAG_SITTING), playEffects = false)
        this.saddled = tag.getBoolean(TAG_SADDLED)
        this.ownerUUID = if (tag.hasUUID(TAG_OWNER)) tag.getUUID(TAG_OWNER) else null
        this.setPhantomSize(PET_PHANTOM_SIZE)
        this.applyPetStats()
        this.setNoGravity(!this.isOrderedToSit())
        if (tag.getBoolean(TAG_WET_TRACKED)) {
            this.wetMemory = PhantomWetness.Memory(
                tracked = true,
                inWater = tag.getBoolean(TAG_IN_WATER),
                inRain = tag.getBoolean(TAG_IN_RAIN),
            )
        }
        this.screamCooldown = tag.getInt(TAG_SCREAM_COOLDOWN).coerceAtLeast(0)
        this.immortal = tag.getBoolean(TAG_IMMORTAL)
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        this.applyPetStats()
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnType: MobSpawnType,
        spawnGroupData: SpawnGroupData?,
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData)
        this.setPhantomSize(PET_PHANTOM_SIZE)
        this.applyPetStats()
        return data
    }

    private fun applyPetStats() {
        this.getAttribute(Attributes.MAX_HEALTH)?.baseValue = PET_MAX_HEALTH
        this.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = PET_ATTACK_DAMAGE
    }

    override fun isSunBurnTick(): Boolean = false

    override fun canDrownInFluidType(type: FluidType): Boolean = false

    override fun decreaseAirSupply(currentAir: Int): Int = currentAir

    override fun canBeLeashed(): Boolean = this.isAlive

    fun boltFromLeash(from: Entity, dropLead: Boolean = true) {
        var dx = this.x - from.x
        var dz = this.z - from.z
        if (dx * dx + dz * dz < 0.04) {
            val ang = this.random.nextFloat() * Mth.TWO_PI
            dx = Mth.cos(ang).toDouble()
            dz = Mth.sin(ang).toDouble()
        }
        val len = hypot(dx, dz).coerceAtLeast(0.1)
        val side = if (this.random.nextBoolean()) 1.0 else -1.0
        val sx = -dz / len * side * 0.9 + dx / len * 0.25
        val sz = dx / len * side * 0.9 + dz / len * 0.25
        val slen = hypot(sx, sz).coerceAtLeast(0.1)
        val speed = 0.14
        this.boltMotion = Vec3(sx / slen * speed, 0.02, sz / slen * speed)
        this.deltaMovement = this.boltMotion
        this.yRot = (Mth.atan2(sz, sx) * Mth.RAD_TO_DEG).toFloat() - 90f
        this.yRotO = this.yRot
        this.boltTicks = 46
        this.navigation.stop()
        if (this.isLeashed) {
            this.dropLeash(true, dropLead)
        }
    }

    override fun handleLeashAtDistance(leashHolder: Entity, distance: Float): Boolean {
        if (this.isOrderedToSit()) {
            if (distance > 10.0f) {
                this.dropLeash(true, true)
            }
            return false
        }
        return super.handleLeashAtDistance(leashHolder, distance)
    }

    // =====================================================================
    // ИИ / цели
    // =====================================================================

    override fun createBodyControl(): BodyRotationControl = TamedPhantomBodyControl(this)

    override fun registerGoals() {
        this.goalSelector.addGoal(1, TamedPhantomDefendGoal(this))
        this.goalSelector.addGoal(2, TamedPhantomFollowOwnerGoal(this))
        this.goalSelector.addGoal(3, TamedPhantomLeashWanderGoal(this))
        this.goalSelector.addGoal(4, TamedPhantomWanderGoal(this))
        this.goalSelector.addGoal(5, TamedPhantomHeldItemLookGoal(this))
        this.goalSelector.addGoal(6, TamedPhantomLookAtPlayerGoal(this))
    }

    override fun canAttackType(type: EntityType<*>): Boolean = this.angerState.isActive

    override fun canAttack(target: LivingEntity): Boolean = target === this.getDefendTarget()

    override fun shouldDespawnInPeaceful(): Boolean = false

    override fun isPersistenceRequired(): Boolean = true

    // =====================================================================
    // Урон / самозащита
    // =====================================================================

    fun startDefending(attackerId: UUID) {
        this.setOrderedToSit(false, playEffects = false)
        this.angerState = PhantomAngerLogic.onHurtBy(attackerId, ServerConfig.CONFIG.defendDurationTicks.get())
        this.setDefending(true)
    }

    fun getDefendTarget(): LivingEntity? {
        if (!this.angerState.isActive) return null
        val id = this.angerState.targetId ?: return null
        val level = this.level()
        if (level !is ServerLevel) return null
        return level.getEntity(id) as? LivingEntity
    }

    // =====================================================================
    // Приручение / кормление / освобождение / седло
    // =====================================================================

    fun tameTo(player: Player) {
        this.tamed = true
        this.ownerUUID = player.uuid
        this.setOrderedToSit(false, playEffects = false)
        this.angerState = PhantomAngerState.NONE
        this.setDefending(false)
        PhantomOwnerRecall.remember(player, this)
        this.playTameSound()
        this.spawnHeartParticles()
    }

    fun release() {
        val previousOwner = this.findOwnerPlayer()
        this.tamed = false
        this.ownerUUID = null
        this.setOrderedToSit(false, playEffects = false)
        this.angerState = PhantomAngerState.NONE
        this.setDefending(false)
        if (previousOwner != null) {
            PhantomOwnerRecall.forget(previousOwner, this.uuid)
        }
        this.dropSaddleIfPresent()
        this.playTameSound()
        this.spawnAngryParticles()
    }

    fun healWithFood(nutrition: Int): Boolean {
        if (!PhantomTamingLogic.canHeal(this.health, this.maxHealth)) return false
        val healAmount = PhantomTamingLogic.healAmount(nutrition, this.health, this.maxHealth)
        if (healAmount <= 0f) return false
        this.heal(healAmount)
        this.playTameSound()
        this.spawnHappyParticles()
        return true
    }

    /** Снимает седло (по запросу ножницами) — падает на землю, как и при освобождении. Возвращает true, если седло реально было. */
    fun removeSaddle(): Boolean {
        if (!this.saddled) return false
        this.dropSaddleIfPresent()
        this.playSound(SoundEvents.SHEEP_SHEAR, 1.0f, 1.0f)
        return true
    }

    private fun dropSaddleIfPresent() {
        if (!this.saddled) return
        this.saddled = false
        val level = this.level()
        if (level is ServerLevel) {
            val drop = ItemEntity(level, this.x, this.y + this.bbHeight * 0.5, this.z, ItemStack(Items.SADDLE))
            drop.setDefaultPickUpDelay()
            level.addFreshEntity(drop)
        }
    }

    private fun playTameSound() {
        this.playSound(SoundEvents.PHANTOM_BITE, 1.0f, 0.9f + this.random.nextFloat() * 0.2f)
    }

    private fun spawnHeartParticles() {
        val level = this.level()
        if (level !is ServerLevel) return
        repeat(6) {
            val dx = this.random.nextGaussian() * 0.15
            val dy = this.random.nextDouble() * 0.5
            val dz = this.random.nextGaussian() * 0.15
            level.sendParticles(ParticleTypes.HEART, this.x, this.y + this.bbHeight * 0.5 + 0.2, this.z, 1, dx, dy, dz, 0.0)
        }
    }

    private fun spawnHappyParticles() {
        val level = this.level()
        if (level !is ServerLevel) return
        repeat(8) {
            val dx = this.random.nextGaussian() * 0.2
            val dy = this.random.nextDouble() * 0.6
            val dz = this.random.nextGaussian() * 0.2
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.x, this.y + this.bbHeight * 0.5 + 0.2, this.z, 1, dx, dy, dz, 0.0)
        }
    }

    private fun spawnAngryParticles() {
        val level = this.level()
        if (level !is ServerLevel) return
        level.sendParticles(
            ParticleTypes.ANGRY_VILLAGER,
            this.x,
            this.y + this.bbHeight * 0.7 + 0.25,
            this.z,
            10,
            0.45,
            0.35,
            0.45,
            0.0,
        )
    }

    private fun spawnGustParticles() {
        val level = this.level()
        if (level !is ServerLevel) return
        level.sendParticles(
            ParticleTypes.SMALL_GUST,
            this.x,
            this.y + this.bbHeight * 0.45,
            this.z,
            12,
            0.4,
            0.2,
            0.4,
            0.0,
        )
    }

    override fun isSaddleable(): Boolean = this.tamed && this.isAlive

    override fun isSaddled(): Boolean = this.saddled

    override fun equipSaddle(stack: ItemStack, soundSource: SoundSource?) {
        this.saddled = true
        this.playSound(SoundEvents.PHANTOM_BITE, 0.7f, 1.2f)
    }

    // =====================================================================
    // Езда верхом
    // =====================================================================

    override fun canAddPassenger(passenger: Entity): Boolean {
        if (!this.saddled || !this.tamed || this.isOrderedToSit()) return false
        if (this.passengers.size >= 2) return false
        val mountingIsOwner = passenger is Player && this.isOwnedBy(passenger)
        val pilotTaken = this.passengers.any { it is Player && this.isOwnedBy(it) }
        val passengerTaken = this.passengers.any { !(it is Player && this.isOwnedBy(it)) }
        return PhantomSeatAssignment.assignSeat(mountingIsOwner, pilotTaken, passengerTaken) != null
    }

    override fun getControllingPassenger(): LivingEntity? {
        val owner = this.ownerUUID ?: return null
        return this.passengers.firstOrNull { it is Player && it.uuid == owner } as? LivingEntity
    }

    // =====================================================================
    // Основной тик
    // =====================================================================

    override fun tick() {
        if (this.level().isClientSide) {
            this.clientHeadPitchO = this.clientHeadPitch
            this.clientHeadPitch = this.headLookPitch
            this.clientRideHeadO = this.clientRideHead
            this.clientRideHead = this.rideHeadPitch
            this.wingFlapRateO = this.wingFlapRate
            this.wingFlapAmpO = this.wingFlapAmp
            this.wingDroopO = this.wingDroop
            this.wingTipHangO = this.wingTipHang
            this.wingTakeoffO = this.wingTakeoffBlend
            this.bankO = this.bank
            this.acroPitchO = this.acroPitch
            this.headBankYawO = this.headBankYaw
            this.tailPitchO = this.tailPitch
            this.swimBlendO = this.swimBlend
            this.wingCrawlO = this.wingCrawl
            this.crawlPhaseO = this.crawlPhase
            if (this.isVehicle && !this.isControlledByLocalInstance) {
                this.acroPitch = this.entityData.get(DATA_ACRO_PITCH)
                this.bank = this.entityData.get(DATA_ACRO_ROLL)
                this.acrobatic = this.entityData.get(DATA_ACROBATIC)
            }
        }
        val yawBefore = this.yRot
        if (this.isOrderedToSit()) {
            this.xRot = 0.0f
        }

        super.tick()

        if (this.level().isClientSide) {
            this.turnYawDelta = PhantomHeadLook.wrapDegrees(this.yRot - yawBefore)
            this.tickFlightVisuals()
            this.advanceWingPhase()
            this.spawnShakeDroplets()
            this.spawnSwimTrail()
        }

        if (this.isOrderedToSit()) {
            this.xRot = 0.0f
            this.setNoGravity(false)
        }
        this.airSupply = this.maxAirSupply

        val level = this.level()
        if (level !is ServerLevel) {
            return
        }

        this.tickWetExit()
        this.tickRideHead()
        this.tickAngerTimer()
        this.tickScream(level)
        this.tickAcrobatics()
        this.tickDiveWind()
        if (this.boltTicks > 0) {
            this.boltTicks--
        }
        if (this.immortal) {
            this.setGlowingTag(true)
            if (this.health < this.maxHealth) {
                this.health = this.maxHealth
            }
            if (this.y < level.minBuildHeight) {
                this.teleportTo(this.x, (level.minBuildHeight + 8).toDouble(), this.z)
                this.deltaMovement = Vec3.ZERO
            }
        }

        if (this.tamed) {
            this.tickRepelWildPhantoms(level)
            this.tickOwnerRecall()

            if (this.isVehicle() && this.controllingPassenger != null) {
                this.tickRiderNightVision()
            } else {
                this.pilotAscending = false
                this.pilotDescending = false
                this.pilotForward = 0f
                this.pilotStrafe = 0f
            }
        }
    }

    private fun tickWetExit() {
        val inWater = this.isInWater
        val inRain = this.isInWaterOrRain && !inWater
        val (next, shake) = PhantomWetness.step(this.wetMemory, inWater, inRain)
        this.wetMemory = next
        if (shake) {
            this.shakeTicks = PhantomShake.LENGTH
            this.playSound(SoundEvents.PLAYER_SPLASH, 0.65f, 1.2f)
        }
        if (this.shakeTicks > 0) {
            this.shakeTicks--
        }
    }

    private fun tickRideHead() {
        val riding = this.isVehicle && this.controllingPassenger != null
        val target = if (!riding) {
            0f
        } else when {
            this.pilotAscending && !this.pilotDescending -> -RIDE_HEAD_DEGREES
            this.pilotDescending && !this.pilotAscending -> RIDE_HEAD_DEGREES
            else -> 0f
        }
        this.rideHeadPitch = PhantomHeadLook.approachDegrees(this.rideHeadPitch, target, 6f)
    }

    private fun spawnShakeDroplets() {
        if (this.shakeTicks <= 0 || this.tickCount % 2 != 0) return
        val level = this.level()
        repeat(4) {
            val dx = (this.random.nextDouble() - 0.5) * 1.6
            val dz = (this.random.nextDouble() - 0.5) * 1.6
            level.addParticle(
                ParticleTypes.SPLASH,
                this.x + dx,
                this.y + 0.35,
                this.z + dz,
                dx * 0.15,
                0.12,
                dz * 0.15,
            )
        }
    }

    private fun spawnSwimTrail() {
        if (!this.isInWater) return
        val level = this.level()
        val yaw = this.yRot * Mth.DEG_TO_RAD
        val rightX = Mth.cos(yaw).toDouble()
        val rightZ = Mth.sin(yaw).toDouble()
        val backX = Mth.sin(yaw).toDouble()
        val backZ = -Mth.cos(yaw).toDouble()
        val motion = this.deltaMovement
        for (side in doubleArrayOf(-0.9, 0.9)) {
            val px = this.x + rightX * side + backX * 0.4
            val py = this.y + 0.28
            val pz = this.z + rightZ * side + backZ * 0.4
            level.addParticle(ParticleTypes.BUBBLE, px, py, pz, -motion.x * 0.25, 0.03, -motion.z * 0.25)
            if (this.random.nextFloat() < 0.4f) {
                level.addParticle(ParticleTypes.BUBBLE_POP, px, py + 0.1, pz, 0.0, 0.02, 0.0)
            }
            if (this.tickCount % 3 == 0 && this.random.nextBoolean()) {
                level.addParticle(ParticleTypes.SPLASH, px, py, pz, -motion.x * 0.04, 0.01, -motion.z * 0.04)
            }
        }
    }

    private fun updateSkim(pressed: Boolean): Boolean {
        if (pressed) {
            this.skimAirTicks = 0
            this.skimGroundTicks++
            if (this.skimGroundTicks >= 2) this.skimLatched = true
        } else {
            this.skimGroundTicks = 0
            if (this.skimLatched) {
                this.skimAirTicks++
                if (this.skimAirTicks == 1) this.takeoffQueued = true
                if (this.skimAirTicks >= 5) this.skimLatched = false
            } else {
                this.skimAirTicks = 0
            }
        }
        return this.skimLatched
    }

    private fun flattenGroundAttitude() {
        this.acroPitch = 0f
        this.acroRoll = 0f
        this.loopProgress = 0f
        this.bank = 0f
        this.bankTarget = 0f
        this.headYawTarget = 0f
        this.acrobatic = false
        this.diveEnergy = 0.0
    }

    private fun tickDiveWind() {
        if (this.windCooldown > 0) this.windCooldown--
        val prevX = this.motionSampleX
        val prevY = this.motionSampleY
        val prevZ = this.motionSampleZ
        this.motionSampleX = this.x
        this.motionSampleY = this.y
        this.motionSampleZ = this.z
        if (prevY.isNaN() || !this.isVehicle) return
        val dy = this.y - prevY
        val horizontal = hypot(this.x - prevX, this.z - prevZ)
        if (dy < -0.28 && horizontal > 0.35 && this.windCooldown <= 0) {
            val volume = ((-dy - 0.2) * 1.4).toFloat().coerceIn(0.3f, 0.8f)
            this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS,
                volume,
                0.92f,
            )
            this.windCooldown = 16
        }
    }

    private fun findOwnerPlayer(): Player? {
        val id = this.ownerUUID ?: return null
        val level = this.level()
        if (level is ServerLevel) {
            return level.getServer()?.playerList?.getPlayer(id)
        }
        return level.players().firstOrNull { it.uuid == id }
    }

    private fun tickOwnerRecall() {
        val owner = this.findOwnerPlayer() ?: return
        if (this.tickCount % 20 == 0) {
            PhantomOwnerRecall.remember(owner, this)
        }
        PhantomOwnerRecall.tryTeleportToOwner(this, owner)
    }

    private fun tickAngerTimer() {
        this.angerState = PhantomAngerLogic.tick(this.angerState)
        this.setDefending(this.angerState.isActive)
    }

    private fun tickRepelWildPhantoms(level: ServerLevel) {
        if (this.repelCooldown-- > 0) return
        this.repelCooldown = ModConfig.REPEL_PUSH_INTERVAL_TICKS

        val box = this.boundingBox.inflate(ServerConfig.CONFIG.repelRadius.get())
        val nearby = level.getEntitiesOfClass(Phantom::class.java, box) { candidate ->
            candidate !== this && !(candidate is TamedPhantomEntity && candidate.tamed)
        }
        for (wild in nearby) {
            val dx = wild.x - this.x
            val dz = wild.z - this.z
            val distSq = dx * dx + dz * dz
            if (distSq < 0.01) continue
            val dist = sqrt(distSq)
            val push = 0.12
            wild.push(dx / dist * push, 0.0, dz / dist * push)
        }
    }

    fun tryOwnerScream(player: Player) {
        if (player.vehicle !== this || !this.isOwnedBy(player)) return
        val serverPlayer = player as? ServerPlayer ?: return
        if (this.screamCooldown > 0) {
            val seconds = (this.screamCooldown + 19) / 20
            serverPlayer.displayClientMessage(
                Component.translatable("tamedphantoms.scream.cooldown", seconds).withStyle(ChatFormatting.RED),
                true,
            )
            return
        }
        val level = this.level()
        if (level is ServerLevel) {
            this.beginScream(level, redEyes = true, blindness = true, awardOwner = serverPlayer)
        }
    }

    private fun tickScream(level: ServerLevel) {
        if (this.screamCooldown > 0) {
            this.screamCooldown--
        }
        if (this.screamTicks > 0) {
            PhantomScream.frighten(level, this, this.screamFears)
            this.screamTicks--
            if (this.screamTicks <= 0) {
                this.screamEyeTicks = 0
                this.screamFears = false
            }
            return
        }
        if (this.screamCooldown > 0 || !this.isAlive) return
        if (this.isDefending()) {
            this.beginScream(level, redEyes = true, blindness = true, awardOwner = null)
            return
        }
        if (!this.tamed && this.tickCount % 20 == 0 &&
            PhantomScream.isFullMoonNight(level) &&
            this.random.nextFloat() < 0.12f
        ) {
            this.beginScream(level, redEyes = false, blindness = false, awardOwner = null)
        }
    }

    private fun beginScream(level: ServerLevel, redEyes: Boolean, blindness: Boolean, awardOwner: ServerPlayer?) {
        val seconds = ServerConfig.CONFIG.screamCooldownSeconds.get().coerceAtLeast(1)
        this.screamCooldown = seconds * 20
        this.screamTicks = PhantomScream.EFFECT_TICKS
        this.screamFears = blindness
        this.screamEyeTicks = if (redEyes) PhantomScream.EFFECT_TICKS else 0
        this.addEffect(MobEffectInstance(MobEffects.GLOWING, PhantomScream.EFFECT_TICKS, 0, false, false, false))
        PhantomScream.play(level, this)
        PhantomScream.frighten(level, this, blindness)
        if (awardOwner != null) {
            ModAdvancements.grantWingedBeast(awardOwner)
        }
    }

    private fun tickAcrobatics() {
        val ridden = this.isVehicle && this.controllingPassenger is Player
        val crawling = ridden && this.updateSkim(PhantomGroundSkim.pressed(this))
        if (crawling) {
            this.flattenGroundAttitude()
            this.entityData.set(DATA_ACRO_PITCH, 0f)
            this.entityData.set(DATA_ACRO_ROLL, 0f)
            this.entityData.set(DATA_ACROBATIC, false)
            return
        }
        val acrobatic = PhantomAcrobatics.allows(
            this.pilotForward.toDouble(),
            this.pilotStrafe.toDouble(),
            crawling,
            this.isOrderedToSit(),
            ridden,
        )
        val climb = when {
            this.pilotAscending && !this.pilotDescending -> 1f
            this.pilotDescending && !this.pilotAscending -> -1f
            else -> 0f
        }
        val stepped = PhantomAcrobatics.step(
            this.acroPitch,
            this.acroRoll,
            this.loopProgress,
            acrobatic,
            climb,
            this.pilotStrafe,
        )
        this.acroPitch = stepped.pitch
        this.acroRoll = stepped.roll
        this.loopProgress = stepped.loop
        this.acrobatic = acrobatic
        this.entityData.set(DATA_ACRO_PITCH, stepped.pitch)
        this.entityData.set(DATA_ACRO_ROLL, stepped.roll)
        this.entityData.set(DATA_ACROBATIC, acrobatic)
        if (stepped.completedLoop) {
            this.becomeImmortal()
        }
    }

    var loopReadyToReport: Boolean = false

    fun tryOwnerLoop(player: Player) {
        if (player.vehicle !== this || !this.isOwnedBy(player)) return
        if (kotlin.math.abs(this.loopProgress) < 270f && kotlin.math.abs(this.acroPitch) < 270f) return
        this.becomeImmortal()
    }

    private fun becomeImmortal() {
        val first = !this.immortal
        this.immortal = true
        if (!first) return
        val rider = this.controllingPassenger as? ServerPlayer ?: return
        ModAdvancements.grantDeadLoop(rider)
    }

    private fun tickFlightVisuals() {
        this.crawlLock = this.updateSkim(PhantomGroundSkim.pressed(this))
        if (this.crawlLock) {
            this.flattenGroundAttitude()
        }
        if (this.isOrderedToSit() || this.crawlLock) {
            this.bankTarget = 0f
            this.headYawTarget = 0f
        }
        val vertical = this.y - this.yo
        val (forward, strafe) = PhantomFlightAttitude.split(this.yRot, this.x - this.xo, this.z - this.zo)
        val localRide = this.isControlledByLocalInstance && this.controllingPassenger is Player
        val remoteRide = this.isVehicle && !localRide
        if (!localRide && !this.isOrderedToSit() && !remoteRide) {
            val moving = !PhantomFlightAttitude.wantsHover(forward, strafe)
            this.hoverBlend = PhantomHover.step(this.hoverBlend, moving)
            val pose = PhantomFlightAttitude.pose(forward, strafe, vertical, this.hoverBlend)
            this.bankTarget = pose.bank
            this.headYawTarget = pose.headYaw
        }
        if (!this.isVehicle || this.crawlLock) {
            this.headYawTarget = 0f
        }
        if (this.crawlLock) {
            this.bankTarget = 0f
            if (localRide) {
                val flatten = this.wingGroundBlend.coerceIn(0f, 1f)
                this.xRot *= 1f - flatten
                this.setRot(this.yRot, this.xRot)
            }
        }
        if (localRide) {
            val pilot = this.controllingPassenger as Player
            val (ascending, descending) = PilotInputAccess.read(
                this.level().isClientSide,
                this.pilotAscending,
                this.pilotDescending,
            )
            val climb = when {
                ascending && !descending -> 1f
                descending && !ascending -> -1f
                else -> 0f
            }
            val acrobatic = PhantomAcrobatics.allows(
                pilot.zza.toDouble(),
                pilot.xxa.toDouble(),
                crawling = this.crawlLock,
                sitting = this.isOrderedToSit(),
                ridden = true,
            )
            val stepped = PhantomAcrobatics.step(
                this.acroPitch,
                this.bank,
                this.loopProgress,
                acrobatic,
                climb,
                pilot.xxa,
            )
            this.acroPitch = stepped.pitch
            this.loopProgress = stepped.loop
            this.acrobatic = acrobatic
            if (acrobatic) {
                this.bank = stepped.roll
                this.bankTarget = stepped.roll
            }
            if (stepped.completedLoop && this.isOwnedBy(pilot)) {
                this.loopReadyToReport = true
            }
        }
        if (!remoteRide && !(localRide && this.acrobatic)) {
            this.bank = PhantomHeadLook.approachDegrees(this.bank, this.bankTarget, 6f)
        }
        val yawTarget = this.headYawTarget.coerceIn(-PhantomFlightAttitude.HEAD_YAW, PhantomFlightAttitude.HEAD_YAW)
        this.headBankYaw = PhantomHeadLook.approachDegrees(this.headBankYaw, yawTarget, 3.5f)
            .coerceIn(-PhantomFlightAttitude.HEAD_YAW, PhantomFlightAttitude.HEAD_YAW)
        this.tailPitch = PhantomHeadLook.approachDegrees(
            this.tailPitch,
            PhantomFlightAttitude.tailPitchDegrees(vertical),
            4.5f,
        )
        this.swimBlend = if (this.isUnderWater) {
            (this.swimBlend + 0.07f).coerceAtMost(1f)
        } else {
            (this.swimBlend - 0.05f).coerceAtLeast(0f)
        }
    }

    private fun advanceWingPhase() {
        val vertical = this.y - this.yo
        val horizontal = hypot(this.x - this.xo, this.z - this.zo)
        val skimming = this.crawlLock
        val launch = this.takeoffQueued && this.wingGroundBlend > 0.35f
        this.takeoffQueued = false
        this.wingTouchedGround = skimming
        this.wingTakeoffBlend = PhantomWingbeat.stepTakeoff(this.wingTakeoffBlend, launch)
        this.wingGroundBlend = PhantomWingbeat.stepGround(this.wingGroundBlend, skimming)
        val hover = this.hoverBlend.coerceIn(0f, 1f)
        val descending = PhantomWingbeat.descending(vertical, hover, this.wingGroundBlend)
        this.wingGlideBlend = PhantomWingbeat.stepGlide(this.wingGlideBlend, descending)
        val pose = PhantomWingbeat.pose(
            vertical,
            horizontal,
            hover,
            this.wingGroundBlend,
            this.wingTakeoffBlend,
            this.wingGlideBlend,
            this.swimBlend,
        )
        this.wingFlapRate = pose.rate
        this.wingFlapAmp = pose.amplitude
        this.wingDroop = pose.droop
        this.wingTipHang = pose.tipHang
        if (this.wingPhase.isNaN()) {
            this.wingPhase = (this.tickCount - 1).toFloat()
        }
        this.wingPhase += pose.rate
        this.wingCrawl = this.wingGroundBlend * (1f - this.wingTakeoffBlend)
        if (skimming) {
            val multiple = PhantomFlightPace.pace(this) * PhantomFlightPace.BASELINE
            this.crawlPhase += PhantomCrawl.advance(horizontal, multiple)
        }
    }

    private fun tickRiderNightVision() {
        for (passenger in this.passengers) {
            if (passenger is LivingEntity) {
                passenger.addEffect(
                    MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, true),
                )
            }
        }
    }

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)
        if (passenger is LivingEntity) {
            val effect = passenger.getEffect(MobEffects.NIGHT_VISION)
            if (effect != null && effect.duration <= NIGHT_VISION_DURATION_TICKS && effect.amplifier == 0) {
                passenger.removeEffect(MobEffects.NIGHT_VISION)
            }
        }
    }

    // =====================================================================
    // Управляемый полёт
    // =====================================================================

    override fun causeFallDamage(fallDistance: Float, multiplier: Float, source: DamageSource): Boolean = false

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (this.immortal) return false
        return super.hurt(source, amount)
    }

    override fun isInvulnerableTo(source: DamageSource): Boolean {
        if (this.immortal) return true
        return super.isInvulnerableTo(source)
    }

    override fun kill() {
        if (this.immortal) return
        super.kill()
    }

    override fun checkFallDamage(y: Double, onGround: Boolean, state: BlockState, pos: BlockPos) {
        if (this.isOrderedToSit()) {
            if (onGround) {
                this.resetFallDistance()
            } else if (y < 0.0) {
                this.fallDistance -= y.toFloat()
            }
            return
        }
        super.checkFallDamage(y, onGround, state, pos)
    }

    override fun travel(travelVector: Vec3) {
        if (this.isOrderedToSit()) {
            this.travelSitting()
            return
        }
        val controller = this.controllingPassenger
        if (this.tamed && this.isVehicle() && controller is Player && this.isControlledByLocalInstance()) {
            this.travelControlled(controller)
            return
        }
        super.travel(travelVector)
    }

    private fun travelSitting() {
        if (!this.isControlledByLocalInstance) {
            this.calculateEntityAnimation(false)
            return
        }
        var motion = this.deltaMovement
        if (!this.isNoGravity) {
            motion = motion.add(0.0, -0.08, 0.0)
        }
        motion = Vec3(motion.x * 0.12, motion.y, motion.z * 0.12)
        this.move(MoverType.SELF, motion)
        this.deltaMovement = if (this.onGround()) {
            Vec3.ZERO
        } else {
            motion.multiply(0.91, 0.98, 0.91)
        }
        this.calculateEntityAnimation(false)
    }

    private fun travelControlled(pilot: Player) {
        val forwardInput = pilot.zza
        val strafeInput = pilot.xxa
        val (ascending, descending) = PilotInputAccess.read(
            this.level().isClientSide,
            this.pilotAscending,
            this.pilotDescending,
        )
        val wantedY = when {
            ascending && !descending -> 1.0
            descending && !ascending -> -1.0
            else -> 0.0
        }
        val reversing = forwardInput < -1.0E-4f
        val flyingForward = forwardInput > 1.0E-4f
        this.hoverBlend = PhantomHover.step(this.hoverBlend, flyingForward)
        val attitude = PhantomFlightAttitude.pose(
            forwardInput.toDouble(),
            strafeInput.toDouble(),
            wantedY * 0.4,
            this.hoverBlend,
        )
        this.bankTarget = attitude.bank
        this.headYawTarget = attitude.headYaw

        this.yRot = pilot.yRot
        this.yRotO = this.yRot
        this.xRot = attitude.pitch
        this.setRot(this.yRot, this.xRot)
        this.yBodyRot = this.yRot
        this.yHeadRot = this.yBodyRot

        val yawRad = Math.toRadians(this.yRot.toDouble())
        val sin = Math.sin(yawRad)
        val cos = Math.cos(yawRad)

        val forwardX = -sin * forwardInput
        val forwardZ = cos * forwardInput
        val strafeX = cos * strafeInput
        val strafeZ = sin * strafeInput

        var wantedX = forwardX + strafeX
        var wantedZ = forwardZ + strafeZ
        val horizontalLenSq = wantedX * wantedX + wantedZ * wantedZ
        if (horizontalLenSq > 1.0) {
            val norm = 1.0 / sqrt(horizontalLenSq)
            wantedX *= norm
            wantedZ *= norm
        }

        var speed = ModConfig.FLIGHT_SPEED_BLOCKS_PER_TICK *
            PhantomFlightPace.pace(this).toDouble() *
            PhantomFlightPace.waterScale(this.isUnderWater)
        if (reversing) speed *= PhantomHover.REVERSE_SPEED
        if (wantedY < 0.0 && !this.crawlLock) {
            this.diveEnergy = (this.diveEnergy + 0.016).coerceAtMost(1.0)
        } else {
            this.diveEnergy = (this.diveEnergy - 0.04).coerceAtLeast(0.0)
        }
        val dive = 1.0 + this.diveEnergy * 0.9
        val diveVertical = 1.0 + this.diveEnergy * 0.45
        val vSpeed = speed * ModConfig.VERTICAL_SPEED_FACTOR * diveVertical
        val targetVelocity = Vec3(wantedX * speed * dive, wantedY * vSpeed, wantedZ * speed * dive)

        this.deltaMovement = this.deltaMovement.lerp(targetVelocity, ModConfig.FLIGHT_ACCELERATION)
        this.move(MoverType.SELF, this.deltaMovement)
    }
}
