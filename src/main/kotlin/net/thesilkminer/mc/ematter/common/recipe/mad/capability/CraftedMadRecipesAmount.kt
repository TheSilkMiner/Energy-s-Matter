@file:JvmName("CMRA")

package net.thesilkminer.mc.ematter.common.recipe.mad.capability

import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap
import net.minecraft.item.crafting.IRecipe
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString

@CapabilityInject(CraftedMadRecipesAmount::class)
internal lateinit var craftedMadRecipesAmountCapability: Capability<CraftedMadRecipesAmount>

internal interface CraftedMadRecipesAmount : INBTSerializable<NBTTagCompound> {
    fun findAmountFor(name: NameSpacedString): Long
    fun findAmountFor(recipe: IRecipe) = this.findAmountFor(recipe.registryName!!.toNameSpacedString())
    fun increaseAmountFor(name: NameSpacedString)
    fun increaseAmountFor(recipe: IRecipe) = this.findAmountFor(recipe.registryName!!.toNameSpacedString())
}

internal class CraftedMadRecipesAmountCapability : CraftedMadRecipesAmount {
    private val recipeData = Object2LongAVLTreeMap<NameSpacedString>().apply { this.defaultReturnValue(0) }

    override fun findAmountFor(name: NameSpacedString) = this.recipeData.getLong(name)

    override fun increaseAmountFor(name: NameSpacedString) {
        this.recipeData[name] = this.findAmountFor(name) + 1L
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) =
            nbt?.keySet?.forEach { this.recipeData[it.toNameSpacedString()] = nbt.getLong(it).let { amount -> if (amount < 0) 0 else amount } } ?: Unit

    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        this.recipeData.asSequence().filter { it.value != 0L }.forEach { (k, v) -> tag.setLong(k.toString(), v) }
        return tag
    }
}
