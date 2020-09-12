package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared

import crafttweaker.api.player.IPlayer
import crafttweaker.api.recipes.ICraftingInfo
import crafttweaker.api.recipes.ICraftingInventory
import crafttweaker.api.world.IWorld

internal class ZenCraftingInfo(private val craftingInventory: ICraftingInventory) : ICraftingInfo {
    override fun getInventory(): ICraftingInventory = this.craftingInventory
    override fun getPlayer(): IPlayer? = this.inventory.player
    override fun getWorld(): IWorld? = this.player?.world
    override fun getDimension(): Int = this.world?.dimension ?: 0
}
