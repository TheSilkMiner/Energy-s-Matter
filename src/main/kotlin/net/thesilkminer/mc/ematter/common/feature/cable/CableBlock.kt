package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.Properties
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

        val connectionNorth: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_north"))
        val connectionEast: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_east"))
        val connectionSouth: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_south"))
        val connectionWest: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_west"))
        val connectionUp: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_up"))
        val connectionDown: Properties.PropertyAdapter<Boolean> = Properties.PropertyAdapter(PropertyBool.create("connection_down"))
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
    override fun createBlockState() = ExtendedBlockState(this, arrayOf(), arrayOf(connectionNorth, connectionEast, connectionSouth, connectionWest, connectionUp, connectionDown))

    override fun getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos) = (world.getTileEntity(pos) as? CableTileEntity)?.connections?.let {
        (state as IExtendedBlockState).withProperty(connectionNorth, it.hasNorth())
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
