package net.thesilkminer.mc.ematter.common.temperature

import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.loader.Context
import net.thesilkminer.mc.boson.api.loader.Processor
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.MOD_NAME

class TemperatureTableProcessor : Processor<JsonObject> {
    internal companion object {
        private val l = L(MOD_NAME, "TemperatureTableProcessor")

        val map: MutableMap<Block, (IBlockState, TemperatureContext) -> Int> = emptyMap<Block, (IBlockState, TemperatureContext) -> Int>().toMutableMap()
    }

    override fun process(content: JsonObject, identifier: NameSpacedString, globalContext: Context?, phaseContext: Context?) {
        ForgeRegistries.BLOCKS.valuesCollection.find { it.registryName == ResourceLocation(identifier.nameSpace, identifier.path) }?.let {
            map[it] = { _, _ -> content.get("temperature").asInt }
            l.debug("found temperature table for $it with temperature ${content.get("temperature").asInt}")
        }
    }
}
