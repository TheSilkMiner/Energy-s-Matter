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
import net.thesilkminer.mc.ematter.common.BlockEntityRegistration
import net.thesilkminer.mc.ematter.common.ConfigurationRegistrationHandler
import net.thesilkminer.mc.ematter.common.attachBlocksListener
import net.thesilkminer.mc.ematter.common.attachItemsListener
import net.thesilkminer.mc.ematter.common.feature.cable.capability.NetworkManagerCapabilityHandler
import net.thesilkminer.mc.ematter.common.network.GuiHandler
import net.thesilkminer.mc.ematter.common.network.setUpNetworkChannel
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.MadRecipeCapabilityHandler
import net.thesilkminer.mc.ematter.common.recipe.mad.step.attachSteppingFunctionListener
import net.thesilkminer.mc.ematter.common.shared.registerAdditionalLifecycleEventHandlers
import net.thesilkminer.mc.ematter.common.system.completeSystemLoading
import net.thesilkminer.mc.ematter.common.system.handleSystemsSetup
import net.thesilkminer.mc.ematter.common.system.loadSystems
import net.thesilkminer.mc.ematter.common.world.registerWorldGenerationProviders
import net.thesilkminer.mc.ematter.compatibility.CompatibilityProviderHandler

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
            handleSystemsSetup(it)
            it.register(BlockEntityRegistration)
            it.register(CompatibilityProviderHandler)
            it.register(ConfigurationRegistrationHandler)
            registerAdditionalLifecycleEventHandlers(it)
            onlyOn(Distribution.CLIENT) { { SidedEventHandler.setUpSidedHandlers(it) } }
        }
    }

    @Mod.EventHandler
    fun onPreInitialization(e: FMLPreInitializationEvent) {
        l.info("Pre-initialization")
        MinecraftForge.EVENT_BUS.let {
            l.info("Setting up additional event handlers")
            it.register(MadRecipeCapabilityHandler)
            it.register(NetworkManagerCapabilityHandler)
        }
        CompatibilityProviderHandler.firePreInitializationEvent()
        // Since model loading happens between pre-init and init, I'm assuming this is where model loaders
        // should be registered
        onlyOn(Distribution.CLIENT) { SidedEventHandler::registerCustomModelLoaders }
    }

    @ExperimentalUnsignedTypes
    @Mod.EventHandler
    fun onInitialization(e: FMLInitializationEvent) {
        l.info("Initialization")
        MadRecipeCapabilityHandler.registerCapability()
        NetworkManagerCapabilityHandler.registerCapability()
        setUpNetworkChannel()
        registerWorldGenerationProviders()
        GuiHandler().register()
        onlyOn(Distribution.CLIENT) { SidedEventHandler::registerBlockEntityRenders }
        loadSystems()
    }

    @Mod.EventHandler
    fun onLoadFinished(e: BosonPreAvailableEvent) {
        l.info("Pre available")
        completeSystemLoading()
    }

    @Mod.EventHandler
    fun onFingerprintViolation(e: FMLFingerprintViolationEvent) {
        logViolationMessage(MOD_NAME, e)
    }
}
