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
                GlStateManager.translate(x + 0.5, y + 0.95, z + 0.5)
                val model = this.itemRenderer.getItemModelWithOverrides(stack, te.world, null)
                val transformedModel = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GROUND, false)
                if (rotate) GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F)
                GlStateManager.translate(te.stackPositionX.toDouble() / 16.0, 0.0, te.stackPositionY.toDouble() / 16.0)
                GlStateManager.rotate(te.stackRotation.toFloat(), 0.0F, 1.0F, 0.0F)
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F)
                this.itemRenderer.renderItem(stack, transformedModel)
            }
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
