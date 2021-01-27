package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.block.state.IBlockState
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightProvider
import net.thesilkminer.mc.ematter.client.shared.expandForHighlight
import net.thesilkminer.mc.ematter.client.shared.renderDefaultHighlight
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock

internal class MadHighlightProvider : CustomHighlightProvider {
    private val madBlock by lazy(Blocks.molecularAssemblerDevice::get)

    override fun matches(state: IBlockState): Boolean = state.block == this.madBlock

    override fun renderBoundingBox(state: IBlockState, x: Double, y: Double, z: Double) {
        MadBlock.volumes.map { it.expandForHighlight().offset(x, y, z) }.forEach { renderDefaultHighlight(it) }
    }
}
