package net.thesilkminer.mc.ematter.common.feature.transmutator

import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.ItemStackHandler
import net.thesilkminer.kotlin.commons.lang.reloadableLazy
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Consumer
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.tag.blockTagType
import net.thesilkminer.mc.boson.prefab.tag.isInTag
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.Items
import net.thesilkminer.mc.ematter.common.mole.MoleHolder
import net.thesilkminer.mc.ematter.common.mole.MoleState
import net.thesilkminer.mc.ematter.common.temperature.TemperatureContext
import net.thesilkminer.mc.ematter.common.temperature.TemperatureTables
import kotlin.random.Random

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE", "EXPERIMENTAL_UNSIGNED_LITERALS")
class TransmutatorTileEntity : TileEntity(), ITickable, Holder, Consumer {
    internal companion object {
        private val MAX_CAPACITY = 248_139UL

        private const val MOLE_TEST: Int = 10
        private const val ENERGY_TEST: ULong = 1_000UL

        private const val WASTE_TIME: Int = 20
        private const val PRODUCE_TIME: Int = 100

        private const val NBT_POWER_KEY = "power"
        private const val NBT_INPUTS_KEY = "inputs"
        private const val NBT_OUTPUT_KEY = "output"
    }

    private val heaters by lazy { bosonApi.tagRegistry[blockTagType, NameSpacedString(MOD_ID, "heaters/molecular_transmutator")] }

    private var heatSource = Blocks.AIR
    private var currentDayMoment: TemperatureContext.DayMoment = TemperatureContext.DayMoment.DAY
    private val notWastePercentage = reloadableLazy { (TemperatureTables[heatSource](TemperatureContext(this.world, this.pos, this.currentDayMoment)) / 2000).coerceAtMost(1) }
    private var recalculationNeeded: Boolean = false

    private var inputList: List<MoleHolder> = listOf()
    private var overallMoles: Int = 0
    private var selectedOutput: ItemStack = ItemStack.EMPTY
    private val stackHandler: ItemStackHandler = object : ItemStackHandler(1) {
        override fun onContentsChanged(slot: Int) = this@TransmutatorTileEntity.markDirty()
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = ItemStack.EMPTY.also {
            for (i in 0 until stack.count) {
                this@TransmutatorTileEntity.inputList.plus(MoleHolder(MOLE_TEST))
                this@TransmutatorTileEntity.overallMoles += MOLE_TEST
            }
        }
        override fun setStackInSlot(slot: Int, stack: ItemStack) = Unit.also { this.insertItem(slot, stack, false) }
    }

    private var wasteTimer: Int = WASTE_TIME
    private var produceTimer: Int = PRODUCE_TIME

    private var powerStored: ULong = 0UL

    override val storedPower: ULong get() = this.powerStored
    override val maximumCapacity: ULong get() = MAX_CAPACITY

    override fun update() {
        if (this.world.isRemote) return
        if (this.recalculationNeeded) this.recalculateData()
        this.waste()
        this.produce()
    }

    private fun waste() {
        if (--this.wasteTimer > 0) return
        this.wasteTimer = WASTE_TIME
        this.inputList
                .onEach {
                    if (it.state == MoleState.HEATING) return@onEach
                    if (Random.Default.nextFloat() <= this.notWastePercentage.value) return@onEach
                    var moleCount: Int = it.moleCount
                    it.nextState()
                    this.overallMoles -= (moleCount + it.moleCount)
                }
                .filterNot { it.state == MoleState.WASTE_L3 }
    }

    private fun produce() {
        if (--this.produceTimer > 0) return
        this.produceTimer = PRODUCE_TIME
        when {
            this.powerStored < ENERGY_TEST -> {
                this.powerStored = 0UL
                this.overallMoles = 0
                this.inputList = listOf()
                this.output(ItemStack(Items.waste.get(), 1))
            }
            this.overallMoles < MOLE_TEST -> {
                this.powerStored -= ENERGY_TEST
                this.overallMoles = 0
                this.inputList = listOf()
                this.output(ItemStack(Items.waste.get(), 1))
            }
            else -> {
                this.powerStored -= ENERGY_TEST
                this.overallMoles -= MOLE_TEST
                for (i in 0 until MOLE_TEST) {
                    this.inputList.first().let {
                        it.reduceMoleCount()
                        if (it.moleCount == 0) this.inputList.minus(it)
                    }
                }
                this.output(selectedOutput)
            }
        }
    }

