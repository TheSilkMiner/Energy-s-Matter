package net.thesilkminer.mc.ematter.common.shared

import net.minecraftforge.common.property.IUnlistedProperty
import net.thesilkminer.mc.boson.api.direction.Direction

class PropertyDirectionSet(private val name: String) : IUnlistedProperty<Set<Direction>> {

    override fun getName() = this.name

    override fun isValid(value: Set<Direction>?) = value != null

    @Suppress("unchecked_cast")
    override fun getType() = Set::class.java as Class<Set<Direction>>

    override fun valueToString(value: Set<Direction>?) = value?.joinToString() { it.name.toLowerCase() } ?: "null"
}
