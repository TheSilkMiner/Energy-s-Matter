/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

package net.thesilkminer.mc.ematter.common.feature.mad

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryCraftResult
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.inventory.Slot
import net.minecraft.inventory.SlotCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.IRecipe
import net.minecraft.network.play.server.SPacketSetSlot
import net.minecraft.world.World
import net.thesilkminer.mc.ematter.common.shared.bindPlayerInventory
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.craftedMadRecipesAmountCapability
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper

internal class MadContainer(private val te: MadTileEntity, private val playerInventory: InventoryPlayer) : Container() {
    private class FakeSlot(inventory: IInventory, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
        override fun isItemValid(stack: ItemStack) = false
        override fun canTakeStack(playerIn: EntityPlayer) = false
    }

    private class PoweredCraftingSlot(private val te: MadTileEntity, private val recipe: () -> IRecipe?, private val player: EntityPlayer, matrix: InventoryCrafting,
                                      craftResult: InventoryCraftResult, index: Int, x: Int, y: Int) : SlotCrafting(player, matrix, craftResult, index, x, y) {
        @Suppress("EXPERIMENTAL_API_USAGE")
        override fun canTakeStack(playerIn: EntityPlayer) = this.recipe().let { it != null && (it !is MadRecipe || it.getPowerRequiredFor(this.player) <= this.te.storedPower) }

        override fun onCrafting(stack: ItemStack) {
            if (!this.player.world.isRemote) {
                (this.inventory as? InventoryCraftResult)?.recipeUsed?.let { this.player.getCapability(craftedMadRecipesAmountCapability, null)!!.increaseAmountFor(it) }
            }
            super.onCrafting(stack)
        }
    }

    private val craftingMatrix = CraftingInventoryWrapper(MadContainer::class, this, this.te.inventory, 5, 5)
    private val craftResult = InventoryCraftResult()
    internal val alternativeCraftResultLeft = InventoryCraftResult()
    internal val alternativeCraftResultRight = InventoryCraftResult()

    private val foundRecipes = mutableListOf<IRecipe>()
    private var currentMain = 0

    internal val currentRecipe get() = this.foundRecipes.getOrNull(0)

    init {
        this.addSlotToContainer(PoweredCraftingSlot(this.te, { this.currentRecipe }, playerInventory.player, this.craftingMatrix,
                this.craftResult, 0, 83, 11))
        this.addSlotToContainer(FakeSlot(this.alternativeCraftResultLeft, 0, 55, 23))
        this.addSlotToContainer(FakeSlot(this.alternativeCraftResultRight, 0, 111, 23))
        this.craftingMatrix.let {
            (0..4).forEach { row ->
                (0..4).forEach { column ->
                    val index = 5 * row + column
                    val x = 47 + column * 18
                    val y = 47 + row * 18
                    this.addSlotToContainer(Slot(it, index, x, y))
                }
            }
        }
        bindPlayerInventory(playerInventory, initialX = 11, initialY = 160, addSlotToContainer = this::addSlotToContainer)

        this.onCraftMatrixChanged(this.craftingMatrix)
    }

    override fun canInteractWith(playerIn: EntityPlayer) = this.te.isUsableByPlayer(playerIn)

    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = this.inventorySlots[index]

        if (slot != null && slot.hasStack && slot.canTakeStack(playerIn)) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()

            stack = when (index) {
                0 -> this.transferCraftingStack(playerIn, stack, stackInSlot, slot)
                1, 2 -> null
                in 3..27 -> this.transferMatrixStack( stack, stackInSlot)
                in 28..54 -> this.transferInventoryStack( stack, stackInSlot)
                in 55..63 -> this.transferHotStack(stack, stackInSlot)
                else -> throw IndexOutOfBoundsException("$index is outside bounds [0, ${this.inventorySlots.count()})")
            } ?: return ItemStack.EMPTY

            if (stack.isEmpty) slot.putStack(ItemStack.EMPTY) else slot.onSlotChanged()

            if (stack.count == stackInSlot.count) return ItemStack.EMPTY

            val takenStack = slot.onTake(playerIn, stackInSlot)

