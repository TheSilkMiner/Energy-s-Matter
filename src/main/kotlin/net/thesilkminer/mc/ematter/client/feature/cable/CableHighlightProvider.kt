package net.thesilkminer.mc.ematter.client.feature.cable

import net.minecraft.block.state.IBlockState
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightProvider
import net.thesilkminer.mc.ematter.client.shared.renderCorrespondingDefaultHighlight
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock

internal class CableHighlightProvider : CustomHighlightProvider {
    private companion object {
        private val directions = Direction.values().asSequence()
    }

    override fun matches(state: IBlockState): Boolean = state.block == Blocks.cable()

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) {
        directions.forEach {
            if (state.getValue(CableBlock.connections.getValue(it))) {
                CableBlock.volumes.getValue(it).renderCorrespondingDefaultHighlight(x, y, z)
            }
        }
    }
}
