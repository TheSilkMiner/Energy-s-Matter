package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.Item
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME

internal object MoleTables {
    private val l = L(MOD_NAME, "Mole Tables")
    private val tables = mutableMapOf<Item, Int>()

    private val defaultMoleCount = 0

    private var locked = false

    internal fun freeze() {
        this.locked = true
    }

    internal operator fun get(item: Item) = this.tables[item] ?: this.warnOnMissing(item)

    internal operator fun set(item: Item, moleCount: Int) {
        if (this.locked) throw IllegalStateException("Unable to modify mole tables after loading has completed")
        this.tables[item] = moleCount
    }

    private fun warnOnMissing(item: Item): Int {
        l.warn("The mole table for '${item}' is currently missing! It will be replaced with the default table (${this.defaultMoleCount} moles)")
        this.tables[item] = this.defaultMoleCount
        return this.tables.getValue(item)
    }
}