            if (index == 0) playerIn.dropItem(takenStack, false)
        }

        return stack
    }

    override fun onCraftMatrixChanged(inventoryIn: IInventory) {
        super.onCraftMatrixChanged(inventoryIn)
        this.findMatchingRecipes(this.te.world, this.playerInventory.player, this.craftingMatrix, this.craftResult, this.alternativeCraftResultLeft, this.alternativeCraftResultRight)
    }

    private fun transferCraftingStack(player: EntityPlayer, stack: ItemStack, stackInSlot: ItemStack, slot: Slot): ItemStack? {
        stackInSlot.item.onCreated(stackInSlot, player.world, player)

        if (!this.mergeItemStack(stackInSlot, 28, 64, true)) return null

        slot.onSlotChange(stackInSlot, stack)

        return stack
    }

    private fun transferMatrixStack(stack: ItemStack, stackInSlot: ItemStack): ItemStack? {
        if (!this.mergeItemStack(stackInSlot, 28, 64, false)) return null
        this.onCraftMatrixChanged(this.craftingMatrix)
        return stack
    }

    private fun transferInventoryStack(stack: ItemStack, stackInSlot: ItemStack): ItemStack? {
        if (!this.mergeItemStack(stackInSlot, 3, 28, false) && // Merge in matrix
                !this.mergeItemStack(stackInSlot, 55, 64, false)) { // Merge in hot bar
            return null
        }
        return stack
    }

    private fun transferHotStack(stack: ItemStack, stackInSlot: ItemStack): ItemStack? {
        if (!this.mergeItemStack(stackInSlot, 3, 28, false) && // Merge in matrix
                !this.mergeItemStack(stackInSlot, 28, 55, false)) { // Merge in inventory
            return null
        }
        return stack
    }

    private fun findMatchingRecipes(world: World, player: EntityPlayer, matrix: InventoryCrafting, mainResult: InventoryCraftResult,
                                    alternativeLeft: InventoryCraftResult, alternativeRight: InventoryCraftResult) {
        val recipeBook = if (player is EntityPlayerMP) player.recipeBook else if (player is EntityPlayerSP) player.recipeBook else null

        val matchingRecipes = CraftingManager.REGISTRY
                .asSequence()
                .filter { it.matches(matrix, world) }
                .filter { it.isDynamic || !world.gameRules.getBoolean("doLimitedCrafting") || (recipeBook != null && recipeBook.isUnlocked(it)) }
                .toList()

        this.foundRecipes.clear()
        this.foundRecipes.addAll(matchingRecipes)
        this.currentMain = 0

        if (world.isRemote) {
            this.updateClientRecipeStatus()
        } else {
            if (player is EntityPlayerMP) this.updateServerRecipeStatus(matrix, player, mainResult, alternativeLeft, alternativeRight)
        }
    }

    private fun updateClientRecipeStatus() = Unit // TODO("Something")

    private fun updateServerRecipeStatus(matrix: InventoryCrafting, player: EntityPlayerMP, mainResult: InventoryCraftResult,
                                         alternativeLeft: InventoryCraftResult, alternativeRight: InventoryCraftResult) {
        val mainRecipe = if (this.foundRecipes.isNotEmpty()) this.foundRecipes[this.currentMain.orWrap()] else null
        val leftRecipe = if (this.foundRecipes.count() > 1) this.foundRecipes[(this.currentMain + 1).orWrap()] else null
        val rightRecipe = if (this.foundRecipes.count() > 2) this.foundRecipes[(this.currentMain + 2).orWrap()] else null

        val mainStack = mainRecipe?.getCraftingResult(matrix) ?: ItemStack.EMPTY
        val leftStack = leftRecipe?.getCraftingResult(matrix) ?: ItemStack.EMPTY
        val rightStack = rightRecipe?.getCraftingResult(matrix) ?: ItemStack.EMPTY

        mainResult.recipeUsed = mainRecipe
        mainResult.setInventorySlotContents(0, mainStack)
        player.connection.sendPacket(SPacketSetSlot(this.windowId, 0, mainStack))

        alternativeLeft.recipeUsed = leftRecipe
        alternativeLeft.setInventorySlotContents(0, leftStack)
        player.connection.sendPacket(SPacketSetSlot(this.windowId, 1, leftStack))

        alternativeRight.recipeUsed = rightRecipe
        alternativeRight.setInventorySlotContents(0, rightStack)
        player.connection.sendPacket(SPacketSetSlot(this.windowId, 2, rightStack))
    }

    internal fun switchToNextRecipe() {
        (this.currentMain + 1).orWrap()
        if (this.playerInventory.player.world.isRemote) {
            this.updateClientRecipeStatus()
        } else {
            this.updateServerRecipeStatus(this.craftingMatrix, this.playerInventory.player as? EntityPlayerMP ?: return, this.craftResult,
                    this.alternativeCraftResultLeft, this.alternativeCraftResultRight)
        }
    }

    private fun Int.orWrap() = if (this@MadContainer.foundRecipes.isEmpty()) 0 else this % this@MadContainer.foundRecipes.count()
}
