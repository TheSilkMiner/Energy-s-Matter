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
        TODO("Not yet implemented")
    }

    private fun BlockPos.toIntArray() = IntArray(3).apply {
        this[0] = this@toIntArray.x
        this[1] = this@toIntArray.y
        this[2] = this@toIntArray.z
    }

    private fun NBTTagCompound.getBlockPos(key: String) = this.getIntArray(key).let { xyz -> BlockPos(xyz[0], xyz[1], xyz[2]) }
}
