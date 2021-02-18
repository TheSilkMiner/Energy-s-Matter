package net.thesilkminer.mc.ematter.common.feature.cable.capability

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.MOD_ID

@Suppress("unused", "experimental_api_usage")
object NetworkManagerCapabilityHandler {

    private class NetworkManagerCapabilityProvider : ICapabilityProvider, ICapabilitySerializable<NBTTagCompound> {

        // gets set in #onWorldCapabilityAttach
        lateinit var world: World

        private val capabilityInstance by lazy { NetworkManagerCapability().apply { this.world = this@NetworkManagerCapabilityProvider.world } }

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
            capability == cableNetworkCapability && facing == null

        override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
            if (capability == cableNetworkCapability && facing == null) this.capabilityInstance.uncheckedCast() else null

        override fun serializeNBT(): NBTTagCompound =
            this.capabilityInstance.serializeNBT()

        override fun deserializeNBT(nbt: NBTTagCompound?) =
            this.capabilityInstance.deserializeNBT(nbt)
    }

    private class NetworkManagerCapabilityStorage : Capability.IStorage<NetworkManager> {

        override fun writeNBT(capability: Capability<NetworkManager>?, instance: NetworkManager?, side: EnumFacing?): NBTBase? {
            if (capability != cableNetworkCapability) return null
            if (instance == null) throw IllegalArgumentException("Unable to write data into a null capability instance")
            return instance.serializeNBT()
        }

        override fun readNBT(capability: Capability<NetworkManager>?, instance: NetworkManager?, side: EnumFacing?, nbt: NBTBase?) {
            if (capability != cableNetworkCapability) return
            if (instance == null) throw IllegalArgumentException("Unable to read data into from a null capability instance")
            if (nbt !is NBTTagCompound) throw IllegalArgumentException("The NBT type ${if (nbt == null) "null" else nbt::class.java.simpleName} isn't valid for this capability")
            instance.deserializeNBT(nbt)
        }
    }

    internal fun registerCapability() =
        CapabilityManager.INSTANCE.register(NetworkManager::class.java, NetworkManagerCapabilityStorage(), ::NetworkManagerCapability)

    @SubscribeEvent
    fun onWorldCapabilityAttach(e: AttachCapabilitiesEvent<World>) =
        e.addCapability(ResourceLocation(MOD_ID, "network_manager"), NetworkManagerCapabilityProvider().apply {
            this.world = e.`object`
        })

    // a) it's more efficient doing it like that then each cable that loads triggers a reload
    // b) it was possible that a block is not yet loaded when the network tries to access it; now we make sure that everything in the chunk is loaded before the network access something
    @SubscribeEvent
    fun onChunkLoad(e: ChunkEvent.Load) =
        if (!e.world.isRemote) e.chunk.networks.forEach { it.reloadConsumerCache() } else Unit

    @SubscribeEvent
    fun onChunkUnload(e: ChunkEvent.Unload) =
        if (!e.world.isRemote) e.chunk.networks.forEach { it.reloadConsumerCache() } else Unit
}
