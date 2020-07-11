package net.thesilkminer.mc.ematter.compatibility.crafttweaker

import net.minecraftforge.common.MinecraftForge
import net.thesilkminer.mc.boson.api.modid.CRAFT_TWEAKER_2
import net.thesilkminer.mc.boson.prefab.compatibility.ModCompatibilityProvider
import net.thesilkminer.mc.ematter.compatibility.EnergyIsMatterCompatibilityProvider
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.step.attachCraftTweakerSteppingFunctionSerializerRegistry

internal class CraftTweakerCompatibilityProvider : ModCompatibilityProvider(CRAFT_TWEAKER_2), EnergyIsMatterCompatibilityProvider {
    override fun onPreInitialization() {
        attachCraftTweakerSteppingFunctionSerializerRegistry(MinecraftForge.EVENT_BUS)
    }
}
