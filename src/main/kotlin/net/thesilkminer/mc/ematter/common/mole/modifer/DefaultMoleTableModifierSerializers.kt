package net.thesilkminer.mc.ematter.common.mole.modifer

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.ematter.common.mole.MoleContext
import net.thesilkminer.mc.ematter.common.mole.Moles
import kotlin.math.roundToInt

internal class MoleAdditionModifierSerializer : IForgeRegistryEntry.Impl<MoleTableModifierSerializer>(), MoleTableModifierSerializer {

    override fun read(json: JsonObject): (MoleContext, Moles) -> Moles {
        val value: Int = json["value"].asInt

        if (value < 0) throw JsonParseException("Failed to parse mole table addition modifier: value was negative! Use subtraction modifier instead.")

        return { _, it -> it + value }
    }
}

internal class MoleSubtractionModifierSerializer : IForgeRegistryEntry.Impl<MoleTableModifierSerializer>(), MoleTableModifierSerializer {

    override fun read(json: JsonObject): (MoleContext, Moles) -> Moles {
        val value: Int = json["value"].asInt

        if (value < 0) throw JsonParseException("Failed to parse mole table subtraction modifier: value was negative! Use addition modifier instead.")

        return { _, it -> it - value }
    }
}

internal class MoleMultiplicationModifierSerializer : IForgeRegistryEntry.Impl<MoleTableModifierSerializer>(), MoleTableModifierSerializer {

    override fun read(json: JsonObject): (MoleContext, Moles) -> Moles {
        val value: Double = json["value"].asDouble

        if (value < 0) throw JsonParseException("Failed to parse mole table multiplication modifier: value was negative! A negative amount of moles makes no sense.")

        return { _, it -> (it * value).roundToInt() }
    }
}

internal class MoleDivisionModifierSerializer : IForgeRegistryEntry.Impl<MoleTableModifierSerializer>(), MoleTableModifierSerializer {

    override fun read(json: JsonObject): (MoleContext, Moles) -> Moles {
        val value: Double = json["value"].asDouble

        if (value < 0) throw JsonParseException("Failed to parse mole table division modifier: value was negative! A negative amount of moles makes no sense.")

        return { _, it -> (it / value).roundToInt() }
    }
}
