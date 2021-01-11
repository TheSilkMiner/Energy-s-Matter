package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.Item
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME

internal typealias Moles = Int

/*
 * Format writeup:
 *
 *  {
 *    "entries": [
 *      {
 *        "conditions": [
 *          {
 *            "type": "modid:name"          // see "net/thesilkminer/mc/ematter/common/mole/condition/MoleTableConditionSerializers.kt"
 *            "data": "condition_based"
 *          ]
 *        ],
 *        "moles": 10
 *      },
 *      {
 *        "moles": 5
 *      }
 *    ],
 *    "modifiers": [
 *      {
 *        "conditions": [
 *          {
 *            "type": "modid:name"          // see "net/thesilkminer/mc/ematter/common/mole/condition/MoleTableConditionSerializers.kt"
 *            "data": "condition_based"
 *          }
 *        ],
 *        "type": "modid:name"              // see "net/thesilkminer/mc/ematter/common/mole/modifier/MoleTableModifierSerializers.kt"
 *        "data": "modifer_based"
 *      }
 *    ]
 *  }
 *
 * For entries either zero or one catch-all conditions must be present. If there is non, it defaults to 0 as mole count. If there are more than one it fails to parse the json.
 * The first entry where all conditions are satisfied will be taken (so put your catch-all condition at the bottom).
 *
 * For modifiers there are no limitations regarding catch-all conditions. They will just get applied every time.
 * Each modifier where all conditions are satisfied will be applied in the order of declaration.
 */

internal object MoleTables {

    private val l = L(MOD_NAME, "Mole Tables")
    private val tables: MutableMap<Item, (MoleContext) -> Moles> = mutableMapOf()

    private val defaultTable: (MoleContext) -> Moles = { _ -> 0 }

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
        l.warn("The mole table for '${item.registryName}' is currently missing! It will be replaced with the default table (which evaluates to 0 moles)")
        this.tables[item] = this.defaultTable
        return this.tables.getValue(item)
    }
}
