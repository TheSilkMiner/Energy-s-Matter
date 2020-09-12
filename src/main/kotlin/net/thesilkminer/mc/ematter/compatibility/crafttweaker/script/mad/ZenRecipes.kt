@file:JvmName("ZR")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad

import crafttweaker.CraftTweakerAPI
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.item.ItemStackUnknown
import crafttweaker.api.player.IPlayer
import crafttweaker.api.recipes.ICraftingInventory
import crafttweaker.api.recipes.IRecipeFunction
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.NonNullList
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.crafting.IShapedRecipe
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.kotlin.commons.lang.invoke
import net.thesilkminer.kotlin.commons.lang.plusAssign
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step.ZenSteppingFunction
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step.toCommandString
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.ZenCraftingInfo
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.buildMarks
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.toZen
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.toNative
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.toNativeStack
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.toZen

@ExperimentalUnsignedTypes
internal sealed class ZenMadRecipeBase(val group: NameSpacedString?, private val output: IItemStack, val steppingFunction: ZenSteppingFunction)
    : IForgeRegistryEntry.Impl<IRecipe>(), MadRecipe, ZenMadRecipe {

    abstract val nativeIngredients: NonNullList<Ingredient>
    override val recipeName = this.registryName!!.toNameSpacedString().let { ZenNameSpacedString(it.nameSpace, it.path) }
    override val internal get() = this

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // Apparently, World can be null because mods have decided so... I hate it so much.
    override fun matches(inventory: ICraftingInventory?) = (inventory?.internal as? InventoryCrafting)?.let { this.matches(it, inventory.player?.world?.toNative()) } ?: false

    override fun getCraftingResult(inventory: ICraftingInventory?) = (inventory?.internal as? InventoryCrafting)?.let { this.getCraftingResult(it).toZen() }
    override fun zenGetPowerRequiredFor(player: IPlayer) = player.toNative()?.let { this.steppingFunction.getPowerCostFor(it, this).toLong() } ?: 0L
    override fun getName() = this.recipeName.path
    override fun hasRecipeAction() = false
    override fun applyTransformers(inventory: ICraftingInventory?, byPlayer: IPlayer?) = Unit
    override fun hasRecipeFunction() = true
    override fun getOutput() = this.output
    override fun getResourceDomain() = this.recipeName.nameSpace
    override fun isHidden() = false
    override fun getFullResourceName() = this.recipeName.asString()
    override fun getRecipeOutput() = this.output.toNativeStack().copy() as ItemStack
    override fun isDynamic() = false
    override fun getIngredients() = this.nativeIngredients
    override fun getGroup() = this.group?.toString() ?: ""
}

