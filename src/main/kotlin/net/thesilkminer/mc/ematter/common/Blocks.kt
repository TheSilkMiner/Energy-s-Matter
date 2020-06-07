@file:JvmName("B")

package net.thesilkminer.mc.ematter.common

import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlock

private val blockList = mutableListOf<RegistryObject<out Block>>()
private val blockRegistry = DeferredRegister(ForgeRegistries.BLOCKS, MOD_ID)

internal object Blocks {
    val molecularAssemblerDevice = register("molecular_assembler_device") {
        MadBlock().setCreativeTab(mainItemGroup).setTranslationKey("ematter.molecular_assembler_device").setHardness(8.0F).apply { this.setHarvestLevel("pickaxe", 2) }
    }
}

internal fun attachBlocksListener(bus: EventBus) = blockRegistry.subscribeOnto(bus)
internal val blocks get() = blockList.toList()

private fun <T : Block> register(name: String, supplier: () -> T) = blockRegistry.register(name, supplier).also { blockList += it }
