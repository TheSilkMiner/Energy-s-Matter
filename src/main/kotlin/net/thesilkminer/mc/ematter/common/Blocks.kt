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
import net.minecraft.block.BlockOre
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.item.Item
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.anvil.AnvilBlock
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock
import net.thesilkminer.mc.ematter.common.feature.seebeck.SeebeckBlock
import net.thesilkminer.mc.ematter.common.feature.tool.PICK_AXE
import java.util.Random

private val blockList = mutableListOf<RegistryObject<out Block>>()
private val blockRegistry = DeferredRegister(MOD_ID, ForgeRegistries.BLOCKS)

@Suppress("unused")
internal object Blocks {
    val anvil = register("anvil") { makeBasicBlock("anvil", 5.0, resistance = 2000.0, harvestLevel = 3, constructor = ::AnvilBlock) }
    val cable = register("cable") { makeBasicBlock("cable", 5.0, harvestLevel = 1, constructor = ::CableBlock) }
    val copperBlock = register("copper_block") {
        object : Block(Material.IRON) {
            init { this.blockSoundType = SoundType.METAL }
        }.applyBlockDefaults("copper_block", 5.0, 10.0, PICK_AXE, 1)
    }
    val copperOre = register("copper_ore") {
        object : BlockOre() {
            override fun getItemDropped(state: IBlockState, rand: Random, fortune: Int): Item = Items.rawCopper()
            override fun quantityDropped(random: Random): Int = 1 + random.nextInt(2)
        }.applyBlockDefaults("copper_ore", 3.0, 5.0, PICK_AXE, 1)
    }
    val molecularAssemblerDevice = register("molecular_assembler_device") { makeBasicBlock("molecular_assembler_device", 8.0, harvestLevel = 2, constructor = ::MadBlock) }
    val seebeckGenerator = register("seebeck_generator") { makeBasicBlock("seebeck_generator", 8.0, harvestLevel = 2, constructor = ::SeebeckBlock) }
}

internal fun attachBlocksListener(bus: EventBus) = blockRegistry.subscribeOnto(bus).also { Blocks.toString() } // Statically initialize blocks
internal val blocks get() = blockList.toList()

private fun <T : Block> register(name: String, supplier: () -> T) = blockRegistry.register(name, supplier).also { blockList += it }

private inline fun makeBasicBlock(key: String, hardness: Double, resistance: Double? = null,
                                  harvestTool: String? = PICK_AXE, harvestLevel: Int? = null, constructor: () -> Block): Block =
        constructor().applyBlockDefaults(key, hardness, resistance, harvestTool, harvestLevel)
private inline fun makeMaterialBlock(material: Material, key: String, hardness: Double, resistance: Double? = null,
                                     harvestTool: String? = PICK_AXE, harvestLevel: Int? = null, constructor: (Material) -> Block = ::Block): Block =
        constructor(material).applyBlockDefaults(key, hardness, resistance, harvestTool, harvestLevel)

private fun Block.applyBlockDefaults(key: String, hardness: Double, resistance: Double?, harvestTool: String?, harvestLevel: Int?): Block {
    this.creativeTab = mainItemGroup
    this.translationKey = "$MOD_ID.$key"
    this.setHardness(hardness.toFloat())
    if (resistance != null) this.setResistance(resistance.toFloat())
    if (harvestTool != null && harvestLevel != null) this.setHarvestLevel(harvestTool, harvestLevel)
    return this
}