@ExperimentalUnsignedTypes
internal class ZenShapedMadRecipe(group: NameSpacedString?, private val width: Int, private val height: Int, private val ingredients: Array<Array<IIngredient?>>,
                                  output: IItemStack, private val allowMirroring: Boolean, steppingFunction: ZenSteppingFunction, private val recipeFunction: IRecipeFunction)
    : ZenMadRecipeBase(group, output, steppingFunction), IShapedRecipe {

    override val nativeIngredients by lazy {
        val height = this.ingredients.count()
        val width = this.ingredients.map { it.count() }.max() ?: 0
        val ingredients = NonNullList.withSize(height * width, Ingredient.EMPTY)
        this.ingredients.forEachIndexed { rowIndex, column ->
            column.forEachIndexed { columnIndex, ingredient ->
                if (ingredient != null) ingredients[rowIndex * width + columnIndex] = ingredient.toNative()
            }
        }
        ingredients as NonNullList<Ingredient>
    }

    private val mirroredIngredients by lazy { this.ingredients.mirror() }

    override fun isShaped() = true
    override fun hasTransformers() = this.ingredients.flatten().filterNotNull().any { it.hasTransformers() }
    override fun getIngredients2D() = this.ingredients.copyOf()
    override fun getIngredients1D() = this.ingredients.flatten().toTypedArray()
    override fun canFit(width: Int, height: Int) = this.width <= width && this.height <= height
    override fun getRecipeHeight() = this.height
    override fun getRecipeWidth() = this.width
    @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = this.steppingFunction.getPowerCostFor(player, this)

    override fun toCommandString(): String {
        // function registerShaped(name as NameSpacedString, group as string?, ingredients as IIngredient?[][], result as IItemStack, steppingFunction as SteppingFunction,
        //                         allowMirroring as boolean, recipeFunction as IRecipeFunction?)
        val stringBuilder = StringBuilder("MolecularAssemblerDevice.registerShaped(")

        // name as NameSpacedString
        stringBuilder += "NameSpacedString.from(\""
        stringBuilder += this.resourceDomain
        stringBuilder += "\", \""
        stringBuilder += this.name
        stringBuilder += "\"), "

        // group as string?
        if (this.group == null) {
            stringBuilder += "null"
        } else {
            stringBuilder += '"'
            stringBuilder += this.group.toString().removePrefix("minecraft:")
            stringBuilder += '"'
        }
        stringBuilder += ", "

        // ingredients as IIngredient?[][]
        stringBuilder += '['
        this.ingredients.forEachIndexed { rowIndex, row ->
            stringBuilder += '['
            row.forEachIndexed { columnIndex, ingredient ->
                stringBuilder += if (ingredient == null) "null" else ingredient.toCommandString()!!
                if (columnIndex != this.width - 1) stringBuilder += ", "
            }
            stringBuilder += ']'
            if (rowIndex != this.height - 1) stringBuilder += ", "
        }
        stringBuilder += "], "

        // result as IItemStack
        stringBuilder += try { this.output.toCommandString() } catch (e: IllegalStateException) { "<unknown>" }
        stringBuilder += ", "

        // steppingFunction as SteppingFunction
        stringBuilder += this.steppingFunction.toCommandString()
        stringBuilder += ", "

        // allowMirroring as boolean
        stringBuilder += this.allowMirroring
        stringBuilder += ", "

        // recipeFunction as IRecipeFunction
        stringBuilder += "function (out, ins, cInfo) { ... }"

        stringBuilder += ");"
        return stringBuilder()
    }

    override fun getCraftingResult(inv: InventoryCrafting): ItemStack {
        if (inv !is CraftingInventoryWrapper) return ItemStack.EMPTY
        if (inv.containerClass != MadContainer::class) return ItemStack.EMPTY

        val output = { this.recipeOutput.copy() as ItemStack }

        val normalOffset = this.findTargetGridOffset(inv, this.ingredients)
        if (normalOffset != null) return output().applyRecipeFunction(inv, this.ingredients, normalOffset)

        val mirroredOffset = if (this.allowMirroring) this.findTargetGridOffset(inv, this.mirroredIngredients) else null
        if (mirroredOffset != null) return output().applyRecipeFunction(inv, this.mirroredIngredients, mirroredOffset)

        return ItemStack.EMPTY
    }

    override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> {
        val targetList = NonNullList.withSize(this.width * this.height, ItemStack.EMPTY)

        if (inv !is CraftingInventoryWrapper) return targetList
        if (inv.containerClass != MadContainer::class) return targetList

        val normalOffset = this.findTargetGridOffset(inv, this.ingredients)
        if (normalOffset != null) return inv.findRemainingItems(this.ingredients, normalOffset, targetList)

        val mirroredOffset = this.findTargetGridOffset(inv, this.mirroredIngredients)
        if (mirroredOffset != null) return inv.findRemainingItems(this.mirroredIngredients, mirroredOffset, targetList)

        return targetList
    }

    override fun matches(inv: InventoryCrafting, worldIn: World?): Boolean {
        if (inv !is CraftingInventoryWrapper) return false
        if (inv.containerClass != MadContainer::class) return false

        return this.findTargetGridOffset(inv, this.ingredients) != null || (this.allowMirroring && this.findTargetGridOffset(inv, this.mirroredIngredients) != null)
    }

    private fun findTargetGridOffset(inv: CraftingInventoryWrapper, ingredients: Array<Array<IIngredient?>>): Pair<Int, Int>? {
        (0..(inv.width - this.width)).forEach { xTranslation ->
            (0..(inv.height - this.height)).forEach { yTranslation ->
                if (this.matches(inv, ingredients, xTranslation, yTranslation)) return xTranslation to yTranslation
            }
        }
        return null
    }

    private fun matches(inv: CraftingInventoryWrapper, ingredients: Array<Array<IIngredient?>>, xZero: Int, yZero: Int): Boolean {
        (0 until inv.width).forEach { xBasis ->
            (0 until inv.height).forEach { yBasis ->
                val x = xBasis - xZero
                val y = yBasis - yZero

                val ingredient = ingredients[x][y]?.toNative() ?: Ingredient.EMPTY

                if (!ingredient.apply(inv.getStackInRowAndColumn(xBasis, yBasis))) return false
            }
        }
        return true
    }

    private fun Array<Array<IIngredient?>>.mirror(): Array<Array<IIngredient?>> {
        val rowHolder = Array(this.count()) { arrayOf<IIngredient?>() } // Columns will get yeeted anyway so...
        this.forEachIndexed { rowIndex, row ->
            val columnSize = row.count()
            val columnHolder = arrayOfNulls<IIngredient?>(columnSize)

            row.forEachIndexed { columnIndex, ingredient ->
                val newIndex = columnSize - 1 - columnIndex
                columnHolder[newIndex] = ingredient
            }

            rowHolder[rowIndex] = columnHolder
        }

        return rowHolder
    }

    private fun ItemStack.applyRecipeFunction(inventory: CraftingInventoryWrapper, ingredients: Array<Array<IIngredient?>>, offset: Pair<Int, Int>): ItemStack {
        val (rowBasis, columnBasis) = offset
        val output = this.toZen()
        val marks = inventory.buildMarks(rowBasis, columnBasis, ingredients)
        val cInfo = ZenCraftingInfo(inventory.toZen())
        return try {
            this@ZenShapedMadRecipe.recipeFunction.process(output, marks, cInfo).toNativeStack()
        } catch (e: Exception) {
            CraftTweakerAPI.logError("Unable to execute recipe function for recipe '${this@ZenShapedMadRecipe.registryName}'", e)
            ItemStack.EMPTY
        }
    }

    private fun InventoryCrafting.findRemainingItems(ingredients: Array<Array<IIngredient?>>, offset: Pair<Int, Int>, list: NonNullList<ItemStack>): NonNullList<ItemStack> {
        val (rowBasis, columnBasis) = offset
        (0 until this@ZenShapedMadRecipe.width).forEach { column ->
            (0 until this@ZenShapedMadRecipe.height).forEach { row ->
                val index = column + columnBasis + (row + rowBasis) * this.width
                val stack = this.getStackInSlot(index)
                // If it's inside bounds
                if (row < ingredients.count() && column < ingredients[row].count()) {
                    // A 'null' container means we need to perform vanilla logic
                    val container = ingredients[row][column]?.let { ingredient ->
                        when {
                            ingredient.hasNewTransformers() -> {
                                val newOutput = try {
                                    ingredient.applyNewTransform(stack.toZen())
                                } catch (e: Exception) {
                                    CraftTweakerAPI.logError("Unable to execute ingredient transformer for '$stack' in recipe '${this@ZenShapedMadRecipe.registryName}'", e)
                                    // We set newOutput to null so that an error in executing the ingredient transformer means we fall back to vanilla logic
                                    null
                                }
                                if (newOutput != ItemStackUnknown.INSTANCE) newOutput else null
                            }
                            ingredient.hasTransformers() -> {
                                CraftTweakerAPI.logWarning("The old transformation pipeline isn't supported in recipe '${this@ZenShapedMadRecipe.registryName}': use IItemTransformerNew")
                                null
                            }
                            else -> null
                        }
                    }
                    list[index] = container?.toNativeStack() ?: ForgeHooks.getContainerItem(stack)
                }
            }
        }
        return list
    }
}