    private fun output(stack: ItemStack) {
        var toOutput = stack
        this.world.getTileEntity(this.pos.down())?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP).let {
            if (it != null) toOutput = ItemHandlerHelper.insertItem(it, toOutput, false)
            if (toOutput == ItemStack.EMPTY) return
        }
        this.world.spawnEntity(EntityItem(this.world, this.pos.x.toDouble(), this.pos.y.toDouble() + 1, this.pos.z.toDouble(), toOutput))
    }

    override fun tryAccept(power: ULong, from: Direction): ULong = this.andNotify {
        if (from != Direction.UP) return@andNotify 0UL
        val after = this.powerStored + power
        if (after <= MAX_CAPACITY) {
            this.powerStored = after
            return@andNotify power
        }
        val diff = after - MAX_CAPACITY
        this.powerStored = MAX_CAPACITY
        return@andNotify power - diff
    }

    private fun recalculateData() {
        this.recalculationNeeded = false
        this.currentDayMoment = TemperatureContext.DayMoment[this.world.worldTime]
        this.getFacing().let { facing ->
            if (facing == null) return
            this.world.getBlockState(this.pos.offset(facing)).let { heatSource ->
                this.heatSource = if (heatSource isInTag this.heaters) heatSource.block else Blocks.AIR
            }
        }
        this.notWastePercentage.reload()
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && this.isItemInput(facing)) return true
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && this.isItemInput(facing)) return this.stackHandler.uncheckedCast()
        return super.getCapability(capability, facing)
    }

    private fun isItemInput(facing: EnumFacing?): Boolean = when (facing) {
        null, EnumFacing.UP, EnumFacing.DOWN, this.getFacing(), this.getFacing()?.opposite -> false
        else -> true
    }

    private fun getFacing(): EnumFacing? = this.world.getBlockState(this.pos).let { state ->
        state.block.let { block ->
            if (block !is TransmutatorBlock) return null
            block.getFacing(state)
        }
    }

    override fun serializeNBT(): NBTTagCompound {
        return super.serializeNBT().apply {
            this.setLong(NBT_POWER_KEY, this@TransmutatorTileEntity.powerStored.toLong())
            this.setTag(NBT_INPUTS_KEY, this@TransmutatorTileEntity.serializeInputsNBT())
            this.setTag(NBT_OUTPUT_KEY, this@TransmutatorTileEntity.selectedOutput.serializeNBT())
        }
    }

    private fun serializeInputsNBT(): NBTTagCompound {
        return NBTTagCompound().apply {
            this.setInteger("size", this@TransmutatorTileEntity.inputList.size)
            for (i in this@TransmutatorTileEntity.inputList.indices) {
                this.setTag(i.toString(), this@TransmutatorTileEntity.inputList[i].serializeNBT())
            }
        }
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        this.powerStored = nbt.getLong(NBT_POWER_KEY).toULong()
        this.deserializeInputsNBT(nbt)
        this.selectedOutput.deserializeNBT(nbt.getCompoundTag(NBT_OUTPUT_KEY))
    }

    private fun deserializeInputsNBT(nbt: NBTTagCompound) {
        for (i in 0 until nbt.getInteger("size")) {
            this.inputList.plus(MoleHolder(nbt.getCompoundTag(i.toString())))
        }
    }

    internal fun changeOutput(playerIn: EntityPlayer, hand: EnumHand) = Unit.also { this.selectedOutput = playerIn.getHeldItem(hand).copy().apply { this.count = 1 } }
    internal fun requestRecalculation() = Unit.also { this.recalculationNeeded = true }

    private inline fun <T> andNotify(block: () -> T) = block().also { this.markDirty() }.also { this.notifyUpdate() }
    private fun notifyUpdate() = this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, Constants.BlockFlags.DEFAULT) }
}
