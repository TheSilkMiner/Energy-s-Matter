package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset
import net.thesilkminer.mc.boson.prefab.energy.consumerCapability
import net.thesilkminer.mc.boson.prefab.energy.getEnergyConsumer
import net.thesilkminer.mc.ematter.common.feature.cable.capability.networkManager

@Suppress("EXPERIMENTAL_API_USAGE")
internal class CableTileEntity : TileEntity() {

    // needed in #onNeighborChanged to determine if a consumer got removed or added
    private val neighboringConsumers: MutableSet<Direction> = mutableSetOf()

    // lifecycle
    fun onAdd() {
        if (this.world.isRemote) return

        this.world.networkManager?.addCable(this.pos)

        this.neighboringConsumers.addAll(
            Direction.values().asSequence()
                .filterNot { side ->
                    this.world.getTileEntity(this.pos.offset(side))?.getEnergyConsumer(side.opposite)?.let { it is CableNetwork } ?: true
                }
                .onEach { this.world.networkManager?.addConsumer(this.pos.offset(it), it.opposite) }
        )
    }

    fun onRemove() {
        if (this.world.isRemote) return

        this.neighboringConsumers.forEach { this.world.networkManager?.removeConsumer(this.pos.offset(it), it.opposite) }

        this.world.networkManager?.removeCable(this.pos)
    }

    // load/unload
    override fun onLoad() =
        if (!this.world.isRemote) this.world.networkManager?.loadConsumers(this.pos) ?: Unit else Unit

    override fun onChunkUnload() =
        if (!this.world.isRemote) this.world.networkManager?.unloadConsumers(this.pos) ?: Unit else Unit

    // reacting to other things
    fun onNeighborChanged(side: Direction) {
        if (this.world.isRemote) return

        var flag = false

        if (side in this.neighboringConsumers) {
            this.neighboringConsumers.remove(side)
            this.world.networkManager?.removeConsumer(this.pos.offset(side), side.opposite)

            flag = true
        }

        this.world.getTileEntity(this.pos.offset(side))?.getEnergyConsumer(side.opposite)?.let { consumer ->
            if (consumer is CableNetwork) return@let

            this.neighboringConsumers.add(side)
            this.world.networkManager?.addConsumer(this.pos.offset(side), side.opposite)

            flag = true
        }

        if (flag) this.markDirty()
    }

    // capability handling
    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        this.world.networkManager?.get(this.pos)?.let { network ->
            if (network.consumers.isNotEmpty() && capability == consumerCapability) return true
        }
        return super.hasCapability(capability, facing)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        this.world.networkManager?.get(this.pos)?.let { network ->
            if (network.consumers.isNotEmpty() && capability == consumerCapability) return network.uncheckedCast()
        }
        return super.getCapability(capability, facing)
    }

    // nbt stuff
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        compound.setIntArray("sides", this.neighboringConsumers.toIntArray())
        return super.writeToNBT(compound)
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        this.neighboringConsumers.clear()
        this.neighboringConsumers.addAll(compound.getDirectionList("sides"))
    }

    private fun Collection<Direction>.toIntArray() = this.map { it.ordinal }.toIntArray()
    private fun NBTTagCompound.getDirectionList(key: String) = this.getIntArray(key).map { Direction.values()[it] }

    // TODO("n1kx", "best feature eu-west: blocking connections")
}
