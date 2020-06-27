@file:JvmName("I")

package net.thesilkminer.mc.ematter.common

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemMultiTexture
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier

private val customItemBlocks = listOf<RegistryObject<out Block>>(Blocks.molecularAssemblerDevice)

private val itemList = mutableListOf<RegistryObject<out Item>>()
private val itemRegistry = DeferredRegister(MOD_ID, ForgeRegistries.ITEMS).also { registerItemBlocks(it) }

internal object Items

internal object ItemBlocks {
    val molecularAssemblerDevice = register(Blocks.molecularAssemblerDevice.name.path) {
        ItemMultiTexture(Blocks.molecularAssemblerDevice(), Blocks.molecularAssemblerDevice(), ItemMultiTexture.Mapper { MadTier.fromMeta(it.metadata).translationKey })
    }
}

internal fun attachItemsListener(bus: EventBus) = itemRegistry.subscribeOnto(bus).also { sequenceOf(Items, ItemBlocks).forEach { it.toString() } } // Statically initialize items
internal val items get() = itemList.toList()

private fun <T : Item> register(name: String, supplier: () -> T) = register(itemRegistry, name, supplier)
private fun <T : Item> register(register: DeferredRegister<Item>, name: String, supplier: () -> T) = register.register(name, supplier).also { itemList += it }

private fun registerItemBlocks(registry: DeferredRegister<Item>) {
    // Loop for normal item blocks
    blocks.asSequence().filter { it !in customItemBlocks }.forEach { register(registry, it.name.path) { ItemBlock(it()) } }
    // Special cases
    // MAD ItemBlock is registered above
}
