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
        private const val BLINK_TIME_CONSTANT = 40

        private val textureSize = 80 to 120
        private val shifts = 10 to -(textureSize.second / 2 - 20)
        private val textShift = 7 to 40
        private val digitPositions = (80 to 0) to (13 to 38)
        private val digitRenderSize = digitPositions.second.first + 3 to digitPositions.second.second
        private val kelvinIcon = (89 to 38) to (9 to 22)
        private val batteryIcon = (80 to 38) to (8 to 22)
        private val blockIcon = (80 to 60) to (21 to 24)
        private val renderPositions = arrayOf(57 to 14, 57 to 85, 18 to 85)

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
                // TODO("Make the unset only happen when the client exits the world?")
                TemperatureStorage.unset()
            }
        }

        private fun shouldDisplay(minecraft: Minecraft) =
                minecraft.player.heldItemMainhand.item == Items.thermometer() || minecraft.player.heldItemOffhand.item == Items.thermometer()
    }

    private fun render() = withStateRestoring {
        GlStateManager.pushMatrix()
        val scaledResolution = ScaledResolution(this.minecraft)
        val topLeftX = scaledResolution.scaledWidth - textureSize.first - shifts.first
        val topLeftY = scaledResolution.scaledHeight / 2 + shifts.second
        this.renderBackground(topLeftX, topLeftY)
        this.renderIcons(topLeftX, topLeftY, this.minecraft.world?.worldTime ?: 0L)
        this.renderTemperature(topLeftX, topLeftY)
        GlStateManager.popMatrix()
    }

    private fun renderBackground(x: Int, y: Int) {
        this.drawTexturedModalRect(x, y, 0, 0, textureSize.first, textureSize.second)
    }

    private fun renderIcons(beginX: Int, beginY: Int, worldTime: Long) {
        val (kelvin, bat, block) = renderPositions
        this.drawTexturedModalRect(beginX + bat.first, beginY + bat.second, batteryIcon.first.first, batteryIcon.first.second, batteryIcon.second.first, batteryIcon.second.second)
        if (TemperatureStorage.hasTemperature) {
            this.drawTexturedModalRect(beginX + kelvin.first, beginY + kelvin.second, kelvinIcon.first.first, kelvinIcon.first.second, kelvinIcon.second.first, kelvinIcon.second.second)
            if (worldTime % BLINK_TIME_CONSTANT < (BLINK_TIME_CONSTANT / 2)) {
                val texX = blockIcon.first.first + blockIcon.second.first * if (TemperatureStorage.dark) 1 else 0
                this.drawTexturedModalRect(beginX + block.first, beginY + block.second, texX, blockIcon.first.second, blockIcon.second.first, blockIcon.second.second)
            }
        }
    }

    private fun renderTemperature(topLeftX: Int, topLeftY: Int) =
            this.renderTemperature(topLeftX, topLeftY, if (TemperatureStorage.hasTemperature) TemperatureStorage.temperature.toString() else "----")

    private fun renderTemperature(topLeftX: Int, topLeftY: Int, temperature: String) {
        val x = topLeftX + textShift.first
        val y = topLeftY + textShift.second
        val digitCoordinates = temperature.toDigits().toCoordinates()
        digitCoordinates.forEachIndexed { index, coordinate ->
            if (coordinate != -1) {
                this.drawTexturedModalRect(x + index * digitRenderSize.first, y, coordinate, digitPositions.first.second, digitPositions.second.first, digitPositions.second.second)
            }
        }
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

    private fun String.toDigits(): IntArray {
        if (this.count() > 4) return "hi".toDigits()
        return this.toDigitsRecurse(IntArray(4) { -1 }).shift()
    }

    private tailrec fun String.toDigitsRecurse(array: IntArray, index: Int = 0): IntArray {
        if (this.count() <= 0) return array
        val firstChar = this[0]
        val rest = this.substring(startIndex = 1)
        array[index] = firstChar.toIndex()
        return rest.toDigitsRecurse(array, index + 1)
    }

    private fun Char.toIndex(): Int = when (this) {
        in '0'..'9' -> this.toInt() - '0'.toInt()
        '-' -> 10
        'h' -> 11
        'i' -> 12
        else -> throw IllegalArgumentException(this.toString())
    }

    private fun IntArray.shift(): IntArray {
        if (this[this.lastIndex] != -1) return this
        val amountOfNegs = this.count { it == -1 }
        this.copyInto(this, destinationOffset = amountOfNegs, startIndex = 0, endIndex = this.count() - amountOfNegs)
        this.fill(element = -1, fromIndex = 0, toIndex = amountOfNegs)
        return this
    }

    private tailrec fun IntArray.toCoordinates(index: Int = 0): IntArray {
        if (index >= this.count()) return this
        val digit = this[index]
        if (digit != -1) {
            this[index] = digitPositions.second.first * digit + digitPositions.first.first
        }
        return this.toCoordinates(index + 1)
    }
}
