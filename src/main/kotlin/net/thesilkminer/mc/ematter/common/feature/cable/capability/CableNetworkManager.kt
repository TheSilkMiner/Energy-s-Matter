package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.ematter.common.feature.cable.CableNetwork

@CapabilityInject(NetworkManager::class)
internal lateinit var cableNetworkCapability: Capability<NetworkManager>

internal interface NetworkManager : INBTSerializable<NBTTagCompound> {
    @ExperimentalUnsignedTypes
    operator fun get(pos: BlockPos): CableNetwork?

    fun add(pos: BlockPos)
    fun remove(pos: BlockPos)
}

internal fun World.getNetworkManager() = this.getCapability(cableNetworkCapability, null)
