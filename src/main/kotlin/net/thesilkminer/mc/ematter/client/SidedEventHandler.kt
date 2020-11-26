/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

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