@ExperimentalUnsignedTypes
internal class ZenShapelessMadRecipe(group: NameSpacedString?, private val size: Int, private val ingredients: Array<IIngredient>, output: IItemStack,
                                     steppingFunction: ZenSteppingFunction, private val recipeFunction: IRecipeFunction)
    : ZenMadRecipeBase(group, output, steppingFunction) {

    override val nativeIngredients by lazy {
        val ingredients = NonNullList.withSize(this.size, Ingredient.EMPTY)
        this.ingredients.forEachIndexed { index, ingredient -> ingredients[index] = ingredient.toNative() }
        ingredients as NonNullList<Ingredient>
    }

    override fun isShaped() = false
    override fun hasTransformers() = this.ingredients.any { it.hasTransformers() }
    override fun getIngredients2D() = arrayOf(this.ingredients.copyOf())
    override fun getIngredients1D() = this.ingredients.copyOf()
    override fun canFit(width: Int, height: Int) = width * height >= this.size
    @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = this.steppingFunction.getPowerCostFor(player, this)

    override fun toCommandString(): String {
        // function registerShapeless(name as NameSpacedString, group as string?, ingredients as IIngredient?[], result as IItemStack, steppingFunction as SteppingFunction,
        //                            recipeFunction as IRecipeFunction?)
        val stringBuilder = StringBuilder("MolecularAssemblerDevice.registerShapeless(")

        // name as NameSpacedString
        stringBuilder += "NameSpacedString.from(\""
        stringBuilder += this.resourceDomain
        stringBuilder += "\", \""
        stringBuilder += this.name
        stringBuilder += "\"), "

        // group as string?
        if (this.group == null) {
            stringBuilder += "null"
        } else {
            stringBuilder += '"'
            stringBuilder += this.group.toString().removePrefix("minecraft:")
            stringBuilder += '"'
        }
        stringBuilder += ", "

        // ingredients as IIngredient?[]
        stringBuilder += '['
        this.ingredients.forEachIndexed { index, ingredient ->
            stringBuilder += ingredient.toCommandString()!!
            if (index != this.size - 1) stringBuilder += ", "
        }
        stringBuilder += "], "

        // result as IItemStack
        stringBuilder += try { this.output.toCommandString() } catch (e: IllegalStateException) { "<unknown>" }
        stringBuilder += ", "

        // steppingFunction as SteppingFunction
        stringBuilder += this.steppingFunction.toCommandString()
        stringBuilder += ", "

        // recipeFunction as IRecipeFunction?
        stringBuilder += "function (out, ins, cInfo) { ... }"

        stringBuilder += ");"
        return stringBuilder()
    }

    override fun getCraftingResult(inv: InventoryCrafting): ItemStack {
        if (inv !is CraftingInventoryWrapper) return ItemStack.EMPTY
        if (inv.containerClass != MadContainer::class) return ItemStack.EMPTY

        if (this.matches(inv)) return this.recipeOutput.copy().applyRecipeFunction(inv, this.ingredients)

        return ItemStack.EMPTY
    }

    override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> {
        val targetList = NonNullList.withSize(this.size, ItemStack.EMPTY)

        if (inv !is CraftingInventoryWrapper) return targetList
        if (inv.containerClass != MadContainer::class) return targetList

        return inv.findRemainingItems(this.ingredients, targetList)
    }

    override fun matches(inv: InventoryCrafting, worldIn: World?): Boolean {
        if (inv !is CraftingInventoryWrapper) return false
        if (inv.containerClass != MadContainer::class) return false

        return this.matches(inv)
    }

    private fun matches(inv: CraftingInventoryWrapper): Boolean {
        val visitedMarker = BooleanArray(inv.sizeInventory)

        this.ingredients.forEach { ingredient ->
            var matched = false
            (0 until inv.sizeInventory).forEach { slot ->
                if (!matched) {
                    val stack = inv.getStackInSlot(slot)
                    if (!stack.isEmpty && !visitedMarker[slot]) {
                        val matches = ingredient.matches(stack.toZen())
                        if (matches) {
                            visitedMarker[slot] = true
                            matched = true
                        }
                    }
                }
            }
            if (!matched) return false
        }

        return (0 until inv.sizeInventory).asSequence()
                .filterNot { visitedMarker[it] }
                .map { inv.getStackInSlot(it) }
                .all { it.isEmpty }
    }

    private fun ItemStack.applyRecipeFunction(inventory: CraftingInventoryWrapper, ingredients: Array<IIngredient>): ItemStack {
        val output = this.toZen()
        val marks = inventory.buildMarks(ingredients)
        val cInfo = ZenCraftingInfo(inventory.toZen())
        return try {
            this@ZenShapelessMadRecipe.recipeFunction.process(output, marks, cInfo).toNativeStack()
        } catch (e: Exception) {
            CraftTweakerAPI.logError("Unable to execute recipe function for recipe '${this@ZenShapelessMadRecipe.registryName}'", e)
            ItemStack.EMPTY
        }
    }

    private fun InventoryCrafting.findRemainingItems(ingredients: Array<IIngredient>, list: NonNullList<ItemStack>): NonNullList<ItemStack> {
        val visitedMarker = BooleanArray(this.sizeInventory)

        ingredients.forEachIndexed { index, ingredient ->
            var matched = false
            (0 until this.sizeInventory).forEach { slotIndex ->
                if (!matched) {
                    val stack = this.getStackInSlot(slotIndex)
                    if (!stack.isEmpty && !visitedMarker[slotIndex]) {
                        val zenStack = stack.toZen()
                        if (ingredient.matches(zenStack)) {
                            // A 'null' container means we need to perform vanilla logic
                            val container = when {
                                ingredient.hasNewTransformers() -> {
                                    val newOutput = try {
                                        ingredient.applyNewTransform(zenStack)
                                    } catch (e: Exception) {
                                        CraftTweakerAPI.logError("Unable to execute ingredient transformer for '$stack' in recipe '${this@ZenShapelessMadRecipe.registryName}'", e)
                                        // We set newOutput to null so that an error in executing the ingredient transformer means we fall back to vanilla logic
                                        null
                                    }
                                    if (newOutput != ItemStackUnknown.INSTANCE) newOutput else null
                                }
                                ingredient.hasTransformers() -> {
                                    CraftTweakerAPI.logWarning(
                                            "The old transformation pipeline isn't supported in recipe '${this@ZenShapelessMadRecipe.registryName}': use IItemTransformerNew"
                                    )
                                    null
                                }
                                else -> null
                            }
                            list[index] = container?.toNativeStack() ?: ForgeHooks.getContainerItem(stack)
                            visitedMarker[slotIndex] = true
                            matched = true
                        }
                    }
                }
            }
        }
        return list
    }
}

