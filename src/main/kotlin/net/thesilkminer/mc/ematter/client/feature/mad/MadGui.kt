package net.thesilkminer.mc.ematter.client.feature.mad

import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiButtonImage
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.boson.prefab.energy.toUserFriendlyAmount
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.feature.mad.MadTileEntity
import kotlin.math.roundToInt

internal class MadGui(private val te: MadTileEntity, playerInventory: InventoryPlayer) : GuiContainer(MadContainer(te, playerInventory)) {
    private companion object {
        private val BACKGROUND = ResourceLocation(MOD_ID, "textures/gui/container/molecular_assembler_device.png")
    }

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

    private fun renderPowerBar() {
        @Suppress("EXPERIMENTAL_API_USAGE") val actualSize = this.te.storedPower.toDouble() * 79.0 / this.te.maximumCapacity.toDouble()
        val adjusted = actualSize.roundToInt()
        val xBegin = this.guiLeft + 91 - adjusted
        val xEnd = this.guiLeft + 92 + adjusted
        this.drawTexturedModalRect(xBegin, this.guiTop + 146, xBegin, 251, xEnd - xBegin, 3)
        this.drawTexturedModalRect(xBegin + 1, this.guiTop + 146, xBegin + 1, 248, xEnd - xBegin - 2, 3)
    }

    private fun setRecipeChangeButtonStatus() {
        (this.inventorySlots!! as MadContainer).let { container ->
            container.alternativeCraftResultLeft.getStackInSlot(0).let { left ->
                this.buttonList.first { it.id == 41 }.visible = !left.isEmpty
            }
            container.alternativeCraftResultRight.getStackInSlot(0).let { right ->
                this.buttonList.first { it.id == 43 }.visible = !right.isEmpty
            }
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

    // TODO("Send a packet from client to server to perform the operation")
    override fun actionPerformed(button: GuiButton) = when (button.id) {
        41 -> (this.inventorySlots!! as MadContainer).switchToNextRecipe()
        43 -> (this.inventorySlots!! as MadContainer).apply { this.switchToNextRecipe() }.switchToNextRecipe()
        else -> Unit
    }
}
