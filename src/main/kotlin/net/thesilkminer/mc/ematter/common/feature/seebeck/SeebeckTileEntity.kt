package net.thesilkminer.mc.ematter.common.feature.seebeck

import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.api.energy.Producer
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.holderCapability
import net.thesilkminer.mc.boson.prefab.energy.producerCapability
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE", "EXPERIMENTAL_UNSIGNED_LITERALS")
internal class SeebeckTileEntity : TileEntity(), Producer, Holder, ITickable {
    private companion object {
        private const val ROOM_TEMPERATURE = 295
        private const val MAX_CAPACITY = 3_141UL //same storage the basic mad has
        // TODO("See if the actual capacity should be amped up by a small margin")

        private const val TRANSFER_RATE = 3L
        private const val SEEBECK_CONVERSION_DATA = 0.001

        // TODO("move this into a config file")
        private const val POWER_MULTIPLIER = 1.0
        private const val WARM_UP_TIME = 60
        private const val BURST_TIME = 40

        private const val NBT_POWER_KEY = "power"
        private const val NBT_THRESHOLD_KEY = "threshold_time"
        private const val NBT_PRODUCING_BURST_KEY = "producing_time"
        private const val NBT_PUSHING_BURST_KEY = "pushing_time"
    }

    private var tempDifference = 0U
    private var effectiveness = 0.0
    private var recalculationNeeded = false

    private var warmUp = WARM_UP_TIME
    private var producingBurst = BURST_TIME
    private var pushingBurst = BURST_TIME

    private var powerProduced = 0UL
    private var powerStored = 0UL

    override val producedPower get() = this.powerProduced
    override val storedPower get() = this.powerStored
    override val maximumCapacity get() = MAX_CAPACITY

    override fun update() {
        if (this.world.isRemote) return
        if (this.recalculationNeeded) return this.recalculateNeighbors() // We waste one tick on the recalculation: this is intended

        this.generatePower()
        this.pushPower()
    }

    private fun generatePower() {
        // Power is generated every burst time, in an amount that matches the tick rate
        // The formula is based on temperature difference, effectiveness, and power multiplier

        // No need to generate power if we are not even effective
        if (this.effectiveness <= 0.0) return

        // Warmup Time
        if (this.warmUp > 0) {
            --this.warmUp
            return
        }

        // No need to generate power if we have nowhere to store it: we should get rid of the one we have at the moment
        // first
        if (this.storedPower >= MAX_CAPACITY) return

        val nextPower = this.tempDifference.toDouble() * SEEBECK_CONVERSION_DATA * this.effectiveness * POWER_MULTIPLIER
        this.powerProduced = nextPower.roundToInt().toULong() // This way the value is always updated regardless of burst

        --this.producingBurst
        if (this.producingBurst > 0) return

        (nextPower * BURST_TIME.toDouble()).roundToInt().toULong().let {
            if (this.powerStored + it > MAX_CAPACITY) {
                this.powerStored = MAX_CAPACITY
                this.powerProduced = 0UL
            } else {
                this.powerStored += it
            }
        }

        this.producingBurst = BURST_TIME
    }

    private fun pushPower() {
        // Pushes every burst time the same amount of power that it would be pushed every tick according to transfer rate
        --this.pushingBurst
        if (this.pushingBurst > 0) return

        val powerToPush = (TRANSFER_RATE * BURST_TIME.toLong()).toULong()
        if (this.storedPower < powerToPush) return

        this.world.getTileEntity(this.pos.up())?.getCapability(consumerCapability, Direction.DOWN.toFacing())?.let {
            this.powerStored -= it.tryAccept(powerToPush, Direction.DOWN)
        }
        this.pushingBurst = BURST_TIME
    }

    // TODO("Do we really need to save the producing and pushing burst values?")
    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.powerStored = compound.getLong(NBT_POWER_KEY).toULong()
        this.warmUp = compound.getInteger(NBT_THRESHOLD_KEY)
        this.producingBurst = compound.getInteger(NBT_PRODUCING_BURST_KEY)
        this.pushingBurst = compound.getInteger(NBT_PUSHING_BURST_KEY)
    }

    // TODO("Do we really need to save the producing and pushing burst values?")
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setLong(NBT_POWER_KEY, this.powerStored.toLong())
        compound.setInteger(NBT_THRESHOLD_KEY, this.warmUp)
        compound.setInteger(NBT_PRODUCING_BURST_KEY, this.producingBurst)
        compound.setInteger(NBT_PUSHING_BURST_KEY, this.pushingBurst)
        return compound
    }

    // Accessing the world on 'onLoad' may be dangerous in new MC versions: this should make porting easier
    override fun onLoad() = if (!this.world.isRemote) this.requestRecalculation() else Unit

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == producerCapability && (facing == EnumFacing.UP || facing == null)) return true
        if (capability == holderCapability && facing == null) return true
        return super.hasCapability(capability, facing)
    }

    override fun <T: Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == producerCapability && (facing == EnumFacing.UP || facing == null)) return this.uncheckedCast()
        if (capability == holderCapability && facing == null) return this.uncheckedCast()
        return super.getCapability(capability, facing)
    }

    internal fun requestRecalculation() {
        this.recalculationNeeded = true
    }

    /**
     * Recalculates the [tempDifference] and the [effectiveness]. Should be called every time a neighbor changes or the block
     * gets (first) loaded.
     */
    private fun recalculateNeighbors() {
        var heatSources = 0
        var coolants = 0

        this.tempDifference = Direction.values()
                .asSequence()
                .minus(Direction.UP)
                .map { this.findNeighborIn(it) }
                .filter { it.block != Blocks.AIR }
                .map { (TemperatureSources.values().find { source -> it.block == source.block }?.value ?: ROOM_TEMPERATURE) }
                .map { it - ROOM_TEMPERATURE }
                .filterNot { it == 0 }
                .onEach { if (it > 0) ++heatSources else ++coolants }
                .map { abs(it).toUInt() }
                .plus(0u)
                .reduce { acc, it -> acc + it }

        // For every non cooled heat source the effectiveness reduces; if there are 5 heat sources or coolants the effectiveness is 0
        this.effectiveness = 1.0 - 0.2 * (heatSources - coolants * 2).let { if (it >= 0) it else if (coolants == 5) 5 else 0 }
        if (this.effectiveness == 0.0) {
            this.powerProduced = 0uL
            this.producingBurst = BURST_TIME
        }

        // Every time a block gets changed the sbg needs a short amount of time to restart
        this.warmUp = WARM_UP_TIME
        this.recalculationNeeded = false
    }

    private fun findNeighborIn(direction: Direction) = this.world.getBlockState(this.pos.offset(direction.toFacing()))
}
