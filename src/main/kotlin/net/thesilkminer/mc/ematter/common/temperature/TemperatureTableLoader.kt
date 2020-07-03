package net.thesilkminer.mc.ematter.common.temperature

import net.thesilkminer.mc.boson.api.loader.loader
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.prefab.loader.context.BaseContextBuilder
import net.thesilkminer.mc.boson.prefab.loader.filter.JsonFileFilter
import net.thesilkminer.mc.boson.prefab.loader.filter.RegularFileFilter
import net.thesilkminer.mc.boson.prefab.loader.locator.DataPackLikeModContainerLocator
import net.thesilkminer.mc.boson.prefab.loader.locator.ResourcesDirectoryLocator
import net.thesilkminer.mc.boson.prefab.loader.naming.DefaultIdentifierBuilder
import net.thesilkminer.mc.boson.prefab.loader.preprocessor.CatchingPreprocessor
import net.thesilkminer.mc.boson.prefab.loader.preprocessor.JsonConverterPreprocessor
import net.thesilkminer.mc.boson.prefab.loader.processor.CatchingProcessor
import net.thesilkminer.mc.boson.prefab.loader.progress.ActiveModContainerVisitor
import net.thesilkminer.mc.boson.prefab.loader.progress.ProgressBarVisitor
import net.thesilkminer.mc.ematter.MOD_NAME

private val l = L(MOD_NAME, "TemperatureTableLoader")

private val temperatureLoader = loader {
    name = "Temperature Tables Loader"
    progressVisitor = ProgressBarVisitor().chain(ActiveModContainerVisitor())
    globalContextBuilder = BaseContextBuilder()
    identifierBuilder = DefaultIdentifierBuilder(removeExtension = true)
    locators {
        locator { DataPackLikeModContainerLocator(targetDirectory = "temperature_tables")}
        locator { ResourcesDirectoryLocator(targetDirectory = "temperature_tables") }
    }
    phases {
        "Registering Temperature Tables" {
            filters {
                filter { RegularFileFilter() }
                filter { JsonFileFilter() }
            }
            preprocessor = CatchingPreprocessor(logger = l, preprocessor = JsonConverterPreprocessor())
            processor = CatchingProcessor(logger = l, processor = TemperatureTableProcessor())
        }
    }
}

internal fun loadTemperatureTables() {
    l.debug("Preparing to load temperature tables from data-packs")
    temperatureLoader.load()
}
