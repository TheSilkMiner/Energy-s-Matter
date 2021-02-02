package net.thesilkminer.mc.ematter.client.feature.cable

import net.minecraft.block.state.IBlockState
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightProvider
import net.thesilkminer.mc.ematter.client.shared.expandForHighlight
import net.thesilkminer.mc.ematter.client.shared.renderDefaultHighlight
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock

internal class CableHighlightProvider : CustomHighlightProvider {
    private companion object {
        private val directions = Direction.values().asSequence()
    }

    override fun matches(state: IBlockState): Boolean = state.block == Blocks.cable()

    override fun renderBoundingBox(state: IBlockState, x: Double, y: Double, z: Double) {
        directions.filter { state.getValue(CableBlock.connections[it] ?: throw IllegalStateException("No property for direction '$it' found: this is a critical error")) }
                .map { CableBlock.boxes[it]?.expandForHighlight()?.offset(x, y, z) }
                .filterNotNull()
                .forEach { it.renderDefaultHighlight() }
    }
}