@ExperimentalUnsignedTypes
internal class ZenMadRecipeWrapper(private val wrapped: MadRecipe) : ZenMadRecipeBase(wrapped.group.let { if (it.isEmpty()) null else it.toNameSpacedString() },
        wrapped.recipeOutput.toZen()!!, ZenSteppingFunctionWrapper(wrapped)) {

    private class ZenSteppingFunctionWrapper(wrapped: MadRecipe) : ZenSteppingFunction {
        private val steppingFunction by lazy {
            try {
                wrapped::class.java.getDeclaredField("steppingFunction").apply { this.isAccessible = true }[this].uncheckedCast<SteppingFunction>()
            } catch (e: ReflectiveOperationException) {
                object : SteppingFunction {
                    init {
                        CraftTweakerAPI.logError("Unable to obtain stepping function for recipe '${wrapped.registryName!!}': defaulting to no-op")
                    }

                    override fun getPowerCostAt(x: Long) = 0UL
                }
            }
        }

        override fun getPowerCostAt(x: Long) = this.steppingFunction.getPowerCostAt(x)
        override fun zenGetPowerCostAt(x: Long) = this.getPowerCostAt(x).toLong()
        override fun toCommandString() = this.steppingFunction.toCommandString()
    }

    init {
        this.wrapped.registryName?.let { this.registryName = it }
    }

    override val nativeIngredients: NonNullList<Ingredient> get() = this.wrapped.ingredients

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // Apparently, World can be null because mods have decided so... I hate it so much.
    override fun matches(inv: InventoryCrafting, worldIn: World?) = this.wrapped.matches(inv, worldIn)
    override fun getCraftingResult(inv: InventoryCrafting) = this.wrapped.getCraftingResult(inv) as ItemStack
    @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = this.wrapped.getPowerRequiredFor(player)
    override fun canFit(width: Int, height: Int) = this.wrapped.canFit(width, height)
    override fun isShaped() = this.wrapped is IShapedRecipe
    override fun hasTransformers() = false
    override fun getIngredients1D() = this.nativeIngredients.toZen().copyOf()

    override fun toCommandString(): String {
        // function registerShaped(name as NameSpacedString, group as string?, ingredients as IIngredient?[][], result as IItemStack, steppingFunction as SteppingFunction,
        //                         allowMirroring as boolean, recipeFunction as IRecipeFunction?)
        // function registerShapeless(name as NameSpacedString, group as string?, ingredients as IIngredient?[], result as IItemStack, steppingFunction as SteppingFunction,
        //                            recipeFunction as IRecipeFunction?)

        // function registerShape{d|less}
        val stringBuilder = StringBuilder("MolecularAssemblerDevice.registerShape")
        stringBuilder += if (this.isShaped) "d" else "less"

        // name as NameSpacedString
        stringBuilder += "(NameSpacedString.from(\""
        stringBuilder += this.resourceDomain
        stringBuilder += ", "
        stringBuilder += this.name
        stringBuilder += "\"), "

        // group as string?
        if (this.group == null) {
            stringBuilder += "null"
        } else {
            stringBuilder += '"'
            stringBuilder += this.group.toString().removePrefix("minecraft:")
            stringBuilder += '"'
        }

        // ingredients as IIngredient?[]{[]}
        stringBuilder += '['
        if (this.isShaped) {
            this.ingredients2D.forEachIndexed { rowIndex, row ->
                stringBuilder += '['
                row.forEachIndexed { columnIndex, ingredient ->
                    stringBuilder += if (ingredient == null) "null" else ingredient.toCommandString()!!
                    if (columnIndex != row.count() - 1) stringBuilder += ", "
                }
                stringBuilder += ']'
                if (rowIndex != this.ingredients2D.count() - 1) stringBuilder += ", "
            }
        } else {
            this.ingredients1D.filterNotNull().let {
                it.forEachIndexed { index, ingredient ->
                    stringBuilder += ingredient.toCommandString()!!
                    if (index != it.count() - 1) stringBuilder += ", "
                }
            }
        }
        stringBuilder += "], "

        // result as IItemStack
        stringBuilder += try { this.output.toCommandString() } catch (e: IllegalStateException) { "<unknown>" }
        stringBuilder += ", "

        // steppingFunction as SteppingFunction
        stringBuilder += this.steppingFunction.toCommandString()
        stringBuilder += ", "

        if (this.isShaped) {
            // allowMirroring as boolean
            stringBuilder += try {
                this.wrapped::class.java.getDeclaredField("allowMirroring").apply { this.isAccessible = true }[this.wrapped].uncheckedCast<Boolean>()
            } catch (e: ReflectiveOperationException) {
                false
            }
            stringBuilder += ", "
        }

        // recipeFunction as IRecipeFunction?
        stringBuilder += "function (out, ins, cInfo) { ... });"

        return stringBuilder()
    }

    override fun getIngredients2D(): Array<Array<IIngredient?>> {
        val list = this.ingredients1D.copyOf()

        if (!this.isShaped) return arrayOf(list)

        val shaped = this.wrapped as IShapedRecipe

        val output = Array(shaped.recipeHeight) { arrayOfNulls<IIngredient?>(shaped.recipeWidth) }
        (0 until shaped.recipeHeight).forEach { row ->
            (0 until shaped.recipeWidth).forEach { column ->
                output[row][column] = list[row * shaped.recipeWidth + column]
            }
        }
        return output
    }
}
