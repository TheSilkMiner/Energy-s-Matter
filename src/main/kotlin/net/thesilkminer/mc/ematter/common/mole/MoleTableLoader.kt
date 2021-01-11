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
import net.thesilkminer.mc.ematter.common.mole.modifer.get
import net.thesilkminer.mc.ematter.common.mole.modifer.moleTableModifierSerializerRegistry

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
                    throw IllegalStateException(
                        """
                        File name 'pattern.json' is invalid.
                        That name is reserved in JSON and has a special meaning that does not apply to this case.
                        Please remove or rename the invalid file.
                        ID of the broken entry: '$identifier'
                    """.trimIndent()
                    )
                }
            }
        }
        "Loading Mole Tables" {
            filters {
                filter { RegularFileFilter() }
                filter { JsonFileFilter() }
            }
            preprocessor = CatchingPreprocessor(logger = l, preprocessor = JsonConverterPreprocessor())
            processor = CatchingProcessor(logger = l, processor = MoleTableProcessor)
        }
    }
}

private object MoleTableProcessor : Processor<JsonObject> {

    private val l = L(MOD_NAME, "Mole Tables")

    override fun process(content: JsonObject, identifier: NameSpacedString, globalContext: Context?, phaseContext: Context?) {
        val target = ForgeRegistries.ITEMS.getValue(identifier.toResourceLocation()) ?: return l.warn("Found mole table for unrecognized target '$identifier': ignoring")

        val isNormal = content.has("entries")
        val isRedirect = content.has("from")
        if (isNormal && isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got both")
        if (!isNormal && !isRedirect) throw JsonSyntaxException("Expected either 'entries' or 'from' but got none")

        // determines the moles for a given context
        val table: (MoleContext) -> Moles = if (isNormal) {
            // determines which of the entries should get used for the given context
            val entries: (MoleContext) -> Moles = this.processEntries(JsonUtils.getJsonArray(content, "entries"))

            // first determines which of the modifiers should get used for the given context
            // then applies the modifier function to the given moles
            val modifiers: (MoleContext, Moles) -> Moles =
                if (content.has("modifiers")) {
                    this.processModifiers(JsonUtils.getJsonArray(content, "modifiers"))
                } else {
                    { _, moles -> moles }
                }

            { context -> modifiers(context, entries(context)) }
        } else this.processRedirectTable(content)

        try {
            MoleTables[target] = table
        } catch (e: IllegalStateException) {
            throw JsonParseException("Unable to set the parsed mole table as the mole table for '$identifier': ${e.message}", e)
        }

        l.debug("Successfully loaded mole table for $identifier")
    }

    private fun processEntries(data: JsonArray): (MoleContext) -> Moles {
        // represents all entries as a list consisting of a mole count and the associated conditions
        val entries: List<Pair<Moles, List<(MoleContext) -> Boolean>>> =
            data.asSequence()
                .mapIndexed { index, entry -> JsonUtils.getJsonObject(entry, "entries[$index]") }
                .map(this::processEntry)
                .toList()

        // check if no more than 1 catch-all condition is present
        this.validateConditions(entries.map { it.second }.toList())

        return this.mergeEntries(entries.map { it.first to this.mergeConditions(it.second) })
    }

    private fun processEntry(data: JsonObject): Pair<Moles, List<(MoleContext) -> Boolean>> {
        val conditionsList: List<(MoleContext) -> Boolean> = this.processConditions(JsonUtils.getJsonArray(data, "conditions", JsonArray()))

        val moles = JsonUtils.getInt(data, "moles")
        if (moles < 0) throw JsonSyntaxException("Found a non-positive mole count!")

        return moles to conditionsList
    }

    private fun processModifiers(data: JsonArray): (MoleContext, Moles) -> Moles {
        // represents all modifiers as a list consisting of the modifying function and the associated condition
        val modifiers: List<Pair<(MoleContext, Moles) -> Moles, (MoleContext) -> Boolean>> =
            data.asSequence()
                .mapIndexed { index, entry -> JsonUtils.getJsonObject(entry, "modifiers[$index]") }
                .map(this::processModifier)
                .toList()

        return this.mergeModifiers(modifiers)
    }

    private fun processModifier(data: JsonObject): Pair<(MoleContext, Moles) -> Moles, (MoleContext) -> Boolean> {
        val conditions: (MoleContext) -> Boolean = this.mergeConditions(this.processConditions(JsonUtils.getJsonArray(data, "conditions", JsonArray())))

        val modifier: (MoleContext, Moles) -> Moles = try {
            // get the modifier serializer associated with that data (uses the 'type' tag to determine the modifier type)
            // then parse the json to an actual modifying function
            moleTableModifierSerializerRegistry[data].read(data)
        } catch (e: Exception) {
            throw JsonParseException("Error while processing modifier: ${e.message}", e)
        }

        return modifier to conditions
    }

    private fun processConditions(data: JsonArray): List<(MoleContext) -> Boolean> =
        data.asSequence()
            .mapIndexed { index, condition -> JsonUtils.getJsonObject(condition, "conditions[$index]") }
            .map(this::processCondition)
            .toList()

    private fun processCondition(data: JsonObject): (MoleContext) -> Boolean =
        try {
            // get the condition serializer associated with that data (uses the 'type' tag to determine the condition type)
            // then parse the json to an actual condition
            moleTableConditionSerializerRegistry[data].read(data)
        } catch (e: Exception) {
            throw JsonParseException("Error while processing condition: ${e.message}", e)
        }

    private fun validateConditions(conditions: List<List<(MoleContext) -> Boolean>>) {
        if (conditions.count { it.isEmpty() } > 1) {
            throw JsonParseException("Found more than one catch-all condition! A mole table must either have zero or exact one such conditions.")
        }
    }

    private fun mergeConditions(conditions: List<(MoleContext) -> Boolean>): (MoleContext) -> Boolean =
        { context -> conditions.all { condition -> condition(context) } }

    private fun mergeEntries(entries: List<Pair<Moles, (MoleContext) -> Boolean>>): (MoleContext) -> Moles =
        { context -> entries.firstOrNull { pair -> pair.second(context) }?.first ?: 0 }

    private fun mergeModifiers(modifiers: List<Pair<(MoleContext, Moles) -> Moles, (MoleContext) -> Boolean>>): (MoleContext, Moles) -> Moles =
        { context, moles -> modifiers.filter { it.second(context) }.map { it.first }.fold(moles) { previously, func -> func(context, previously) } }

    private fun processRedirectTable(data: JsonObject): (MoleContext) -> Moles {
        val from = JsonUtils.getString(data, "from").toNameSpacedString()
        val targetItem = ForgeRegistries.ITEMS.getValue(from.toResourceLocation()) ?: throw JsonParseException("Unable to find item '$from'")

        return { MoleTables[targetItem](it) }
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
