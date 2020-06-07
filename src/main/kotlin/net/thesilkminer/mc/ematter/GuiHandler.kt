package net.thesilkminer.mc.ematter

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.distribution.onlyOn
import net.thesilkminer.mc.ematter.client.feature.mad.MadGui
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.feature.mad.MadTileEntity

internal class GuiHandler : IGuiHandler {
    internal companion object {
        internal const val MAD_GUI = 0
    }

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
