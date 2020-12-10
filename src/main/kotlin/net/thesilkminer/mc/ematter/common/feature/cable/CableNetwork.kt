package net.thesilkminer.mc.ematter.common.feature.cable

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.INBTSerializable

// TODO("n1kx", "save networks")
@ExperimentalUnsignedTypes
internal class CableNetwork : INBTSerializable<NBTTagCompound> {

    // block positions do not load chunks when I use them; TE do, so using them brings additional problems i do not wanna deal with
    // TODO("n1kx", "call some mark dirty methods in here")
    val cables: MutableSet<BlockPos> = mutableSetOf()

    // TODO("n1kx", "think about funny ways to store, add and remove consumers")

    operator fun contains(pos: BlockPos): Boolean = pos in this.cables

    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        this.cables.forEachIndexed { index, pos ->
            tag.setIntArray("cable$index", IntArray(3).apply {
                this[0] = pos.x
                this[1] = pos.y
                this[2] = pos.z
            })
        }
        return tag
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) {
        nbt?.keySet?.forEach { key ->
            nbt.getIntArray(key).let { xyz ->
                this.cables.add(BlockPos(xyz[0], xyz[1], xyz[2]))
            }
        }
    }
}
