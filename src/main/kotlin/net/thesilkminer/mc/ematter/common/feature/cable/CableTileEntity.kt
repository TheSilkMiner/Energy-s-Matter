package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.tileentity.TileEntity

@ExperimentalUnsignedTypes
internal class CableTileEntity : TileEntity() {

    override fun onLoad() = if (!this.world.isRemote) NetworkManager.add(this.pos.toImmutable()) else Unit
    fun onRemove() = if (!this.world.isRemote) NetworkManager.remove(this.pos) else Unit

    // TODO("n1kx", "best feature eu-west: blocking connections")
}
