package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Consumer
import net.thesilkminer.mc.boson.prefab.energy.getEnergyConsumer

@ExperimentalUnsignedTypes
internal class CableNetwork(val world: World) : INBTSerializable<NBTTagCompound>, Consumer {

    // block position are far easier to work with
    val cables: MutableSet<BlockPos> = mutableSetOf()
    val consumers: MutableSet<BlockPos> = mutableSetOf()

    // contains only consumers located next to a loaded cable
    val loadedConsumers: MutableMap<BlockPos, MutableSet<Direction>> = mutableMapOf()

    // stores loaded consumers as consumer objects
    // gets used in Consumer#tryAccept so we don't have to recalculate that each time it gets called
    private val consumerCache: MutableSet<Pair<Consumer, Direction>> = mutableSetOf()

    operator fun contains(pos: BlockPos): Boolean = pos in this.cables

    fun reloadConsumerCache() {
        this.consumerCache.clear()

        this.consumerCache.addAll(
            this.loadedConsumers.asSequence()
                .map { (this.world.getTileEntitySafely(it.key) ?: throw IllegalStateException("Tried to access a tile entity which was expected to be loaded but wasn't.")) to it.value.first() }
                .map { (it.first.getEnergyConsumer(it.second) ?: throw IllegalStateException("Tried to get a consumer from a tile entity but failed.")) to it.second }
        )
    }

    // INBTSerializable >>
    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        tag.setTag("cables", NBTTagCompound().apply {
            this@CableNetwork.cables.forEachIndexed { index, pos ->
                this.setIntArray("$index", pos.toIntArray())
            }
        })
        tag.setTag("consumers", NBTTagCompound().apply {
            this@CableNetwork.consumers.forEachIndexed { index, pos ->
                this.setIntArray("$index", pos.toIntArray())
            }
        })
        return tag
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) {
        (nbt?.getTag("cables") as? NBTTagCompound)?.let { tag ->
            tag.keySet.forEach { index ->
                this.cables.add(tag.getBlockPos(index))
            }
        }
        (nbt?.getTag("consumers") as? NBTTagCompound)?.let { tag ->
            tag.keySet.forEach { index ->
                this.consumers.add(tag.getBlockPos(index))
            }
        }
        // TileEntity::onLoad fires after this so we don't have to build loadedConsumers here, the te will do that for us
    }
    // << INBTSerializable

    // Consumer >>
    override fun tryAccept(power: ULong, from: Direction): ULong {
        if (this.consumerCache.isEmpty()) return 0UL // early return if nothing exists which could consume power

        val notTransferable: ULong = if (power.coerceAtMost(1024UL) == power) 0UL else power - 1024UL // take transfer limits in account
        var leftToTransfer: ULong = power - notTransferable

        val consumers = consumerCache.toMutableSet() // don't change the cache

        while (leftToTransfer > 0UL && consumers.isNotEmpty()) {
            // split the power between all consumers evenly
            val splitted: ULong = ((leftToTransfer - leftToTransfer % consumers.size.toULong()) / consumers.size.toULong()).let { if (it != 0UL) it else 1UL }

            for (consumer in consumers.toList()) {
                val consumed = consumer.first.tryAccept(splitted, consumer.second)

                leftToTransfer -= consumed
                if (leftToTransfer == 0UL) break // break to make sure that unsigned long does not fall below zero (0UL - 1UL = ULong.MAX_VALUE)

                if (consumed == 0UL) consumers.remove(consumer) // if the consumer hasn't accepted any power now it won't do next iteration either
            }
        }
        return power - (notTransferable + leftToTransfer)
    }
    // << Consumer

    // helper functions
    private fun BlockPos.toIntArray() = IntArray(3).apply {
        this[0] = this@toIntArray.x
        this[1] = this@toIntArray.y
        this[2] = this@toIntArray.z
    }

    private fun NBTTagCompound.getBlockPos(key: String) = this.getIntArray(key).let { xyz -> BlockPos(xyz[0], xyz[1], xyz[2]) }

    private fun World.getTileEntitySafely(pos: BlockPos) =
        if (this.isBlockLoaded(pos)) this.getTileEntity(pos) else null
}
