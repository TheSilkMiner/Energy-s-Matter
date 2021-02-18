@file:JvmName("BEE")

package net.thesilkminer.mc.ematter.common.shared

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants

internal inline fun <T> TileEntity.withSync(save: Boolean = true, block: () -> T): T {
    val data = block()
    this.sync(save)
    return data
}

internal fun TileEntity.sync(save: Boolean = true) {
    if (save) this.markDirty()
    this.sendBlockUpdate()
}

internal fun TileEntity.sendBlockUpdate() {
    if (this.world.isRemote) return
    val state = this.world.getBlockState(this.pos)
    this.world.notifyBlockUpdate(this.pos, state, state, Constants.BlockFlags.DEFAULT)
}

internal fun TileEntity.isUsableByPlayer(player: EntityPlayer, maxDistance: Double = 64.0): Boolean =
    this.world.getTileEntity(this.pos) === this && player.getDistanceSq(this.pos.add(0.5, 0.5, 0.5)) <= maxDistance
