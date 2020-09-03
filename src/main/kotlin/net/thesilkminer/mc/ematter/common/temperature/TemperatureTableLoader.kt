@file:JvmName("TTL")

package net.thesilkminer.mc.ematter.common.temperature

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.loader.Context
import net.thesilkminer.mc.boson.api.loader.Processor
import net.thesilkminer.mc.boson.api.loader.loader
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.prefab.loader.context.BaseContextBuilder
import net.thesilkminer.mc.boson.prefab.loader.filter.JsonFileFilter
import net.thesilkminer.mc.boson.prefab.loader.filter.RegularFileFilter
import net.thesilkminer.mc.boson.prefab.loader.filter.SpecialFileFilter
import net.thesilkminer.mc.boson.prefab.loader.locator.DataPackLikeModContainerLocator
import net.thesilkminer.mc.boson.prefab.loader.locator.ResourcesDirectoryLocator
import net.thesilkminer.mc.boson.prefab.loader.naming.DefaultIdentifierBuilder
import net.thesilkminer.mc.boson.prefab.loader.preprocessor.CatchingPreprocessor
import net.thesilkminer.mc.boson.prefab.loader.preprocessor.JsonConverterPreprocessor
import net.thesilkminer.mc.boson.prefab.loader.processor.CatchingProcessor
import net.thesilkminer.mc.boson.prefab.loader.progress.ProgressBarVisitor
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.temperature.condition.get
import net.thesilkminer.mc.ematter.common.temperature.condition.temperatureTableConditionSerializerRegistry

private val l = L(MOD_NAME, "Temperature Tables")

private val temperatureLoader = loader {
    name = "Temperature Tables Loader"
    progressVisitor = ProgressBarVisitor()
    globalContextBuilder = BaseContextBuilder()
    identifierBuilder = DefaultIdentifierBuilder(removeExtension = true)
    locators {
        locator { DataPackLikeModContainerLocator(targetDirectory = "temperature_tables") }
        locator { ResourcesDirectoryLocator(targetDirectory = "temperature_tables") }
    }
    phases {
        "Ensuring schema cleanliness" {
            filters {
                filter { SpecialFileFilter(kind = SpecialFileFilter.Kind.JSON_SCHEMA) }
            }
            processor = object : Processor<Any> {
                override fun process(content: Any, identifier: NameSpacedString, globalContext: Context?, phaseContext: Context?) {
                    throw IllegalStateException("File name 'pattern.json' is invalid.\nThat name is reserved in JSON and has a special meaning " +
                            "that does not apply to this case.\nPlease remove or rename the invalid file.\nID of the broken entry: $identifier" )
                }
            }
        }
        "Loading Temperature Tables" {
            filters {
                filter { RegularFileFilter() }
                filter { JsonFileFilter() }
            }
            preprocessor = CatchingPreprocessor(logger = l, preprocessor = JsonConverterPreprocessor())
            processor = CatchingProcessor(logger = l, processor = TemperatureTableProcessor())
        }
    }
}

private class TemperatureTableProcessor : Processor<JsonObject> {
    private companion object {
        private val l = L(MOD_NAME, "Temperature Tables")
    }

    override fun process(content: JsonObject, identifier: NameSpacedString, globalContext: Context?, phaseContext: Context?) {
        val target = ForgeRegistries.BLOCKS.getValue(identifier.toResourceLocation()) ?: return l.warn("Found temperature table for unrecognized target '$identifier': ignoring")

        val isNormal = content.has("entries")
        val isRedirect = content.has("from")
        if (isNormal && isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got both")
        if (!isNormal && !isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got none")

        val temperatureTable = if (isNormal) this.processNormalLootTable(content) else this.processRedirectLootTable(content)
        try {
            TemperatureTables[target] = temperatureTable
        } catch (e: IllegalStateException) {
            throw JsonParseException("Unable to set the parsed temperature table as the temperature table for '$identifier': ${e.message}", e)
        }
    }

    private fun processNormalLootTable(data: JsonObject): (TemperatureContext) -> Kelvin {
        val entries = JsonUtils.getJsonArray(data, "entries")
        val entryFunctions = entries.asSequence()
                .mapIndexed { index, entry -> JsonUtils.getJsonObject(entry, "entries[$index]") }
                .map(this::processEntry)
                .toList()
        return this.merge(entryFunctions)
    }

    private fun processRedirectLootTable(data: JsonObject): (TemperatureContext) -> Kelvin {
        val from = JsonUtils.getString(data, "from").toNameSpacedString()
        val targetBlock = ForgeRegistries.BLOCKS.getValue(from.toResourceLocation()) ?: throw JsonParseException("Unable to find block '$from'")

        return { TemperatureTables[targetBlock](it) }
    }

    private fun processEntry(data: JsonObject): Pair<() -> Int, List<(TemperatureContext) -> Boolean>> {
        val conditions = JsonUtils.getJsonArray(data, "conditions", JsonArray())
        val listOfConditions = conditions.asSequence()
                .mapIndexed { index, condition -> JsonUtils.getJsonObject(condition, "conditions[$index]") }
                .map(this::processCondition)
                .toList()
        val temperature = JsonUtils.getInt(data, "temperature")
        if (temperature <= 0) throw JsonParseException("Found a temperature which is non-positive: you cannot reach absolute zero (0 K) or further beyond")
        return { temperature } to listOfConditions
    }

    private fun processCondition(data: JsonObject) = temperatureTableConditionSerializerRegistry[data].read(data)

    private fun merge(functions: List<Pair<() -> Int, List<(TemperatureContext) -> Boolean>>>): (TemperatureContext) -> Int {
        val catchAllPair = this.validate(functions)
        val conditionBased = functions.minus(catchAllPair)
        return { context -> (conditionBased.firstOrNull { pair -> pair.second.all { condition -> condition(context) } }?.first ?: catchAllPair.first)() }
    }

    private fun validate(functions: List<Pair<() -> Int, List<(TemperatureContext) -> Boolean>>>) =
            try {
                functions.asSequence().single { it.second.isEmpty() }
            } catch (e: IllegalArgumentException) {
                throw JsonParseException("A set of entries in a temperature tables must have ONE catch-all condition: no more no less", e)
            }
}

internal fun loadTemperatureTables() {
    l.info("Loading temperature tables from data-packs")
    temperatureLoader.load()
}

internal fun freezeTemperatureTables() {
    l.info("Freezing temperature tables to prevent tampering")
    TemperatureTables.freeze()
}
