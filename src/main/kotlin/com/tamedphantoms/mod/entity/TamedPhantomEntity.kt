package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.api.TamedPhantomsApi
import com.tamedphantoms.mod.config.ModConfig
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.event.PhantomInteractionHandler
import com.tamedphantoms.mod.util.PhantomFlightModel
import com.tamedphantoms.mod.util.PhantomManeuvers
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.item.DyeColor
import org.joml.Vector3f
import com.tamedphantoms.mod.entity.ai.TamedPhantomBodyControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomDefendGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomFollowOwnerGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomHeldItemLookGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLeashWanderGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLookAtPlayerGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomLookControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomMoveControl
import com.tamedphantoms.mod.entity.ai.TamedPhantomRefuseGoal
import com.tamedphantoms.mod.entity.ai.TamedPhantomWanderGoal
import com.tamedphantoms.mod.input.PilotInputAccess
import com.tamedphantoms.mod.network.PhantomMoonPulsePayload
import com.tamedphantoms.mod.util.PhantomAcrobatics
import com.tamedphantoms.mod.util.PhantomAngerLogic
import com.tamedphantoms.mod.util.PhantomPack
import com.tamedphantoms.mod.util.PhantomFlightPace
import com.tamedphantoms.mod.event.ModAdvancements
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.util.PhantomCrawl
import com.tamedphantoms.mod.util.PhantomEffectSpeed
import com.tamedphantoms.mod.util.PhantomFlightAttitude
import com.tamedphantoms.mod.util.PhantomScream
import com.tamedphantoms.mod.util.PhantomGroundSkim
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHeldLook
import com.tamedphantoms.mod.util.PhantomHover
import com.tamedphantoms.mod.util.PhantomShake
import com.tamedphantoms.mod.util.PhantomWetness
import com.tamedphantoms.mod.util.PhantomWingbeat
import com.tamedphantoms.mod.util.PhantomAngerState
import com.tamedphantoms.mod.util.PhantomSeatAssignment
import com.tamedphantoms.mod.util.PhantomTamingLogic
import com.tamedphantoms.mod.util.PhantomTreats
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import com.tamedphantoms.mod.platform.ModNetwork
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
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootTable
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
        private val DATA_TAKEOFF: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_BODY_DYE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_SADDLE_DYE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_GLOW: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_SHAKE_STYLE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)
        private val DATA_TUCK: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_EFFORT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.FLOAT)
        private val DATA_MANEUVER: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(TamedPhantomEntity::class.java, EntityDataSerializers.INT)

        /** Краска не нанесена. */
        const val NO_DYE = -1

        /** Отряхивание: брызги воды. Неотрицательное значение — брызги этого цвета RGB. */
        const val SHAKE_WATER = -1

        /** Отряхивание без брызг. */
        const val SHAKE_DRY = -2

        /** Отряхивание искрами лавы. */
        const val SHAKE_LAVA = -3

        var clientAfterTick: ((TamedPhantomEntity) -> Unit)? = null

        private const val TAG_TAMED = "Tamed"
        private const val TAG_OWNER = "Owner"
        private const val TAG_SITTING = "Sitting"
        private const val TAG_SADDLED = "Saddled"
        private const val TAG_WET_TRACKED = "WetTracked"
        private const val TAG_IN_WATER = "InWater"
        private const val TAG_IN_RAIN = "InRain"
        private const val TAG_SCREAM_COOLDOWN = "ScreamCooldown"
        private const val TAG_IMMORTAL = "Immortal"
        private const val TAG_BODY_DYE = "BodyDye"
        private const val TAG_SADDLE_DYE = "SaddleDye"
        private const val TAG_GLOW = "Glow"

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

        /** Тангаж корпуса, который уходит в обычный `xRot`. Остальное — фигура в [acroPitch]. */
        private const val BODY_PITCH_LIMIT = 40f

        /** Доля разницы между курсом и взглядом седока, которую фантом проходит за тик. */
        private const val TURN_FOLLOW = 0.5f

        /** Самый быстрый поворот верхом, градусы за тик. */
        private const val MAX_TURN_STEP = 16f

        /** Шанс за секунду, что освобождённый фантом крикнет на моба рядом, и пауза после этого. */
        private const val PLAY_SCREAM_CHANCE = 0.08f
        private const val PLAY_SCREAM_RANGE = 12.0
        private const val PLAY_SCREAM_PAUSE = 600
    }

    init {
        this.moveControl = TamedPhantomMoveControl(this)
        this.lookControl = TamedPhantomLookControl(this)
        this.setPhantomSize(PET_PHANTOM_SIZE)
    }

    private var angerState: PhantomAngerState = PhantomAngerState.NONE

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
        builder.define(DATA_TAKEOFF, 0)
        builder.define(DATA_BODY_DYE, NO_DYE)
        builder.define(DATA_SADDLE_DYE, NO_DYE)
        builder.define(DATA_GLOW, false)
        builder.define(DATA_SHAKE_STYLE, SHAKE_WATER)
        builder.define(DATA_TUCK, 0f)
        builder.define(DATA_EFFORT, 0f)
        builder.define(DATA_MANEUVER, 0)
    }

    /** Цвет краски тела: id [net.minecraft.world.item.DyeColor] или [NO_DYE]. Глаза не красятся. */
    var bodyDye: Int
        get() = this.entityData.get(DATA_BODY_DYE)
        set(value) = this.entityData.set(DATA_BODY_DYE, value.coerceIn(NO_DYE, 15))

    /** Цвет краски седла, отдельно от тела. */
    var saddleDye: Int
        get() = this.entityData.get(DATA_SADDLE_DYE)
        set(value) = this.entityData.set(DATA_SADDLE_DYE, value.coerceIn(NO_DYE, 15))

    /** Светится как источник света: рисуется в полную яркость, а свет даёт аддон. */
    var luminous: Boolean
        get() = this.entityData.get(DATA_GLOW)
        set(value) = this.entityData.set(DATA_GLOW, value)

    private val shakeStyle: Int
        get() = this.entityData.get(DATA_SHAKE_STYLE)

    /** Запустить отряхивание. [style] — [SHAKE_WATER], [SHAKE_DRY] или цвет брызг RGB. */
    fun startShake(style: Int, sound: Boolean = true) {
        this.entityData.set(DATA_SHAKE_STYLE, style)
        this.shakeTicks = PhantomShake.LENGTH
        if (sound) {
            val shakeSound = if (style == SHAKE_LAVA) SoundEvents.LAVA_EXTINGUISH else SoundEvents.PLAYER_SPLASH
            this.playSound(shakeSound, 0.65f, 1.2f)
        }
    }

    /** Злится на игрока: облако гнева, рык и самозащита на обычное время. */
    fun provokeBy(player: Player) {
        this.startDefending(player.uuid)
        this.spawnAngryParticles()
        this.playSound(SoundEvents.PHANTOM_HURT, 1.0f, 0.55f + this.random.nextFloat() * 0.1f)
    }

    /** Зелье мгновенного исцеления злит только этого фантома, без стаи. */
    fun scorn(player: Player) {
        this.defend(player.uuid)
        this.spawnAngryParticles()
        this.playSound(SoundEvents.PHANTOM_HURT, 1.0f, 0.55f + this.random.nextFloat() * 0.1f)
    }

    /** Огнестойкость фантому не нужна: то же зелье даёт сопротивление урону. */
    override fun addEffect(effectInstance: MobEffectInstance, entity: Entity?): Boolean {
        return super.addEffect(PhantomTreats.forPhantom(effectInstance), entity)
    }

    /** Ответ на крик хозяина. Новый круг ответов не поднимает. */
    fun answerScream() {
        val level = this.level() as? ServerLevel ?: return
        if (!this.isAlive || !this.tamed) return
        this.beginScream(level, redEyes = true, blindness = true, awardOwner = null)
    }

    /** Взмах крыльев в этом кадре виден у пилота: тугость сложенных крыльев и сила взмаха. */
    var flightTuck: Float = 0f
    var flightTuckO: Float = 0f
    var flightEffort: Float = 0f
    var flightEffortO: Float = 0f

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
    private var wasInLava: Boolean = false

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

    /** Размах шага у земли: 0 — стоит, крылья в покое. */
    var crawlStride: Float = 0f
    var crawlStrideO: Float = 0f

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
    private var riderTakeoffHold: Int = 0
    private var takeoffRamp: Double = 1.0
    private var takeoffArmed: Boolean = false
    private var crawlPlant: Int = 0
    private var lastFeedTick: Int = -100
    var cameraPitch: Float = 0f
    var cameraPitchO: Float = 0f
    var cameraRoll: Float = 0f
    var cameraRollO: Float = 0f
    var refuseVisual: Int = 0
    var nodVisual: Int = 0

    /** Куда смотреть головой, пока идёт мотание или кивок. Считает клиентский тик. */
    var offerAimYaw: Float = 0f
    var offerAimPitch: Float = 0f

    var takeoffHoldTicks: Int
        get() = this.entityData.get(DATA_TAKEOFF)
        set(value) = this.entityData.set(DATA_TAKEOFF, value)

    fun crawlVisual(partial: Float): Float =
        Mth.lerp(partial, this.wingCrawlO, this.wingCrawl).coerceIn(0f, 1f)

    fun bankVisual(partial: Float): Float =
        Mth.lerp(partial, this.bankO, this.bank) * (1f - this.crawlVisual(partial))

    /** Лишний тангаж сверх позы полёта. Копится, пока держат набор или снижение. */
    var acroPitch: Float = 0f
    var acroPitchO: Float = 0f
    private var loopProgress: Float = 0f

    /** Летит вперёд или вбок: камера и модель могут крутиться дальше фиксированного наклона. */
    var acrobatic: Boolean = false

    var immortal: Boolean
        get() = this.entityData.get(DATA_IMMORTAL)
        set(value) {
            val was = this.entityData.get(DATA_IMMORTAL)
            this.entityData.set(DATA_IMMORTAL, value)
            if (value) {
                this.setGlowingTag(true)
            } else if (was) {
                this.setGlowingTag(false)
            }
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

    /** Курс модели в кадре: тот же, что берёт рендерер фантома. */
    fun renderedBodyYaw(partial: Float): Float = Mth.rotLerp(partial, this.yBodyRotO, this.yBodyRot)

    /**
     * Тангаж модели в градусах: ванильный [PhantomRenderer] крутит `xRot`,
     * а свой рендерер добавляет интерполяцию и акробатику. Камера смотрит
     * только [ridePitchVisual], седло должно следовать за всей моделью.
     */
    fun renderedBodyPitch(partial: Float): Float {
        val crawl = this.crawlVisual(partial)
        val shown = Mth.lerp(partial, this.xRotO, this.xRot) * (1f - crawl)
        val acro = Mth.lerp(partial, this.acroPitchO, this.acroPitch) * (1f - crawl)
        return if (this.isVehicle || crawl > 0f) shown + acro else this.xRot + acro
    }

    /** Куда сейчас смотреть. Выставляют цели взгляда и самообороны, крутит [TamedPhantomLookControl]. */
    var glanceTarget: LivingEntity? = null

    /** Пока фантом замер на еду или отказ, другие цели взгляда не перебивают [glanceTarget]. */
    var offeringLock: Boolean = false

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
        if (changed && !this.level().isClientSide) {
            val owner = this.findOwnerPlayer()
            if (owner != null) PhantomOwnerRecall.remember(owner, this)
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

    /**
     * Поза, которую посчитала модель полёта у пилота. Сервер раздаёт её остальным
     * и по ней же копит угол петли: так [tryOwnerLoop] проверяет не только слово клиента.
     */
    fun setPilotAttitude(pitch: Float, roll: Float, tuck: Float, effort: Float, acrobatic: Boolean) {
        val total = PhantomFlightModel.wrap(this.xRot + pitch)
        val turned = PhantomFlightModel.wrap(total - this.reportedPitch)
        this.reportedPitch = total
        this.loopProgress = if (acrobatic && kotlin.math.abs(turned) < 45f) this.loopProgress + turned else 0f
        if (kotlin.math.abs(total) < 3f && kotlin.math.abs(this.loopProgress) < 60f) this.loopProgress = 0f
        this.entityData.set(DATA_ACRO_PITCH, PhantomFlightModel.wrap(pitch))
        this.entityData.set(DATA_ACRO_ROLL, PhantomFlightModel.wrap(roll))
        this.entityData.set(DATA_ACROBATIC, acrobatic)
        this.entityData.set(DATA_TUCK, tuck.coerceIn(0f, 1f))
        this.entityData.set(DATA_EFFORT, effort.coerceIn(0f, 1f))
    }

    private var reportedPitch: Float = 0f

    /** Клиент пилота: круиз и двойные нажатия, их выставляет обработчик клавиш. */
    var clientCruise: Boolean = false
    var clientLoopRequest: Boolean = false
    var clientRollRequest: Int = 0

    private val flightModel = PhantomFlightModel()
    private var lastPilotYaw: Float = Float.NaN
    private var cruiseSpeedUsed: Double = ModConfig.FLIGHT_SPEED_BLOCKS_PER_TICK

    /** Воздушная скорость в долях крейсерской: у пилота из модели, у остальных по смещению. */
    var airspeedRatio: Float = 0f
        private set

    /** 0..1 — крылья сложены назад, как у сокола в пике. */
    var wingSweep: Float = 0f
    var wingSweepO: Float = 0f

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
        tag.putInt(TAG_BODY_DYE, this.bodyDye)
        tag.putInt(TAG_SADDLE_DYE, this.saddleDye)
        tag.putBoolean(TAG_GLOW, this.luminous)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        this.tamed = tag.getBoolean(TAG_TAMED)
        this.setOrderedToSit(tag.getBoolean(TAG_SITTING), playEffects = false)
        this.saddled = tag.getBoolean(TAG_SADDLED)
        this.ownerUUID = if (tag.hasUUID(TAG_OWNER)) tag.getUUID(TAG_OWNER) else null
        this.applyConfiguredSize()
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
        this.bodyDye = if (tag.contains(TAG_BODY_DYE)) tag.getInt(TAG_BODY_DYE) else NO_DYE
        this.saddleDye = if (tag.contains(TAG_SADDLE_DYE)) tag.getInt(TAG_SADDLE_DYE) else NO_DYE
        this.luminous = tag.getBoolean(TAG_GLOW)
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        if (key == DATA_MANEUVER) {
            this.maneuverStart = this.tickCount
        }
        this.applyPetStats()
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnType: MobSpawnType,
        spawnGroupData: SpawnGroupData?,
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData)
        this.applyConfiguredSize()
        this.applyPetStats()
        return data
    }

    private fun applyConfiguredSize() {
        val size = ServerConfig.phantomSize().coerceIn(0, 64)
        if (this.phantomSize != size) {
            this.setPhantomSize(size)
        }
    }

    private fun applyPetStats() {
        this.getAttribute(Attributes.MAX_HEALTH)?.baseValue = PET_MAX_HEALTH
        this.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = PET_ATTACK_DAMAGE
    }

    override fun isSunBurnTick(): Boolean = false

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
        this.spawnAngryParticles()
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
        this.goalSelector.addGoal(2, TamedPhantomRefuseGoal(this))
        this.goalSelector.addGoal(3, TamedPhantomFollowOwnerGoal(this))
        this.goalSelector.addGoal(4, TamedPhantomLeashWanderGoal(this))
        this.goalSelector.addGoal(5, TamedPhantomWanderGoal(this))
        this.goalSelector.addGoal(6, TamedPhantomHeldItemLookGoal(this))
        this.goalSelector.addGoal(7, TamedPhantomLookAtPlayerGoal(this))
    }

    override fun canAttackType(type: EntityType<*>): Boolean = this.angerState.isActive

    override fun canAttack(target: LivingEntity): Boolean = target === this.getDefendTarget()

    override fun shouldDespawnInPeaceful(): Boolean = false

    override fun isPersistenceRequired(): Boolean = true

    // =====================================================================
    // Урон / самозащита
    // =====================================================================

    /** Злится на обидчика. Если это игрок, злится и стая ([PhantomPack]): только она, дальше по цепочке не зовёт. */
    fun startDefending(attackerId: UUID) {
        this.defend(attackerId)
        val level = this.level() as? ServerLevel ?: return
        if (level.getEntity(attackerId) !is Player) return
        val mates = if (this.tamed) {
            level.getEntities(ModEntities.TAMED_PHANTOM.get()) { it !== this }
        } else {
            level.getEntitiesOfClass(TamedPhantomEntity::class.java, this.boundingBox.inflate(PhantomPack.RELEASED_RADIUS)) { it !== this }
        }
        for (mate in mates) {
            if (mate.isAlive && PhantomPack.joins(this.tamed, this.ownerUUID, mate.tamed, mate.ownerUUID, mate.distanceToSqr(this))) {
                mate.defend(attackerId)
            }
        }
    }

    private fun defend(attackerId: UUID) {
        this.setOrderedToSit(false, playEffects = false)
        this.angerState = PhantomAngerLogic.onHurtBy(attackerId, ServerConfig.defendDurationTicks())
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
        TamedPhantomsApi.released(this, previousOwner)
        this.dropSaddleIfPresent()
        this.playTameSound()
        this.spawnAngryParticles()
    }

    fun healWithFood(nutrition: Int): Boolean {
        if (this.tickCount == this.lastFeedTick) return false
        if (!PhantomTamingLogic.canHeal(this.health, this.maxHealth)) return false
        val healAmount = PhantomTamingLogic.healAmount(nutrition, this.health, this.maxHealth)
        if (healAmount <= 0f) return false
        this.lastFeedTick = this.tickCount
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
        this.saddleDye = NO_DYE
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

    fun rejectOffering(from: Player) {
        if (!this.isVehicle) {
            this.glanceTarget = from
            val yaw = PhantomHeadLook.yawDegrees(this.x, this.z, from.x, from.z)
            this.yRot = yaw
            this.yBodyRot = yaw
            this.yHeadRot = yaw
        }
        this.spawnAngryParticles()
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

    /**
     * Ваниль выбирает место по порядку посадки. Хозяин, севший вторым, оказался бы сзади,
     * поэтому переднее место всегда за хозяином, заднее — за пассажиром.
     */
    override fun getPassengerAttachmentPoint(passenger: Entity, dimensions: EntityDimensions, partialTick: Float): Vec3 {
        val seat = if (passenger is Player && this.isOwnedBy(passenger)) {
            PhantomSeatAssignment.PILOT_SEAT
        } else {
            PhantomSeatAssignment.PASSENGER_SEAT
        }
        return dimensions.attachments().getClamped(EntityAttachment.PASSENGER, seat, this.yRot)
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        if (!this.level().isClientSide) {
            PhantomInteractionHandler.rememberHit(player, this, this.position().add(vec))
        }
        return super.interactAt(player, vec, hand)
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
            this.crawlStrideO = this.crawlStride
            this.cameraPitchO = this.cameraPitch
            this.cameraRollO = this.cameraRoll
            this.flightTuckO = this.flightTuck
            this.flightEffortO = this.flightEffort
            this.wingSweepO = this.wingSweep
            this.normalizeTurns()
            if (this.isVehicle && !this.isControlledByLocalInstance) {
                this.acroPitch = unwrapNear(this.acroPitch, this.entityData.get(DATA_ACRO_PITCH))
                this.bank = unwrapNear(this.bank, this.entityData.get(DATA_ACRO_ROLL))
                this.acrobatic = this.entityData.get(DATA_ACROBATIC)
                this.flightTuck = this.entityData.get(DATA_TUCK)
                this.flightEffort = this.entityData.get(DATA_EFFORT)
            }
        }
        val yawBefore = this.yRot
        if (this.isOrderedToSit()) {
            this.xRot = 0.0f
        }

        super.tick()

        if (this.level().isClientSide) {
            if (!(this.isControlledByLocalInstance && this.controllingPassenger is Player)) {
                this.flightModel.reset()
                this.lastPilotYaw = Float.NaN
                this.clientCruise = false
            }
            this.turnYawDelta = PhantomHeadLook.wrapDegrees(this.yRot - yawBefore)
            this.tickFlightVisuals()
            this.advanceWingPhase()
            this.spawnShakeDroplets()
            this.spawnSwimTrail()
            this.spawnCrawlFootprints()
            this.tickCameraFollow()
            this.tickRefuseVisual()
            clientAfterTick?.invoke(this)
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

        this.applyConfiguredSize()
        this.tickWetExit()
        this.tickLavaExit()
        this.tickRideHead()
        this.tickAngerTimer()
        this.tickScream(level)
        this.tickAcrobatics()
        if (this.takeoffHoldTicks > 0) {
            this.takeoffHoldTicks--
        }
        if (this.boltTicks > 0) {
            this.boltTicks--
        }
        if (this.immortal && !ServerConfig.immortalPhantoms()) {
            this.immortal = false
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
            this.startShake(SHAKE_WATER)
        }
        if (this.shakeTicks > 0) {
            this.shakeTicks--
        }
    }

    private fun tickLavaExit() {
        val lava = this.isInLava
        if (this.wasInLava && !lava) {
            this.startShake(SHAKE_LAVA)
        }
        this.wasInLava = lava
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
        val style = this.shakeStyle
        if (style == SHAKE_DRY) return
        val particle = when (style) {
            SHAKE_WATER -> ParticleTypes.SPLASH
            SHAKE_LAVA -> ParticleTypes.LAVA
            else -> dustOf(style, 1.1f)
        }
        val level = this.level()
        repeat(4) {
            val dx = (this.random.nextDouble() - 0.5) * 1.6
            val dz = (this.random.nextDouble() - 0.5) * 1.6
            level.addParticle(
                particle,
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
        val moved = hypot(this.x - this.xo, hypot(this.y - this.yo, this.z - this.zo))
        val turned = kotlin.math.abs(Mth.wrapDegrees(this.yRot - this.yRotO)) +
            kotlin.math.abs(this.xRot - this.xRotO)
        if (moved < 0.006 && turned < 0.4f) return
        val level = this.level()
        val motion = this.deltaMovement
        for (side in doubleArrayOf(-1.0, 1.0)) {
            val tip = this.wingTip(side, onGround = false)
            level.addParticle(
                ParticleTypes.BUBBLE,
                tip.x,
                tip.y,
                tip.z,
                -motion.x * 0.2,
                0.03,
                -motion.z * 0.2,
            )
            if (this.random.nextFloat() < 0.35f) {
                level.addParticle(ParticleTypes.BUBBLE_POP, tip.x, tip.y + 0.08, tip.z, 0.0, 0.02, 0.0)
            }
        }
    }

    private fun spawnCrawlFootprints() {
        if (this.wingCrawl < 0.55f || this.isInWater) return
        if (hypot(this.x - this.xo, this.z - this.zo) < 0.01) return
        val step = Mth.sin(this.crawlPhase)
        val plant = when {
            step > 0.78f -> 1
            step < -0.78f -> -1
            else -> 0
        }
        if (plant == 0) {
            this.crawlPlant = 0
            return
        }
        if (plant == this.crawlPlant) return
        this.crawlPlant = plant
        val tip = this.wingTip(if (plant > 0) -1.0 else 1.0, onGround = true)
        val level = this.level()
        var at = BlockPos.containing(tip.x, this.y - 0.02, tip.z)
        var state = level.getBlockState(at)
        if (state.getCollisionShape(level, at).isEmpty) {
            at = at.below()
            state = level.getBlockState(at)
        }
        if (state.getCollisionShape(level, at).isEmpty) return
        val particle = BlockParticleOption(ParticleTypes.BLOCK, state)
        repeat(4) {
            level.addParticle(
                particle,
                tip.x + (this.random.nextDouble() - 0.5) * 0.16,
                this.y + 0.08,
                tip.z + (this.random.nextDouble() - 0.5) * 0.16,
                (this.random.nextDouble() - 0.5) * 0.02,
                0.012,
                (this.random.nextDouble() - 0.5) * 0.02,
            )
        }
    }

    /** Кончик крыла: side −1 левый, +1 правый. */
    private fun wingTip(side: Double, onGround: Boolean): Vec3 {
        val yaw = this.yRot * Mth.DEG_TO_RAD
        val scale = 1.0 + 0.15 * this.phantomSize
        val reach = 1.35 * scale
        val rightX = Mth.cos(yaw).toDouble()
        val rightZ = Mth.sin(yaw).toDouble()
        val forwardX = -Mth.sin(yaw).toDouble()
        val forwardZ = Mth.cos(yaw).toDouble()
        val along = if (onGround) 0.15 * scale else 0.35 * scale
        val py = if (onGround) this.y + 0.06 else this.y + 0.34 * scale
        return Vec3(
            this.x + rightX * side * reach + forwardX * along,
            py,
            this.z + rightZ * side * reach + forwardZ * along,
        )
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
                if (this.skimAirTicks == 1 && this.wingTakeoffBlend < 0.25f) this.takeoffQueued = true
                if (this.skimAirTicks >= 5) this.skimLatched = false
            } else {
                this.skimAirTicks = 0
            }
        }
        return this.skimLatched
    }

    private fun flattenGroundAttitude() {
        this.acroPitch = settleTurn(this.acroPitch, 30f)
        this.loopProgress = 0f
        this.bank = settleTurn(this.bank, 30f)
        this.bankTarget = 0f
        this.headYawTarget = 0f
        this.acrobatic = false
    }

    /**
     * Разгон с земли примерно за 6 тиков: сразу отрыв, скорость до полной.
     * Возвращает долю целевой скорости, 1 — обычный полёт.
     */
    private fun surfaceTakeoffScale(wantedY: Double): Double {
        val skimming = (this.crawlLock || this.wingGroundBlend > 0.45f) && !this.isInWater
        val justArmed = wantedY > 0.0 && skimming && !this.takeoffArmed && this.riderTakeoffHold <= 0
        if (justArmed) {
            this.takeoffArmed = true
            this.takeoffRamp = 0.28
            this.riderTakeoffHold = 6
            this.takeoffQueued = true
        }
        if (wantedY <= 0.0) this.takeoffArmed = false
        val lifting = this.riderTakeoffHold > 0 && wantedY > 0.0
        if (this.riderTakeoffHold > 0) {
            this.riderTakeoffHold--
            if (lifting && !justArmed) {
                this.takeoffRamp = (this.takeoffRamp + 0.12).coerceAtMost(1.0)
            }
        } else {
            this.takeoffRamp = 1.0
        }
        return if (lifting) this.takeoffRamp else 1.0
    }

    /** 0..1 — сила встречного ветра для звука: верхом, на скорости выше крейсерской. */
    fun windStrength(): Float {
        if (!this.isVehicle || this.isInWater || this.isUnderWater || this.isOrderedToSit()) return 0f
        return ((this.airspeedRatio - 0.85f) / 1.1f).coerceIn(0f, 1f)
    }

    /** В воздухе: не сидит, не в воде и не ползёт по земле. */
    fun airborne(): Boolean = !this.isOrderedToSit() && !this.isInWater && !this.crawlLock

    fun beginSurfaceTakeoff() {
        if (this.takeoffHoldTicks > 0) return
        this.takeoffHoldTicks = 6
    }

    private fun tickCameraFollow() {
        // Набор и пикирование камера повторяет наполовину, фигуру — целиком и плотнее:
        // при отставании больше 180° камера крутилась бы обратно.
        val figure = this.flightModel.looping || this.flightModel.rolling ||
            kotlin.math.abs(PhantomHeadLook.wrapDegrees(this.acroPitch)) > 1f
        val share = if (figure) 1f else 0.65f
        val follow = if (figure) 0.45f else 0.12f
        val targetPitch = PhantomHeadLook.wrapDegrees(this.ridePitchVisual(1f)) * share
        val targetRoll = this.bankVisual(1f)
        this.cameraPitch += PhantomHeadLook.wrapDegrees(targetPitch - this.cameraPitch) * follow
        this.cameraRoll += PhantomHeadLook.wrapDegrees(targetRoll - this.cameraRoll) * follow
    }

    private fun tickRefuseVisual() {
        if (!this.tamed || this.isVehicle) {
            this.refuseVisual = 0
            this.nodVisual = 0
            return
        }
        val found = PhantomHeldLook.nearestOffer(
            this,
            this.ownerUUID,
            PhantomTamingLogic.canHeal(this.health, this.maxHealth),
        )
        val player = found?.player
        if (found == null || player == null) {
            this.refuseVisual = 0
            this.nodVisual = 0
            return
        }
        this.offerAimYaw = PhantomHeadLook.yawDegrees(this.x, this.z, player.x, player.z)
        this.offerAimPitch = PhantomHeadLook.pitchDegrees(
            this.x, this.eyeY, this.z, player.x, player.eyeY, player.z,
        )
        when (found.offering) {
            PhantomHeldLook.Offering.SHAKE -> {
                this.refuseVisual = 1
                this.nodVisual = 0
            }
            PhantomHeldLook.Offering.NOD -> {
                this.nodVisual = 1
                this.refuseVisual = 0
            }
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
            PhantomScream.echo(level, this, serverPlayer)
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
            this.beginScream(level, redEyes = false, blindness = false, awardOwner = null, moonPulse = true)
            return
        }
        if (this.playScreamPause > 0) {
            this.playScreamPause--
            return
        }
        // Освобождённый пугает мобов криком ради забавы: без слепоты, и потом надолго затихает.
        if (!this.tamed && !this.isVehicle && this.tickCount % 20 == 0 && this.random.nextFloat() < PLAY_SCREAM_CHANCE) {
            val prey = level.getEntitiesOfClass(Mob::class.java, this.boundingBox.inflate(PLAY_SCREAM_RANGE)) {
                it !is Phantom && it.isAlive
            }.minByOrNull { it.distanceToSqr(this) } ?: return
            this.glanceTarget = prey
            this.beginScream(level, redEyes = false, blindness = false, awardOwner = null)
            this.playScreamPause = PLAY_SCREAM_PAUSE + this.random.nextInt(PLAY_SCREAM_PAUSE)
        }
    }

    private var playScreamPause = PLAY_SCREAM_PAUSE

    private fun beginScream(level: ServerLevel, redEyes: Boolean, blindness: Boolean, awardOwner: ServerPlayer?, moonPulse: Boolean = false) {
        val seconds = ServerConfig.screamCooldownSeconds().coerceAtLeast(1)
        this.screamCooldown = seconds * 20
        this.screamTicks = PhantomScream.EFFECT_TICKS
        this.screamFears = blindness
        this.screamEyeTicks = if (redEyes) PhantomScream.EFFECT_TICKS else 0
        this.addEffect(MobEffectInstance(MobEffects.GLOWING, PhantomScream.EFFECT_TICKS, 0, false, false, false))
        PhantomScream.play(level, this)
        PhantomScream.note(level, this, blindness)
        PhantomScream.frighten(level, this, blindness)
        if (moonPulse) {
            ModNetwork.sendToDimension(level, PhantomMoonPulsePayload)
            for (listener in PhantomScream.listeners(level, this)) {
                ModAdvancements.grantSickMoon(listener)
            }
        }
        if (awardOwner != null) {
            ModAdvancements.grantWingedBeast(awardOwner)
        }
    }

    /** Верхом позу присылает пилот. Без всадника счёт петли пилота сбрасывается. */
    private fun tickAcrobatics() {
        if (this.isVehicle && this.controllingPassenger is Player) {
            if (this.maneuverKind() != null) this.endManeuver()
            return
        }
        this.loopProgress = 0f
        this.reportedPitch = 0f
    }

    private var maneuverSerial = 0
    private var maneuverStart = 0

    /** Скорость входа в фигуру. Известна только серверу, клиенту она не нужна. */
    var maneuverSpeed: Double = 0.4
        private set

    val maneuverSide: Float
        get() = PhantomManeuvers.side(this.entityData.get(DATA_MANEUVER))

    fun maneuverKind(): PhantomManeuvers.Kind? = PhantomManeuvers.decode(this.entityData.get(DATA_MANEUVER))

    /** Тики с начала фигуры по своим часам: у клиента и сервера разные счётчики тиков. */
    fun maneuverElapsed(): Int = this.tickCount - this.maneuverStart

    fun startManeuver(kind: PhantomManeuvers.Kind, speed: Double): Boolean {
        if (this.level().isClientSide || this.maneuverKind() != null) return false
        this.maneuverSerial++
        this.maneuverSpeed = speed
        this.entityData.set(DATA_MANEUVER, PhantomManeuvers.encode(kind, this.maneuverSerial))
        return true
    }

    fun endManeuver() {
        if (this.level().isClientSide) return
        this.entityData.set(DATA_MANEUVER, 0)
    }

    var loopReadyToReport: Boolean = false

    fun tryOwnerLoop(player: Player) {
        if (player.vehicle !== this || !this.isOwnedBy(player)) return
        if (kotlin.math.abs(this.loopProgress) < 270f) return
        this.loopProgress = 0f
        this.becomeImmortal()
    }

    private fun becomeImmortal() {
        if (!ServerConfig.immortalPhantoms()) return
        val first = !this.immortal
        this.immortal = true
        if (!first) return
        val rider = this.controllingPassenger as? ServerPlayer ?: return
        ModAdvancements.grantDeadLoop(rider)
    }

    private fun tickFlightVisuals() {
        this.crawlLock = this.updateSkim(PhantomGroundSkim.pressed(this))
        // Верхом позу у земли выравнивает модель полёта пилота. Второе выравнивание поверх неё дёргало посадку.
        if (this.crawlLock && !this.isVehicle) {
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
        if (!localRide) {
            val moved = sqrt(forward * forward + strafe * strafe + vertical * vertical)
            val ratio = (moved / this.cruiseReference()).toFloat()
            this.airspeedRatio += (ratio - this.airspeedRatio) * 0.25f
        }
        val free = !localRide && !remoteRide && !this.isOrderedToSit()
        val maneuver = this.maneuverKind()
        val elapsed = this.maneuverElapsed()
        if (free && PhantomManeuvers.active(maneuver, elapsed) && maneuver != null) {
            val pose = PhantomManeuvers.attitude(maneuver, elapsed, this.maneuverSide)
            this.hoverBlend = PhantomHover.step(this.hoverBlend, true)
            this.acroPitch = unwrapNear(this.acroPitch, pose.pitch)
            this.bank = unwrapNear(this.bank, pose.roll)
            this.bankTarget = this.bank
            this.headYawTarget = 0f
            this.flightTuck = pose.tuck
            this.flightEffort = pose.effort
        } else if (free) {
            val moving = !PhantomFlightAttitude.wantsHover(forward, strafe)
            this.hoverBlend = PhantomHover.step(this.hoverBlend, moving)
            val pose = PhantomFlightAttitude.pose(forward, strafe, vertical, this.hoverBlend)
            val turnBank = this.turnYawDelta * PhantomFlightModel.TURN_BANK * this.airspeedRatio.coerceIn(0f, 1.6f)
            this.bankTarget = (pose.bank + turnBank).coerceIn(-45f, 45f)
            this.headYawTarget = pose.headYaw
            this.acroPitch = settleTurn(this.acroPitch, 12f)
            val stooping = vertical < -0.3 && this.airspeedRatio > 1.1f
            this.flightTuck = PhantomFlightModel.approach(this.flightTuck, if (stooping) 1f else 0f, 0.08f)
            val work = when {
                this.hoverBlend > 0.5f -> 1f
                vertical > 0.06 -> 0.9f
                this.airspeedRatio < 0.4f -> 0.7f
                else -> 0.35f
            }
            this.flightEffort = PhantomFlightModel.approach(this.flightEffort, work * (1f - this.flightTuck), 0.12f)
        }
        if (!this.isVehicle || this.crawlLock) {
            this.headYawTarget = 0f
        }
        if (this.crawlLock) {
            this.bankTarget = 0f
        }
        if (!remoteRide && !localRide && !PhantomManeuvers.active(maneuver, elapsed)) {
            this.bank = settleTurn(this.bank, 12f)
            this.bank = PhantomHeadLook.approachDegrees(this.bank, this.bankTarget, 6f)
        }
        val yawTarget = this.headYawTarget.coerceIn(-PhantomFlightAttitude.HEAD_YAW, PhantomFlightAttitude.HEAD_YAW)
        this.headBankYaw = PhantomHeadLook.approachDegrees(this.headBankYaw, yawTarget, 3.5f)
            .coerceIn(-PhantomFlightAttitude.HEAD_YAW, PhantomFlightAttitude.HEAD_YAW)
        // Хвост как руль высоты: на выходе из пике и в петле опускается сильнее, чем от одного подъёма.
        val pull = PhantomHeadLook.wrapDegrees(this.acroPitch - this.acroPitchO) * 0.9f
        this.tailPitch = PhantomHeadLook.approachDegrees(
            this.tailPitch,
            (PhantomFlightAttitude.tailPitchDegrees(vertical) - pull).coerceIn(-30f, 30f),
            4.5f,
        )
        this.swimBlend = if (this.isUnderWater) {
            (this.swimBlend + 0.07f).coerceAtMost(1f)
        } else {
            (this.swimBlend - 0.05f).coerceAtLeast(0f)
        }
        val fastSweep = ((this.airspeedRatio - 1.2f) / 0.8f).coerceIn(0f, 1f) * 0.35f
        this.wingSweep = PhantomFlightModel.approach(this.wingSweep, maxOf(this.flightTuck, fastSweep) * (1f - this.swimBlend), 0.12f)
    }

    private fun cruiseReference(): Double =
        (ModConfig.FLIGHT_SPEED_BLOCKS_PER_TICK * PhantomFlightPace.pace(this) *
            PhantomFlightPace.waterScale(this.isUnderWater)).coerceAtLeast(0.05)

    /** Угол, близкий к прошлому: разворот через ±180 не крутит модель назад. */
    private fun unwrapNear(previous: Float, raw: Float): Float = previous + PhantomHeadLook.wrapDegrees(raw - previous)

    /** После фигуры угол доводится до ближайшего полного оборота, а не раскручивается обратно. */
    private fun settleTurn(value: Float, step: Float): Float {
        val target = Math.round(value / 360f) * 360f
        return PhantomFlightModel.approach(value, target, step)
    }

    /**
     * Полные обороты сдвигаются вместе с прошлым значением, интерполяция кадра не ломается.
     * Угол держится в ±180: после петли тангаж снова около нуля, а не 360.
     */
    private fun normalizeTurns() {
        fun shift(value: Float): Float = if (kotlin.math.abs(value) > 180f) Math.round(value / 360f) * 360f else 0f
        shift(this.acroPitch).let { this.acroPitch -= it; this.acroPitchO -= it }
        shift(this.bank).let { this.bank -= it; this.bankO -= it }
        shift(this.cameraPitch).let { this.cameraPitch -= it; this.cameraPitchO -= it }
        shift(this.cameraRoll).let { this.cameraRoll -= it; this.cameraRollO -= it }
    }

    private fun advanceWingPhase() {
        val vertical = this.y - this.yo
        val horizontal = hypot(this.x - this.xo, this.z - this.zo)
        val skimming = this.crawlLock
        val launch = (this.takeoffQueued && this.wingGroundBlend > 0.35f) ||
            this.riderTakeoffHold > 0 ||
            this.takeoffHoldTicks > 0
        this.takeoffQueued = false
        this.wingTouchedGround = skimming
        this.wingTakeoffBlend = PhantomWingbeat.stepTakeoff(this.wingTakeoffBlend, launch)
        this.wingGroundBlend = PhantomWingbeat.stepGround(this.wingGroundBlend, skimming)
        val hover = this.hoverBlend.coerceIn(0f, 1f)
        val descending = PhantomWingbeat.descending(vertical, hover, this.wingGroundBlend)
        this.wingGlideBlend = PhantomWingbeat.stepGlide(this.wingGlideBlend, descending)
        val motionPose = PhantomWingbeat.pose(
            vertical,
            horizontal,
            hover,
            this.wingGroundBlend,
            this.wingTakeoffBlend,
            this.wingGlideBlend,
            this.swimBlend,
        )
        val airborne = (1f - this.wingGroundBlend) * (1f - this.wingTakeoffBlend)
        val pose = PhantomWingbeat.effort(
            motionPose,
            this.flightEffort,
            this.flightTuck,
            airborne * (1f - this.swimBlend),
            swim = this.swimBlend,
            swimSpeed = this.airspeedRatio,
        )
        this.wingFlapRate = pose.rate
        this.wingFlapAmp = pose.amplitude
        this.wingDroop = pose.droop
        this.wingTipHang = pose.tipHang
        if (this.wingPhase.isNaN()) {
            this.wingPhase = (this.tickCount - 1).toFloat()
        }
        val previousPhase = this.wingPhase
        this.wingPhase += pose.rate
        this.wingCrawl = this.wingGroundBlend * (1f - this.wingTakeoffBlend)
        if (skimming) {
            val multiple = PhantomFlightPace.pace(this) * PhantomFlightPace.BASELINE
            this.crawlPhase += PhantomCrawl.advance(horizontal, multiple)
        }
        this.crawlStride = PhantomCrawl.stride(this.crawlStride, if (skimming) horizontal else 0.0)
        this.emitWingEffects(previousPhase, this.wingPhase, pose.amplitude)
    }

    /**
     * Звук взмаха и частицы с кончиков крыльев идут в такт той же фазе, что и модель.
     * Ванильный такт по номеру тика `PhantomSitFlapMixin` у этого фантома глушит.
     */
    private fun emitWingEffects(previousPhase: Float, phase: Float, amplitude: Float) {
        if (this.isOrderedToSit()) return
        val step = PhantomWingbeat.DEGREES_PER_TICK * Mth.DEG_TO_RAD
        val offset = this.uniqueFlapTickOffset.toFloat()
        val before = Mth.cos((offset + previousPhase) * step + Mth.PI)
        val now = Mth.cos((offset + phase) * step + Mth.PI)
        val level = this.level()
        if (before > 0f && now <= 0f && amplitude > 0.22f && !this.isSilent) {
            level.playLocalSound(
                this.x,
                this.y,
                this.z,
                SoundEvents.PHANTOM_FLAP,
                this.soundSource,
                0.95f * amplitude.coerceIn(0.35f, 1.25f),
                0.95f + this.random.nextFloat() * 0.05f,
                false,
            )
        }
        val size = this.phantomSize
        val yaw = this.yRot * Mth.DEG_TO_RAD
        val reachX = Mth.cos(yaw) * (1.3f + 0.21f * size)
        val reachZ = Mth.sin(yaw) * (1.3f + 0.21f * size)
        val height = (0.3f + now * 0.45f * amplitude.coerceIn(0f, 1.3f)) * (size * 0.2f + 1f)
        val particle = this.wingParticle()
        level.addParticle(particle, this.x + reachX, this.y + height, this.z + reachZ, 0.0, 0.0, 0.0)
        level.addParticle(particle, this.x - reachX, this.y + height, this.z - reachZ, 0.0, 0.0, 0.0)
    }

    /** Частицы крыльев: ванильный мицелий или пыль цвета краски тела. */
    fun wingParticle(): ParticleOptions {
        val dye = this.bodyDye
        if (dye < 0) return ParticleTypes.MYCELIUM
        return dustOf(DyeColor.byId(dye).textureDiffuseColor, 0.75f)
    }

    private fun dustOf(rgb: Int, scale: Float): ParticleOptions = DustParticleOptions(
        Vector3f(((rgb shr 16) and 0xFF) / 255f, ((rgb shr 8) and 0xFF) / 255f, (rgb and 0xFF) / 255f),
        scale,
    )

    private fun tickRiderNightVision() {
        for (passenger in this.passengers) {
            if (passenger is LivingEntity) {
                passenger.addEffect(
                    MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, true),
                )
            }
        }
    }

    override fun getDefaultLootTable(): ResourceKey<LootTable> = BuiltInLootTables.EMPTY

    override fun dropAllDeathLoot(level: ServerLevel, damageSource: DamageSource) {
        this.dropSaddleIfPresent()
        super.dropAllDeathLoot(level, damageSource)
    }

    override fun dropCustomDeathLoot(level: ServerLevel, damageSource: DamageSource, recentlyHit: Boolean) {
        val owner = this.findOwnerPlayer()
        if (owner != null) PhantomOwnerRecall.forget(owner, this.uuid)
        super.dropCustomDeathLoot(level, damageSource, recentlyHit)
        this.dropSaddleIfPresent()
        val count = 1 + this.random.nextInt(12)
        this.spawnAtLocation(ItemStack(Items.PHANTOM_MEMBRANE, count), 0.5f)
    }

    /** Снять пассажира, не проходя проверку «два Shift». Только для восстановления ссылки. */
    internal fun detachPassengerNow(passenger: Entity) {
        super.removePassenger(passenger)
    }

    override fun removePassenger(passenger: Entity) {
        if (PhantomDismount.bypassing()) {
            super.removePassenger(passenger)
            return
        }
        val player = passenger as? Player
        if (player == null) {
            super.removePassenger(passenger)
            return
        }
        val keepMounted = PhantomDismount.shouldKeepMounted(player, this)
        try {
            if (keepMounted) {
                PhantomDismount.restore(player, this)
                return
            }
            if (player is ServerPlayer) {
                PhantomDismount.allowClient(player)
            }
            super.removePassenger(passenger)
            val effect = player.getEffect(MobEffects.NIGHT_VISION)
            if (effect != null && effect.duration <= NIGHT_VISION_DURATION_TICKS && effect.amplifier == 0) {
                player.removeEffect(MobEffects.NIGHT_VISION)
            }
        } finally {
            PhantomDismount.finish()
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

    override fun positionRider(passenger: Entity, callback: Entity.MoveFunction) {
        super.positionRider(passenger, callback)
        if (this.isOrderedToSit()) return
        val offset = passenger.position().subtract(this.position())
        val previous = Vec3(this.xo, this.yo, this.zo).add(offset)
        passenger.xo = previous.x
        passenger.yo = previous.y
        passenger.zo = previous.z
        passenger.xOld = previous.x
        passenger.yOld = previous.y
        passenger.zOld = previous.z
    }

    /**
     * Насколько визуальное седло уезжает от точки крепления.
     * Модель крутится вокруг позиции сущности, а не вокруг точки над спиной:
     * при наборе скорости xRot уводит седло, и игрок, сидящий в точке крепления, отстаёт.
     * Нулевой тангаж и крен дают нулевой сдвиг.
     */
    fun seatVisualShift(passenger: Entity, partial: Float): Vec3 {
        val pitch = this.renderedBodyPitch(partial)
        val bank = this.bankVisual(partial)
        if (pitch == 0f && bank == 0f) return Vec3.ZERO
        val yaw = this.renderedBodyYaw(partial)
        val rad = (Math.PI / 180.0).toFloat()
        val local = passenger.position().subtract(this.position()).yRot(yaw * rad)
        val turned = local.zRot(-bank * rad).xRot(pitch * rad)
        return turned.subtract(local).yRot(-yaw * rad)
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
            val drop = if (this.hasEffect(MobEffects.SLOW_FALLING)) 0.01 else 0.08
            motion = motion.add(0.0, -drop, 0.0)
        }
        motion = Vec3(motion.x * 0.12, PhantomEffectSpeed.adjustVertical(this, motion.y), motion.z * 0.12)
        this.move(MoverType.SELF, motion)
        this.deltaMovement = if (this.onGround()) {
            Vec3.ZERO
        } else {
            motion.multiply(0.91, 0.98, 0.91)
        }
        this.calculateEntityAnimation(false)
    }

    private fun travelControlled(pilot: Player) {
        val client = this.level().isClientSide
        val hungry = this.hasEffect(MobEffects.HUNGER)
        val forwardInput = PhantomEffectSpeed.axis(if (client && this.clientCruise) 1f else pilot.zza, hungry)
        val strafeInput = PhantomEffectSpeed.axis(pilot.xxa, hungry)
        val (ascending, descending) = PilotInputAccess.read(
            client,
            this.pilotAscending,
            this.pilotDescending,
        )
        val wantedY = PhantomEffectSpeed.axis(
            when {
                ascending && !descending -> 1.0
                descending && !ascending -> -1.0
                else -> 0.0
            },
            hungry,
        )
        val lift = this.surfaceTakeoffScale(wantedY)

        // Курс догоняет взгляд седока плавно: рывки мыши за тик не дёргают корпус и крен.
        val previousYaw = if (this.lastPilotYaw.isNaN()) pilot.yRot else this.yRot
        val yawDelta = (PhantomHeadLook.wrapDegrees(pilot.yRot - previousYaw) * TURN_FOLLOW)
            .coerceIn(-MAX_TURN_STEP, MAX_TURN_STEP)
        this.lastPilotYaw = pilot.yRot
        this.yRot = previousYaw + yawDelta
        this.yRotO = this.yRot

        val cruiseSpeed = ModConfig.FLIGHT_SPEED_BLOCKS_PER_TICK *
            PhantomFlightPace.pace(this).toDouble() *
            PhantomFlightPace.waterScale(this.isUnderWater) *
            PhantomEffectSpeed.scale(this) *
            PhantomEffectSpeed.scale(pilot) *
            PhantomEffectSpeed.dolphin(this)
        this.cruiseSpeedUsed = cruiseSpeed
        val medium = when {
            this.isUnderWater -> PhantomFlightModel.Medium.WATER
            this.crawlLock -> PhantomFlightModel.Medium.GROUND
            else -> PhantomFlightModel.Medium.AIR
        }
        val out = this.flightModel.step(
            PhantomFlightModel.Input(
                forward = forwardInput,
                strafe = strafeInput,
                climb = wantedY.toFloat(),
                cruiseSpeed = cruiseSpeed,
                yawDelta = yawDelta,
                medium = medium,
                lift = lift,
                loopRequest = this.clientLoopRequest,
                rollRequest = this.clientRollRequest,
            ),
        )
        this.clientLoopRequest = false
        this.clientRollRequest = 0
        if (this.flightModel.completedLoop && this.isOwnedBy(pilot)) {
            this.loopReadyToReport = true
        }

        // Корпус идёт по траектории. Выше предела наклона угол уходит в фигуру, его рисует рендерер.
        this.hoverBlend = this.flightModel.hover
        val path = this.flightModel.pathPitch
        // Под водой фантом лежит ровно, как скат, без позы машущего на месте.
        val hoverPose = if (medium == PhantomFlightModel.Medium.WATER) 0f else this.hoverBlend
        val visual = path + (PhantomHover.pitchOffset(1f) - path) * hoverPose
        this.xRot = visual.coerceIn(-BODY_PITCH_LIMIT, BODY_PITCH_LIMIT)
        this.acroPitch = unwrapNear(this.acroPitch, visual - this.xRot)
        this.bank = unwrapNear(this.bank, this.flightModel.roll)
        this.bankTarget = this.bank
        this.headYawTarget = -strafeInput.coerceIn(-1f, 1f) * PhantomFlightAttitude.HEAD_YAW
        this.acrobatic = this.flightModel.looping || PhantomAcrobatics.allows(
            forwardInput.toDouble(),
            strafeInput.toDouble(),
            crawling = this.crawlLock,
            sitting = this.isOrderedToSit(),
            ridden = true,
        )
        this.flightTuck = this.flightModel.tuck
        this.flightEffort = this.flightModel.effort
        this.airspeedRatio = (kotlin.math.abs(this.flightModel.speed) / cruiseSpeed).toFloat()

        this.setRot(this.yRot, this.xRot)
        this.yBodyRot = this.yRot
        this.yHeadRot = this.yBodyRot

        val yawRad = Math.toRadians(this.yRot.toDouble())
        val sin = Math.sin(yawRad)
        val cos = Math.cos(yawRad)
        val target = Vec3(
            -sin * out.forward + cos * out.lateral,
            PhantomEffectSpeed.adjustVertical(this, out.vertical),
            cos * out.forward + sin * out.lateral,
        )
        // На большой скорости разворот вписывается дугой: скорость доворачивает за курсом не сразу.
        val grip = if (this.airspeedRatio > 1.4f) 0.3 else 0.55
        this.deltaMovement = this.deltaMovement.lerp(target, grip)
        this.move(MoverType.SELF, this.deltaMovement)
        if (this.horizontalCollision && medium == PhantomFlightModel.Medium.AIR && this.airspeedRatio > 0.6f) {
            this.flightModel.onCollision()
        }
    }
}
