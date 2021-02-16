@file:JvmName("OGM")

package net.thesilkminer.mc.ematter.common.world.ore

import net.minecraft.util.math.ChunkPos
import net.minecraft.world.DimensionType
import net.minecraft.world.World
import net.minecraft.world.biome.Biome
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.gen.ChunkGeneratorDebug
import net.minecraft.world.gen.ChunkGeneratorEnd
import net.minecraft.world.gen.ChunkGeneratorHell
import net.minecraft.world.gen.IChunkGenerator
import net.minecraftforge.common.BiomeDictionary
import net.minecraftforge.fml.common.IWorldGenerator
import net.minecraftforge.fml.common.registry.GameRegistry
import net.thesilkminer.mc.ematter.common.Blocks
import java.util.Random

private const val ORE_GENERATION_WEIGHT = 5 // 4? 20? 42? 1? What the fuck?

internal data class OreGenerationContext(val world: World, val chunkPos: ChunkPos, val biome: Biome, val dimensionType: DimensionType,
                                         val chunkGenerator: IChunkGenerator, val chunkProvider: IChunkProvider)

internal interface OreGenerationProvider {
    fun canGenerate(random: Random, context: OreGenerationContext): Boolean
    fun generateOre(random: Random, context: OreGenerationContext)
}

private object OreGenerationManager {
    private class DelegatingGenerationManager(private val providersView: List<OreGenerationProvider>) : IWorldGenerator {
        override fun generate(random: Random?, chunkX: Int, chunkZ: Int, world: World?, chunkGenerator: IChunkGenerator?, chunkProvider: IChunkProvider?) {
            if (random == null) return

            val chunkPos = ChunkPos(chunkX, chunkZ)
            val biome = world?.getBiomeForCoordsBody(chunkPos.getBlock(8, 0, 8)) ?: return
            val dimensionType = world.provider.dimensionType.adjustForGenerator(chunkGenerator ?: return, biome) ?: return
            val context = OreGenerationContext(world, chunkPos, biome, dimensionType, chunkGenerator, chunkProvider ?: return)

            this.providersView.asSequence()
                    .filter { it.canGenerate(random, context) }
                    .forEach { it.generateOre(random, context) }
        }

        private fun DimensionType.adjustForGenerator(generator: IChunkGenerator, biome: Biome): DimensionType? = when {
            BiomeDictionary.Type.NETHER in BiomeDictionary.getTypes(biome) || generator is ChunkGeneratorHell -> DimensionType.NETHER
            BiomeDictionary.Type.END in BiomeDictionary.getTypes(biome) || generator is ChunkGeneratorEnd -> DimensionType.THE_END
            generator is ChunkGeneratorDebug -> null
            else -> this
        }
    }

    private val registeredProviders = mutableListOf<OreGenerationProvider>()
    val delegatingGenerator by lazy { DelegatingGenerationManager(this.registeredProviders) as IWorldGenerator }

    fun registerOreGenerationProviders() {
        registeredProviders += DefaultOreGenerationProvider(Blocks.copperOre, null, DimensionType.OVERWORLD, 4..63, 9, 20)
    }
}

internal fun registerOreGenerationManager() {
    OreGenerationManager.registerOreGenerationProviders()
    GameRegistry.registerWorldGenerator(OreGenerationManager.delegatingGenerator, ORE_GENERATION_WEIGHT)
}
