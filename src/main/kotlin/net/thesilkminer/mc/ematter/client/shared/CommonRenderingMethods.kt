@file:JvmName("CRM")

package net.thesilkminer.mc.ematter.client.shared

import net.minecraft.client.renderer.GlStateManager

internal inline fun withMatrix(block: () -> Unit) {
    GlStateManager.pushMatrix()
    block()
    GlStateManager.popMatrix()
}
