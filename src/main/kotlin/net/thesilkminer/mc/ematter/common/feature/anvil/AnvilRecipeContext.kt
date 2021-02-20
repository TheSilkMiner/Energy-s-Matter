package net.thesilkminer.mc.ematter.common.feature.anvil

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.thesilkminer.mc.ematter.common.shared.isUsableByPlayer

internal data class AnvilRecipeContext(private val blockEntity: AnvilBlockEntity, internal val hits: Byte, internal val stack: ItemStack) {
    internal class Wrapper(private val context: AnvilRecipeContext) : Container() {
        override fun canInteractWith(playerIn: EntityPlayer): Boolean = this.context.blockEntity.isUsableByPlayer(playerIn)

        internal fun asContext() = this.context
    }
}
