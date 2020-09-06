@file:JvmName("DSFS")

package net.thesilkminer.mc.ematter.common.recipe.mad.step

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistryEntry
import kotlin.math.E
import kotlin.math.pow

@ExperimentalUnsignedTypes
internal class ConstantSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class ConstantSteppingFunction(private val cost: ULong) : SteppingFunction {
        override fun getPowerCostAt(x: Long) = this.cost
    }

    override fun read(json: JsonObject): SteppingFunction {
        if (!json.has("value")) throw JsonSyntaxException("Missing member 'value': expected to find a number")
        val element = json["value"]
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) throw JsonSyntaxException("Expected 'value' to be a number, but instead found a ${JsonUtils.toString(element)}")
        return ConstantSteppingFunction(element.asJsonPrimitive.asLong.toULong())
    }
}

@ExperimentalUnsignedTypes
internal class ExponentialSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class ExponentialSteppingFunction(private val coefficient: Long, private val base: Double, private val mirror: Boolean, private val translation: Long) : SteppingFunction {
        // y = a * b^((-1)^m * (x - t))
        override fun getPowerCostAt(x: Long) = (this.coefficient * this.base.pow((x - this.translation).toDouble() * (-1).pow(this.mirror))).toULong()
        @Suppress("NOTHING_TO_INLINE") private inline fun Int.pow(boolean: Boolean) = this.toDouble().pow(if (boolean) 0 else 1)
    }

    override fun read(json: JsonObject): SteppingFunction {
        val coefficient = json.long("coefficient", 1L)
        val base = json.double("base", E)
        val mirror = JsonUtils.getBoolean(json, "mirror", false)
        val translation = json.long("translation", 0L)

        if (base <= 0) throw JsonSyntaxException("An exponential function with a non-positive base is not defined! Expected > 0, but found $base")
        if (coefficient <= 0) throw JsonParseException("It is impossible to define a negative exponential function, since it will never be positive: expected > 0, but found $coefficient")

        return ExponentialSteppingFunction(coefficient, base, mirror, translation)
    }

    private fun JsonObject.double(name: String, default: Double): Double {
        if (!this.has(name)) return default
        val value = this[name]
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) throw JsonSyntaxException("Expected to find a number for argument '$name', but instead found ${JsonUtils.toString(value)}")
        return value.asJsonPrimitive.asDouble
    }
}

@ExperimentalUnsignedTypes
internal class LinearSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class LinearSteppingFunction(private val slope: Long, private val intercept: Long) : SteppingFunction {
        override fun getPowerCostAt(x: Long) = (this.slope * x + this.intercept).toULong()
    }

    override fun read(json: JsonObject): SteppingFunction {
        val slope = json.long("slope")
        val intercept = json.long("intercept", 0L)

        // Sanity checks
        // coefficient = 0 -> constant function
        if (slope == 0L) throw JsonSyntaxException("A linear formula cannot have a zero slope: use 'constant' instead")
        // coefficient < 0 -> line that goes into the negatives
        if (slope < 0L) throw JsonParseException("It is illegal to define a line with a negative slop: it will end up in the negatives, but we found $slope")
        // if intercept < 0 -> the line starts in the negatives
        if (intercept < 0L) throw JsonParseException("The intercept is negative ($intercept), so the line starts within negatives: this is illegal")

        return LinearSteppingFunction(slope, intercept)
    }
}

@ExperimentalUnsignedTypes
internal class PiecewiseSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private data class PiecewisePiece(val start: Long, val end: Long, val function: SteppingFunction) // [start, end)

    private class PiecewiseSteppingFunction(private val pieces: List<PiecewisePiece>) : SteppingFunction {
        override fun getPowerCostAt(x: Long) = this.findPiece(x).function.getPowerCostAt(x)
        private fun findPiece(x: Long) = this.pieces.first { it.start <= x && x < it.end }
    }

    override fun read(json: JsonObject): SteppingFunction {
        val pieces = JsonUtils.getJsonArray(json, "pieces")
                .asSequence()
                .mapIndexed { i, item -> JsonUtils.getJsonObject(item, "pieces[$i]") }
                .map(this::buildPiece)
                .sortedBy { it.start }
                .toList()
        pieces.validate()
        return PiecewiseSteppingFunction(pieces)
    }

    private fun buildPiece(element: JsonObject): PiecewisePiece {
        val start = element.long("start")
        val end = element.long("end", Long.MAX_VALUE)
        val serializedSteppingFunction = JsonUtils.getJsonObject(element, "function")
        val function = steppingFunctionSerializerRegistry[serializedSteppingFunction].read(serializedSteppingFunction)
        return PiecewisePiece(start, end, function)
    }

    private fun List<PiecewisePiece>.validate() {
        var startPoint = -1L
        var endPoint = 0L
        this.forEach { piece ->
            if (piece.start != endPoint) throw JsonParseException("Found a piece whose beginning doesn't match the expected value (found: ${piece.start}, expected: $endPoint)")
            if (piece.start >= piece.end) throw JsonParseException("Found a piece that begins after its end (found ${piece.end}, expected at least ${piece.start})")
            if (startPoint == -1L) startPoint = piece.start
            endPoint = piece.end
        }
        if (startPoint != 0L) throw JsonParseException("The set of pieces found does not begin at 0, rather at $startPoint: this is illegal")
        if (endPoint != Long.MAX_VALUE) throw JsonParseException("The set of pieces found does not cover the whole set of positive longs, rather it stops at $endPoint: this is illegal")
    }
}

@ExperimentalUnsignedTypes
internal class QuadraticSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class QuadraticSteppingFunction(private val quadraticCoefficient: Long, private val unitCoefficient: Long, private val intercept: Long) : SteppingFunction {
        override fun getPowerCostAt(x: Long) = ((this.quadraticCoefficient * x * x) + (this.unitCoefficient * x) + this.intercept).toULong()
    }

    override fun read(json: JsonObject): SteppingFunction {
        val quadraticCoefficient = json.long("quadratic_coefficient")
        val unitCoefficient = json.long("unit_coefficient", 0L)
        val intercept = json.long("intercept", 0L)

        // Sanity checks
        // 0x^2 + bx + c -> line which is illegal
        if (quadraticCoefficient == 0L) throw JsonSyntaxException("A quadratic formula cannot have a zero quadratic coefficient: use 'linear' instead!")
        // a < 0 gives a descending parabola, which is not allowed
        if (quadraticCoefficient < 0L) throw JsonParseException("It is illegal to define a descending parabola as a stepping function: expected positive, found $quadraticCoefficient")
        // If intercept is negative, the vertex is negative, which is not allowed
        if (intercept < 0L) throw JsonParseException("The given parabola intercepts the y axis in the negatives: this is not allowed (expected non-negative, found $intercept)")

        // Vertex calculations
        val vertexX = -(unitCoefficient.toDouble() / (2 * quadraticCoefficient.toDouble()))
        if (vertexX > 0) {
            val vertexY = -(((unitCoefficient.toDouble()).let { it * it } - 4 * quadraticCoefficient * intercept) / (4 * quadraticCoefficient.toDouble()))
            if (vertexY < 0) throw JsonParseException("The given parabola has a negative vertex coordinate: this is not allowed")
        }

        return QuadraticSteppingFunction(quadraticCoefficient, unitCoefficient, intercept)
    }
}
