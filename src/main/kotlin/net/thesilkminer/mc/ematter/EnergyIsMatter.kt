package net.thesilkminer.mc.ematter

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLConstructionEvent
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.distribution.onlyOn
import net.thesilkminer.mc.boson.api.event.BosonPreAvailableEvent
import net.thesilkminer.mc.boson.api.fingerprint.logViolationMessage
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.client.SidedEventHandler
import net.thesilkminer.mc.ematter.common.TileEntityRegistration
import net.thesilkminer.mc.ematter.common.attachBlocksListener
import net.thesilkminer.mc.ematter.common.attachItemsListener
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.MadRecipeCapabilityHandler
import net.thesilkminer.mc.ematter.common.recipe.mad.step.attachSteppingFunctionListener

@Mod(modid = MOD_ID, name = MOD_NAME, version = MOD_VERSION, dependencies = MOD_DEPENDENCIES,
        acceptedMinecraftVersions = MOD_MC_VERSION, certificateFingerprint = MOD_CERTIFICATE_FINGERPRINT,
        modLanguageAdapter = KOTLIN_LANGUAGE_ADAPTER, modLanguage = "kotlin")
object EnergyIsMatter {
    private val l = L(MOD_NAME, "Lifecycle")

    @Mod.EventHandler
    fun onModConstruction(e: FMLConstructionEvent) {
        l.info("Construction")
        MinecraftForge.EVENT_BUS.let {
            l.info("Setting up registries")
            attachBlocksListener(it)
            attachItemsListener(it)
            attachSteppingFunctionListener(it)
            it.register(TileEntityRegistration)
        }
    }

    @Mod.EventHandler
    fun onPreInitialization(e: FMLPreInitializationEvent) {
        l.info("Pre-initialization")
        MinecraftForge.EVENT_BUS.let {
            l.info("Setting up additional event handlers")
            it.register(MadRecipeCapabilityHandler)
            onlyOn(Distribution.CLIENT) { { it.register(SidedEventHandler) } }
        }
    }

    @Mod.EventHandler
    fun onInitialization(e: FMLInitializationEvent) {
        MadRecipeCapabilityHandler.registerCapability()
        GuiHandler().register()
    }

    @Mod.EventHandler
    fun onLoadFinished(e: BosonPreAvailableEvent) {
        l.info("Pre available")
    }

    @Mod.EventHandler
    fun onFingerprintViolation(e: FMLFingerprintViolationEvent) {
        logViolationMessage(MOD_NAME, e)
    }
}
