/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

@file:JvmName("JEIRU")

package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.SimpleReloadableResourceManager
import net.minecraft.util.ResourceLocation
import net.thesilkminer.mc.ematter.client.shared.withMatrix
import org.lwjgl.opengl.GL11
import kotlin.math.PI
import kotlin.math.atan2

private val smallFontRenderer by lazy {
    Minecraft.getMinecraft().let {
        FontRenderer(it.gameSettings, ResourceLocation("textures/font/ascii.png"), it.textureManager, true).apply {
            (it.resourceManager as? SimpleReloadableResourceManager)?.registerReloadListener(this)
        }
    }
}

internal fun renderArrow(minecraft: Minecraft, initialPosition: Pair<Double, Double>, finalPosition: Pair<Double, Double>, color: Int, scale: Double) {
    renderLine(minecraft, initialPosition, finalPosition, color, scale)
    withColorAndWidth(minecraft, color, scale) { scaleFactor ->
        val angle = atan2(finalPosition.second - initialPosition.second, finalPosition.first - initialPosition.first).toDegrees()
        withMatrix {
            GlStateManager.translate(finalPosition.first, finalPosition.second, 0.0)
            GlStateManager.rotate(angle.toFloat(), 0.0F, 0.0F, 1.0F)
            GlStateManager.scale(scaleFactor, scaleFactor, 1.0)
            GlStateManager.glBegin(GL11.GL_TRIANGLES)
            GL11.glVertex2d(3.0, 0.0)
            GL11.glVertex2d(0.0, -1.5)
            GL11.glVertex2d(0.0, 1.5)
            GlStateManager.glEnd()
        }
    }
}

internal fun renderLine(minecraft: Minecraft, initialPosition: Pair<Double, Double>, finalPosition: Pair<Double, Double>, color: Int, scale: Double) {
    withColorAndWidth(minecraft, color, scale) {
        GlStateManager.glBegin(GL11.GL_LINES)
        GL11.glVertex2d(initialPosition.first, initialPosition.second)
        GL11.glVertex2d(finalPosition.first, finalPosition.second)
        GlStateManager.glEnd()
    }
}

internal fun renderNormalText(minecraft: Minecraft, text: String, coordinates: Pair<Double, Double>, color: Int) = renderText(minecraft, minecraft.fontRenderer, text, coordinates, color)

internal fun renderSmallText(minecraft: Minecraft, text: String, coordinates: Pair<Double, Double>, color: Int) = renderText(minecraft, smallFontRenderer, text, coordinates, color)

private fun renderText(minecraft: Minecraft, fontRenderer: FontRenderer, text: String, coordinates: Pair<Double, Double>, color: Int) {
    withColorAndWidth(minecraft, 0xFFFFFFFF.toInt(), 1.0) {
        withMatrix {
            fontRenderer.drawString(text, coordinates.first.toFloat(), coordinates.second.toFloat(), color.toArgb().swapAlpha(), false)
        }
    }
}

private inline fun withColorAndWidth(minecraft: Minecraft, color: Int, scale: Double, block: (scaleFactor: Double) -> Unit) {
    val scaleFactor = ScaledResolution(minecraft).scaleFactor
    val red = ((color shr 24) and 255).toFloat() / 255.0F
    val green = ((color shr 16) and 255).toFloat() / 255.0F
    val blue = ((color shr 8) and 255).toFloat() / 255.0F
    val alpha = ((color shr 0) and 255).toFloat() / 255.0F
    GlStateManager.color(red, green, blue, alpha)
    GlStateManager.glLineWidth((scaleFactor * scale).toFloat())
    block(scaleFactor.toDouble())
    GlStateManager.glLineWidth(1.0F)
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
}

@Suppress("NOTHING_TO_INLINE") private inline fun Double.toDegrees() = this * 180.0 / PI
@Suppress("NOTHING_TO_INLINE") private inline fun Int.toArgb() = (this ushr 8) or ((this and 255) shl 24)
@Suppress("NOTHING_TO_INLINE") private inline fun Int.swapAlpha() = ((0xFF - (this ushr 24)) shl 24) or (this and 0x00FFFFFF)
