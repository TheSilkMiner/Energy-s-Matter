package net.thesilkminer.mc.ematter.common.feature.thermometer

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.prefab.direction.toFacing
import net.thesilkminer.mc.ematter.common.commonConfiguration
import net.thesilkminer.mc.ematter.common.network.sendPacket
import net.thesilkminer.mc.ematter.common.network.thermometer.ThermometerSendTemperaturePacket
import net.thesilkminer.mc.ematter.common.system.temperature.TemperatureContext
import net.thesilkminer.mc.ematter.common.system.temperature.TemperatureTables
import kotlin.random.Random

internal class ThermometerItem : Item() {
    private companion object {
        private val accuracyEntry = commonConfiguration["behavior", "thermometer"]["fluctuation"]
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        if (worldIn.isRemote || playerIn !is EntityPlayerMP) return super.onItemRightClick(worldIn, playerIn, handIn)

        val rayTraceResult = this.rayTrace(worldIn, playerIn, true)
                ?: RayTraceResult(RayTraceResult.Type.MISS, playerIn.positionVector, Direction.UP.toFacing(), BlockPos(playerIn.positionVector))

        val targetState = if (rayTraceResult.typeOfHit == RayTraceResult.Type.MISS) Blocks.AIR else worldIn.getBlockState(rayTraceResult.blockPos).block
        val currentTemperature = TemperatureTables[targetState](TemperatureContext(worldIn, rayTraceResult.blockPos, TemperatureContext.DayMoment[worldIn.worldTime]))
        val fluctuation = accuracyEntry().int

        playerIn.sendPacket(ThermometerSendTemperaturePacket(currentTemperature + Random.nextInt(from = -fluctuation, until = fluctuation + 1)))

        return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn))
    }
}
