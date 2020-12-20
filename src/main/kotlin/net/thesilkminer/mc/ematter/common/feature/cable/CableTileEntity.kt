package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.tileentity.TileEntity
import net.thesilkminer.mc.ematter.common.feature.cable.capability.networkManager

@ExperimentalUnsignedTypes
internal class CableTileEntity : TileEntity() {

    fun onAdd() =
        if (!this.world.isRemote) this.world.networkManager?.add(this.pos) ?: Unit else Unit

    fun onRemove() =
        if (!this.world.isRemote) this.world.networkManager?.remove(this.pos) ?: Unit else Unit

    // TODO("n1kx", "best feature eu-west: blocking connections")
}
