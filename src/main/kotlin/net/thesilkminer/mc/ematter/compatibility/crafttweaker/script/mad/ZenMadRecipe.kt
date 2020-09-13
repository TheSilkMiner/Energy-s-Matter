package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad

import crafttweaker.annotations.ZenRegister
import crafttweaker.api.player.IPlayer
import crafttweaker.api.recipes.ICraftingRecipe
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenGetter
import stanhebben.zenscript.annotations.ZenMethod

@ExperimentalUnsignedTypes
@ZenClass("mods.ematter.mad.MadRecipe")
@ZenRegister
interface ZenMadRecipe : ICraftingRecipe {
    @ZenMethod("getPowerRequiredFor") fun zenGetPowerRequiredFor(player: IPlayer): Long
    @get:ZenGetter("recipeName") val recipeName: ZenNameSpacedString

    val internal: MadRecipe
}
