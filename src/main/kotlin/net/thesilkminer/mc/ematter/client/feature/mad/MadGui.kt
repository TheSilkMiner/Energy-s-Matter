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

package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiButtonImage
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.boson.prefab.energy.toUserFriendlyAmount
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadBlockEntity
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.network.mad.MadRecipeSwitchButtonClickPacket
import net.thesilkminer.mc.ematter.common.network.sendToServer
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import kotlin.math.min
import kotlin.math.roundToInt

internal class MadGui(private val te: MadBlockEntity, playerInventory: InventoryPlayer) : GuiContainer(MadContainer(te, playerInventory)) {
    private companion object {
        private val BACKGROUND = ResourceLocation(MOD_ID, "textures/gui/container/molecular_assembler_device.png")
    }

    private val container get() = this.inventorySlots!! as MadContainer

    init {
        this.xSize = 182
        this.ySize = 245
    }

    override fun initGui() {
        super.initGui()
        this.addButton(GuiButtonImage(41, this.guiLeft + 57, this.guiTop + 11, 12, 8, 182, 0, 8, BACKGROUND))
        this.addButton(GuiButtonImage(43, this.guiLeft + 113, this.guiTop + 11, 12, 8, 182, 0, 8, BACKGROUND))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.drawGradientRect(0, 0, this.width, this.height, 0x80101025.toInt(), 0x90101025.toInt())
        MinecraftForge.EVENT_BUS.post(GuiScreenEvent.BackgroundDrawnEvent(this))
        super.drawScreen(mouseX, mouseY, partialTicks)
        this.renderHoveredToolTip(mouseX, mouseY)
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)

        this.mc.renderEngine.bindTexture(BACKGROUND)
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)

        this.renderPowerBar()
        this.setRecipeChangeButtonStatus()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun renderPowerBar() {
        this.renderNeededPowerBar()

        if (this.container.currentRecipe.power > this.te.maximumCapacity) return

        this.renderActualPowerBar()
        this.renderCurrentStatusPowerBar()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun renderNeededPowerBar() {
        val hasEnough = this.container.currentRecipe.power <= this.te.maximumCapacity

        GlStateManager.color(if (hasEnough) 0.8F else 1.0F, 0.0F, 0.0F, 1.0F)

        val actualSize = min(this.container.currentRecipe.power, this.te.maximumCapacity).toDouble() * 79.0 / this.te.maximumCapacity.toDouble()
        val adjusted = actualSize.roundToInt()
        val xBegin = this.guiLeft + 91 - adjusted
        val xEnd = this.guiLeft + 91 + adjusted

        val times = if (hasEnough) 1 else 3
        (0 until times).forEach { _ -> this.drawTexturedModalRect(xBegin, this.guiTop + 146, xBegin - this.guiLeft, 245, xEnd - xBegin, 3) }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun renderActualPowerBar() {
        val actualSize = this.te.storedPower.toDouble() * 79.0 / this.te.maximumCapacity.toDouble()
        val adjusted = actualSize.roundToInt()
        val xBegin = this.guiLeft + 91 - adjusted
        val xEnd = this.guiLeft + 91 + adjusted
        this.drawTexturedModalRect(xBegin, this.guiTop + 146, xBegin - this.guiLeft, 251, xEnd - xBegin, 3)
        this.drawTexturedModalRect(xBegin + 1, this.guiTop + 146, xBegin - this.guiLeft, 248, xEnd - xBegin - 1, 3)
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun renderCurrentStatusPowerBar() {
        GlStateManager.color(0.0F, 0.8F, 0.0F, 1.0F)

        val targetPower = min(this.container.currentRecipe.power, this.te.storedPower)
        val actualSize = targetPower.toDouble() * 79.0 / this.te.maximumCapacity.toDouble()
        val adjusted = actualSize.roundToInt()
        val xBegin = this.guiLeft + 91 - adjusted
        val xEnd = this.guiLeft + 91 + adjusted
        this.drawTexturedModalRect(xBegin, this.guiTop + 146, xBegin - this.guiLeft, 245, xEnd - xBegin, 3)

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
    }

    private fun setRecipeChangeButtonStatus() {
        this.container.alternativeCraftResultLeft.getStackInSlot(0).let { left ->
            this.buttonList.first { it.id == 41 }.visible = !left.isEmpty
        }
        this.container.alternativeCraftResultRight.getStackInSlot(0).let { right ->
            this.buttonList.first { it.id == 43 }.visible = !right.isEmpty
        }
    }

    override fun renderHoveredToolTip(p_191948_1_: Int, p_191948_2_: Int) {
        super.renderHoveredToolTip(p_191948_1_, p_191948_2_)
        this.renderPowerBarTooltip(p_191948_1_, p_191948_2_)
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun renderPowerBarTooltip(mouseX: Int, mouseY: Int) {
        if ((mouseX - this.guiLeft) in 13..170 && (mouseY - this.guiTop) in 147..149) {
            val currentPower = this.te.storedPower.toUserFriendlyAmount()
            val totalPower = this.te.maximumCapacity.toUserFriendlyAmount()
            this.drawHoveringText("gui.$MOD_ID.mad.power_level".toLocale(currentPower, totalPower), mouseX, mouseY)
        }
    }

    override fun actionPerformed(button: GuiButton) = when (button.id) {
        41, 43 -> MadRecipeSwitchButtonClickPacket(if (button.id == 43) 1 else 0).sendToServer()
        else -> Unit
    }

    @Suppress("EXPERIMENTAL_API_USAGE") // Note we are taking data from the client rather than the server: this means that the cap has to always be in sync
    private val IRecipe?.power get() = if (this is MadRecipe) this.getPowerRequiredFor(this@MadGui.mc.player) else 0.toULong()
}
