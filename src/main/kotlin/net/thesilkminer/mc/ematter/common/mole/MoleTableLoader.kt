@file:JvmName("MTL")

package net.thesilkminer.mc.ematter.common.mole

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
import net.thesilkminer.mc.ematter.common.mole.condition.get
import net.thesilkminer.mc.ematter.common.mole.condition.moleTableConditionSerializerRegistry

private val l = L(MOD_NAME, "Mole Tables")

private val moleLoader = loader {
    name = "Mole Tables Loader"
    progressVisitor = ProgressBarVisitor()
    globalContextBuilder = BaseContextBuilder()
    identifierBuilder = DefaultIdentifierBuilder(removeExtension = true)
    locators {
        locator { DataPackLikeModContainerLocator(targetDirectory = "mole_tables") }
        locator { ResourcesDirectoryLocator(targetDirectory = "mole_tables") }
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
        "Loading Mole Tables" {
            filters {
                filter { RegularFileFilter() }
                filter { JsonFileFilter() }
            }
            preprocessor = CatchingPreprocessor(logger = l, preprocessor = JsonConverterPreprocessor())
            processor = CatchingProcessor(logger = l, processor = MoleTableProcessor())
        }
    }
}

private class MoleTableProcessor : Processor<JsonObject> {
    private companion object {
        private val l = L(MOD_NAME, "Mole Tables")
    }

    override fun process(content: JsonObject, identifier: NameSpacedString, globalContext: Context?, phaseContext: Context?) {
        val target = ForgeRegistries.ITEMS.getValue(identifier.toResourceLocation()) ?: return l.warn("Found mole table for unrecognized target '$identifier': ignoring")

        val isNormal = content.has("entries")
        val isRedirect = content.has("from")
        if (isNormal && isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got both")
        if (!isNormal && !isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got none")

        val moleTable = if (isNormal) this.processNormalTable(content) else this.processRedirectTable(content)
        try {
            MoleTables[target] = moleTable
        } catch (e: IllegalStateException) {
            throw JsonParseException("Unable to set the parsed mole table as the mole table for '$identifier': ${e.message}", e)
        }
    }

    private fun processNormalTable(data: JsonObject): (MoleContext) -> Moles {
        val entries = JsonUtils.getJsonArray(data, "entries")
        val entryFunctions = entries.asSequence()
                .mapIndexed { index, entry -> JsonUtils.getJsonObject(entry, "entries[$index]") }
                .map(this::processEntry)
                .toList()
        return this.merge(entryFunctions)
    }

    private fun processRedirectTable(data: JsonObject): (MoleContext) -> Moles {
        val from = JsonUtils.getString(data, "from").toNameSpacedString()
        val targetItem = ForgeRegistries.ITEMS.getValue(from.toResourceLocation()) ?: throw JsonParseException("Unable to find block '$from'")

        return { MoleTables[targetItem](it) }
    }

    private fun processEntry(data: JsonObject): Pair<() -> Moles, List<(MoleContext) -> Boolean>> {
        val conditions = JsonUtils.getJsonArray(data, "conditions", JsonArray())
        val listOfConditions = conditions.asSequence()
                .mapIndexed { index, condition -> JsonUtils.getJsonObject(condition, "conditions[$index]") }
                .map(this::processCondition)
                .toList()
        val moleCount = JsonUtils.getInt(data, "moles")
        if (moleCount < 0) throw JsonParseException("Found a mole count which is non-positive: this is not possible")
        return { moleCount } to listOfConditions
    }

    private fun processCondition(data: JsonObject) = moleTableConditionSerializerRegistry[data].read(data)

    private fun merge(functions: List<Pair<() -> Moles, List<(MoleContext) -> Boolean>>>): (MoleContext) -> Moles {
        val catchAllPair = this.validate(functions)
        val conditionBased = functions.minus(catchAllPair)
        return { context -> (conditionBased.firstOrNull { pair -> pair.second.all { condition -> condition(context) } }?.first ?: catchAllPair.first)() }
    }

    private fun validate(functions: List<Pair<() -> Int, List<(MoleContext) -> Boolean>>>) =
            try {
                functions.single { it.second.isEmpty() }
            } catch (e: IllegalArgumentException) {
                throw JsonParseException("A set of entries in a mole table must have ONE catch-all condition: no more no less", e)
            }
}

internal fun loadMoleTables() {
    l.info("Loading mole tables from data-packs")
    moleLoader.load()
}

internal fun freezeMoleTables() {
    l.info("Freezing mole tables to prevent tampering")
    MoleTables.freeze()
}
