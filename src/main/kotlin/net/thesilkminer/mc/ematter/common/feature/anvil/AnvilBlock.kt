package net.thesilkminer.mc.ematter.common.feature.anvil

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.direction.toDirection
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.boson.prefab.tag.isInTag
import net.thesilkminer.mc.boson.prefab.tag.itemTagType
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.shared.emptyVolume
import net.thesilkminer.mc.ematter.common.shared.volumes

internal class AnvilBlock : Block(Material.ANVIL) {
    internal companion object {
        // private val accessibilityConfig
        private val hammers by lazy { bosonApi.tagRegistry[itemTagType, NameSpacedString(MOD_ID, "anvil_hammers")] }

        internal val axis = PropertyDirection.create("axis") { it != null && (it.toDirection() == Direction.NORTH || it.toDirection() == Direction.EAST) }

        // Recursive type checking? How?
        private val northVolumes: Sequence<AxisAlignedBB> = volumes {
            this.box(2, 0, 2, 14, 4, 14)
            this.box(4, 4, 3, 12, 5, 13)
            this.box(6, 5, 4, 10, 10, 12)
            this.box(3, 10, 0, 13, 16, 16)
        }
        private val eastVolumes: Sequence<AxisAlignedBB> = volumes {
            this.box(2, 0, 2, 14, 4, 14)
            this.box(3, 4, 4, 13, 5, 12)
            this.box(4, 5, 6, 12, 10, 10)
            this.box(0, 10, 3, 16, 16, 13)
        }

        internal val volumes = mapOf(Direction.NORTH to northVolumes, Direction.EAST to eastVolumes)
    }

    init {
        this.blockSoundType = SoundType.ANVIL
        this.lightOpacity = 0
        this.defaultState = this.blockState.baseState.withProperty(axis, Direction.NORTH.toFacing())
    }

    override fun getStateForPlacement(world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float,
                                      meta: Int, placer: EntityLivingBase, hand: EnumHand): IBlockState {
        val targetedAxis = try { placer.horizontalFacing.rotateY().axis } catch (e: IllegalStateException) { EnumFacing.Axis.X }
        val axisValue = if (targetedAxis == EnumFacing.Axis.X) Direction.EAST else Direction.NORTH
        return this.defaultState.withProperty(axis, axisValue.toFacing())
    }

    override fun onBlockClicked(worldIn: World, pos: BlockPos, playerIn: EntityPlayer) {
        if (worldIn.isRemote) return
        if (worldIn.getTileEntity(pos) !is AnvilBlockEntity) return super.onBlockClicked(worldIn, pos, playerIn)

        val heldItem = playerIn.getHeldItem(EnumHand.MAIN_HAND)
        if (!(heldItem isInTag hammers)) return

        (worldIn.getTileEntity(pos) as AnvilBlockEntity).processSmash(playerIn, heldItem, EnumHand.MAIN_HAND)
    }

    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand,
                                  facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (worldIn.isRemote) return true
        if (worldIn.getTileEntity(pos) !is AnvilBlockEntity) return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)

        (worldIn.getTileEntity(pos) as AnvilBlockEntity).processInteraction(playerIn, playerIn.getHeldItem(hand), playerIn.isSneaking, hand)
        return true
    }

    // TODO("Maybe getDrops is a better location for this?")
    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        (worldIn.getTileEntity(pos) as? AnvilBlockEntity)?.let {
            if (!it.clientStackToDisplay.isEmpty) {
                InventoryHelper.spawnItemStack(worldIn, pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5, it.clientStackToDisplay)
            }
        }
        return super.breakBlock(worldIn, pos, state)
    }

    override fun addCollisionBoxToList(state: IBlockState, worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB,
                                       collidingBoxes: MutableList<AxisAlignedBB>, entityIn: Entity?, isActualState: Boolean) {
        volumes[state.getValue(axis).toDirection()]?.map { it.offset(pos) }?.filter { entityBox.intersects(it) }?.let { collidingBoxes.addAll(it) }
    }

    override fun collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult? =
            volumes[blockState.getValue(axis).toDirection()]?.map { this.rayTrace(pos, start, end, it) }?.firstOrNull { it != null }

    override fun hasTileEntity(state: IBlockState): Boolean = true
    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = AnvilBlockEntity()
    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, axis)
    override fun getMetaFromState(state: IBlockState): Int = if (state.getValue(axis).toDirection() == Direction.NORTH) 1 else 0
    override fun getStateFromMeta(meta: Int): IBlockState = this.defaultState.withProperty(axis, (if ((meta and 1) == 0) Direction.EAST else Direction.NORTH).toFacing())
    override fun isOpaqueCube(state: IBlockState): Boolean = false
    override fun isFullCube(state: IBlockState): Boolean = false
    override fun getRenderLayer(): BlockRenderLayer = BlockRenderLayer.CUTOUT
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = emptyVolume

    private fun AnvilBlockEntity.processInteraction(player: EntityPlayer, heldItem: ItemStack, sneaking: Boolean, hand: EnumHand) = when {
        heldItem.isEmpty && sneaking -> this.processExtraction(player)
        heldItem isInTag hammers && !sneaking && (/*accessibility || */ player.isCreative) -> this.processSmash(player, heldItem, hand)
        !sneaking -> this.processInsertion(player, heldItem, hand)
        else -> Unit
    }

    private fun AnvilBlockEntity.processExtraction(player: EntityPlayer) {
        val (result, content) = this.takeItem()
        if (result && !player.inventory.addItemStackToInventory(content)) {
            ForgeHooks.onPlayerTossEvent(player, content, false)
        }
    }

    private fun AnvilBlockEntity.processSmash(player: EntityPlayer, heldItem: ItemStack, hand: EnumHand) {
        if (this.iWonderWhoWantsToSmashMe()) {
            if (!player.isCreative) {
                heldItem.damageItem(1, player)
            } else {
                player.swingArm(hand)
            }
        }
    }

    private fun AnvilBlockEntity.processInsertion(player: EntityPlayer, heldItem: ItemStack, hand: EnumHand) {
        val (result, remainder) = this.placeItem(heldItem)
        if (result) player.setHeldItem(hand, remainder)
    }
}
