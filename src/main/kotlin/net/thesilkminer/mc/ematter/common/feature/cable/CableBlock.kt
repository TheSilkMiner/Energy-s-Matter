package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ExperimentalUnsignedTypes
internal class CableBlock : Block(MATERIAL_CABLE) {

    internal companion object {

        val MATERIAL_CABLE: Material = object : Material(MapColor.AIR) {
            init {
                this.setRequiresTool()
                this.setImmovableMobility()
            }
        }
    }

    override fun hasTileEntity(state: IBlockState): Boolean = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity = CableTileEntity()

    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableTileEntity)?.onRemove()
        super.breakBlock(worldIn, pos, state)
    }

    override fun onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableTileEntity)?.onAdd() // this call actually creates the tile entity but that's fine since the chunk would create it in the same tick anyways
    }

    // TODO("n1kx", "make rendering go brrrrr again")
}
