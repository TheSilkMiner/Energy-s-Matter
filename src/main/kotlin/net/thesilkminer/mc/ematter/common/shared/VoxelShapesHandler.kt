@file:JvmName("VSH")

package net.thesilkminer.mc.ematter.common.shared

import net.minecraft.block.Block
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

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

internal fun volumes(builder: VolumeBuilder.() -> Unit): Iterable<AxisAlignedBB> = VoxelShapesBuilder().apply(builder).toSequence().asIterable()

@Suppress("NOTHING_TO_INLINE")
internal inline fun handleCollisionVolumes(pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: MutableList<AxisAlignedBB>, volumes: Iterable<AxisAlignedBB>) {
    volumes.forEach { handleCollisionVolume(pos, entityBox, collidingBoxes, it) }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun handleCollisionVolume(pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: MutableList<AxisAlignedBB>, volume: AxisAlignedBB) {
    val translatedMinX = volume.minX + pos.x
    val translatedMinY = volume.minY + pos.y
    val translatedMinZ = volume.minZ + pos.z
    val translatedMaxX = volume.maxX + pos.x
    val translatedMaxY = volume.maxY + pos.y
    val translatedMaxZ = volume.maxZ + pos.z
    if (entityBox.intersects(translatedMinX, translatedMinY, translatedMinZ, translatedMaxX, translatedMaxY, translatedMaxZ)) {
        collidingBoxes.add(AxisAlignedBB(translatedMinX, translatedMinY, translatedMinZ, translatedMaxX, translatedMaxY, translatedMaxZ))
    }
}

internal inline fun performVolumeRayTrace(pos: BlockPos, start: Vec3d, end: Vec3d, rayTraceFunction: (BlockPos, Vec3d, Vec3d, AxisAlignedBB) -> RayTraceResult?,
                                                volumes: Iterable<AxisAlignedBB>): RayTraceResult? {
    volumes.forEach {
        val trace = rayTraceFunction(pos, start, end, it)
        if (trace != null) return trace
    }
    return null
}
