@file:JvmName("CTSFS")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.step

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.modid.CRAFT_TWEAKER_2
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunctionSerializer
import stanhebben.zenscript.expression.Expression

@ExperimentalUnsignedTypes
private val crtSteppingFunctionSerializerRegistry = DeferredRegister(CRAFT_TWEAKER_2, SteppingFunctionSerializer::class)

@ExperimentalUnsignedTypes
@Suppress("unused")
internal object CraftTweakerSteppingFunctionSerializers {
    val zs = crtSteppingFunctionSerializerRegistry.register("zenscript", ::ZenScriptBasedSteppingFunctionSerializer)
}

@ExperimentalUnsignedTypes
internal class ZenScriptBasedSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class ZenScriptBasedSteppingFunction(val expression: Expression) : SteppingFunction {
        override fun getPowerCostAt(x: Long): ULong {
            TODO("Not yet implemented")
        }
    }

    override fun read(json: JsonObject): SteppingFunction {
        if (!json.has("expression")) throw JsonSyntaxException("Missing 'expression': this is required for a ZS-based stepping function")
        val expressionElement = json["expression"]
        val expression = this.findExpression(expressionElement)
        return ZenScriptBasedSteppingFunction(expression.compile())
    }

    private fun findExpression(json: JsonElement): String =
            if (json.isJsonPrimitive) JsonUtils.getString(json, "expression") else JsonUtils.getJsonArray(json, "expression").stringify()

    private fun JsonArray.stringify() = this.asSequence()
            .mapIndexed { index, element -> JsonUtils.getString(element, "$index") }
            .joinToString(separator = "\n")

    private fun String.compile(): Expression {
        TODO()
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun attachCraftTweakerSteppingFunctionSerializerRegistry(bus: EventBus) =
        crtSteppingFunctionSerializerRegistry.subscribeOnto(bus).also { CraftTweakerSteppingFunctionSerializers.toString() }
