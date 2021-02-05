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
internal class TransmutationRecipeImpl(private val group: NameSpacedString?, override val moles: Moles, override val power: ULong, private val result: ItemStack) :
    IForgeRegistryEntry.Impl<IRecipe>(), TransmutationRecipe {

    override fun getGroup() = this.group?.toString() ?: ""

    override fun matches(inv: InventoryCrafting, worldIn: World) =
        inv is TransmutationInventory && inv.result.item == this.result.item

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

        val power = JsonUtils.getInt(json, "power")
        if (power < 1) throw JsonSyntaxException("Only a positive power is allowed")

        val result = RecipeLoadingProcessor.getItemStack(JsonUtils.getJsonObject(json, "result"), context, parseNbt = true)

        return TransmutationRecipeImpl(if (group.isNotEmpty()) group.toNameSpacedString() else null, moles, power.toULong(), result)
    }
}

internal class TransmutationInventory(var result: ItemStack) : InventoryCrafting(object : Container() {
    override fun canInteractWith(playerIn: EntityPlayer): Boolean = true
}, 0, 0)
