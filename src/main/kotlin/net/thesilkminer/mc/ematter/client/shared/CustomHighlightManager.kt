@file:JvmName("CHM")

package net.thesilkminer.mc.ematter.client.shared

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.client.feature.cable.CableHighlightProvider
import net.thesilkminer.mc.ematter.client.feature.mad.MadHighlightProvider
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock
import net.thesilkminer.mc.ematter.common.feature.seebeck.SeebeckBlock

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
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
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

private class DefaultCustomHighlightProvider(block: RegistryObject<Block>, private val volumes: Sequence<AxisAlignedBB>) : CustomHighlightProvider {
    private val block by lazy(block::get)

    override fun matches(state: IBlockState): Boolean = state.block == this.block

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) {
        this.volumes.map { it.expandForHighlight().offset(x, y, z) }.forEach { it.renderDefaultHighlight() }
    }
}

// Nik, however did you find this... what the fuck?
internal const val HIGHLIGHT_EXPANSION_FACTOR = 0.0020000000949949026

internal fun AxisAlignedBB.expandForHighlight() = this.grow(HIGHLIGHT_EXPANSION_FACTOR)

internal fun AxisAlignedBB.renderDefaultHighlight() {
    RenderGlobal.drawSelectionBoundingBox(this,0.0F, 0.0F, 0.0F, 0.4F)
}
