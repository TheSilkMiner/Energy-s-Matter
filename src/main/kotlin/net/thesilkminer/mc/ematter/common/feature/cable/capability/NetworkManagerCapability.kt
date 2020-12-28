package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@Suppress("EXPERIMENTAL_API_USAGE")
internal class NetworkManagerCapability : INetworkManager {

    // gets set by NetworkManagerCapabilityProvider
    override lateinit var world: World

    private val networks: MutableSet<CableNetwork> = mutableSetOf()

    // INetworkManager
    override fun get(pos: BlockPos): CableNetwork? = this.networks.find { pos in it }

    override fun addCable(pos: BlockPos) {
        val adjacentNetworks: Set<CableNetwork> = pos.getAdjacentNetworks()

        // 0 -> no networks adjacent -> create new
        // 1 -> add to only one network -> just add it
        // more -> pos will be a node -> merge adjacent networks
        when (adjacentNetworks.count()) {
            0 -> this.networks.add(CableNetwork(this.world).apply { this.cables.add(pos) })
            1 -> adjacentNetworks.first().cables.add(pos)
            else -> this.merge(*adjacentNetworks.toTypedArray(), mergePos = pos)
        }
    }

    override fun removeCable(pos: BlockPos) {
        this[pos]?.let { network ->
            val adjacentCables: Set<BlockPos> = pos.getAdjacentCables()

            // 0 -> last remaining pos in network -> remove network
            // 1 -> endpoint of network -> just remove it
            // more -> cable is a node -> split network
            when (adjacentCables.count()) {
                0 -> this.networks.remove(network)
                1 -> network.cables.remove(pos)
                else -> this.split(network, splitPos = pos)
            }
        }
    }

    override fun addConsumer(pos: BlockPos, side: Direction) {
        // TODO("n1kx", "lol what have I done, pos is pos of consumer not of cable, FIX!")
        this[pos]?.let { network ->
            network.consumers.add(pos)

            network.loadedConsumers.putIfAbsent(pos, mutableSetOf(side))?.let { sides ->
                if (sides.add(side)) network.reloadConsumerCache()
            }
            network.reloadConsumerCache()
        }
    }

    override fun removeConsumer(pos: BlockPos, side: Direction) {
        // TODO("n1kx", "lol what have I done, pos is pos of consumer not of cable, FIX!")
        this[pos]?.let { network ->
            network.consumers.remove(pos)

            network.loadedConsumers[pos]?.let { sides ->
                if (sides.remove(side)) {
                    if (sides.isEmpty()) network.loadedConsumers.remove(pos)
                    network.reloadConsumerCache()
                }
            }
        }
    }

    override fun loadConsumers(pos: BlockPos) {
        this[pos]?.let { network ->
            val flag = Direction.values().asSequence()
                .map { pos.offset(it) to it.opposite }
                .filter { it.first in network.consumers }
                .onEach { network.loadedConsumers.putIfAbsent(it.first, mutableSetOf(it.second))?.add(it.second) }
                .count() != 0

            if (flag) network.reloadConsumerCache()
        }
    }

    override fun unloadConsumers(pos: BlockPos) {
        this[pos]?.let { network ->
            val flag = Direction.values().asSequence()
                .map { pos.offset(it) to it.opposite }
                .filter { it.first in network.consumers }
                .onEach {
                    network.loadedConsumers[it.first]?.let { sides ->
                        if (sides.remove(it.second)) {
                            if (sides.isEmpty()) network.loadedConsumers.remove(pos)
                        }
                    }
                }
                .count() != 0

            if (flag) network.reloadConsumerCache() // flag can be true even if loadedConsumers did not change; let me know if you have a better idea
        }
    }

    /** merges all networks into the first; merge pos gets added to this network too */
    private fun merge(vararg networks: CableNetwork, mergePos: BlockPos) {
        for (i in 1 until networks.size) {
            networks.first().cables.addAll(networks[i].cables)
            this.networks.remove(networks[i])
        }

        networks.first().cables.add(mergePos)
    }

    /** removes split pos and then re adds each position to this manager; this "splits" the network */
    private fun split(network: CableNetwork, splitPos: BlockPos) {
        this.networks.remove(network)
        network.cables.remove(splitPos)
        network.cables.forEach { this.addCable(it) }
    }

    // INBTSerializable
    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        this.networks.forEachIndexed { index, cableNetwork -> tag.setTag("$index", cableNetwork.serializeNBT()) }
        return tag
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) {
        this.networks.clear()
        nbt?.keySet?.forEach { index ->
            (nbt.getTag(index) as? NBTTagCompound)?.let { tag ->
                this.networks.add(CableNetwork(this.world).apply { this.deserializeNBT(tag) })
            }
        }
    }

    // helper functions
    private fun BlockPos.getAdjacentNetworks(): Set<CableNetwork> = Direction.values().asSequence()
        .map { this@NetworkManagerCapability[this.offset(it.toFacing())] }
        .filter { it != null }
        .map { it as CableNetwork }
        .toSet()

    private fun BlockPos.getAdjacentCables(): Set<BlockPos> = Direction.values().asSequence()
        .map { this.offset(it.toFacing()) }
        .filter { this@NetworkManagerCapability[it] != null }
        .toSet()
}
