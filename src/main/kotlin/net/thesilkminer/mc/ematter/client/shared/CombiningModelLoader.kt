package net.thesilkminer.mc.ematter.client.shared

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
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
import java.util.function.Function

internal object CombiningModelLoader : ICustomModelLoader {

    private val loaderName = NameSpacedString(MOD_ID, "combining")
    private var resourceManager: IResourceManager? = null

    internal fun register() = ModelLoaderRegistry.registerLoader(this)

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        this.resourceManager = resourceManager
    }

    override fun accepts(modelLocation: ResourceLocation) = try {
        this.withModelJson(modelLocation) {
            this["loader"]?.let {
                it.isJsonPrimitive && it.asString.toNameSpacedString() == loaderName
            } ?: false
        } ?: false
    } catch (e: Exception) {
        false
    }

    override fun loadModel(modelLocation: ResourceLocation) = this.withModelJson(modelLocation) {
        CombinedModel(this.getAsJsonArray("combine").asSequence()
            .map { it.asJsonObject }
            .map {
                val model = ResourceLocation(it["model"].asString).asBlockLocation()
                val state = Pair(it["x"]?.asInt ?: 0, it["y"]?.asInt ?: 0)
                model to state
            }
            .toList(), if (this.has("display")) this["display"].asJsonObject else JsonObject(), ResourceLocation(this["particle"].asString))
    } ?: throw IllegalStateException("Model '$modelLocation' is being loaded without it existing!")

    private fun <R> withModelJson(modelLocation: ResourceLocation, block: JsonObject.() -> R): R? =
        this.resourceManager?.getResource(ResourceLocation(modelLocation.namespace, "${modelLocation.path}.json"))?.use { res ->
            InputStreamReader(res.inputStream).use { stream ->
                val json = GsonBuilder().setLenient().create().fromJson(stream, JsonObject::class.java)
                return json.block()
            }
        }

    private fun ResourceLocation.asBlockLocation() = ResourceLocation(this.namespace, "block/${this.path}")
}

internal class CombinedModel(combine: List<Pair<ResourceLocation, Pair<Int, Int>>>, display: JsonObject, private val particle: ResourceLocation) : IModel {

    private val transforms: ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> = display.getDisplayTransforms()

    private val models: List<Pair<IModel, ModelRotation>> = combine.map { ModelLoaderRegistry.getModel(it.first) to ModelRotation.getModelRotation(it.second.first, it.second.second) }.toList()

    override fun bake(state: IModelState, format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) =
        BakedItemModel(this.models.getQuads(format, bakedTextureGetter), bakedTextureGetter.apply(this.particle), this.transforms, ItemOverrideList.NONE, false)

    // Helper Functions >>
    private fun List<Pair<IModel, ModelRotation>>.getQuads(format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) =
        ImmutableList.builder<BakedQuad>().addAll(
            this.map { it.first.bake(it.second, format, bakedTextureGetter) }
                .map { it.getQuads(null, null, 0) }
                .flatten()
                .asIterable()
        ).build()

    private fun JsonObject.getDisplayTransforms() =
        ModelBlock.deserialize(JsonObject().apply { this.add("display", this@getDisplayTransforms) }.toString()).allTransforms.getTransformMap()

    private fun ItemCameraTransforms.getTransformMap() = PerspectiveMapWrapper.getTransforms(this)
}
