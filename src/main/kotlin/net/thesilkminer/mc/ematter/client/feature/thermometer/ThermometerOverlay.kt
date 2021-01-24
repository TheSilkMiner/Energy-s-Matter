package net.thesilkminer.mc.ematter.client.feature.thermometer

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.Items

internal class ThermometerOverlay(private val minecraft: Minecraft) : Gui() {
    internal companion object {
        private const val TEXTURE_WIDTH = 80
        private const val TEXTURE_HEIGHT = 120
        private const val DISTANCE_FROM_RIGHT_BORDER = 10
        private const val MIDDLE_SHIFTING = -(TEXTURE_HEIGHT / 2 - 20)

        private val thermometerOverlayTexture = NameSpacedString(MOD_ID, "textures/gui/hud/thermometer.png")

        private var currentOverlay = null as ThermometerOverlay?

        @SubscribeEvent
        internal fun onPostGameOverlayRendering(event: RenderGameOverlayEvent.Post) {
            if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return
            val minecraft = Minecraft.getMinecraft()

            if (this.shouldDisplay(minecraft)) {
                if (this.currentOverlay == null) this.currentOverlay = ThermometerOverlay(minecraft)
                this.currentOverlay!!.render()
            } else if (this.currentOverlay != null) {
                this.currentOverlay = null
            }
        }

        private fun shouldDisplay(minecraft: Minecraft) =
                minecraft.player.heldItemMainhand.item == Items.thermometer() || minecraft.player.heldItemOffhand.item == Items.thermometer()
    }

    private fun render() = withStateRestoring {
        GlStateManager.pushMatrix()
        val scaledResolution = ScaledResolution(this.minecraft)
        val x = scaledResolution.scaledWidth - TEXTURE_WIDTH - DISTANCE_FROM_RIGHT_BORDER
        val y = scaledResolution.scaledHeight / 2 + MIDDLE_SHIFTING
        this.drawTexturedModalRect(x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT)
        GlStateManager.popMatrix()
    }

    private inline fun withStateRestoring(block: () -> Unit) {
        GlStateManager.disableBlend()
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        this.minecraft.textureManager.bindTexture(thermometerOverlayTexture.toResourceLocation())
        block()
        this.minecraft.textureManager.bindTexture(ICONS)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        GlStateManager.enableBlend()
    }
}
