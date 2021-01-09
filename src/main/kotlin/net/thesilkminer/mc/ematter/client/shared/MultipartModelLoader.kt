package net.thesilkminer.mc.ematter.client.shared

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.gson.*
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.*
import net.minecraftforge.common.model.IModelState
import net.minecraftforge.common.model.TRSRTransformation
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.MOD_ID
import java.io.InputStreamReader
import java.util.*
import java.util.function.Function
import kotlin.NoSuchElementException

/*
 * the whole loader is based upon vanillas item models
 *
 * {
 *  "loader": "ematter:multipart",
 *  "parts": [
 *    {
 *      "model": "modid:model_location",    // can be both block and item model locations
 *      "x": 90,                            // defaults to 0
 *      "y": 270                            // defaults to 0
 *    },
 *    {
 *      ...
 *    }
 *  ],
 *  "display": {                            // defaults to item/generated
 *    ...
 *  },
 *  "texture": {                            // optional
 *    "particle": "modid:texture_location"
 *  }
 * }
 */

internal object MultipartModelLoader : ICustomModelLoader {

    private val name = NameSpacedString(MOD_ID, "multipart")
    private var resourceManager: IResourceManager? = null

    internal fun register() = ModelLoaderRegistry.registerLoader(this)

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        this.resourceManager = resourceManager
    }

    override fun accepts(modelLocation: ResourceLocation) = try {
        this.withModelJson(modelLocation) {
            this["loader"]?.let {
                it.isJsonPrimitive && it.asString.toNameSpacedString() == this@MultipartModelLoader.name
            } ?: false
        } ?: false
    } catch (e: Exception) {
        false
    }

    override fun loadModel(modelLocation: ResourceLocation) = this.withModelJson(modelLocation) {
        MultipartModel.deserialize(this)
    } ?: throw IllegalStateException("Model '$modelLocation' is being loaded without it existing!")

    private fun <R> withModelJson(modelLocation: ResourceLocation, block: JsonObject.() -> R): R? =
        this.resourceManager?.getResource(ResourceLocation(modelLocation.namespace, "${modelLocation.path}.json"))?.use { res ->
            InputStreamReader(res.inputStream).use { stream ->
                val json = GsonBuilder().setLenient().create().fromJson(stream, JsonObject::class.java)
                return json.block()
            }
        }
}

internal class MultipartModel(
    private val parts: List<Triple<ResourceLocation, IModel, ModelRotation>>,
    private val transforms: ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation>,
    private val textureMap: Map<String, ResourceLocation> = emptyMap()
) : IModel {

    override fun bake(state: IModelState, format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) =
        BakedItemModel(
            this.parts.getQuads(format, bakedTextureGetter),
            bakedTextureGetter.apply(this.textureMap.getOrDefault("particle", ResourceLocation("missingno"))),
            this.transforms,
            ItemOverrideList.NONE,
            false
        )

    private fun List<Triple<ResourceLocation, IModel, ModelRotation>>.getQuads(format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) =
        ImmutableList.builder<BakedQuad>().addAll(
            this.map { it.second.bake(it.third, format, bakedTextureGetter) }
                .map { it.getQuads(null, null, 0) }
                .flatten()
                .asIterable()
        ).build()

    override fun getDependencies(): Collection<ResourceLocation> = this.parts.map { it.first }.toSet()
    override fun getTextures(): Collection<ResourceLocation> = this.textureMap.values

    companion object {

        private val defaultTransforms: ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> =
            ModelLoaderRegistry.getModel(ResourceLocation("minecraft", "item/handheld"))
                .asVanillaModel()
                .orElseThrow { NoSuchElementException("Could not get item/generated as vanilla model!") }
                .let { PerspectiveMapWrapper.getTransforms(it.allTransforms) }

        fun deserialize(json: JsonObject): MultipartModel {
            val parts = this.getParts(json["parts"].asJsonArray)
            val transforms = if (json.has("display")) this.getTransforms(json["display"].asJsonObject) else this.defaultTransforms
            val texture = if (json.has("texture")) {
                json["texture"].asJsonObject.let { textures -> textures.entrySet().map { it.key to ResourceLocation(it.value.asString) } }.toMap()
            } else emptyMap()

            return MultipartModel(parts, transforms, texture)
        }

        private fun getParts(list: JsonArray): List<Triple<ResourceLocation, IModel, ModelRotation>> = list.asSequence()
            .map(JsonElement::getAsJsonObject)
            .map { ResourceLocation(it["model"].asString) to Pair(it.get("x")?.asInt ?: 0, it.get("y")?.asInt ?: 0) }
            .map { Triple(it.first, ModelLoaderRegistry.getModel(it.first), ModelRotation.getModelRotation(it.second.first, it.second.second)) }
            .toList()

        // sadly I can't just deserialize to ItemCameraTransforms
        // well, technically I could write my own serializer but this works as well
        private fun getTransforms(json: JsonObject): ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> =
            ModelBlock.deserialize(JsonObject().apply { this.add("display", json) }.toString()).allTransforms.let { transforms ->
                PerspectiveMapWrapper.getTransforms(transforms)
            }
    }
}
