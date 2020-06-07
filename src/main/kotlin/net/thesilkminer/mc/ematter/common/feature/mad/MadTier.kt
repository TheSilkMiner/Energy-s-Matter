package net.thesilkminer.mc.ematter.common.feature.mad

import net.minecraft.util.IStringSerializable

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
internal enum class MadTier(private val nbtName: String, val targetMeta: Int, val translationKey: String, val capacity: ULong) : IStringSerializable {
    BASIC("basic", 0, "basic", 3_141UL), // * 79
    STANDARD("standard", 1, "standard", 248_139UL), // * 191
    ADVANCED("advanced", 2, "advanced", 47_394_549UL), // * 311
    ELITE("elite", 3, "elite", 14_739_704_739UL);

    internal companion object {
        internal fun fromMeta(meta: Int): MadTier = values().let { it[if (meta < 0 || meta >= it.count()) 0 else meta] }
    }

    override fun getName() = this.nbtName
}
