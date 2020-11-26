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

package net.thesilkminer.mc.ematter.common.network.mad

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer

internal class MadRecipeSwitchButtonClickPacket(private var buttonId: Byte) : IMessage {
    constructor() : this(-1)

    internal val button get() = this.buttonId

    override fun fromBytes(buf: ByteBuf) {
        this.buttonId = buf.readByte()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeByte(this.buttonId.toInt())
    }
}

internal class MadRecipeSwitchButtonClickPacketHandler : IMessageHandler<MadRecipeSwitchButtonClickPacket, IMessage?> {
    private val l = L(MOD_NAME, "Packet Handler")

    override fun onMessage(message: MadRecipeSwitchButtonClickPacket, ctx: MessageContext): IMessage? {
        if (ctx.side != Side.SERVER) throw IllegalArgumentException("Expected to receive packet on the server, but instead it was received on the client!")
        if (message.button < 0 || message.button > 1) throw IndexOutOfBoundsException("Button ID was not valid: ${message.button} is not in [0, 2)")
        ctx.serverHandler.player?.serverWorld?.addScheduledTask {
            val openContainer = ctx.serverHandler.player?.openContainer
            if (openContainer == null) {
                l.warn("The player that sent this packet ($message) does not have a container open! This is illegal!")
                return@addScheduledTask
            }
            if (openContainer !is MadContainer) {
                l.warn("The container currently open by the player that sent this packet ($message) is not a MAD container! This is illegal!")
                return@addScheduledTask
            }
            val amounts = 1 + message.button.toInt()
            (0 until amounts).forEach { _ -> openContainer.switchToNextRecipe() }
        }
        return null
    }
}
