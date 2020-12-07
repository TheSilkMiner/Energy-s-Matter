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

@file:JvmName("B")

package net.thesilkminer.mc.ematter.common

import net.minecraft.block.Block
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock
import net.thesilkminer.mc.ematter.common.feature.seebeck.SeebeckBlock

private val blockList = mutableListOf<RegistryObject<out Block>>()
private val blockRegistry = DeferredRegister(MOD_ID, ForgeRegistries.BLOCKS)

@Suppress("unused")
internal object Blocks {
    val molecularAssemblerDevice = register("molecular_assembler_device") {
        MadBlock().setCreativeTab(mainItemGroup).setTranslationKey("ematter.molecular_assembler_device").setHardness(8.0F).apply { this.setHarvestLevel("pickaxe", 2) }
    }
    val seebeckGenerator = register("seebeck_generator") {
        SeebeckBlock().setCreativeTab(mainItemGroup).setTranslationKey("ematter.seebeck_generator").setHardness(8.0F).apply { this.setHarvestLevel("pickaxe", 2) }
    }
    val cable = register("cable") {
        CableBlock().setCreativeTab(mainItemGroup).setTranslationKey("ematter.cable").setHardness(5.0F).apply { this.setHarvestLevel("pickaxe", 1)}
    }
}

internal fun attachBlocksListener(bus: EventBus) = blockRegistry.subscribeOnto(bus).also { Blocks.toString() } // Statically initialize blocks
internal val blocks get() = blockList.toList()

private fun <T : Block> register(name: String, supplier: () -> T) = blockRegistry.register(name, supplier).also { blockList += it }
