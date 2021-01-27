package net.thesilkminer.mc.ematter.common.network.thermometer

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.boson.api.distribution.onlyOn
import net.thesilkminer.mc.ematter.client.feature.thermometer.TemperatureStorage

internal class ThermometerSendTemperaturePacket(private var internalTemperature: Int) : IMessage {
    constructor() : this(0)

    internal val temperature get() = this.internalTemperature

    override fun fromBytes(buf: ByteBuf) {
        this.internalTemperature = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(this.internalTemperature)
    }
}

internal class ThermometerSendTemperaturePacketHandler : IMessageHandler<ThermometerSendTemperaturePacket, IMessage?> {
    override fun onMessage(message: ThermometerSendTemperaturePacket, ctx: MessageContext?): IMessage? {
        onlyOn(Distribution.CLIENT) {
            {
                Minecraft.getMinecraft().addScheduledTask {
                    TemperatureStorage.temperature = message.temperature
                }
            }
        }
        return null
    }
}
