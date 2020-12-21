package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@CapabilityInject(INetworkManager::class)
internal lateinit var cableNetworkCapability: Capability<INetworkManager>

internal interface INetworkManager : INBTSerializable<NBTTagCompound> {

    var world: World

    @ExperimentalUnsignedTypes
    operator fun get(pos: BlockPos): CableNetwork?

    fun add(pos: BlockPos)
    fun remove(pos: BlockPos)

    fun addConsumer(pos: BlockPos)
    fun removeConsumer(pos: BlockPos)

    fun loadConsumers(pos: BlockPos)
    fun unloadConsumers(pos: BlockPos)
}

internal val World.networkManager get() = this.getCapability(cableNetworkCapability, null)
