package net.thesilkminer.mc.ematter.common.feature.mad

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
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
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.thesilkminer.mc.ematter.EnergyIsMatter
import net.thesilkminer.mc.ematter.common.network.GuiHandler

internal class MadBlock : Block(Material.IRON) {
    internal companion object {
        internal val TIER = PropertyEnum.create("tier", MadTier::class.java)
        private val BOUNDING_BOX = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.6875, 1.0)
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
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = BOUNDING_BOX
}
