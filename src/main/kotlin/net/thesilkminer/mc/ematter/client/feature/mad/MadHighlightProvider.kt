package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.AxisAlignedBB
import net.thesilkminer.mc.ematter.client.clientConfiguration
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightProvider
import net.thesilkminer.mc.ematter.client.shared.HIGHLIGHT_EXPANSION_FACTOR
import net.thesilkminer.mc.ematter.client.shared.expandForHighlight
import net.thesilkminer.mc.ematter.client.shared.renderDefaultHighlight
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock
import org.lwjgl.opengl.GL11

internal class MadHighlightProvider : CustomHighlightProvider {
    private val simpleVolume by lazy { MadBlock.useSimpleVolume }
    private val config by lazy { clientConfiguration["performance", "molecular_assembler_device"]["force_full_highlight"] }

    override fun matches(state: IBlockState): Boolean = state.block == Blocks.molecularAssemblerDevice()

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) =
            if (this.simpleVolume && !this.config().boolean) this.renderSimple(x, y, z) else this.renderMultiple(x, y, z)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun renderSimple(x: Double, y: Double, z: Double) {
        MadBlock.simpleVolume.expandForHighlight().offset(x, y, z).renderDefaultHighlight()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun renderMultiple(x: Double, y: Double, z: Double) {
        val rawVolumes = MadBlock.volumes
        val tex = Tessellator.getInstance()
        val buffer = tex.buffer
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR)
        rawVolumes.forEach { it.draw(buffer, x, y, z, HIGHLIGHT_EXPANSION_FACTOR) }
        tex.draw()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun AxisAlignedBB.draw(buffer: BufferBuilder, x: Double, y: Double, z: Double, growFactor: Double) {
        val minX = this.minX - growFactor + x
        val minY = this.minY - growFactor + y
        val minZ = this.minZ - growFactor + z
        val maxX = this.maxX + growFactor + x
        val maxY = this.maxY + growFactor + y
        val maxZ = this.maxZ + growFactor + z
        val r = 0.0F
        val g = 0.0F
        val b = 0.0F
        val a = 0.4F
        // bottom rectangle
        buffer.line(minX, minY, minZ, maxX, minY, minZ, r, g, b, a)
        buffer.line(maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a)
        buffer.line(maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a)
        buffer.line(minX, minY, maxZ, minX, minY, minZ, r, g, b, a)
        // top rectangle
        buffer.line(minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a)
        buffer.line(maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a)
        buffer.line(maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a)
        buffer.line(minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a)
        // connecting lines
        buffer.line(minX, minY, minZ, minX, maxY, minZ, r, g, b, a)
        buffer.line(maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a)
        buffer.line(maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a)
        buffer.line(minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun BufferBuilder.line(fromX: Double, fromY: Double, fromZ: Double, toX: Double, toY: Double, toZ: Double, r: Float, g: Float, b: Float, a: Float) {
        this.vertex(fromX, fromY, fromZ, r, g, b, a)
        this.vertex(toX, toY, toZ, r, g, b, a)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun BufferBuilder.vertex(x: Double, y: Double, z: Double, r: Float, g: Float, b: Float, a: Float) {
        this.pos(x, y, z).color(r, g, b, a).endVertex()
    }
}
