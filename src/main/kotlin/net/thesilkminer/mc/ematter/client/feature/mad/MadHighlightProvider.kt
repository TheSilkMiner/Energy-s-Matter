package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.block.state.IBlockState
import net.thesilkminer.mc.ematter.client.clientConfiguration
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightProvider
import net.thesilkminer.mc.ematter.client.shared.renderAllDefaultHighlights
import net.thesilkminer.mc.ematter.client.shared.renderCorrespondingDefaultHighlight
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock

internal class MadHighlightProvider : CustomHighlightProvider {
    private val simpleVolume by lazy { MadBlock.useSimpleVolume }
    private val config by lazy { clientConfiguration["performance", "molecular_assembler_device"]["force_full_highlight"] }

    override fun matches(state: IBlockState): Boolean = state.block == Blocks.molecularAssemblerDevice()

    override fun renderHighlight(state: IBlockState, x: Double, y: Double, z: Double) =
            if (this.simpleVolume && !this.config().boolean) this.renderSimple(x, y, z) else this.renderMultiple(x, y, z)

    private fun renderSimple(x: Double, y: Double, z: Double) {
        MadBlock.simpleVolume.renderCorrespondingDefaultHighlight(x, y, z)
    }

    private fun renderMultiple(x: Double, y: Double, z: Double) {
        MadBlock.volumes.renderAllDefaultHighlights(x, y, z)
    }
}
