@file:JvmName("DOGP")

package net.thesilkminer.mc.ematter.common.world.ore

import net.minecraft.block.Block
import net.minecraft.block.BlockStone
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.world.DimensionType
import net.minecraft.world.biome.Biome
import net.minecraft.world.gen.feature.WorldGenMinable
import net.thesilkminer.mc.boson.api.configuration.Entry
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import java.util.Random

private val defaultReplacementPredicate: (IBlockState?) -> Boolean = { it != null && it.block == Blocks.STONE && it.getValue(BlockStone.VARIANT).isNatural }

@Suppress("GrazieInspection")
internal data class GenerationParameters(val configurationEntry: Entry?, val dimensionType: DimensionType?, val allowedHeights: IntRange,
                                         val veinSize: Int, val attempts: Int, val probability: Double = 1.0, val biomeFilter: List<Biome> = listOf(),
                                         val replacePredicate: (IBlockState?) -> Boolean = defaultReplacementPredicate) {
    init {
        require(!this.allowedHeights.isEmpty()) { "Allowed heights must not be an empty range, but instead found ${this.allowedHeights}" }
        require(256 !in this.allowedHeights) { "Max height exceeds vanilla upper bound of '255' in ${this.allowedHeights}" }
        require(-1 !in this.allowedHeights) { "Min height exceed vanilla lower bound of '0' in ${this.allowedHeights}" }
        require(this.veinSize > 0) { "Vein size must be positive, but got ${this.veinSize}" }
        require(this.attempts > 0) { "Attempts must be positive, but got ${this.attempts}" }
        require(this.probability in 0.0..1.0) { "Probability is outside of range 0.0..1.0: ${this.probability}" }
    }
}

internal class DefaultOreGenerationProvider(private val targetState: () -> IBlockState, private val generationParameters: GenerationParameters) : OreGenerationProvider {
    constructor(target: RegistryObject<Block>, generationParameters: GenerationParameters) : this({ target().defaultState }, generationParameters)
    constructor(targetState: () -> IBlockState, configurationEntry: Entry?, dimensionType: DimensionType?, allowedHeights: IntRange,
                veinSize: Int, attempts: Int, probability: Double = 1.0, biomeFilter: List<Biome> = listOf(), replacePredicate: (IBlockState?) -> Boolean = defaultReplacementPredicate) :
            this(targetState, GenerationParameters(configurationEntry, dimensionType, allowedHeights, veinSize, attempts, probability, biomeFilter, replacePredicate))
    constructor(target: RegistryObject<Block>, configurationEntry: Entry?, dimensionType: DimensionType?, allowedHeights: IntRange,
                veinSize: Int, attempts: Int, probability: Double = 1.0, biomeFilter: List<Biome> = listOf(), replacePredicate: (IBlockState?) -> Boolean = defaultReplacementPredicate) :
            this(target, GenerationParameters(configurationEntry, dimensionType, allowedHeights, veinSize, attempts, probability, biomeFilter, replacePredicate))

    private val vanillaGenerator by lazy { WorldGenMinable(this.targetState(), this.generationParameters.veinSize, this.generationParameters.replacePredicate) }

    override fun canGenerate(random: Random, context: OreGenerationContext): Boolean {
        val configurationEntry = this.generationParameters.configurationEntry
        val dimensionType = this.generationParameters.dimensionType
        val biomeFilter = this.generationParameters.biomeFilter

        if (configurationEntry != null && !configurationEntry().boolean) return false
        if (dimensionType != null && dimensionType != context.dimensionType) return false
        if (biomeFilter.isNotEmpty() && context.biome !in biomeFilter) return false

        return random.nextDouble() < this.generationParameters.probability
    }

    override fun generateOre(random: Random, context: OreGenerationContext) {
        (0 until this.generationParameters.attempts).forEach { _ ->
            val pos = context.chunkPos.getBlock(random.nextInt(16), this.generationParameters.allowedHeights.random(random), random.nextInt(16))
            this.vanillaGenerator.generate(context.world, random, pos)
        }
    }

    @Suppress("NOTHING_TO_INLINE") private inline fun IntRange.random(random: Random) = random.nextInt(this.last - this.first) + this.first
}
