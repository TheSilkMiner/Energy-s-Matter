package net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole

import mezz.jei.api.ingredients.IIngredientRenderer
import mezz.jei.gui.elements.DrawableResource
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.ResourceLocation
import net.thesilkminer.mc.boson.api.locale.Color
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.withStackRenderState

object MoleIngredientRenderer : IIngredientRenderer<MoleWrapper> {

    private val moleIcon by lazy { DrawableResource(ResourceLocation(MOD_ID, "textures/gui/jei_plugin/moles.png"), 0, 0, 16, 16, 0, 0, 0, 0, 16, 16) }

    override fun render(minecraft: Minecraft, xPosition: Int, yPosition: Int, ingredient: MoleWrapper?) {
        if (ingredient == null) return

        withStackRenderState {
            moleIcon.draw(minecraft, xPosition, yPosition)
        }
    }

    override fun getTooltip(minecraft: Minecraft, ingredient: MoleWrapper, tooltipFlag: ITooltipFlag) =
        mutableListOf(MoleIngredientHelper.getDisplayName(ingredient)).apply {
            if (ingredient !== defaultMoleWrapper) this.add("Amount: ${"${ingredient.count} Mol".toLocale(null, color = Color.GOLD)}")
        }
}
