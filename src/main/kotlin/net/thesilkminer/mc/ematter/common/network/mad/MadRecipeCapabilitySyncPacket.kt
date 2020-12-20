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
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.distribution.onlyOn
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.craftedMadRecipesAmountCapability

internal class MadRecipeCapabilitySyncPacket(private var capabilityTag: NBTTagCompound?) : IMessage {
    constructor() : this(null)

    internal val tag get() = this.capabilityTag

    override fun fromBytes(buf: ByteBuf) {
        val byte = buf.readByte()
        if (byte <= 0) return
        this.capabilityTag = ByteBufUtils.readTag(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        if (this.capabilityTag == null) {
            buf.writeByte(0)
            return
        }
        buf.writeByte(1)
        ByteBufUtils.writeTag(buf, this.capabilityTag)
    }
}

internal class MadRecipeCapabilitySyncPacketHandler : IMessageHandler<MadRecipeCapabilitySyncPacket, IMessage?> {
    override fun onMessage(message: MadRecipeCapabilitySyncPacket, ctx: MessageContext?): IMessage? {
        if (message.tag == null) return null

        onlyOn(Distribution.CLIENT) {
            {
                Minecraft.getMinecraft().addScheduledTask {
                    val cap = Minecraft.getMinecraft().player.getCapability(craftedMadRecipesAmountCapability, null)
                            ?: throw IllegalStateException("Capability was not attached to client player")
                    cap.deserializeNBT(message.tag)
                }
            }
        }

        return null
    }
}
