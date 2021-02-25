package net.thesilkminer.mc.ematter.common.feature.anvil

import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.common.recipe.anvil.AnvilRecipe
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper
import net.thesilkminer.mc.ematter.common.shared.sync
import net.thesilkminer.mc.ematter.common.shared.withSync
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

internal class AnvilBlockEntity : TileEntity() {
    private companion object {
        private const val INVENTORY_KEY = "content"
        private const val SMASHES_KEY = "smashes"
        private const val ROTATION_KEY = "rotation"
        private const val POSITION_KEY = "position"
        private const val FRAME_TIME_KEY = "ft"

        // Create a new specific random that we are going to use as an offspring of the current system-wide one
        private val random = Random(Random.nextLong())
    }

    private val inventory = object : ItemStackHandler(1) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            this@AnvilBlockEntity.sync()
        }

        override fun getStackLimit(slot: Int, stack: ItemStack): Int = 1
    }
    private var smashes: Byte = 0

    private var stack
        get() = this.inventory.getStackInSlot(0)
        set(value) = this.inventory.setStackInSlot(0, value)

    private var recipeFound = false

    internal val clientStackToDisplay get() = this.stack
    internal var stackRotation: Double = 0.0
        private set(value) { field = min(max(0.0, value), 360.0) }
    internal var stackPositionX: Double = 0.0
        private set(value) { field = min(max(-4.0, value), 4.0) }
    internal var stackPositionY: Double = 0.0
        private set(value) { field = min(max(-2.0, value), 2.0) }

    internal var clientFrameTime: Byte = 0

    override fun onLoad() {
        if (!this.world.isRemote) this.attemptToCraftRecipe(AnvilRecipe.Kind.SMASHING)
    }

    internal fun placeItem(stack: ItemStack): Pair<Boolean, ItemStack> {
        if (stack.isEmpty) return false to ItemStack.EMPTY

        val copy = stack.copy()
        val remainder = this.inventory.insertItem(0, copy, false)
        val insertionSuccessful = !ItemStack.areItemStacksEqual(copy, remainder)

        if (insertionSuccessful) {
            this.withSync {
                // NOTE: This will also sync the inventory
                this.smashes = 0
                this.stackRotation = random.nextDouble(from = 0.0, until = 360.0)
                this.stackPositionX = random.nextDouble(from = -4.0, until = 4.0)
                this.stackPositionY = random.nextDouble(from = -2.0, until = 2.0)
                this.recipeFound = false
            }
        }

        return insertionSuccessful to remainder
    }

    internal fun takeItem(): Pair<Boolean, ItemStack> {
        val stack = this.stack
        if (stack.isEmpty) return false to ItemStack.EMPTY

        this.withSync {
            this.stack = ItemStack.EMPTY
            this.smashes = 0
            this.recipeFound = false
        }

        return true to stack
    }

    internal fun iWonderWhoWantsToSmashMe(isCopper: Boolean): Boolean {
        this.world.playSound(null, this.pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.3F, 1.0F)

        if (this.stack.isEmpty) return false // If no stack is on the anvil, we cannot smash
        if (this.recipeFound) return false // If we already crafted a recipe, we won't smash again
        if (this.smashes == Byte.MAX_VALUE) return false // If someone smashed an anvil 127 times already...
        withSync {
            this.stackRotation = this.stackRotation + random.nextDouble(from = -15.0, until = 15.0)
            this.stackPositionX = this.stackPositionX + random.nextDouble(from = -4.0, until = 4.0)
            this.stackPositionY = this.stackPositionY + random.nextDouble(from = -2.0, until = 2.0)
            this.clientFrameTime = 6
        }
        if (isCopper && this.smashes == 0.toByte()) {
            // Bonus smash
            this.smashes = 1
            this.attemptToCraftRecipe(AnvilRecipe.Kind.SMASHING)
            if (this.recipeFound) return true
        }
        ++this.smashes
        this.attemptToCraftRecipe(AnvilRecipe.Kind.SMASHING)
        return true
    }

    @Suppress("SameParameterValue")
    private fun attemptToCraftRecipe(kind: AnvilRecipe.Kind) {
        val context = AnvilRecipeContext(this, this.smashes, this.stack)
        val wrapper = CraftingInventoryWrapper(AnvilRecipeContext.Wrapper::class, AnvilRecipeContext.Wrapper(context), this.inventory, 1, 1)

        val matchingRecipe = CraftingManager.REGISTRY
                .asSequence()
                .filter { it.matches(wrapper, this.world) }
                .filterIsInstance<AnvilRecipe>()
                .filter { it.kind == kind }
                .firstOrNull() ?: return

        val result = matchingRecipe.getCraftingResult(wrapper)

        withSync {
            this.inventory.setStackInSlot(0, result)
            this.recipeFound = true
        }
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null) return true
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null) return this.inventory.uncheckedCast()
        return super.getCapability(capability, facing)
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.inventory.deserializeNBT(compound.getCompoundTag(INVENTORY_KEY))
        this.smashes = compound.getByte(SMASHES_KEY)
        this.stackRotation = compound.getDouble(ROTATION_KEY)
        compound.getTagList(POSITION_KEY, Constants.NBT.TAG_DOUBLE).let {
            this.stackPositionX = it.getDoubleAt(1)
            this.stackPositionY = it.getDoubleAt(0)
        }
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        val target = super.writeToNBT(compound)
        target.setTag(INVENTORY_KEY, this.inventory.serializeNBT())
        target.setByte(SMASHES_KEY, this.smashes)
        target.setDouble(ROTATION_KEY, this.stackRotation)
        target.setTag(POSITION_KEY, NBTTagList().apply {
            this.appendTag(NBTTagDouble(this@AnvilBlockEntity.stackPositionY))
            this.appendTag(NBTTagDouble(this@AnvilBlockEntity.stackPositionX))
        })
        return target
    }

    override fun getUpdateTag(): NBTTagCompound = NBTTagCompound().apply {
        this.setTag(INVENTORY_KEY, this@AnvilBlockEntity.inventory.serializeNBT())
        this.setByte(SMASHES_KEY, this@AnvilBlockEntity.smashes)
        this.setDouble(ROTATION_KEY, this@AnvilBlockEntity.stackRotation)
        this.setTag(POSITION_KEY, NBTTagList().apply {
            this.appendTag(NBTTagDouble(this@AnvilBlockEntity.stackPositionY))
            this.appendTag(NBTTagDouble(this@AnvilBlockEntity.stackPositionX))
        })
        this.setByte(FRAME_TIME_KEY, this@AnvilBlockEntity.clientFrameTime)
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        this.inventory.deserializeNBT(tag.getCompoundTag(INVENTORY_KEY))
        this.smashes = tag.getByte(SMASHES_KEY)
        this.stackRotation = tag.getDouble(ROTATION_KEY)
        tag.getTagList(POSITION_KEY, Constants.NBT.TAG_DOUBLE).let {
            this.stackPositionX = it.getDoubleAt(1)
            this.stackPositionY = it.getDoubleAt(0)
        }
        this.clientFrameTime = tag.getByte(FRAME_TIME_KEY)
    }

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) = this.handleUpdateTag(pkt.nbtCompound)
    override fun getUpdatePacket(): SPacketUpdateTileEntity? = SPacketUpdateTileEntity(this.pos, -1, this.updateTag)
}