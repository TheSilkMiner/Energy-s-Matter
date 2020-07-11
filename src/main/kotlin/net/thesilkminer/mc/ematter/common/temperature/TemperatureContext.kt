package net.thesilkminer.mc.ematter.common.temperature

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

data class TemperatureContext(val world: World, val pos: BlockPos)
