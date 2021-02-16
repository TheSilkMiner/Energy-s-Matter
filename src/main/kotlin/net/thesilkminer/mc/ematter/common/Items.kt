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
import net.thesilkminer.mc.ematter.common.feature.thermometer.ThermometerItem

private val customItemBlocks = listOf<RegistryObject<out Block>>(Blocks.molecularAssemblerDevice)

private val itemList = mutableListOf<RegistryObject<out Item>>()
private val itemRegistry = DeferredRegister(MOD_ID, ForgeRegistries.ITEMS).also { registerItemBlocks(it) }

@Suppress("unused")
internal object Items {
    val copperIngot = register("copper_ingot") { Item().setCreativeTab(mainItemGroup).setTranslationKey("ematter.copper_ingot") }
    val copperSheet = register("copper_sheet") { Item().setCreativeTab(mainItemGroup).setTranslationKey("ematter.copper_sheet") }
    val thermometer = register("thermometer") {
        ThermometerItem().setCreativeTab(mainItemGroup).setTranslationKey("ematter.thermometer").setMaxStackSize(1).setFull3D()
    }
    val rawCopper = register("raw_copper") { Item().setCreativeTab(mainItemGroup).setTranslationKey("ematter.raw_copper") }
}

internal object ItemBlocks {
    val molecularAssemblerDevice = register(Blocks.molecularAssemblerDevice.name.path) {
        ItemMultiTexture(Blocks.molecularAssemblerDevice(), Blocks.molecularAssemblerDevice()) { MadTier.fromMeta(it.metadata).translationKey }
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
