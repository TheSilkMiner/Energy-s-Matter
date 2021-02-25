package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.ForgeHooksClient
import net.thesilkminer.mc.ematter.client.clientConfiguration
import net.thesilkminer.mc.ematter.client.shared.withMatrix
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlockEntity

internal class MadBlockEntityRender : TileEntitySpecialRenderer<MadBlockEntity>() {
    private enum class SpinMeaning {
        NONE,
        CHARGE,
        RECIPE
    }

    private companion object {
        private const val Y_OFFSET = -0.02083
        private const val ROTATION_SPEED = 60.0F

        private val reshuffleArray = intArrayOf(0, 1, 2, 3, 4, 15, 16, 17, 18, 5, 14, 23, 24, 19, 6, 13, 22, 21, 20, 7, 12, 11, 10, 9, 8)
        private val slotsOffsets by lazy {
            MadBlock.volumes
                    .take(25)
                    .map { doubleArrayOf((it.minX + it.maxX) / 2.0, it.maxY + Y_OFFSET, (it.minZ + it.maxZ) / 2.0) }
                    .toList()
                    .reshuffle()
        }
        private val resultOffset = doubleArrayOf(0.5, 0.6666667, 0.5)

        private val spinMeaning by lazy { SpinMeaning.values()[clientConfiguration["behavior", "molecular_assembler_device"]["spin_meaning"]().int] }

        private fun List<DoubleArray>.reshuffle(): Array<DoubleArray> = reshuffleArray.map { this[it] }.toTypedArray()
    }

    private val itemRenderer by lazy { Minecraft.getMinecraft().renderItem }

    override fun render(te: MadBlockEntity, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        super.render(te, x, y, z, partialTicks, destroyStage, alpha)
        val inventory = te.inventory.let { inv ->
            (0 until inv.slots).asSequence()
                    .map { inv.getStackInSlot(it) }
                    .map { it to this.itemRenderer.getItemModelWithOverrides(it, te.world, null) }
                    .toList()
        }
        val output = te.clientPossibleRecipe.first

        withState {
            RenderHelper.enableStandardItemLighting()
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
            Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)

            inventory.forEachIndexed { index, pair ->
                withMatrix {
                    val (offsetX, offsetY, offsetZ) = slotsOffsets[index]
                    GlStateManager.translate(x + offsetX, y + offsetY, z + offsetZ)
                    GlStateManager.scale(0.25, 0.25, 0.25)
                    val model = ForgeHooksClient.handleCameraTransforms(pair.second, ItemCameraTransforms.TransformType.GROUND, false)
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F) // Euler angles 🙄
                    this.itemRenderer.renderItem(pair.first, model)
                }
            }

            if (!output.isEmpty) {
                withMatrix {
                    val renderTime = te.world.totalWorldTime + partialTicks
                    val yOffset = resultOffset[1] // TODO("Sine wave motion")
                    GlStateManager.translate(x + resultOffset[0], y + yOffset, z + resultOffset[2])
                    GlStateManager.rotate(renderTime * this.findSpinFactor(te, spinMeaning), 0.0F, 1.0F, 0.0F)
                    val bakedModel = this.itemRenderer.getItemModelWithOverrides(output, te.world, null)
                    val model = ForgeHooksClient.handleCameraTransforms(bakedModel, ItemCameraTransforms.TransformType.GROUND, false)
                    this.itemRenderer.renderItem(output, model)
                }
            }
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun findSpinFactor(be: MadBlockEntity, spinMeaning: SpinMeaning): Float = when (spinMeaning) {
        SpinMeaning.NONE -> ROTATION_SPEED / 5.0F
        SpinMeaning.CHARGE -> (be.storedPower.toFloat() / be.maximumCapacity.toFloat()) * ROTATION_SPEED + 1.0F
        SpinMeaning.RECIPE -> (be.clientPossibleRecipe.second.toFloat() / be.maximumCapacity.toFloat()) * ROTATION_SPEED + 1.0F
    }

    private inline fun withState(block: () -> Unit) {
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        block()
        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
    }
}
