package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@CapabilityInject(INetworkManager::class)
internal lateinit var cableNetworkCapability: Capability<INetworkManager>

@Suppress("experimental_api_usage")
internal interface INetworkManager : INBTSerializable<NBTTagCompound> {

    /** the world this capability is attached to */
    var world: World

    /** returns the network at that position or null if there is none */
    operator fun get(pos: BlockPos): CableNetwork?

    fun getAllNetworks(): Set<CableNetwork>

    /** can lead to network merging */
    fun addCable(pos: BlockPos)

    /** can lead to network splitting */
    fun removeCable(pos: BlockPos)

    /** pos is position of cable this consumer is attached to, side is side of cable where it is attached; automatically loads the consumer at that position too */
    fun addConsumer(pos: BlockPos, side: Direction)

    /** pos is position of cable this consumer is attached to, side is side of cable where it is attached; automatically unloads the consumer at that position too */
    fun removeConsumer(pos: BlockPos, side: Direction)

    /** loads all consumers neighboring that position */
    fun loadConsumers(pos: BlockPos)

    /** unloads all consumers neighboring to that position */
    fun unloadConsumers(pos: BlockPos)
}

internal val World.networkManager
    get() = this.getCapability(cableNetworkCapability, null)
        ?: throw IllegalStateException("Could not get the network manager for $this. Somehow our capability got removed or the adding got bypassed. This is a serious issue and should be reported to the mod authors!")

internal val World.networks
    get() = this.networkManager.getAllNetworks()

@Suppress("experimental_api_usage")
internal val Chunk.networks
    get() = this.world.networks.filter { network -> network.cables.find { it in this.tileEntityMap.keys } != null }.toSet()
