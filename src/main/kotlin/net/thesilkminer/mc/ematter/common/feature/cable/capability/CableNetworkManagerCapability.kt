package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@ExperimentalUnsignedTypes
internal class CableNetworkManagerCapability : NetworkManager {

    private val networks: MutableSet<CableNetwork> = mutableSetOf()

    override fun get(pos: BlockPos): CableNetwork? = this.networks.find { pos in it }

    override fun add(pos: BlockPos) {
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

    override fun remove(pos: BlockPos) {
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

    private fun BlockPos.getAdjacentNetworks(): Set<CableNetwork> = Direction.values().asSequence()
        .map { this@CableNetworkManagerCapability[this.offset(it.toFacing())] }
        .filter { it != null }
        .map { it as CableNetwork }
        .toSet()

    private fun BlockPos.getAdjacentCables(): Set<BlockPos> = Direction.values().asSequence()
        .map { this.offset(it.toFacing()) }
        .filter { this@CableNetworkManagerCapability[it] != null }
        .toSet()

    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        this.networks.forEachIndexed { index, cableNetwork -> tag.setTag("$index", cableNetwork.serializeNBT()) }
        return tag
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) {
        this.networks.clear()
        nbt?.keySet?.forEach { index ->
            (nbt.getTag(index) as? NBTTagCompound)?.let { tag ->
                this.networks.add(CableNetwork().apply { this.deserializeNBT(tag) })
            }
        }
    }
}
