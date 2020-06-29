package net.thesilkminer.mc.ematter.common.recipe.mad.capability

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeCapabilitySyncPacket
import net.thesilkminer.mc.ematter.common.network.sendPacket
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper

internal object MadRecipeCapabilityHandler {
    private class MadRecipeCapabilityPlayerProvider : ICapabilityProvider, ICapabilitySerializable<NBTTagCompound> {
        private val capabilityInstance by lazy { CraftedMadRecipesAmountCapability() }

        override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
                if (capability == craftedMadRecipesAmountCapability && facing == null) this.capabilityInstance.uncheckedCast() else null

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) = capability == craftedMadRecipesAmountCapability && facing == null
        override fun deserializeNBT(nbt: NBTTagCompound?) = this.capabilityInstance.deserializeNBT(nbt)
        override fun serializeNBT(): NBTTagCompound = this.capabilityInstance.serializeNBT()
    }

    private class MadRecipeCapabilityStorage : Capability.IStorage<CraftedMadRecipesAmount> {
        override fun readNBT(capability: Capability<CraftedMadRecipesAmount>?, instance: CraftedMadRecipesAmount?, side: EnumFacing?, nbt: NBTBase?) {
            if (capability != craftedMadRecipesAmountCapability) return
            if (instance == null) throw IllegalArgumentException("Unable to read data into a null capability instance")
            if (nbt !is NBTTagCompound) throw IllegalArgumentException("The NBT type ${if (nbt == null) "null" else nbt::class.java.simpleName} isn't valid for this capability")
            instance.deserializeNBT(nbt)
        }

        override fun writeNBT(capability: Capability<CraftedMadRecipesAmount>?, instance: CraftedMadRecipesAmount?, side: EnumFacing?): NBTBase? {
            if (capability != craftedMadRecipesAmountCapability) return null
            if (instance == null) throw IllegalArgumentException("Unable to serialize data from a null capability instance")
            return instance.serializeNBT()
        }
    }

    internal fun registerCapability() = CapabilityManager.INSTANCE.register(CraftedMadRecipesAmount::class.java, MadRecipeCapabilityStorage(), ::CraftedMadRecipesAmountCapability)

    @SubscribeEvent
    @Suppress("unused")
    fun onEntityCapabilityAttach(event: AttachCapabilitiesEvent<Entity>) {
        (event.`object` as? EntityPlayer)?.let {
            event.addCapability(ResourceLocation(MOD_ID, "crafted_mad_recipes_amount"), MadRecipeCapabilityPlayerProvider())
        }
    }

    @SubscribeEvent
    @Suppress("unused")
    fun onPlayerClone(event: PlayerEvent.Clone) {
        if (!event.isWasDeath) return
        val oldData = event.original.getCapability(craftedMadRecipesAmountCapability, null) ?: return
        val newData = event.entityPlayer.getCapability(craftedMadRecipesAmountCapability, null) ?: return
        newData.deserializeNBT(oldData.serializeNBT())
    }

    @SubscribeEvent
    @Suppress("unused")
    fun onPlayerLoggedIn(event: net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent) {
        this.syncCapabilities(event.player)
    }

    @SubscribeEvent
    @Suppress("unused")
    fun onPlayerChangedDimension(event: net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent) {
        this.syncCapabilities(event.player)
    }

    @SubscribeEvent
    @Suppress("unused")
    fun onPlayerRespawn(event: net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent) {
        this.syncCapabilities(event.player)
    }

    @SubscribeEvent
    @Suppress("unused")
    fun onItemCrafted(event: net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent) {
        // The below explicit equality is needed due to nullable checks being in place
        if (event.craftMatrix?.let { it !is CraftingInventoryWrapper || it.containerClass != MadContainer::class } == true) return
        this.syncCapabilities(event.player)
    }

    private fun syncCapabilities(player: EntityPlayer) {
        if (player !is EntityPlayerMP) return
        val cap = player.getCapability(craftedMadRecipesAmountCapability, null) ?: return
        val compound = cap.serializeNBT()
        player.sendPacket(MadRecipeCapabilitySyncPacket(compound))
    }
}
