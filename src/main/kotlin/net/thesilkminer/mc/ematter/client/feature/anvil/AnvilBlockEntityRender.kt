package net.thesilkminer.mc.ematter.client.feature.anvil

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraftforge.client.ForgeHooksClient
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.toDirection
import net.thesilkminer.mc.ematter.client.shared.withMatrix
import net.thesilkminer.mc.ematter.common.feature.anvil.AnvilBlock
import net.thesilkminer.mc.ematter.common.feature.anvil.AnvilBlockEntity

internal class AnvilBlockEntityRender : TileEntitySpecialRenderer<AnvilBlockEntity>() {
    private companion object {
        private const val JUMP_OFFSET = 0.01837F
    }

    private val itemRenderer by lazy { Minecraft.getMinecraft().renderItem }

    override fun render(te: AnvilBlockEntity, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        super.render(te, x, y, z, partialTicks, destroyStage, alpha)

        val stack = te.clientStackToDisplay
        if (stack.isEmpty) return

        val rotate = te.world.getBlockState(te.pos).getValue(AnvilBlock.axis).toDirection() == Direction.NORTH

        withState {
            RenderHelper.enableStandardItemLighting()
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
            Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)

            withMatrix {
                GlStateManager.translate(x + 0.5, y + 0.9, z + 0.5)
                val model = this.itemRenderer.getItemModelWithOverrides(stack, te.world, null)
                val transformedModel = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GROUND, false)
                if (rotate) GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F)
                GlStateManager.translate(te.stackPositionX / 16.0, 0.0, te.stackPositionY / 16.0)
                if (te.clientFrameTime != 0.toByte()) this.adjustForFrameTime(te.clientFrameTime--)
                GlStateManager.rotate(te.stackRotation.toFloat(), 0.0F, 1.0F, 0.0F)
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F)
                this.itemRenderer.renderItem(stack, transformedModel)
            }
        }
    }

    private fun adjustForFrameTime(time: Byte) {
        when {
            time >= 4 -> GlStateManager.translate(0.0F, -(time - 6).toFloat() * JUMP_OFFSET, 0.0F)
            time < 2 -> GlStateManager.translate(0.0F, JUMP_OFFSET, 0.0F)
            else -> GlStateManager.translate(0.0F, 2.0F * JUMP_OFFSET, 0.0F)
        }
    }

    private inline fun withState(block: () -> Unit) {
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        block()
        GlStateManager.disableBlend()
        GlStateManager.disableRescaleNormal()
    }
}
