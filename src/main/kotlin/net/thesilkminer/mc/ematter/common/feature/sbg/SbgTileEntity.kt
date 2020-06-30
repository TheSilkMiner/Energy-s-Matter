package net.thesilkminer.mc.ematter.common.feature.sbg

import net.minecraft.block.Block
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
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.holderCapability
import net.thesilkminer.mc.boson.prefab.energy.producerCapability
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress( "EXPERIMENTAL_API_USAGE" , "EXPERIMENTAL_OVERRIDE" , "EXPERIMENTAL_UNSIGNED_LITERALS" )
class SbgTileEntity: TileEntity(), Producer, Holder, ITickable {

    internal companion object {
        private const val ROOM_TEMPERATURE: Int = 295
        private const val MAX_CAPACITY: ULong = 3_141uL //same storage the basic mad has

        private const val TRANSFER_RATE: ULong = 3uL

        //TODO move this into a config file
        private const val POWER_MULTIPLIER: Double = 1.0

        private const val THRESHOLD_TIME: UInt = 60u
        private const val BURST_TIME: UInt = 40u

        //nbt keys
        private const val POWER_KEY = "power"
        private const val THRESHOLD_KEY = "threshold_time"
        private const val PRODUCING_BURST_KEY = "producing_time"
        private const val PUSHING_BURST_KEY = "pushing_time"
    }

    //temperature fields
    private var tempDifference: UInt = 0u
    private var effectiveness: Double = 0.0

    //timer fields
    private var threshold: UInt = THRESHOLD_TIME
    private var producingBurst: UInt = BURST_TIME
    private var pushingBurst: UInt = BURST_TIME

    //power fields
    private var powerProduced: ULong = 0uL
    private var powerStored: ULong = 0uL

    override val producedPower: ULong get() = this.powerProduced
    override val storedPower: ULong get() = this.powerStored
    override val maximumCapacity: ULong get() = MAX_CAPACITY

    /**
     * Tries every tick to generate power (Ampere). Only does so if this is a server instance, the [tempDifference] is not 0
     * and the [threshold] is at 0.
     */
    override fun update() {
        if (this.world.isRemote) return

        this.generatePower()
        this.pushPower()
    }

    /**
     * Generates Ampere based on [tempDifference], [effectiveness] and the [POWER_MULTIPLIER].
     * Ampere gets generated every [BURST_TIME] but in an amount that equals generation every tick.
     */
    private fun generatePower() {
        if (this.effectiveness > 0.0) {
            if (this.threshold > 0u) --this.threshold
            else if (this.storedPower < MAX_CAPACITY) {
                val nextPowerProduced = this.tempDifference.toDouble() * 0.001 * this.effectiveness * POWER_MULTIPLIER

                //is outside the (below) if statement to always show the correct value and not only update every two seconds
                this.powerProduced = nextPowerProduced.roundToInt().toULong()

                if (--this.producingBurst == 0u) {
                    //if an increase of the stored power by the nextPowerProduced value would be greater than the max capacity
                    //the storedPower gets set to the max value
                    ((nextPowerProduced * BURST_TIME.toDouble()).roundToInt().toULong()).let {
                        if (this.powerStored + it > MAX_CAPACITY) {
                            this.powerStored = MAX_CAPACITY
                            this.powerProduced = 0uL
                        } else this.powerStored += it
                    }
                    this.producingBurst = BURST_TIME
                }
            }
        }
    }

    /**
     * Tries to push power to a (on the top located) consumer. Pushed every [BURST_TIME] but in an amount that equals
     * pushing every tick. How much gets pushed is based on [TRANSFER_RATE]
     */
    private fun pushPower() {
        if (--this.pushingBurst == 0u) {
            if (this.storedPower >= TRANSFER_RATE * BURST_TIME) {
                this.world.getTileEntity(this.pos.up())?.getCapability(consumerCapability, EnumFacing.DOWN)?.let {
                    this.powerStored -= it.tryAccept(TRANSFER_RATE * BURST_TIME, Direction.DOWN)
                }
            }
            this.pushingBurst = BURST_TIME
        }
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        this.powerStored = compound.getLong(POWER_KEY).toULong()
        this.threshold = compound.getInteger(THRESHOLD_KEY).toUInt()
        this.producingBurst = compound.getInteger(PRODUCING_BURST_KEY).toUInt()
        this.pushingBurst = compound.getInteger(PUSHING_BURST_KEY).toUInt()
        super.readFromNBT(compound)
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        compound.setLong(POWER_KEY, this.powerStored.toLong())
        compound.setInteger(THRESHOLD_KEY, this.threshold.toInt())
        compound.setInteger(PRODUCING_BURST_KEY, this.producingBurst.toInt())
        compound.setInteger(PUSHING_BURST_KEY, this.pushingBurst.toInt())
        return super.writeToNBT(compound)
    }

    /**
     * To (re)calculate [tempDifference] and [effectiveness] on block placement or on chunk loading.
     */
    override fun onLoad() {
        if (this.world.isRemote) return
        this.recalculateNeighbors()
    }

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

    /**
     * Recalculates the [tempDifference] and the [effectiveness]. Should be called every time a neighbor changes or the block
     * gets (first) loaded.
     */
    internal fun recalculateNeighbors() {
        var heatSources = 0
        var coolants = 0

        //cursed code by Silk
        this.tempDifference = Direction.values()
                .asSequence()
                .minus(Direction.UP)
                .map { this.getNeighborAtDir(it) }
                .filter { it != Blocks.AIR }
                .map { (TemperatureSources.values().find { source -> it == source.block }?.value ?: ROOM_TEMPERATURE) }
                .map { it - ROOM_TEMPERATURE }
                .filterNot { it == 0 }
                .onEach { if (it > 0) ++heatSources else ++coolants }
                .map { abs(it).toUInt() }
                .plus(0u)
                .reduce { acc, it -> acc + it }

        //for every non cooled heat source the effectiveness reduces; if there are 5 heat sources or coolants the effectiveness is 0
        this.effectiveness = 1.0 - 0.2 * (heatSources - coolants * 2).let { if (it >= 0) it else if (coolants == 5) 5 else 0 }
        if (this.effectiveness == 0.0) {
            this.powerProduced = 0uL
            this.producingBurst = BURST_TIME
        }

        //every time a block gets changed the sbg needs a short amount of time to restart
        this.threshold = THRESHOLD_TIME
    }

    /**
     * Returns the neighbor block at the given direction.
     * @param direction the direction of the neighbor
     */
    private fun getNeighborAtDir(direction: Direction): Block = when (direction) {
            Direction.NORTH -> this.pos.north()
            Direction.EAST -> this.pos.east()
            Direction.SOUTH -> this.pos.south()
            Direction.WEST -> this.pos.west()
            Direction.UP -> this.pos.up()
            Direction.DOWN -> this.pos.down()
        }.let {
            this.world.getBlockState(it).block
        }

}
