package net.thesilkminer.mc.ematter.common.feature.transmutator

import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemHandlerHelper
import net.thesilkminer.kotlin.commons.lang.reloadableLazy
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Consumer
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.tag.blockTagType
import net.thesilkminer.mc.boson.prefab.tag.isInTag
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.Items
import net.thesilkminer.mc.ematter.common.mole.*
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationInventory
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe
import net.thesilkminer.mc.ematter.common.temperature.TemperatureTables
import net.thesilkminer.mc.ematter.common.temperature.createTemperatureContext
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("experimental_api_usage", "experimental_override", "experimental_unsigned_literals")
class TransmutatorTileEntity : TileEntity(), ITickable, Holder, Consumer {

    internal companion object {

        private val MAX_CAPACITY = 248_139UL

        private const val WASTE_TIME = 20
        private const val PRODUCE_TIME = 100

        private const val NBT_POWER_KEY = "power"
        private const val NBT_MOLES_KEY = "moles"
        private const val NBT_OUTPUT_KEY = "output"
    }

    private val heaters by lazy { bosonApi.tagRegistry[blockTagType, NameSpacedString(MOD_ID, "heaters/molecular_transmutator")] }

    private lateinit var heatSource: Block

    private val wastePercentage = reloadableLazy { (1.0 / TemperatureTables[heatSource](this.world.createTemperatureContext(this.pos))).coerceAtMost(1.0) }

    private var recalculationNeeded = true

    private val inventory = TransmutationInventory(ItemStack.EMPTY)
    private var output = null as TransmutationRecipe?

    private val powerNeeded get() = this.output?.power ?: 0UL
    private val molesNeeded get() = this.output?.moles ?: 0

    private val handler = MoleHandler()
    private val moles = IntArray(4)

    private var wasteTimer = WASTE_TIME
    private var produceTimer = PRODUCE_TIME

    private var powerStored = 0UL

    // ITickable >>
    override fun update() {
        if (this.world.isRemote) return
        if (this.recalculationNeeded) this.recalculateData()
        this.waste()
        this.produce()
    }

    private fun waste() {
        if (--this.wasteTimer > 0) return
        this.wasteTimer = WASTE_TIME

        // store the amount which got removed at each slot
        val removed = IntArray(3)

        this.moles.asSequence().mapIndexed { i, it ->
            (it * this.wastePercentage.value).roundToInt().let { remove ->
                if (i < 3) removed[i] = remove
                it - remove
            }
        }.forEachIndexed(this.moles::set)

        // add the removed to the next slot
        removed.forEachIndexed { i, add -> this.moles[i + 1] += add }
    }

    private fun produce() {
        if (--this.produceTimer > 0) return
        this.produceTimer = PRODUCE_TIME

        this.moles[0] += this.handler.moles
        this.handler.moles = 0

        if (this.output == null) return

        when {
            this.powerStored < this.powerNeeded -> this.performClear()
            this.moles.reduce(Int::plus) < this.molesNeeded -> this.performFail()
            else -> this.performSuccess()
        }
    }

    private fun performClear() {
        this.powerStored = 0UL
        this.moles.asSequence().map { 0 }.forEachIndexed(this.moles::set)
        this.output(ItemStack(Items.waste.get(), Random.Default.nextInt(2)))
    }

    private fun performFail() {
        this.powerStored -= this.powerNeeded
        this.moles.asSequence().map { 0 }.forEachIndexed(this.moles::set)
        this.output(ItemStack(Items.waste.get(), Random.Default.nextInt(1, 4)))
    }

    private fun performSuccess() {
        this.powerStored -= this.powerNeeded

        // remove the needed moles starting with the last element in the array
        var needed = this.molesNeeded
        this.moles.reversed().asSequence().map { moles ->
            if (moles - needed < 0) (0).also { needed -= moles }
            else (moles - needed).also { needed = 0 }
        }.toList().reversed().forEachIndexed(this.moles::set)

        this.output(this.output?.getCraftingResult(this.inventory) ?: ItemStack.EMPTY)
    }

    private fun output(stack: ItemStack) {
        if (stack.isEmpty) return

        this.world.getTileEntity(this.pos.down())?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)?.let {
            val tmp = ItemHandlerHelper.insertItem(it, stack, false)
            if (tmp.isEmpty) return

            stack.count = tmp.count // set the count to whatever is left to output
        }
        this.world.spawnEntity(EntityItem(this.world, this.pos.x.toDouble(), this.pos.y.toDouble() + 1, this.pos.z.toDouble(), stack))
    }
    // << ITickable

    // Consumer >>
    override val storedPower: ULong get() = this.powerStored
    override val maximumCapacity: ULong get() = MAX_CAPACITY

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
    // << Consumer

    // capability handling >>
    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.UP) return true
        if (capability == consumerCapability && this.isPowerInput(facing)) return true
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.UP) return this.handler.uncheckedCast()
        if (capability == consumerCapability && this.isPowerInput(facing)) return this.uncheckedCast()
        return super.getCapability(capability, facing)
    }

    private fun isPowerInput(facing: EnumFacing?): Boolean = when (facing) {
        null, EnumFacing.UP, EnumFacing.DOWN, this.getFacing(), this.getFacing().opposite -> false
        else -> true
    }
    // << capability handling

    // nbt handling >>
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound =
        super.writeToNBT(compound).apply {
            this.setLong(NBT_POWER_KEY, this@TransmutatorTileEntity.powerStored.toLong())
            this.setIntArray(NBT_MOLES_KEY, this@TransmutatorTileEntity.moles)
            this@TransmutatorTileEntity.output?.let {
                this.setTag(NBT_OUTPUT_KEY, it.getCraftingResult(this@TransmutatorTileEntity.inventory).serializeNBT())
            }
        }

    override fun readFromNBT(compound: NBTTagCompound) {
        this.powerStored = compound.getLong(NBT_POWER_KEY).toULong()
        compound.getIntArray(NBT_MOLES_KEY).forEachIndexed { i, moles -> this.moles[i] = moles }
        this.changeOutput(if (compound.hasKey(NBT_OUTPUT_KEY)) ItemStack(compound.getCompoundTag(NBT_OUTPUT_KEY)) else ItemStack.EMPTY)
        super.readFromNBT(compound)
    }
    // << nbt handling

    private fun recalculateData() {
        // recalculate the heat source and the waste percentage
        this.getFacing().let { facing ->
            this.world.getBlockState(this.pos.offset(facing)).let { heatSource ->
                this.heatSource = if (heatSource isInTag this.heaters) heatSource.block else Blocks.AIR
            }
        }
        this.wastePercentage.reload()

        this.recalculationNeeded = false
    }

    private fun getFacing(): EnumFacing = this.world.getBlockState(this.pos).let { state ->
        if (state.block !is TransmutatorBlock) EnumFacing.NORTH else state.getValue(TransmutatorBlock.FACING)
    }

    internal fun changeOutput(stack: ItemStack) = Unit.also {
        this.inventory.result = stack
        this.output = ForgeRegistries.RECIPES.entries.map { it.value }.find { it.matches(this.inventory, this.world) } as? TransmutationRecipe
    }

    internal fun requestRecalculation() = Unit.also { this.recalculationNeeded = true }

    private inline fun <T> andNotify(block: () -> T) = block().also { this.markDirty() }.also { this.notifyUpdate() }
    private fun notifyUpdate() = this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, Constants.BlockFlags.DEFAULT) }
}
