package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset

internal class CableBlock : Block(MATERIAL_CABLE) {

    internal companion object {

        val MATERIAL_CABLE: Material = object : Material(MapColor.AIR) {
            init {
                this.setRequiresTool()
                this.setImmovableMobility()
            }
        }

        val connectionNorth: PropertyBool = PropertyBool.create("north")
        val connectionEast: PropertyBool = PropertyBool.create("east")
        val connectionSouth: PropertyBool = PropertyBool.create("south")
        val connectionWest: PropertyBool = PropertyBool.create("west")
        val connectionUp: PropertyBool = PropertyBool.create("up")
        val connectionDown: PropertyBool = PropertyBool.create("down")
    }

    // TE >>
    override fun hasTileEntity(state: IBlockState): Boolean = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity = CableTileEntity()
    // << TE

    // Lifecycle >>
    override fun onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableTileEntity)?.onAdd() // this actually creates the te but that's fine since it would be created in the same tick anyways (at least I hope it's fine)
    }

    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableTileEntity)?.onRemove()
        super.breakBlock(worldIn, pos, state)
    }
    // << Lifecycle

    // Reactions >>
    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        (worldIn.getTileEntity(pos) as? CableTileEntity)?.onNeighborChanged(Direction.values().find { pos.offset(it) == fromPos }!!)
    }
    // << Reactions

    // BlockState >>
    override fun createBlockState() = BlockStateContainer(this, connectionNorth, connectionEast, connectionSouth, connectionWest, connectionUp, connectionDown)

    override fun getMetaFromState(state: IBlockState) = 0 // mc expects us to override this; otherwise it crashes..

    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos) = (world.getTileEntity(pos) as? CableTileEntity)?.connections?.let {
        state.withProperty(connectionNorth, it.hasNorth())
            .withProperty(connectionEast, it.hasEast())
            .withProperty(connectionSouth, it.hasSouth())
            .withProperty(connectionWest, it.hasWest())
            .withProperty(connectionUp, it.hasUp())
            .withProperty(connectionDown, it.hasDown())
    } ?: state // during world load te is null; we re render later, after te has loaded
    // << BlockState

    // Rendering >>
    override fun isOpaqueCube(state: IBlockState) = false
    override fun isFullCube(state: IBlockState) = false
    // << Rendering
}
