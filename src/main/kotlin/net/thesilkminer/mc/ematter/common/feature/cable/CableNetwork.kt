package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.util.math.BlockPos

// TODO("n1kx", "save networks")
@ExperimentalUnsignedTypes
internal class CableNetwork {

    // block positions do not load chunks when I use them; TE do, so using them brings additional problems i do not wanna deal with
    // TODO("n1kx", "call some mark dirty methods in here")
    val cables: MutableSet<BlockPos> = object : LinkedHashSet<BlockPos>() {
        override fun add(element: BlockPos): Boolean {
            this.addAll(mutableSetOf(element))
            return super.add(element)
        }

        // TEST("n1kx", "test if calling removeAll and retainAll calls the remove message")
        override fun remove(element: BlockPos): Boolean {
            return super.remove(element)
        }

        override fun clear() {
            super.clear()
        }
    }

    // TODO("n1kx", "think about funny ways to store, add and remove consumers")

    operator fun contains(pos: BlockPos): Boolean = pos in this.cables

}
