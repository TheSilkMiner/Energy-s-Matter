package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.util.math.BlockPos
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.ematter.MOD_ID

// TODO("n1kx", "save networks")
@ExperimentalUnsignedTypes
internal class CableNetwork private constructor() {

    internal companion object {

        val l = L(MOD_ID, "cable networks")

        private val networks: MutableSet<CableNetwork> = mutableSetOf()

        operator fun get(pos: BlockPos): CableNetwork? = this.networks.find { pos in it }

        fun add(pos: BlockPos) {
            if (this[pos] != null) return

            val adjacentNetworks: Set<CableNetwork> = pos.getAdjacentNetworks()

            // 0 -> no networks adjacent -> create new network
            // 1 -> gets added to just one network -> just add it
            // more -> cable will be a node -> merging goes brrr
            when (adjacentNetworks.count()) {
                0 -> this.networks.add(CableNetwork().apply { this.cables.add(pos) })
                1 -> adjacentNetworks.first().cables.add(pos)
                else -> this.merge(*adjacentNetworks.toTypedArray(), mergePos = pos)
            }
        }

        fun remove(pos: BlockPos) {
            this[pos]?.let {
                val adjacentCables: Set<BlockPos> = pos.getAdjacentCables()

                // 0 -> network with only one cable -> network gets yeeted
                // 1 -> endpoint of a network -> just need to remove it
                // more -> cable is a node -> we need to split the network
                when (adjacentCables.count()) {
                    0 -> this.networks.remove(it)
                    1 -> it.cables.remove(pos)
                    else -> this.split(it, splitPos = pos)
                }
            }
        }

        // TODO("n1kx", "maybe size check could be more efficient")
        private fun merge(vararg networks: CableNetwork, mergePos: BlockPos) {
            for (i in 1 until networks.size) {
                networks.first().cables.addAll(networks[i].cables)
                this.networks.remove(networks[i])
            }

            networks.first().cables.add(mergePos)
        }

        private fun split(network: CableNetwork, splitPos: BlockPos) {
            this.networks.remove(network)
            network.cables.remove(splitPos)
            network.cables.forEach { this.add(it) } // re adding everything just recalculates the networks and since the split position got removed other cables will think that there is a gap
        }

        fun BlockPos.getAdjacentNetworks(): Set<CableNetwork> = Direction.values().asSequence()
            .map { this@Companion[this.offset(it.toFacing())] }
            .filter { it != null }
            .map { it as CableNetwork }
            .toSet()

        fun BlockPos.getAdjacentCables(): Set<BlockPos> = Direction.values().asSequence()
            .map { this.offset(it.toFacing()) }
            .filter { this@Companion[it] != null }
            .toSet()
    }

    // block positions do not load chunks when I use them; TE do, so using them brings additional problems i do not wanna deal with
    private val cables: MutableSet<BlockPos> = mutableSetOf()

    // TODO("n1kx", "think about funny ways to store, add and remove consumers")

    private operator fun contains(pos: BlockPos): Boolean = pos in this.cables
}
