package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.hasEnergySupport
import net.thesilkminer.mc.boson.prefab.energy.isEnergyConsumer
import net.thesilkminer.mc.ematter.common.feature.cable.capability.networkManager
import net.thesilkminer.mc.ematter.common.shared.DirectionsByte

@Suppress("EXPERIMENTAL_API_USAGE")
internal class CableTileEntity : TileEntity() {

    var connections: DirectionsByte = DirectionsByte(0)
        private set

    private var consumers: DirectionsByte = DirectionsByte(0)

    // needed in #onNeighborChanged to determine if a consumer got removed or added
    private val neighboringConsumers: MutableSet<Direction> = mutableSetOf()

    // Lifecycle >>
    fun onAdd() {
        if (this.world.isRemote) return

        this.world.networkManager.addCable(this.pos)

        Direction.values().forEach { side ->
            this.world.getTileEntitySafely(this.pos.offset(side))?.let { te ->
                if (te.hasEnergySupport(side.opposite)) this.connections = this.connections + side
                if (te.isEnergyConsumer(side.opposite) && te !is CableTileEntity) {
                    this.consumers = this.consumers + side
                    this.world.networkManager.addConsumer(this.pos.offset(side), side.opposite)
                }
            }
        }

        this.markAndNotify()
    }

    fun onRemove() {
        if (this.world.isRemote) return

        this.neighboringConsumers.forEach { this.world.networkManager.removeConsumer(this.pos.offset(it), it.opposite) }

        this.world.networkManager.removeCable(this.pos)
    }
    // << Lifecycle

    // Load/Unload >>
    override fun onLoad() {
        if (this.world.isRemote) return
        this.world.networkManager.loadConsumers(this.pos)
    }

    override fun onChunkUnload() =
        if (!this.world.isRemote) this.world.networkManager.unloadConsumers(this.pos) else Unit
    // << Load/Unload

    // Reactions >>
    fun onNeighborChanged(side: Direction) {
        if (this.world.isRemote) return

        val oldConnections = this.connections
        val oldConsumers = this.consumers

        this.connections = this.connections - side
        this.consumers = this.consumers - side

        if (this.consumers != oldConsumers) this.world.networkManager.removeConsumer(this.pos.offset(side), side.opposite)

        this.world.getTileEntitySafely(this.pos.offset(side))?.let { te ->
            if (te.hasEnergySupport(side.opposite)) this.connections = this.connections + side
            if (te.isEnergyConsumer(side.opposite) && te !is CableTileEntity) {
                this.consumers = this.consumers + side
                this.world.networkManager.addConsumer(this.pos.offset(side), side.opposite)
            }
        }

        if (this.connections != oldConnections) this.markAndNotify()
        else if (this.consumers != oldConsumers) this.markDirty()
    }

    private fun markAndNotify() {
        this.markDirty()
        this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, 0) } // zero just because I can; actually notifies the client
    }
    // << Reactions

    // Capability >>
    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability == consumerCapability) {
            this.world.networkManager[this.pos]?.let { _ -> return true }
        }
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (capability == consumerCapability) {
            this.world.networkManager[this.pos]?.let { return it.uncheckedCast() }
        }
        return super.getCapability(capability, facing)
    }
    // << Capability

    // NBT >>
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        compound.setByte("connections", this.connections.byte)
        compound.setByte("consumers", this.consumers.byte)
        return super.writeToNBT(compound)
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.consumers = DirectionsByte(compound.getByte("consumers"))
        this.connections = DirectionsByte(compound.getByte("connections"))
    }
    // << NBT

    // Networking >>
    override fun getUpdateTag(): NBTTagCompound = NBTTagCompound().apply {
        // pos needed for mc to deserialize on chunk load; see NetHandlerPlayClient#handleChunkData
        this.setInteger("x", this@CableTileEntity.pos.x)
        this.setInteger("y", this@CableTileEntity.pos.y)
        this.setInteger("z", this@CableTileEntity.pos.z)
        this.setByte("connections", this@CableTileEntity.connections.byte)
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        this.connections = DirectionsByte(tag.getByte("connections"))
        this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, Constants.BlockFlags.RERENDER_MAIN_THREAD) }
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity = SPacketUpdateTileEntity(this.pos, -1, this.updateTag)

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) = this.handleUpdateTag(pkt.nbtCompound)
    // << Networking

    // Helper Functions >>
    private fun World.getTileEntitySafely(pos: BlockPos) = if (this.isBlockLoaded(pos)) this.getTileEntity(pos) else null

    // TODO("n1kx", "best feature eu-west: blocking connections")
}
