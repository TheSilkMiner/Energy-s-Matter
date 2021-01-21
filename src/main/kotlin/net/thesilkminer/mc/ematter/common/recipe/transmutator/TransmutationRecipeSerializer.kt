package net.thesilkminer.mc.ematter.common.recipe.transmutator

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.JsonUtils
import net.minecraft.world.World
import net.minecraftforge.common.crafting.IRecipeFactory
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.mod.common.recipe.RecipeLoadingProcessor
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.mole.Moles

@ExperimentalUnsignedTypes
internal class TransmutationRecipe(private val group: NameSpacedString?, val moles: Moles, val power: ULong, private val result: ItemStack) : IForgeRegistryEntry.Impl<IRecipe>(), IRecipe {

    override fun getGroup() = this.group?.toString() ?: ""

    override fun matches(inv: InventoryCrafting, worldIn: World) =
        inv is TransmutationInventoryCrafting && inv.result.item == this.result.item

    override fun getCraftingResult(inv: InventoryCrafting): ItemStack = this.result.copy()

    override fun canFit(width: Int, height: Int) = true

    override fun getRecipeOutput(): ItemStack = this.result.copy()
}

@Suppress("unused")
@ExperimentalUnsignedTypes
internal class TransmutationRecipeSerializer : IRecipeFactory {

    override fun parse(context: JsonContext, json: JsonObject): IRecipe {
        val group = JsonUtils.getString(json, "group", "")

        val moles = JsonUtils.getInt(json, "moles")
        if (moles < 1) throw JsonSyntaxException("Only a positive mole amount is allowed")

        val power = JsonUtils.getInt(json, "moles")
        if (power < 1) throw JsonSyntaxException("Only a positive power is allowed")

        val result = RecipeLoadingProcessor.getItemStack(JsonUtils.getJsonObject(json, "result"), context, parseNbt = true)

        return TransmutationRecipe(if (group.isNotEmpty()) group.toNameSpacedString() else null, moles, power.toULong(), result)
    }
}

internal class TransmutationInventoryCrafting(var result: ItemStack) : InventoryCrafting(object : Container() {
    override fun canInteractWith(playerIn: EntityPlayer): Boolean = true
}, 0, 0)
