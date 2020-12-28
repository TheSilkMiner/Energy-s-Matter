package net.thesilkminer.mc.ematter.common.shared

import net.thesilkminer.mc.boson.api.direction.Direction

@Suppress("unused")
inline class DirectionsByte(val byte: Byte) {

    operator fun plus(side: Direction) = DirectionsByte((this.byte.toInt() or (1 shl side.ordinal)).toByte())

    operator fun minus(side: Direction) = DirectionsByte((this.byte.toInt() and (1 shl side.ordinal).inv()).toByte())

    operator fun contains(side: Direction) = this.byte.toInt() and (1 shl side.ordinal) != 0

    fun hasNorth() = Direction.NORTH in this
    fun hasEast() = Direction.EAST in this
    fun hasWest() = Direction.WEST in this
    fun hasSouth() = Direction.SOUTH in this
    fun hasUp() = Direction.UP in this
    fun hasDown() = Direction.DOWN in this
}
