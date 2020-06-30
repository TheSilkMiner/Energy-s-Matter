@file:JvmName("TE")

package net.thesilkminer.mc.ematter.common

import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.feature.mad.MadTileEntity
import net.thesilkminer.mc.ematter.common.feature.sbg.SbgTileEntity

internal object TileEntities

internal object TileEntityRegistration {
    private val l = L(MOD_NAME, "TileEntity Registration")

    @SubscribeEvent
    fun onBlockRegistration(e: RegistryEvent.Register<Block>) {
        l.info("Hijacking block registry event for tile entity registration")
        GameRegistry.registerTileEntity(MadTileEntity::class.java, ResourceLocation(MOD_ID, "molecular_assembler_device"))
        GameRegistry.registerTileEntity(SbgTileEntity::class.java, ResourceLocation(MOD_ID, "seebeck_generator"))
    }
}
