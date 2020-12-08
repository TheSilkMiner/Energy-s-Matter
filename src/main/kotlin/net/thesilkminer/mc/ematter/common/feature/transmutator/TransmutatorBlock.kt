package net.thesilkminer.mc.ematter.common.feature.transmutator

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class TransmutatorBlock : Block(Material.IRON) {

    internal companion object {

        val FACING: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)
    }

    init {
        this.defaultState = this.blockState.baseState.withProperty(FACING, EnumFacing.NORTH)
    }

    override fun onBlockActivated(
        worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing,
        hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (worldIn.isRemote) return true
        worldIn.getTileEntity(pos).let { te ->
            if (te !is TransmutatorTileEntity) return false
            te.changeOutput(playerIn.getHeldItem(hand))
            return true
        }
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        if (worldIn.isRemote) return
        if (pos.offset(state.getValue(FACING).opposite) != fromPos) return
        worldIn.getTileEntity(pos).let {
            if (it !is TransmutatorTileEntity) return
            it.requestRecalculation()
        }
    }

    override fun hasTileEntity(state: IBlockState): Boolean = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity = TransmutatorTileEntity()

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, FACING)
    override fun getStateForPlacement(
        world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float,
        meta: Int, placer: EntityLivingBase, hand: EnumHand
    ): IBlockState = this.defaultState.withProperty(FACING, placer.horizontalFacing.opposite)

    override fun getStateFromMeta(meta: Int): IBlockState = this.defaultState.withProperty(FACING, EnumFacing.byHorizontalIndex(meta))
    override fun getMetaFromState(state: IBlockState): Int = state.getValue(FACING).horizontalIndex
}
