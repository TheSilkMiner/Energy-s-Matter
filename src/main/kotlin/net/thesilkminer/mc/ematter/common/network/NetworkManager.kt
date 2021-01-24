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

@file:JvmName("NM")

package net.thesilkminer.mc.ematter.common.network

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_CHANNEL_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeCapabilitySyncPacket
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeCapabilitySyncPacketHandler
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeSwitchButtonClickPacket
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeSwitchButtonClickPacketHandler
import net.thesilkminer.mc.ematter.common.network.thermometer.ThermometerSendTemperaturePacket
import net.thesilkminer.mc.ematter.common.network.thermometer.ThermometerSendTemperaturePacketHandler
import kotlin.reflect.KClass

private val l = L(MOD_NAME, "Network Manager")

private lateinit var networkChannel: SimpleNetworkWrapper

internal fun setUpNetworkChannel() {
    l.info("Setting up network channel on ID '$MOD_CHANNEL_ID'")
    networkChannel = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_CHANNEL_ID)

    var id = 0
    networkChannel.register(id++, MadRecipeCapabilitySyncPacket::class, MadRecipeCapabilitySyncPacketHandler::class, Distribution.CLIENT)
    networkChannel.register(id++, MadRecipeSwitchButtonClickPacket::class, MadRecipeSwitchButtonClickPacketHandler::class, Distribution.DEDICATED_SERVER)
    networkChannel.register(id++, ThermometerSendTemperaturePacket::class, ThermometerSendTemperaturePacketHandler::class, Distribution.CLIENT)
    l.info("Successfully registered a total of $id packets")
}

internal fun EntityPlayerMP.sendPacket(packet: IMessage) = packet.sendToPlayer(this)
internal fun IMessage.sendToPlayer(player: EntityPlayerMP) = networkChannel.sendTo(this, player)
internal fun IMessage.sendToAll() = networkChannel.sendToAll(this)
internal fun IMessage.sendToServer() = networkChannel.sendToServer(this)

private fun <M : IMessage, R : IMessage?> SimpleNetworkWrapper.register(id: Int, packet: KClass<M>, handler: KClass<out IMessageHandler<M, R>>, receivingDist: Distribution) =
        this.registerMessage(handler.java, packet.java, id, receivingDist.toSide())

private fun Distribution.toSide() = when (this) {
    Distribution.CLIENT -> Side.CLIENT
    Distribution.DEDICATED_SERVER -> Side.SERVER
}
