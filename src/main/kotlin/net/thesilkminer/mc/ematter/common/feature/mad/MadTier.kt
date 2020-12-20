/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

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
