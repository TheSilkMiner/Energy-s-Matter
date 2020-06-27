package net.thesilkminer.mc.ematter.common.feature.sbg

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

internal class SbgBlock: Block(Material.IRON) {

    //methods for the tile entity
    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = SbgTileEntity()
    override fun hasTileEntity(state: IBlockState): Boolean = true

    /**
     * Gets called whenever a neighbor block changes and calls the recalculateNeighbors() fun inside SbgTileEntity
     */
    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        if (worldIn.isRemote) return

        if (pos.up() != fromPos) {
            worldIn.getTileEntity(pos).apply {
                if (this is SbgTileEntity) {
                    this.recalculateNeighbors()
                }
            }
        }
    }

}
