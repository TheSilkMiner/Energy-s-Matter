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
        val openContainer = ctx.serverHandler.player?.openContainer
        if (openContainer == null) {
            l.warn("The player that sent this packet ($message) does not have a container open! This is illegal!")
            return null
        }
        if (openContainer !is MadContainer) {
            l.warn("The container currently open by the player that sent this packet ($message) is not a MAD container! This is illegal!")
            return null
        }
        val amounts = 1 + message.button.toInt()
        (0 until amounts).forEach { _ -> openContainer.switchToNextRecipe() }
        return null
    }
}
