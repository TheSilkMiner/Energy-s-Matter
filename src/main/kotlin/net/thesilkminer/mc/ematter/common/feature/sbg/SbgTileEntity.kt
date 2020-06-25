package net.thesilkminer.mc.ematter.common.feature.sbg

import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.api.energy.Producer
import net.thesilkminer.mc.boson.prefab.energy.holderCapability
import net.thesilkminer.mc.boson.prefab.energy.producerCapability
import net.thesilkminer.mc.ematter.common.shared.TemperatureSources
import kotlin.math.roundToInt

@Suppress( "EXPERIMENTAL_API_USAGE" , "EXPERIMENTAL_OVERRIDE" , "EXPERIMENTAL_UNSIGNED_LITERALS" )
class SbgTileEntity : TileEntity(), Producer, Holder, ITickable {

    internal companion object {
        private const val ROOM_TEMPERATURE: Int = 295
        //that's the storage of the basic mad
        private const val MAX_CAPACITY: ULong = 3_141uL

        //next one is here in case you want to make this changeable through a config file in the future
        private const val POWER_MULTIPLIER: Double = 1.0

        private const val THRESHOLD_TIME: UInt = 60u
        private const val BURST_TIME: UInt = 40u

        private const val POWER_KEY = "power"
        private const val THRESHOLD_KEY = "threshold_time"
        private const val BURST_KEY = "burst_time"
    }

    //temperature fields
    private var tempDifference: UInt = 0u
    private var effectiveness: Double = 1.0

    //timer fields
    private var threshold: UInt = THRESHOLD_TIME
    private var burstTimer: UInt = BURST_TIME

    //power fields
    private var powerProduced: ULong = 0uL
    private var powerStored: ULong = 0uL

    override val producedPower: ULong get() = this.powerProduced
    override val storedPower: ULong get() = this.powerStored
    override val maximumCapacity: ULong get() = MAX_CAPACITY

    /**
     * Tries every tick to generate power. Only does so if this is a server instance, the [tempDifference] is not 0
     * and the [threshold] is at 0.
     */
    override fun update() {
        if( !super.world.isRemote ) {
            if( this.tempDifference != 0u ) {
                //if the threshold is greater than 0 it gets reduced and no power will be generated
                if( this.threshold > 0u ) {
                    --this.threshold
                }
                else {
                    if( this.storedPower < MAX_CAPACITY ) {
                        this.generatePower()
                    }
                }
            }
        }
    }

    /**
     * Generates Ampere based on [tempDifference], [effectiveness] and the [POWER_MULTIPLIER].
     * Ampere gets generated every [BURST_TIME] but in an amount that equals generation every tick.
     */
    private fun generatePower() {
        val nextPowerProduced: Double = this.tempDifference.toDouble() * 0.005 * this.effectiveness * POWER_MULTIPLIER

        //is outside the (below) if statement to always show the correct value and not only update every two seconds
        //if you think that's inefficient we can move everything in this fun inside the if statement
        this.powerProduced = nextPowerProduced.roundToInt().toULong()
        if( --this.burstTimer == 0u ) {
            if( this.powerStored + nextPowerProduced.roundToInt().toULong() * BURST_TIME >= MAX_CAPACITY ) {
                this.powerStored += MAX_CAPACITY - this.powerStored
                this.powerProduced = 0uL
            }
            else {
                this.powerStored += nextPowerProduced.roundToInt().toULong() * BURST_TIME
            }

            this.burstTimer = BURST_TIME
        }

    }

    override fun readFromNBT( compound: NBTTagCompound ) {
        this.powerStored = compound.getLong( POWER_KEY ).toULong()
        this.threshold = compound.getInteger( THRESHOLD_KEY ).toUInt()
        this.burstTimer = compound.getInteger( BURST_KEY ).toUInt()
        super.readFromNBT( compound )
    }

    override fun writeToNBT( compound: NBTTagCompound ): NBTTagCompound {
        compound.setLong( POWER_KEY , this.powerStored.toLong() )
        compound.setInteger( THRESHOLD_KEY , this.threshold.toInt() )
        compound.setInteger( BURST_KEY , this.burstTimer.toInt() )
        return super.writeToNBT( compound )
    }

    /**
     * To (re)calculate [tempDifference] and [effectiveness] on block placement or on chunk loading
     */
    override fun onLoad() {
        if( !super.world.isRemote ) {
            this.recalculateNeighbors()
        }
    }

    override fun hasCapability( capability: Capability<*> , facing: EnumFacing? ): Boolean {
        if( capability == producerCapability && ( facing == EnumFacing.UP || facing == null ) ) return true
        if( capability == holderCapability && facing == null ) return true
        return super.hasCapability( capability , facing )
    }

    override fun <T : Any?> getCapability( capability: Capability<T> , facing: EnumFacing? ): T? {
        if( capability == producerCapability && ( facing == EnumFacing.UP || facing == null ) ) return this.uncheckedCast()
        if( capability == holderCapability && facing == null ) return this.uncheckedCast()
        return super.getCapability( capability , facing )
    }

    /**
     * Recalculates the [tempDifference] and the [effectiveness]. Should be called every time a neighbor changes or the block
     * gets (first) loaded.
     */
    internal fun recalculateNeighbors() {
        this.effectiveness = 1.0
        this.tempDifference = 0u

        var heatSources = 0
        var coolants = 0

        for( direction in Direction.values() ) {
            if( direction != Direction.UP ) {
                val neighbor: Block = this.getNeighborAtDir( direction )
                for( temperatureSource in TemperatureSources.values() ) {
                    if( neighbor == temperatureSource.block ) {
                        val value = temperatureSource.value - ROOM_TEMPERATURE
                        //if the value is positive it must be a heat source, if it's negative it must be a coolant
                        if( value > 0 ) {
                            this.tempDifference += value.toUInt()
                            heatSources++
                        }
                        else if( value < 0 ) {
                            this.tempDifference += ( value * -1 ).toUInt()
                            coolants++
                        }
                        break
                    }
                }
            }
        }
        if( heatSources == 5 || coolants == 5 ) {
            this.tempDifference = 0u
            this.burstTimer = BURST_TIME
            this.powerProduced = 0u
        }
        else {
            if( heatSources > coolants * 2 ) {
                for( i in 1..( heatSources - coolants * 2 ) ) {
                    this.effectiveness -= 0.2
                }
            }
        }
        this.threshold = THRESHOLD_TIME
    }

    /**
     * Returns the neighbor block at the given direction.
     * @param direction the direction of the neighbor
     */
    private fun getNeighborAtDir( direction: Direction ): Block {
        return when( direction ) {
            Direction.NORTH -> super.pos.north()
            Direction.EAST -> super.pos.east()
            Direction.SOUTH -> super.pos.south()
            Direction.WEST -> super.pos.west()
            Direction.UP -> super.pos.up()
            Direction.DOWN -> super.pos.down()
        }.let {
            super.world.getBlockState( it ).block
        }
    }

}
