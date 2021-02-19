/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

package net.thesilkminer.mc.ematter.common.feature.seebeck

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.thesilkminer.mc.ematter.common.shared.emptyVolume
import net.thesilkminer.mc.ematter.common.shared.handleCollisionVolumes
import net.thesilkminer.mc.ematter.common.shared.performVolumeRayTrace
import net.thesilkminer.mc.ematter.common.shared.volumes

internal class SeebeckBlock : Block(Material.IRON) {
    internal companion object {
        internal val volumes = volumes {
            this.box(15, 2, 2, 16, 14, 14) // east_plate
            this.box(2, 0,2, 14, 1, 14) // down_plate
            this.box(2, 2, 0, 14, 14, 1) // north_plate
            this.box(2, 2, 15, 14, 14, 16) // south_plate
            this.box(0, 2, 2, 1, 14, 14) // west_plate
            this.box(7, 7, 1, 9, 9, 15) // ns_connector
            this.box(1, 7, 7, 15, 9, 9) // ew_connector
            this.box(7, 1, 7, 9, 9, 9) // d_connector
            this.box(4, 4, 4, 12, 12, 12) // housing
            this.box(7, 7, 7, 9, 16, 9) // wire
        }
    }

    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = SeebeckBlockEntity()
    override fun hasTileEntity(state: IBlockState): Boolean = true

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) =
            if (worldIn.isRemote || pos.up() == fromPos) Unit else (worldIn.getTileEntity(pos) as? SeebeckBlockEntity)?.requestRecalculation() ?: Unit

    override fun addCollisionBoxToList(state: IBlockState, worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB,
                                       collidingBoxes: MutableList<AxisAlignedBB>, entityIn: Entity?, isActualState: Boolean) {
        handleCollisionVolumes(pos, entityBox, collidingBoxes, volumes)
    }

    override fun collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult? =
            performVolumeRayTrace(pos, start, end, this::rayTrace, volumes)

    override fun isOpaqueCube(state: IBlockState) = false
    override fun isFullCube(state: IBlockState) = false
    override fun getRenderLayer() = BlockRenderLayer.CUTOUT
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = emptyVolume
}
