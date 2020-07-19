package net.thesilkminer.mc.ematter.client.shared

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.renderer.vertex.VertexFormatElement
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.JsonUtils
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.model.ICustomModelLoader
import net.minecraftforge.client.model.IModel
import net.minecraftforge.client.model.ModelLoaderRegistry
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad
import net.minecraftforge.common.model.IModelState
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import java.io.IOException
import java.io.InputStreamReader
import java.util.function.Function

internal class TriangleBasedModelLoader : ICustomModelLoader {
    private val loaderName = NameSpacedString(MOD_ID, "raw_triangles")
    private var resourceManager: IResourceManager? = null

    internal fun register() = ModelLoaderRegistry.registerLoader(this)

    override fun loadModel(modelLocation: ResourceLocation): IModel = this.withModelJson(modelLocation) {
        val points = JsonUtils.getJsonObject(this, "points").parsePoints()
        val faces = JsonUtils.getJsonArray(this, "faces").parseFaces()
        val textures = JsonUtils.getJsonObject(this, "textures").parseTextures()
        TriangleBasedModel(points, faces, textures)
    } ?: throw IllegalStateException("Model '$modelLocation' is being loaded without it existing!")

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        this.resourceManager = resourceManager
    }

    override fun accepts(modelLocation: ResourceLocation) = try {
        this.withModelJson(modelLocation) {
            this.has("loader") && this["loader"].asString.toNameSpacedString() == loaderName
        } ?: false
    } catch (e: IOException) {
        false
    }

    private fun <R> withModelJson(modelLocation: ResourceLocation, block: JsonObject.() -> R): R? =
            this.resourceManager?.getResource(ResourceLocation(modelLocation.namespace, "${modelLocation.path}.json"))?.use { res ->
                InputStreamReader(res.inputStream).use { stream ->
                    val json = GsonBuilder().setLenient().create().fromJson(stream, JsonObject::class.java)
                    return json.block()
                }
            }

    private fun JsonObject.parsePoints() =
            this.entrySet().asSequence()
                    .map { it.key!! to JsonUtils.getJsonArray(it.value, it.key) }
                    .onEach { if (it.second.count() != 3) throw JsonSyntaxException("Unable to parse a point that doesn't have 3 coordinates: found ${it.second.count()}") }
                    .map { it.first to it.second.mapIndexed { i, c -> JsonUtils.getFloat(c, "${it.first}[$i]").toDouble() }.toTypedArray().toDoubleArray() }
                    .map { it.first to Vec3d(it.second[0], it.second[1], it.second[2]) }
                    .toMap()

    private fun JsonArray.parseFaces() =
            this.asSequence()
                    .mapIndexed { i, arr -> JsonUtils.getJsonObject(arr, "faces[$i]") }
                    .map { it.parseFace() }
                    .toList()

    private fun JsonObject.parseTextures() =
            this.entrySet().asSequence()
                    .map { it.key!! to JsonUtils.getString(it.value, it.key).toNameSpacedString() }
                    .toMap()

    private fun JsonObject.parseFace(): TriangleFace {
        val (v1, v2, v3) = JsonUtils.getJsonArray(this, "vertices").asSequence()
                .mapIndexed { index, vertex -> JsonUtils.getJsonObject(vertex, "vertices[$index]") }
                .mapIndexed { index, vertex -> vertex.parseVertexData("vertices[$index]") }
                .toList()
                .let { if (it.count() != 3) throw JsonSyntaxException("A triangle can have only 3 vertices, but found ${it.count()}") else it }
        val textureName = JsonUtils.getString(this, "texture", "all").removePrefix("#")
        return TriangleFace(v1, v2, v3, textureName)
    }

    private fun JsonObject.parseVertexData(name: String): VertexData {
        val point = JsonUtils.getString(this, "point")
        val (u, v) = JsonUtils.getJsonArray(this, "uv").asSequence()
                .mapIndexed { index, element -> JsonUtils.getFloat(element, "${name}[$index]").toDouble() }
                .toList()
                .let { if (it.count() != 2) throw JsonSyntaxException("UV mappings must have two coordinates, but instead found ${it.count()}") else it }
                .toTypedArray()
                .toDoubleArray()
        return VertexData(point, u, v)
    }
}

private data class VertexData(val point: String, val u: Double, val v: Double)
private data class TriangleFace(val vertex1: VertexData, val vertex2: VertexData, val vertex3: VertexData, val textureName: String)

private data class BakedVertexData(val point: Vec3d, val u: Float, val v: Float)
private data class BakedTriangleFace(val vertex1: BakedVertexData, val vertex2: BakedVertexData, val vertex3: BakedVertexData, val texture: TextureAtlasSprite)

private class TriangleBasedModel(private val points: Map<String, Vec3d>, private val faces: List<TriangleFace>, private val textures: Map<String, NameSpacedString>) : IModel {
    override fun bake(state: IModelState, format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>): IBakedModel {
        val textureBaker = { name: NameSpacedString -> bakedTextureGetter.apply(name.toResourceLocation()) }
        val faces = this.faces.bake(this.points, this.textures, textureBaker)
        val particlesTexture = this.textures["particles"] ?: this.textures["all"] ?: throw IllegalArgumentException("Unable to reference either 'particles' or 'all' for particle texture!")
        return TriangleBasedBakedModel(format, faces, textureBaker(particlesTexture))
    }

    override fun getTextures() = this.textures.map { it.value.toResourceLocation() }.toSet()

