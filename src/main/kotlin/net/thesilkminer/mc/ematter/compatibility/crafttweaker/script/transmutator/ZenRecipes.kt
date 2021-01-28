@file:JvmName("ZR")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.transmutator

import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.player.IPlayer
import crafttweaker.api.recipes.ICraftingInventory
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.world.World
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.kotlin.commons.lang.plusAssign
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.toNative
import net.thesilkminer.mc.boson.compatibility.crafttweaker.toNativeStack
import net.thesilkminer.mc.boson.compatibility.crafttweaker.toZen
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.mole.Moles
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationInventory
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe

@ExperimentalUnsignedTypes
internal open class ZenTransmutationRecipeImpl(private val group: NameSpacedString?, override val moles: Moles, override val power: ULong, private val result: IItemStack) :
    IForgeRegistryEntry.Impl<IRecipe>(), TransmutationRecipe, ZenTransmutationRecipe {

    // TransmutationRecipe >>
    override fun getGroup() = this.group?.toString() ?: ""

    override fun matches(inv: InventoryCrafting, worldIn: World?) =
        inv is TransmutationInventory && inv.result.item == this.result.toNativeStack().item

    override fun canFit(width: Int, height: Int) = true

    override fun getRecipeOutput() = this.result.toNativeStack().copy() as ItemStack
    override fun getCraftingResult(inv: InventoryCrafting) =
        (if (this.matches(inv, null)) this.result.toNativeStack().copy() else ItemStack.EMPTY) as ItemStack
    // << TransmutationRecipe

    // ZenTransmutationRecipe >>
    override val recipeName get() = this.registryName!!.toNameSpacedString().let { ZenNameSpacedString(it.nameSpace, it.path) }
    override val internal get() = this

    override fun matches(inventory: ICraftingInventory?) =
        (inventory?.internal as? InventoryCrafting)?.let { this.matches(it, inventory.player?.world?.toNative()) } ?: false

    override fun getOutput() = this.result
    override fun getCraftingResult(inventory: ICraftingInventory?) =
        (inventory?.internal as? InventoryCrafting)?.let { this.getCraftingResult(it).toZen() }

    override fun getResourceDomain() = this.recipeName.nameSpace
    override fun getName() = this.recipeName.path
    override fun getFullResourceName() = this.recipeName.asString()

    override fun hasTransformers() = false
    override fun hasRecipeAction() = false
    override fun hasRecipeFunction() = false

    override fun applyTransformers(inventory: ICraftingInventory?, byPlayer: IPlayer?) = Unit

    override fun getIngredients1D() = emptyArray<IIngredient>()
    override fun getIngredients2D() = emptyArray<Array<IIngredient>>()

    override fun isHidden() = false
    override fun isShaped() = false

    override fun toCommandString(): String {
        // function register(name as NameSpacedString, group as string?, moles as int, power as long, result as IItemStack)
        val builder = StringBuilder("MolecularTransmutator.register(")

        // name as NameSpacedString
        builder += "NameSpacedString.from(\""
        builder += this.resourceDomain
        builder += "\", \""
        builder += this.name
        builder += "\"), "

        // group as string?
        if (this.group == null) {
            builder += "null"
        } else {
            builder += '"'
            builder += this.group.toString().removePrefix("minecraft:")
            builder += '"'
        }
        builder += ", "

        // moles as int
        builder += this.moles
        builder += ", "

        // power as long
        builder += this.power
        builder += " as long"
        builder += ", "

        // result as IItemStack
        builder += try { this.output.toCommandString() } catch (e: IllegalStateException) { "<unknown>" }
        builder += ", "

        builder.append(");")
        return builder.toString()
    }
    // << ZenTransmutationRecipe
}

@ExperimentalUnsignedTypes
internal fun TransmutationRecipe.toZen(): ZenTransmutationRecipe =
    ZenTransmutationRecipeImpl(this.group.let { if (it.isEmpty()) null else it.toNameSpacedString() }, this.moles, this.power, this.recipeOutput.toZen()!!)
