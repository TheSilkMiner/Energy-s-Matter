package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.energy.Consumer

@ExperimentalUnsignedTypes
internal class CableNetwork(val world: World) : INBTSerializable<NBTTagCompound>, Consumer {

    // block position are far easier to work with
    val cables: MutableSet<BlockPos> = mutableSetOf()
    val consumers: MutableSet<BlockPos> = mutableSetOf()

    // contains only consumers located next to a loaded cable
    val posToLoadedConsumers: MutableMap<BlockPos, MutableSet<Pair<Consumer, Direction>>> = mutableMapOf()
    lateinit var loadedConsumers: Map<Consumer, Direction>

    operator fun contains(pos: BlockPos): Boolean = pos in this.cables

    fun updateLoadedConsumers() {
        this.loadedConsumers = this.posToLoadedConsumers.values.flatten().toMap()
    }

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
    }

    override fun tryAccept(power: ULong, from: Direction): ULong {
        if (this.loadedConsumers.isEmpty()) return 0UL // if there are no consumers we can't consume something

        val notTransferable: ULong = if (power.coerceAtMost(1024UL /* TODO("n1kx", "<--" */) == power) 0UL else power - 1024UL // all the power which is over the maximum
        var powerLeft: ULong = power - notTransferable // stores how many power we still have to transfer

        var consumerSequence: Sequence<Pair<Consumer, Direction>> = this.loadedConsumers.asSequence().map { it.key to it.value } // stores all consumers which accepts power

        while (powerLeft > 0UL) {
            val consumers: ULong = consumerSequence.toList().size.toULong()

            // splits the power between all consumers evenly
            // if we have less power then consumers each consumer gets 1 ampere and we pretend everything worked fine
            val powerSplit: ULong = ((powerLeft - powerLeft % consumers) / consumers).let { if (it != 0UL) it else 1UL }

            var powerConsumed: ULong = 0UL // stores how much power got consumed by each consumer
            consumerSequence = consumerSequence
                .onEach { powerConsumed = it.first.tryAccept(powerSplit, it.second) } // tries to transfer power
                .filter { powerConsumed == 0UL } //we remove every consumer that hasn't accept any power
                .onEach { powerLeft -= powerConsumed } // the power which we need to transfer decreases by the power we just transferred

            if (consumerSequence.toList().isEmpty()) return power - notTransferable - powerLeft // if all consumers are full we can safely return
        }
        return power - notTransferable // if we end up here this means we transferred all possible power
    }

    private fun BlockPos.toIntArray() = IntArray(3).apply {
        this[0] = this@toIntArray.x
        this[1] = this@toIntArray.y
        this[2] = this@toIntArray.z
    }

    private fun NBTTagCompound.getBlockPos(key: String) = this.getIntArray(key).let { xyz -> BlockPos(xyz[0], xyz[1], xyz[2]) }
}
