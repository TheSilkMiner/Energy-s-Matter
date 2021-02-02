package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.block.Block
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
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

        val connections = mapOf(
            Direction.NORTH to PropertyBool.create("north"),
            Direction.EAST to PropertyBool.create("east"),
            Direction.SOUTH to PropertyBool.create("south"),
            Direction.WEST to PropertyBool.create("west"),
            Direction.UP to PropertyBool.create("up"),
            Direction.DOWN to PropertyBool.create("down")
        )

        private const val coreMin = 0.40625
        private const val coreMax = 0.59375
        private const val rodMin = 0.4375
        private const val rodMax = 0.5625

        val coreVolume = AxisAlignedBB(coreMin, coreMin, coreMin, coreMax, coreMax, coreMax)

        val volumes = mapOf(
            Direction.NORTH to AxisAlignedBB(rodMin, rodMin, 0.0, rodMax, rodMax, coreMin),
            Direction.EAST to AxisAlignedBB(coreMax, rodMin, rodMin, 1.0, rodMax, rodMax),
            Direction.SOUTH to AxisAlignedBB(rodMin, rodMin, coreMax, rodMax, rodMax, 1.0),
            Direction.WEST to AxisAlignedBB(0.0, rodMin, rodMin, coreMin, rodMax, rodMax),
            Direction.UP to AxisAlignedBB(rodMin, coreMax, rodMin, rodMax, 1.0, rodMax),
            Direction.DOWN to AxisAlignedBB(rodMin, 0.0, rodMin, rodMax, coreMin, rodMax)
        )
    }

    // TE >>
    override fun hasTileEntity(state: IBlockState): Boolean = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity = CableBlockEntity()
    // << TE

    // Lifecycle >>
    override fun onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableBlockEntity)?.onAdd() // this actually creates the te but that's fine since it would be created in the same tick anyways (at least I hope it's fine)
    }

    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? CableBlockEntity)?.onRemove()
        super.breakBlock(worldIn, pos, state)
    }
    // << Lifecycle

    // Reactions >>
    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        (worldIn.getTileEntity(pos) as? CableBlockEntity)?.onNeighborChanged(Direction.values().find { pos.offset(it) == fromPos }!!)
    }
    // << Reactions

    // BlockState >>
    override fun createBlockState() = BlockStateContainer(
        this,
        connections[Direction.NORTH],
        connections[Direction.EAST],
        connections[Direction.SOUTH],
        connections[Direction.WEST],
        connections[Direction.UP],
        connections[Direction.DOWN]
    )

    override fun getMetaFromState(state: IBlockState) = 0 // mc expects us to override this; otherwise it crashes..

    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos) = (world.getTileEntity(pos) as? CableBlockEntity)?.connections?.let { directions ->
        connections.asSequence().fold(state) { s, property -> s.withProperty(property.value, property.key in directions) }
    } ?: state // during world load te is null; we re render later, after te has loaded
    // << BlockState

    // Rendering >>
    override fun isOpaqueCube(state: IBlockState) = false
    override fun isFullCube(state: IBlockState) = false

    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB {
        return coreVolume
    }

    override fun addCollisionBoxToList(
        state: IBlockState,
        worldIn: World,
        pos: BlockPos,
        entityBox: AxisAlignedBB,
        collidingBoxes: MutableList<AxisAlignedBB>,
        entityIn: Entity?,
        isActualState: Boolean
    ) {
        if (entityBox.intersects(coreVolume.offset(pos))) collidingBoxes.add(coreVolume.offset(pos))

        state.getActualState(worldIn, pos).let { actual ->
            Direction.values().asSequence()
                .filter { actual.getValue(connections.getValue(it)) }
                .map { volumes.getValue(it).offset(pos) }
                .filter { entityBox.intersects(it) }
                .forEach { collidingBoxes.add(it) }
        }
    }

    override fun collisionRayTrace(state: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult? {
        this.rayTrace(pos, start, end, coreVolume)?.let { return it }

        state.getActualState(worldIn, pos).let { actual ->
            Direction.values().asSequence()
                .filter { actual.getValue(connections.getValue(it)) }
                .forEach { this.rayTrace(pos, start, end, volumes.getValue(it))?.let { target -> return target } }
        }

        return null
    }
    // << Rendering
}
