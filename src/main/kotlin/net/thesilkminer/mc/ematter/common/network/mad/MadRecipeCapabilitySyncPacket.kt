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
                val cap = Minecraft.getMinecraft().player.getCapability(craftedMadRecipesAmountCapability, null)
                        ?: throw IllegalStateException("Capability was not attached to client player")
                cap.deserializeNBT(message.tag)
                println(cap.serializeNBT())
            }
        }

        return null
    }
}
