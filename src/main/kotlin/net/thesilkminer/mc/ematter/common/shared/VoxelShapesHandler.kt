@file:JvmName("VSH")

package net.thesilkminer.mc.ematter.common.shared

import net.minecraft.util.math.AxisAlignedBB

// TODO("All of this is literally bodged together to avoid repeating logic. Move this to proper Volume class, probably into Boson")

internal interface VolumeBuilder {
    fun box(fromX: Int, fromY: Int, fromZ: Int, toX: Int, toY: Int, toZ: Int)
}

private class VoxelShapesBuilder : VolumeBuilder {
    private val boxes = mutableListOf<AxisAlignedBB>()

    override fun box(fromX: Int, fromY: Int, fromZ: Int, toX: Int, toY: Int, toZ: Int) {
        this.boxes += AxisAlignedBB(fromX.toDouble() / 16.0 , fromY.toDouble() / 16.0, fromZ.toDouble() / 16.0,
                toX.toDouble() / 16.0, toY.toDouble() / 16.0, toZ.toDouble() / 16.0)
    }

    fun toSequence() = this.boxes.asSequence()
}

internal val emptyVolume = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

internal fun volumes(builder: VolumeBuilder.() -> Unit): Sequence<AxisAlignedBB> = VoxelShapesBuilder().apply(builder).toSequence()


