@file:JvmName("MTL")

package net.thesilkminer.mc.ematter.common.mole

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
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
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_NAME

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

        val moleCount = JsonUtils.getInt(content, "count")
        if (moleCount < 0) throw JsonParseException("[$identifier] Found a mole count which is non-positive: this is not possible")

        MoleTables[target] = moleCount
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
