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

package net.thesilkminer.mc.ematter.common.feature.mad

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.thesilkminer.mc.ematter.EnergyIsMatter
import net.thesilkminer.mc.ematter.common.network.GuiHandler

internal class MadBlock : Block(Material.IRON) {
    internal companion object {
        internal val TIER = PropertyEnum.create("tier", MadTier::class.java)
        private val BOUNDING_BOX = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.6875, 1.0)
        private val EMPTY_VOLUME = box(0, 0, 0, 0, 0, 0)

        internal val volumes = sequenceOf(
                box(1, 10, 1, 3, 11, 3), // nw
                box(4, 10, 1, 6, 11, 3), // nnw
                box(7, 10, 1, 9, 11, 3), // n
                box(10, 10, 1, 12, 11, 3), // nne
                box(13, 10, 1, 15, 11, 3), // ne
                box(13, 10, 4, 15, 11, 6), // nee
                box(13, 10, 7, 15, 11, 9), // e
                box(13, 10, 10, 15, 11, 12), // see
                box(13, 10, 13, 15, 11, 15), // se
                box(10, 10, 13, 12, 11, 15), // sse
                box(7, 10, 13, 9, 11, 15), // s
                box(4, 10, 13, 6, 11, 15), // ssw
                box(1, 10, 13, 3, 11, 15), // sw
                box(1, 10, 10, 3, 11, 12), // sww
                box(1, 10, 7, 3, 11, 9), // w
                box(1, 10, 4, 3, 11, 6), // nww
                box(4, 8, 4, 6, 9, 6), // nw
                box(7, 8, 4, 9, 9, 6), // n
                box(10, 8, 4, 12, 9, 6), // ne
                box(10, 8, 7, 12, 9, 9), // e
                box(10, 8, 10, 12, 9, 12), // se
                box(7, 8, 10, 9, 9, 12), // s
                box(4, 8, 10, 6, 9, 12), // sw
                box(4, 8, 7, 6, 9, 9), // w
                box(7, 6, 7, 9, 7, 9), // center
                box(1, 4, 1, 15, 5, 15), // ring
                box(0, 0, 0, 16, 4, 16), // base
                box(2, 4, 2, 14, 6, 14) // lithium_ion_battery_pack
        )

        private fun box(fromX: Int, fromY: Int, fromZ: Int, toX: Int, toY: Int, toZ: Int) =
                AxisAlignedBB(fromX.toDouble() / 16.0 , fromY.toDouble() / 16.0, fromZ.toDouble() / 16.0,
                        toX.toDouble() / 16.0, toY.toDouble() / 16.0, toZ.toDouble() / 16.0)
    }

    init {
        this.defaultState = this.blockState.baseState.withProperty(TIER, MadTier.BASIC)
    }

    // Just a couple of parameters, I see
    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand,
                                  facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (worldIn.isRemote) return true
        if (worldIn.getTileEntity(pos) !is MadTileEntity) return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)

        playerIn.openGui(EnergyIsMatter, GuiHandler.MAD_GUI, worldIn, pos.x, pos.y, pos.z)
        return true
    }

    // TODO("Maybe getDrops is a better location for this?")
    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? MadTileEntity)?.let { te ->
            (0 until te.inventory.slots)
                    .map { te.inventory.getStackInSlot(it) }
                    .filterNot { it.isEmpty }
                    .forEach { InventoryHelper.spawnItemStack(worldIn, pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5, it) }
        }
        return super.breakBlock(worldIn, pos, state)
    }

    override fun addCollisionBoxToList(state: IBlockState, worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB,
                                       collidingBoxes: MutableList<AxisAlignedBB>, entityIn: Entity?, isActualState: Boolean) {
        collidingBoxes.addAll(volumes.map { it.offset(pos) }.filter { entityBox.intersects(it) })
    }

    override fun collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult? =
            volumes.map { this.rayTrace(pos, start, end, it) }.firstOrNull { it != null }

    override fun hasTileEntity(state: IBlockState) = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = MadTileEntity()
    override fun createBlockState() = BlockStateContainer(this, TIER)
    override fun damageDropped(state: IBlockState) = state.getValue(TIER).targetMeta
    override fun getMetaFromState(state: IBlockState) = state.getValue(TIER).targetMeta
    override fun getStateFromMeta(meta: Int): IBlockState = this.defaultState.withProperty(TIER, MadTier.fromMeta(meta and 3))
    override fun getSubBlocks(itemIn: CreativeTabs, items: NonNullList<ItemStack>) = MadTier.values().forEach { items += ItemStack(this, 1, it.targetMeta) }
    override fun isOpaqueCube(state: IBlockState) = false
    override fun isFullCube(state: IBlockState) = false
    override fun getRenderLayer() = BlockRenderLayer.CUTOUT
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = EMPTY_VOLUME
}
