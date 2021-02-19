@file:JvmName("CHM")

package net.thesilkminer.mc.ematter.client.shared

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.boson.prefab.direction.toDirection
import net.thesilkminer.mc.ematter.client.feature.cable.CableHighlightProvider
import net.thesilkminer.mc.ematter.client.feature.mad.MadHighlightProvider
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.anvil.AnvilBlock
import net.thesilkminer.mc.ematter.common.feature.seebeck.SeebeckBlock
import org.lwjgl.opengl.GL11

internal interface CustomHighlightProvider {
    fun matches(state: IBlockState): Boolean
    fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double)
}

internal object CustomHighlightManager {
    private val highlightProviders = mutableListOf<CustomHighlightProvider>()

    internal fun registerCustomHighlightManagers() {
        this.highlightProviders += CableHighlightProvider()
        this.highlightProviders += DefaultCustomHighlightProvider(Blocks.seebeckGenerator, SeebeckBlock.volumes)
        this.highlightProviders += MadHighlightProvider()
        this.highlightProviders += StateSensitiveCustomHighlightProvider(Blocks.anvil) { AnvilBlock.volumes.getValue(it.getValue(AnvilBlock.axis).toDirection()) }
    }

    @SubscribeEvent
    fun onHighlightDraw(e: DrawBlockHighlightEvent) {
        val pos = e.target?.let { if (it.typeOfHit == RayTraceResult.Type.BLOCK) it.blockPos else null } ?: return
        val player = e.player ?: return
        val world = player.world ?: return
        val state = world.getBlockState(pos).getActualState(world, pos)

        val highlighters = this.highlightProviders.filter { it.matches(state) }

        if (highlighters.isEmpty()) return // Skip calculations if no highlighters need to run

        val factor = e.partialTicks.toDouble()
        val x = this.findCoordinate(player.lastTickPosX, player.posX, factor, pos.x)
        val y = this.findCoordinate(player.lastTickPosY, player.posY, factor, pos.y)
        val z = this.findCoordinate(player.lastTickPosZ, player.posZ, factor, pos.z)

        withHighlightState { highlighters.forEach { it.renderHighlight(state, x, y, z) } }
    }

    private fun findCoordinate(last: Double, current: Double, factor: Double, pos: Int): Double = this.lerp(last, current, factor) * -1 + pos.toDouble()
    @Suppress("SpellCheckingInspection") private fun lerp(last: Double, current: Double, factor: Double): Double = (last + (current - last) * factor)

    private inline fun withHighlightState(block: () -> Unit) = withMatrix {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
        GlStateManager.glLineWidth(2.0F)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)
        block()
        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.glLineWidth(1.0F)
        GlStateManager.disableBlend()
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
    }
}

private class DefaultCustomHighlightProvider(block: RegistryObject<Block>, private val volumes: Iterable<AxisAlignedBB>) : CustomHighlightProvider {
    private val block by lazy(block::get)

    override fun matches(state: IBlockState): Boolean = state.block == this.block

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) {
        this.volumes.renderAllDefaultHighlights(x, y, z)
    }
}

private class StateSensitiveCustomHighlightProvider(block: RegistryObject<Block>, private val lookup: (IBlockState) -> Iterable<AxisAlignedBB>) : CustomHighlightProvider {
    private val block by lazy(block::get)

    override fun matches(state: IBlockState): Boolean = state.block == this.block

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) {
        this.lookup(state).renderAllDefaultHighlights(x, y, z)
    }
}

// Nik, however did you find this... what the fuck?
internal const val HIGHLIGHT_EXPANSION_FACTOR = 0.0020000000949949026

internal fun AxisAlignedBB.renderCorrespondingDefaultHighlight(x: Double, y: Double, z: Double) =
        this.renderCorrespondingHighlight(x, y, z, 0.0F, 0.0F, 0.0F, 0.4F)

internal fun AxisAlignedBB.renderCorrespondingHighlight(x: Double, y: Double, z: Double, r: Float, g: Float, b: Float, a: Float) = withBuffer { buffer ->
    this.draw(buffer, x, y, z, r, g, b, a, HIGHLIGHT_EXPANSION_FACTOR)
}

internal fun Iterable<AxisAlignedBB>.renderAllDefaultHighlights(x: Double, y: Double, z: Double) =
        renderAllHighlights(x, y, z, 0.0F, 0.0F, 0.0F, 0.4F)

internal fun Iterable<AxisAlignedBB>.renderAllHighlights(x: Double, y: Double, z: Double, r: Float, g: Float, b: Float, a: Float) = withBuffer { buffer ->
    this.forEach { it.draw(buffer, x, y, z, r, g, b, a, HIGHLIGHT_EXPANSION_FACTOR) }
}

private inline fun withBuffer(block: (buffer: BufferBuilder) -> Unit) {
    val tex = Tessellator.getInstance()
    val buffer = tex.buffer
    buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR)
    block(buffer)
    tex.draw()
}

private fun AxisAlignedBB.draw(buffer: BufferBuilder, x: Double, y: Double, z: Double, r: Float, g: Float, b: Float, a: Float, growFactor: Double) {
    val minX = this.minX - growFactor + x
    val minY = this.minY - growFactor + y
    val minZ = this.minZ - growFactor + z
    val maxX = this.maxX + growFactor + x
    val maxY = this.maxY + growFactor + y
    val maxZ = this.maxZ + growFactor + z
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
