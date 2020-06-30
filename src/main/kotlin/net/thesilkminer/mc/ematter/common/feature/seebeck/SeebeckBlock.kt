package net.thesilkminer.mc.ematter.common.feature.seebeck

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

internal class SeebeckBlock : Block(Material.IRON) {
    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = SeebeckTileEntity()
    override fun hasTileEntity(state: IBlockState): Boolean = true

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        if (worldIn.isRemote) return
        if (pos.up() == fromPos) return

        (worldIn.getTileEntity(pos) as? SeebeckTileEntity)?.requestRecalculation()
    }

}
