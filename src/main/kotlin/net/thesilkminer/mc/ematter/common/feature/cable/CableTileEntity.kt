package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.getEnergyConsumer
import net.thesilkminer.mc.boson.prefab.energy.hasEnergySupport
import net.thesilkminer.mc.ematter.common.feature.cable.capability.networkManager
import kotlin.experimental.or
import kotlin.math.pow

@Suppress("EXPERIMENTAL_API_USAGE")
internal class CableTileEntity : TileEntity() {

    private var connections: Byte = 0

    fun getConnectionSet(): Set<Direction> = Direction.values().asSequence()
            .filterIndexed { i, _ -> this.connections.toInt() and (1 shl i) != 0 }
            .toSet()

    // needed in #onNeighborChanged to determine if a consumer got removed or added
    private val neighboringConsumers: MutableSet<Direction> = mutableSetOf()

    // Lifecycle >>
    fun onAdd() {
        if (this.world.isRemote) return

        this.world.networkManager?.addCable(this.pos)

        this.neighboringConsumers.addAll(
            Direction.values().asSequence()
                .filterNot { side ->
                    this.world.getTileEntitySafely(this.pos.offset(side))?.getEnergyConsumer(side.opposite)?.let { it is CableNetwork } ?: true
                }
                .onEach { this.world.networkManager?.addConsumer(this.pos.offset(it), it.opposite) }
        )

        this.updateConnections()
        this.markDirty()
    }

    fun onRemove() {
        if (this.world.isRemote) return

        this.neighboringConsumers.forEach { this.world.networkManager?.removeConsumer(this.pos.offset(it), it.opposite) }

        this.world.networkManager?.removeCable(this.pos)
    }
    // << Lifecycle

    // Load/Unload >>
    override fun onLoad() {
        if (this.world.isRemote) return
        this.world.networkManager?.loadConsumers(this.pos)
    }

    override fun onChunkUnload() =
        if (!this.world.isRemote) this.world.networkManager?.unloadConsumers(this.pos) ?: Unit else Unit
    // << Load/Unload

    // Reactions >>
    fun onNeighborChanged(side: Direction) {
        if (this.world.isRemote) return

        if (side in this.neighboringConsumers) {
            this.neighboringConsumers.remove(side)
            this.world.networkManager?.removeConsumer(this.pos.offset(side), side.opposite)
        }

        this.world.getTileEntity(this.pos.offset(side))?.getEnergyConsumer(side.opposite)?.let { consumer ->
            if (consumer is CableNetwork) return@let

            this.neighboringConsumers.add(side)
            this.world.networkManager?.addConsumer(this.pos.offset(side), side.opposite)
        }

        this.updateConnections()
        this.markDirty()
    }

    private fun updateConnections() {
        var newConnections: Byte = 0

        Direction.values().forEach { side ->
            if (this.world.getTileEntity(this.pos.offset(side))?.hasEnergySupport(side.opposite) == true) {
                newConnections = newConnections or 2.0.pow(side.ordinal.toDouble()).toInt().toByte()
            }
        }

        if (this.connections != newConnections) {
            this.connections = newConnections
        }
    }

    override fun markDirty() {
        super.markDirty()
        this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, 0) }
    }
    // << Reactions

    // Capability >>
    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        this.world.networkManager?.get(this.pos)?.let { _ ->
            if (capability == consumerCapability) return true
        }
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        this.world.networkManager?.get(this.pos)?.let { network ->
            if (capability == consumerCapability) return network.uncheckedCast()
        }
        return super.getCapability(capability, facing)
    }
    // << Capability

    // NBT >>
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        compound.setIntArray("sides", this.neighboringConsumers.toIntArray())
        compound.setByte("connections", this.connections)
        return super.writeToNBT(compound)
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.neighboringConsumers.apply {
            this.clear()
            this.addAll(compound.getDirectionList("sides"))
        }
        this.connections = compound.getByte("connections")
    }

    private fun Collection<Direction>.toIntArray() = this.map { it.ordinal }.toIntArray()
    private fun NBTTagCompound.getDirectionList(key: String) = this.getIntArray(key).map { Direction.values()[it] }
    // << NBT

    // Networking >>
    override fun getUpdateTag(): NBTTagCompound = NBTTagCompound().apply {
        // pos needed for mc to deserialize on chunk load; see NetHandlerPlayClient#handleChunkData
        this.setInteger("x", this@CableTileEntity.pos.x)
        this.setInteger("y", this@CableTileEntity.pos.y)
        this.setInteger("z", this@CableTileEntity.pos.z)
        this.setByte("connections", this@CableTileEntity.connections)
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        this.connections = tag.getByte("connections")
        this.world.getBlockState(this.pos).let { this.world.notifyBlockUpdate(this.pos, it, it, Constants.BlockFlags.RERENDER_MAIN_THREAD) }
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity = SPacketUpdateTileEntity(this.pos, -1, this.updateTag)

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) = this.handleUpdateTag(pkt.nbtCompound)
    // << Networking

    // Helper Functions >>
    private fun World.getTileEntitySafely(pos: BlockPos) = if (this.isBlockLoaded(pos)) this.getTileEntity(pos) else null

    // TODO("n1kx", "best feature eu-west: blocking connections")
}
