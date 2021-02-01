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

@file:JvmName("BE")

package net.thesilkminer.mc.ematter.common

import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlockEntity
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlockEntity
import net.thesilkminer.mc.ematter.common.feature.seebeck.SeebeckBlockEntity

internal object BlockEntities

internal object BlockEntityRegistration {
    private val l = L(MOD_NAME, "BlockEntity Registration")

    @SubscribeEvent
    fun onBlockRegistration(e: RegistryEvent.Register<Block>) {
        l.info("Hijacking block registry event for block entity registration")
        GameRegistry.registerTileEntity(MadBlockEntity::class.java, ResourceLocation(MOD_ID, "molecular_assembler_device"))
        GameRegistry.registerTileEntity(SeebeckBlockEntity::class.java, ResourceLocation(MOD_ID, "seebeck_generator"))
        GameRegistry.registerTileEntity(CableBlockEntity::class.java, ResourceLocation(MOD_ID, "cable"))
    }
}
