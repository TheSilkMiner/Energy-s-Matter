@file:JvmName("NM")

package net.thesilkminer.mc.ematter.common.network

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_CHANNEL_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeCapabilitySyncPacket
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeCapabilitySyncPacketHandler

private val l = L(MOD_NAME, "Network Manager")

private lateinit var networkChannel: SimpleNetworkWrapper

internal fun setUpNetworkChannel() {
    l.info("Setting up network channel on ID '$MOD_CHANNEL_ID'")
    networkChannel = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_CHANNEL_ID)

    var id = 0
    networkChannel.registerMessage(MadRecipeCapabilitySyncPacketHandler::class.java, MadRecipeCapabilitySyncPacket::class.java, id++, Side.CLIENT)
    l.info("Successfully registered a total of $id packets")
}

internal fun EntityPlayerMP.sendPacket(packet: IMessage) = networkChannel.sendTo(packet, this)
internal fun sendToAll(packet: IMessage) = networkChannel.sendToAll(packet)