    private fun List<TriangleFace>.bake(points: Map<String, Vec3d>, textures: Map<String, NameSpacedString>, textureBaker: (NameSpacedString) -> TextureAtlasSprite)
            = this.map { it.bake(points, textures, textureBaker) }

    private fun TriangleFace.bake(points: Map<String, Vec3d>, textures: Map<String, NameSpacedString>, textureBaker: (NameSpacedString) -> TextureAtlasSprite): BakedTriangleFace {
        val vertex1 = this.vertex1.bake(points)
        val vertex2 = this.vertex2.bake(points)
        val vertex3 = this.vertex3.bake(points)
        val texture = textureBaker(textures[this.textureName] ?: throw IllegalArgumentException("Unable to resolve reference to texture '${this.textureName}': is it defined in the model?"))
        return BakedTriangleFace(vertex1, vertex2, vertex3, texture)
    }

    private fun VertexData.bake(points: Map<String, Vec3d>): BakedVertexData {
        val point = points[this.point] ?: throw IllegalArgumentException("Unable to resolve reference to point '${this.point}': is it defined in the model?")
        val u = this.u.toFloat()
        val v = this.v.toFloat()
        return BakedVertexData(point, u, v)
    }
}

private class TriangleBasedBakedModel(private val format: VertexFormat, private val faces: List<BakedTriangleFace>, private val particles: TextureAtlasSprite) : IBakedModel {
    private fun bakeQuads() = this.faces.map { it.bake() }

    override fun getQuads(state: IBlockState?, side: EnumFacing?, rand: Long) = if (side == null) listOf() else this.bakeQuads()
    override fun isBuiltInRenderer() = false
    override fun isAmbientOcclusion() = false
    override fun isGui3d() = false
    override fun getOverrides(): ItemOverrideList = ItemOverrideList.NONE
    override fun getParticleTexture() = this.particles

    // Since MC allows us to render squares and squares only, we will adopt a little trick, and we'll replicate the vertex twice in the call, in the correct position
    private fun BakedTriangleFace.bake() = this@TriangleBasedBakedModel.bakeTriangle(this.vertex1, this.vertex2, this.vertex3, this.texture)

    private fun bakeTriangle(vertex1: BakedVertexData, vertex2: BakedVertexData, vertex3: BakedVertexData, sprite: TextureAtlasSprite) =
            this.bakeTriangle(vertex1.point, vertex2.point, vertex3.point, sprite, this.bakeUvs(vertex1, vertex2, vertex3))

    private fun bakeUvs(vertex1: BakedVertexData, vertex2: BakedVertexData, vertex3: BakedVertexData) =
            arrayOf(floatArrayOf(vertex1.u, vertex1.v), floatArrayOf(vertex2.u, vertex2.v), floatArrayOf(vertex3.u, vertex3.v))

    private fun bakeTriangle(base1: Vec3d, base2: Vec3d, vertex: Vec3d, sprite: TextureAtlasSprite, uvs: Array<FloatArray>) =
            this.bakeQuad(base1, base2, vertex, vertex, sprite, uvs.toList().plus(uvs[2].copyOf()).toTypedArray())

    private fun bakeQuad(topLeft: Vec3d, topRight: Vec3d, bottomRight: Vec3d, bottomLeft: Vec3d, sprite: TextureAtlasSprite, uvs: Array<FloatArray>): BakedQuad {
        val normal = bottomRight.subtract(topRight).cross(topLeft.subtract(topRight)).normalize()

        return UnpackedBakedQuad.Builder(this.format).apply {
            this.setTexture(sprite)
            sequenceOf(topLeft, topRight, bottomRight, bottomLeft).forEachIndexed { index, vertex -> this.putVertex(normal, vertex, uvs[index], sprite) }
        }.build()
    }

    private fun UnpackedBakedQuad.Builder.putVertex(normal: Vec3d, vertex: Vec3d, uv: FloatArray, sprite: TextureAtlasSprite) {
        if (uv.count() != 2) throw IllegalArgumentException("$uv is not of valid length (2 vs ${uv.count()})")
        this.putVertex(normal, vertex.x, vertex.y, vertex.z, uv[0], uv[1], sprite)
    }

    private fun UnpackedBakedQuad.Builder.putVertex(normal: Vec3d, x: Double, y: Double, z: Double, u: Float, v: Float, sprite: TextureAtlasSprite) {
        val interpolatedU = sprite.getInterpolatedU(u.toDouble())
        val interpolatedV = sprite.getInterpolatedV(v.toDouble())
        this@TriangleBasedBakedModel.format.elements.forEachIndexed { index, element ->
            when (element.usage) {
                VertexFormatElement.EnumUsage.POSITION -> this.put(index, x, y, z, 1.0)
                VertexFormatElement.EnumUsage.NORMAL -> this.putNormal(index, normal)
                VertexFormatElement.EnumUsage.COLOR -> this.put(index, 1.0, 1.0, 1.0, 1.0)
                VertexFormatElement.EnumUsage.UV -> if (element.index == 0) this.put(index, interpolatedU, interpolatedV, 0.0F, 1.0F) else this.putNormal(index, normal)
                else -> this.put(index)
            }
        }
    }

    private fun UnpackedBakedQuad.Builder.putNormal(index: Int, normal: Vec3d) = this.put(index, normal.x, normal.y, normal.z, 1.0)

    @Suppress("NOTHING_TO_INLINE") private inline fun Vec3d.cross(vec: Vec3d) = this.crossProduct(vec)
    private fun UnpackedBakedQuad.Builder.put(index: Int, vararg data: Double) = this.put(index, *data.map { it.toFloat() }.toTypedArray().toFloatArray())
}
