package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.nbt.NBTTagCompound

class MoleHolder(moleCount: Int) {
    internal companion object {
        private const val NBT_MOLE_COUNT_KEY = "mole_count"
        private const val NBT_STATE_KEY = "state"
    }

    var moleCount: Int = moleCount
        private set
    var state: MoleState
        private set

    init {
        this.state = MoleState.HEATING
    }

    constructor(nbt: NBTTagCompound): this(nbt.getInteger(NBT_MOLE_COUNT_KEY)) {
        this.state = MoleState.values()[nbt.getInteger(NBT_STATE_KEY)]
    }

    internal fun nextState() {
        if (this.state.ordinal == MoleState.values().size) throw Exception()
        if (this.state != MoleState.HEATING) this.moleCount /= 2
        this.state = MoleState.values()[this.state.ordinal + 1]
        if (this.state == MoleState.WASTE_L3) this.moleCount = 0
    }

    internal fun reduceMoleCount() {
        this.moleCount--
    }

    internal fun serializeNBT(): NBTTagCompound {
        return NBTTagCompound().apply {
            this.setInteger(NBT_MOLE_COUNT_KEY, this@MoleHolder.moleCount)
            this.setInteger(NBT_STATE_KEY, this@MoleHolder.state.ordinal)
        }
    }
}