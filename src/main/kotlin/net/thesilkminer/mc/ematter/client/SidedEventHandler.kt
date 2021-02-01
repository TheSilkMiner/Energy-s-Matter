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

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.event.ConfigurationRegisterEvent
import net.thesilkminer.mc.boson.api.registry.RegistryObject
import net.thesilkminer.mc.ematter.client.feature.mad.MadBlockEntityRender
import net.thesilkminer.mc.ematter.client.feature.thermometer.ThermometerOverlay
import net.thesilkminer.mc.ematter.client.shared.CustomHighlightManager
import net.thesilkminer.mc.ematter.client.shared.MultipartModelLoader
import net.thesilkminer.mc.ematter.client.shared.TriangleBasedModelLoader
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.ItemBlocks
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlockEntity
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier
import net.thesilkminer.mc.ematter.common.items
import kotlin.reflect.KClass

object SidedEventHandler {

    private val customModelItems = listOf<RegistryObject<out Item>>(ItemBlocks.molecularAssemblerDevice)

    internal fun setUpSidedHandlers(bus: EventBus) {
        bus.register(this)
        bus.register(ThermometerOverlay)
        bus.register(CustomHighlightManager.also { it.registerCustomHighlightManagers() })
    }

    internal fun registerCustomModelLoaders() {
        TriangleBasedModelLoader().register()
        MultipartModelLoader.register()
    }

    internal fun registerBlockEntityRenders() {
        MadBlockEntityRender().bindTo(MadBlockEntity::class)
    }

    @Suppress("unused")
    @SubscribeEvent
    fun onConfigurationRegistration(e: ConfigurationRegisterEvent) {
        e.configurationRegistry.registerConfiguration(clientConfiguration)
    }

    @Suppress("unused", "unused_parameter")
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

    private fun <T : TileEntity> TileEntitySpecialRenderer<T>.bindTo(target: KClass<T>) =
        ClientRegistry.bindTileEntitySpecialRenderer(target.java, this)

    private const val AABB_GROW = 0.0020000000949949026

    @Suppress("unused")
    @SubscribeEvent
    fun onDrawBlockHighlight(e: DrawBlockHighlightEvent) {
        val state = Minecraft.getMinecraft().world.getBlockState(e.target.blockPos)

        if (state.block != Blocks.cable.get()) return

        val x: Double = (e.player.lastTickPosX + (e.player.posX - e.player.lastTickPosX) * e.partialTicks) * -1 + e.target.blockPos.x
        val y: Double = (e.player.lastTickPosY + (e.player.posY - e.player.lastTickPosY) * e.partialTicks) * -1 + e.target.blockPos.y
        val z: Double = (e.player.lastTickPosZ + (e.player.posZ - e.player.lastTickPosZ) * e.partialTicks) * -1 + e.target.blockPos.z

        withBoundingBoxState {
            state.getActualState(Minecraft.getMinecraft().world, e.target.blockPos).let { actual ->
                Direction.values().asSequence()
                    .filter { actual.getValue(CableBlock.connections.getValue(it)) }
                    .forEach { RenderGlobal.drawSelectionBoundingBox(CableBlock.boxes.getValue(it).grow(AABB_GROW).offset(x, y, z), 0.0f, 0.0f, 0.0f, 0.4f) }
            }
        }
    }
}

internal inline fun withBoundingBoxState(block: () -> Unit) {
    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO
    )
    GlStateManager.glLineWidth(2.0f)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)
    block()
    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()
}
