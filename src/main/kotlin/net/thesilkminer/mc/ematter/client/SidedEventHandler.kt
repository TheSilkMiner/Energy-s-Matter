package net.thesilkminer.mc.ematter.client

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.client.shared.TriangleBasedModelLoader
import net.thesilkminer.mc.ematter.common.ItemBlocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier
import net.thesilkminer.mc.ematter.common.items

object SidedEventHandler {
    private val customModelItems = listOf<RegistryObject<out Item>>(ItemBlocks.molecularAssemblerDevice)

    internal fun registerCustomModelLoaders() {
        TriangleBasedModelLoader().register()
    }

    @SubscribeEvent
    fun onModelRegistration(e: ModelRegistryEvent) {
        // Loop for normal models
        items.asSequence().filter { it !in customModelItems }.map { it() }.forEach { it.registerDefaultModel() }
        // Special cases
        ItemBlocks.molecularAssemblerDevice().apply {
            val registryName = this.registryName!!
            MadTier.values().forEach { this.registerDefaultModel(it.targetMeta, ResourceLocation(registryName.namespace, "${registryName.path}/${it.getName()}")) }
        }
    }

    private fun Item.registerDefaultModel(metadata: Int = 0, registryName: ResourceLocation = this.registryName!!) =
            ModelLoader.setCustomModelResourceLocation(this, metadata, ModelResourceLocation(registryName, "inventory"))
}
