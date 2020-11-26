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

package net.thesilkminer.mc.ematter.common.network

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.distribution.onlyOn
import net.thesilkminer.mc.ematter.EnergyIsMatter
import net.thesilkminer.mc.ematter.client.feature.mad.MadGui
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.feature.mad.MadTileEntity

internal class GuiHandler : IGuiHandler {
    internal companion object {
        internal const val MAD_GUI = 0
    }

    internal fun register() = NetworkRegistry.INSTANCE.registerGuiHandler(EnergyIsMatter, this)

    override fun getClientGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? = onlyOn(Distribution.CLIENT) {
        {
            when (ID) {
                MAD_GUI -> world?.getTileEntity(BlockPos(x, y, z)).let { if (it is MadTileEntity) return@let MadGui(it, player!!.inventory) else null }
                else -> null
            }
        }
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? = when (ID) {
        MAD_GUI -> world?.getTileEntity(BlockPos(x, y, z)).let { if (it is MadTileEntity) return@let MadContainer(it, player!!.inventory) else null }
        else -> null
    }
}
