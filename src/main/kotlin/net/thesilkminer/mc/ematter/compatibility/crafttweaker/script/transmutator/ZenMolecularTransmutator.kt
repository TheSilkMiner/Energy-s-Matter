package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.transmutator

import crafttweaker.CraftTweakerAPI
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IItemStack
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.modid.CRAFT_TWEAKER_2
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.*
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@Suppress("unused")
@ZenClass("mods.ematter.mt.MolecularTransmutator")
@ZenRegister
internal object ZenMolecularTransmutator {

    private val recipesToRemove = mutableListOf<RemoveRecipeAction>()
    private val recipesToAdd = mutableListOf<AddRecipeAction>()
    private var dumpAction = null as DumpAction?

    @ExperimentalUnsignedTypes
    @ZenMethod("register")
    @JvmStatic
    fun register(name: ZenNameSpacedString, group: String?, moles: Int, power: Long, result: IItemStack) {
        if (group != null && group.isEmpty()) return CraftTweakerAPI.logError("Unable to register recipe '$name': empty groups are invalid, use 'null' to specify no group")
        if (moles < 0) return CraftTweakerAPI.logError("Unable to register recipe '$name': negative mole amount is invalid")
        if (power < 0) return CraftTweakerAPI.logError("Unable to register recipe '$name': negative power is invalid")

        if (name.nameSpace != CRAFT_TWEAKER_2) CraftTweakerAPI.logWarning("Recipe '$name' does not specify '$CRAFT_TWEAKER_2' as its namespace: this is discouraged")
        if (group != null && !group.contains(':')) CraftTweakerAPI.logWarning("Recipe '$name' has a group without a namespace: this won't work in 1.13+")

        this.recipesToAdd += AddTransmutationRecipeAction(name.toNative()) {
            ZenTransmutationRecipeImpl(
                group = group?.toNameSpacedString(),
                moles = moles,
                power = power.toULong(),
                result = result
            )
        }
    }

    @ZenMethod("unregister")
    @JvmStatic
    fun unregister(recipe: ZenTransmutationRecipe) {
        this.recipesToRemove += RemoveTargetTransmutationRecipeAction(
            recipe = recipe.internal
        )
    }

    @ZenMethod("unregisterByName")
    @JvmStatic
    fun unregister(name: ZenNameSpacedString) {
        this.recipesToRemove += RemoveNamedTransmutationRecipeAction(
            name = name.toNative()
        )
    }

    @ZenMethod("unregisterAll")
    @JvmStatic
    fun unregisterAll() {
        val actionSequence = ForgeRegistries.RECIPES.asSequence()
            .filter { it is TransmutationRecipe }
            .map { it as TransmutationRecipe }
            .map { RemoveTargetTransmutationRecipeAction(it) }

        this.recipesToRemove += ScheduledActionGroupRemoveAction(
            ScheduledActionGroupAction(
                actions = actionSequence,
                info = "Unregistering all Molecular Transmutator recipes",
                validator = { true }
            )
        )
    }

    @Suppress("experimental_api_usage")
    @ZenMethod("findAll")
    @JvmStatic
    fun findAll(): ZenSequence<ZenTransmutationRecipe> {
        return ZenSequence(
            ForgeRegistries.RECIPES.asSequence()
                .filter { it is TransmutationRecipe }
                .map { if (it is ZenTransmutationRecipe) it else (it as TransmutationRecipe).toZen() }
        )
    }

    @ZenMethod("dump")
    @JvmStatic
    fun dump() {
        CraftTweakerAPI.apply(TransmutatorDumpAction(ZenMolecularTransmutator::findAll))
    }

    @ZenMethod("scheduleDump")
    @JvmStatic
    fun scheduleDump() {
        if (this.dumpAction != null) return CraftTweakerAPI.logError("Recipe dump has already been scheduled")
        this.dumpAction = TransmutatorDumpAction(ZenMolecularTransmutator::findAll)
    }

    internal fun apply() {
        CraftTweakerAPI.logInfo("Applying Molecular Transmutator actions - Step 1: Removals")
        this.recipesToRemove.forEach(CraftTweakerAPI::apply)

        CraftTweakerAPI.logInfo("Applying Molecular Transmutator actions - Step 2: Additions")
        this.recipesToAdd.forEach(CraftTweakerAPI::apply)

        if (this.dumpAction != null) CraftTweakerAPI.apply(this.dumpAction)
        CraftTweakerAPI.logInfo("Applying Molecular Transmutator actions - Completed")
    }

    private fun ZenNameSpacedString.toNative() = NameSpacedString(this.nameSpace, this.path)
}
