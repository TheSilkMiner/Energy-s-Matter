package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.offset
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@Suppress("experimental_api_usage")
internal class NetworkManagerCapability : NetworkManager {

    // gets set by NetworkManagerCapabilityProvider
    override lateinit var world: World

    private val networks: MutableSet<CableNetwork> = mutableSetOf()

    // INetworkManager >>
    override fun get(pos: BlockPos): CableNetwork? = this.networks.find { pos in it }

    override fun getAllNetworks() = this.networks.toSet()

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
        this[pos]?.let { network ->
            network.consumers.add(pos.offset(side))

            network.loadedConsumers.putIfAbsent(pos.offset(side), mutableSetOf())
            if (network.loadedConsumers[pos.offset(side)]?.add(side.opposite) == true) network.reloadConsumerCache()
        }
    }

    override fun removeConsumer(pos: BlockPos, side: Direction) {
        this[pos]?.let { network ->
            network.consumers.remove(pos.offset(side))

            network.loadedConsumers[pos.offset(side)]?.let { sides ->
                if (sides.remove(side.opposite)) {
                    if (sides.isEmpty()) network.loadedConsumers.remove(pos.offset(side))
                    network.reloadConsumerCache()
                }
            }
        }
    }

    override fun loadConsumers(pos: BlockPos) {
        this[pos]?.let { network ->
            Direction.values().asSequence()
                .map { pos.offset(it) to it.opposite }
                .filter { it.first in network.consumers && this.world.isBlockLoaded(it.first) }
                .forEach { network.loadedConsumers.putIfAbsent(it.first, mutableSetOf(it.second))?.add(it.second) }
        }
    }

    override fun unloadConsumers(pos: BlockPos) {
        this[pos]?.let { network ->
            Direction.values().asSequence()
                .map { pos.offset(it) to it.opposite }
                .filter { it.first in network.consumers }
                .forEach {
                    network.loadedConsumers[it.first]?.let { sides ->
                        if (sides.remove(it.second)) {
                            if (sides.isEmpty()) network.loadedConsumers.remove(pos)
                        }
                    }
                }
        }
    }
    // << INetworkManager

    // network operations >>
    /** merges all networks into the first; merge pos gets added to this network too */
    private fun merge(vararg networks: CableNetwork, mergePos: BlockPos) {
        for (i in 1 until networks.size) {
            networks.first().cables.addAll(networks[i].cables)
            this.networks.remove(networks[i])
        }

        networks.first().cables.add(mergePos)
    }

    /** removes split pos and then re adds each position to this manager; that's what we call "splitting" the network */
    private fun split(network: CableNetwork, splitPos: BlockPos) {
        this.networks.remove(network)
        network.cables.remove(splitPos)
        network.cables.forEach { this.addCable(it) }
    }
    // << network operations

    // INBTSerializable >>
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
    // << INBTSerializable

    // helper functions >>
    private fun BlockPos.getAdjacentNetworks(): Set<CableNetwork> = Direction.values().asSequence()
        .map { this@NetworkManagerCapability[this.offset(it)] }
        .filter { it != null }
        .map { it as CableNetwork }
        .toSet()

    private fun BlockPos.getAdjacentCables(): Set<BlockPos> = Direction.values().asSequence()
        .map { this.offset(it) }
        .filter { this@NetworkManagerCapability[it] != null }
        .toSet()
}
