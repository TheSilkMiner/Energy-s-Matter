package net.thesilkminer.mc.ematter.common

import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.temperature.loadTemperatureTables

@Mod.EventBusSubscriber(modid = MOD_ID)
@Suppress("unused")
object RegistrationHandler {
    private val l = L(MOD_NAME, "Registration Handler")

    @JvmStatic
    @SubscribeEvent
    fun onRecipeRegistry(e: RegistryEvent.Register<IRecipe>) {
        //not sure if that's the right event. I just took one where I know that it's after the Block Registry
        l.debug("Received Register event for Recipes: starting loading of temperature tables")
        loadTemperatureTables()
    }
}
