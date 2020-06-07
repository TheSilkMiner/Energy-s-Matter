package net.thesilkminer.mc.ematter.common.feature.mad

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Consumer
import net.thesilkminer.mc.boson.api.energy.Holder
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.holderCapability
import kotlin.math.max
import kotlin.math.min

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")
internal class MadTileEntity : TileEntity(), Consumer, Holder {
    private companion object {
        private const val INVENTORY_KEY = "inventory"
        private const val POWER_KEY = "power"
    }

    internal val inventory = object : ItemStackHandler(5 * 5) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            this@MadTileEntity.markDirty()

            with (this@MadTileEntity.world.getBlockState(this@MadTileEntity.pos)) {
                this@MadTileEntity.world.notifyBlockUpdate(this@MadTileEntity.pos, this, this, Constants.BlockFlags.DEFAULT)
            }
        }
    }

    private var currentPower: ULong = 0.toULong()
    private var maxPower: ULong = 0.toULong()
    private lateinit var tier: MadTier

    override val storedPower: ULong get() = this.currentPower
    override val maximumCapacity: ULong get() = this.maxPower

    override fun onLoad() {
        this.tier = this.world.getBlockState(this.pos).getValue(MadBlock.TIER)
        this.populateFromTier()
    }

    private fun populateFromTier() {
        this.maxPower = this.tier.capacity
        this.currentPower = max(min(this.maxPower, this.currentPower), 0.toULong())
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == holderCapability && facing == null) return true
        if (capability == consumerCapability && (facing == EnumFacing.DOWN || facing == null)) return true
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null) return true
        return super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == holderCapability && facing == null) return this.uncheckedCast()
        if (capability == consumerCapability && (facing == EnumFacing.DOWN || facing == null)) return this.uncheckedCast()
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null) return this.inventory.uncheckedCast()
        return super.getCapability(capability, facing)
    }

    internal fun isUsableByPlayer(player: EntityPlayer) =
            this.world.getTileEntity(this.pos) === this && player.getDistanceSq(this.pos.add(0.5, 0.5, 0.5)) <=64.0

    @ExperimentalUnsignedTypes
    override fun tryAccept(power: ULong, from: Direction): ULong {
        if (from != Direction.DOWN) return 0UL
        val after = this.currentPower + power
        if (after <= this.maxPower) {
            this.currentPower = after
            return power
        }
        val diff = after - this.maxPower
        this.currentPower = this.maxPower
        return power - diff
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.currentPower = compound.getLong(POWER_KEY).toULong()
        this.inventory.deserializeNBT(compound.getCompoundTag(INVENTORY_KEY))
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setLong(POWER_KEY, this@MadTileEntity.currentPower.toLong())
        compound.setTag(INVENTORY_KEY, this@MadTileEntity.inventory.serializeNBT())
        return compound
    }

    override fun getUpdateTag(): NBTTagCompound = NBTTagCompound().apply { this.setTag(INVENTORY_KEY, this@MadTileEntity.inventory.serializeNBT()) }
    override fun handleUpdateTag(tag: NBTTagCompound) = this.inventory.deserializeNBT(tag.getCompoundTag(INVENTORY_KEY))
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) = this.handleUpdateTag(pkt.nbtCompound)
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, -1, this.updateTag)
}
