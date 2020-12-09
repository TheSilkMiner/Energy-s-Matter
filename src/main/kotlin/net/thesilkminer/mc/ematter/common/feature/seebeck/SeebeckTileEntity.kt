/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

package net.thesilkminer.mc.ematter.common.feature.seebeck

import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.thesilkminer.kotlin.commons.lang.reloadableLazy
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.api.energy.Producer
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.holderCapability
import net.thesilkminer.mc.boson.prefab.energy.producerCapability
import net.thesilkminer.mc.boson.prefab.tag.blockTagType
import net.thesilkminer.mc.boson.prefab.tag.isInTag
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.temperature.TemperatureContext
import net.thesilkminer.mc.ematter.common.temperature.TemperatureTables
import net.thesilkminer.mc.ematter.common.temperature.createTemperatureContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE", "EXPERIMENTAL_UNSIGNED_LITERALS")
internal class SeebeckTileEntity : TileEntity(), Producer, Holder, ITickable {

    private companion object {

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

    private val airTemperature = reloadableLazy { TemperatureTables[Blocks.AIR](this.world.createTemperatureContext(this.pos)) }
    private val sources by lazy { bosonApi.tagRegistry[blockTagType, NameSpacedString(MOD_ID, "seebeck_generator_sources")] }

    private var tempDifference = 0U
    private var effectiveness = 0.0
    private var recalculationNeeded = false

    private var warmUp = WARM_UP_TIME
    private var producingBurst = BURST_TIME
    private var pushingBurst = BURST_TIME

    private var powerProduced = 0UL
    private var powerStored = 0UL

    private var currentDayMoment: TemperatureContext.DayMoment = TemperatureContext.DayMoment.DAY

    override val producedPower get() = this.powerProduced
    override val storedPower get() = this.powerStored
    override val maximumCapacity get() = MAX_CAPACITY

    override fun update() {
        if (this.world.isRemote) return
        if (!this.recalculationNeeded) this.queryWorldTime() // By querying the world time, we check whether we need to recalculate something or not
        if (this.recalculationNeeded) return this.recalculateData() // We waste one tick on the recalculation: this is intended

        this.generatePower()
        this.pushPower()
    }

    private fun queryWorldTime() = if (this.currentDayMoment != TemperatureContext.DayMoment[this.world.worldTime]) this.requestRecalculation() else Unit

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

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
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
    private fun recalculateData() {
        var heatSources = 0
        var coolants = 0

        val previousDifference = this.tempDifference
        val previousEffectiveness = this.effectiveness

        // Set the day moment to the correct one
        this.currentDayMoment = TemperatureContext.DayMoment[this.world.worldTime]

        // Run air recalculation prior to usage, otherwise this is pretty stupid
        this.airTemperature.reload()

        this.tempDifference = Direction.values()
            .asSequence()
            .minus(Direction.UP)
            .map { this.pos.offset(it.toFacing()) }
            .map { this.world.getBlockState(it) to this.world.createTemperatureContext(it) }
            .map { it.first.let { state -> if (state.isRecognized()) state.block else Blocks.AIR } to it.second }
            .map { TemperatureTables[it.first](it.second) }
            .map { it - this.airTemperature.value }
            .filterNot { it == 0 }
            .onEach { if (it > 0) ++heatSources else ++coolants }
            .map { abs(it).toUInt() }
            .plus(0U)
            .reduce { acc, it -> acc + it }

        // For every non cooled heat source the effectiveness reduces; if there are 5 heat sources or coolants the effectiveness is 0
        this.effectiveness = 1.0 - 0.2 * (heatSources - coolants * 2).let { if (it >= 0) it else if (coolants == 5) 5 else 0 }
        if (this.effectiveness == 0.0) {
            this.powerProduced = 0UL
            this.producingBurst = BURST_TIME
        }

        // Every time a block gets changed the sbg needs a short amount of time to restart
        // It's also true that the time of day changing really shouldn't affect this
        // Therefore we consider it a block change if the effectiveness or the temperature difference has changed
        // The latter in any case, the second only if it's major (>= 15 K)
        // Only in such a case would the warm-up be reset
        val hasEffectivenessChanged = abs(this.effectiveness - previousEffectiveness) <= 0.0001
        val hasTemperatureChanged = abs(previousDifference.toLong() - this.tempDifference.toLong()) < 15L
        if (hasEffectivenessChanged || hasTemperatureChanged) this.warmUp = WARM_UP_TIME

        // Disable the need for a recalculation
        this.recalculationNeeded = false
    }

    private fun IBlockState.isRecognized() = this isInTag this@SeebeckTileEntity.sources
}
