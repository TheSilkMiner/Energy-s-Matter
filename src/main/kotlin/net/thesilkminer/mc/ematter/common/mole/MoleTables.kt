package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.Item
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME

internal typealias Moles = Int


internal object MoleTables {
    private val l = L(MOD_NAME, "Mole Tables")
    private val tables = mutableMapOf<Item, (MoleContext) -> Moles>()

    private val defaultTable = { _: MoleContext -> 0 }

    private var locked = false

    internal fun freeze() {
        this.locked = true
    }

    internal operator fun get(item: Item) = this.tables[item] ?: this.warnOnMissing(item)

    internal operator fun set(item: Item, table: (MoleContext) -> Moles) {
        if (this.locked) throw IllegalStateException("Unable to modify mole tables after loading has completed")
        this.tables[item] = table
    }

    private fun warnOnMissing(item: Item): (MoleContext) -> Moles {
        l.warn("The mole table for '${item}' is currently missing! It will be replaced with the default table (which evaluates to 0 moles)")
        this.tables[item] = this.defaultTable
        return this.tables.getValue(item)
    }
}
