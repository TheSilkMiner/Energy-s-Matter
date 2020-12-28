package net.thesilkminer.mc.ematter.client.feature.cable

import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.*
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.ICustomModelLoader
import net.minecraftforge.client.model.IModel
import net.minecraftforge.client.model.ModelLoaderRegistry
import net.minecraftforge.common.model.IModelState
import net.minecraftforge.common.property.IExtendedBlockState
import net.thesilkminer.mc.boson.api.direction.Direction
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.cable.CableBlock
import java.util.function.Function

internal object CableModelLoader : ICustomModelLoader {

    private val loaderName = NameSpacedString(MOD_ID, "cables")

    fun register() = ModelLoaderRegistry.registerLoader(this)

    override fun onResourceManagerReload(resourceManager: IResourceManager) = Unit

    override fun accepts(modelLocation: ResourceLocation) =
        modelLocation is ModelResourceLocation && modelLocation.namespace == MOD_ID && modelLocation.path.contains(Regex("cable"))

    override fun loadModel(modelLocation: ResourceLocation) =
        CableModel((modelLocation as? ModelResourceLocation) ?: throw IllegalArgumentException("$loaderName expected $modelLocation to be ModelResourceLocation but cast failed."))

    override fun toString(): String = loaderName.toString()
}

internal class CableModel(modelLocation: ModelResourceLocation, val type: String = modelLocation.path) : IModel {

    private val coreModel: IModel = ModelLoaderRegistry.getModel(ResourceLocation(modelLocation.namespace, "block/${this.type}/core"))
    private val rodModel: IModel = ModelLoaderRegistry.getModel(ResourceLocation(modelLocation.namespace, "block/${this.type}/rod"))

    override fun bake(state: IModelState, format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) =
        CableBakedModel(this.coreModel, this.rodModel, state, format, bakedTextureGetter)
}

internal class CableBakedModel(coreModel: IModel, rodModel: IModel, state: IModelState, format: VertexFormat, bakedTextureGetter: Function<ResourceLocation, TextureAtlasSprite>) : IBakedModel {

    private val coreQuads: MutableList<BakedQuad> = coreModel.bake(state, format, bakedTextureGetter).getQuads(null, null, 0)
    private val rodQuads: Map<Direction, MutableList<BakedQuad>> = mapOf(
        Direction.NORTH to rodModel.bake(ModelRotation.X0_Y0, format, bakedTextureGetter).getQuads(null, null, 0),
        Direction.EAST to rodModel.bake(ModelRotation.X0_Y90, format, bakedTextureGetter).getQuads(null, null, 0),
        Direction.SOUTH to rodModel.bake(ModelRotation.X0_Y180, format, bakedTextureGetter).getQuads(null, null, 0),
        Direction.WEST to rodModel.bake(ModelRotation.X0_Y270, format, bakedTextureGetter).getQuads(null, null, 0),
        Direction.UP to rodModel.bake(ModelRotation.X270_Y0, format, bakedTextureGetter).getQuads(null, null, 0),
        Direction.DOWN to rodModel.bake(ModelRotation.X90_Y0, format, bakedTextureGetter).getQuads(null, null, 0)
    )

    private val coreTexture: TextureAtlasSprite = bakedTextureGetter.apply(ResourceLocation(MOD_ID, "blocks/cable/core"))

    @ExperimentalUnsignedTypes
    override fun getQuads(state: IBlockState?, face: EnumFacing?, rand: Long): MutableList<BakedQuad> {
        if (state == null) {
            //if state is null this got called to render an item
            val quads: MutableList<BakedQuad> = mutableListOf()
            quads.addAll(this.coreQuads)
            quads.addAll(this.rodQuads.getValue(Direction.EAST))
            quads.addAll(this.rodQuads.getValue(Direction.WEST))
            return quads
        }
        //we don't want to render specific sides individually
        if (face != null) return mutableListOf()
        if (state !is IExtendedBlockState) throw IllegalArgumentException("Tried to get the quads for $this but $state is no ${IExtendedBlockState::class.simpleName}!")

        val quads: MutableList<BakedQuad> = mutableListOf()
        quads.addAll(this.coreQuads)
        state.getConnectionSet().forEach { quads.addAll(this.rodQuads.getValue(it)) }
        return quads
    }

    override fun getParticleTexture(): TextureAtlasSprite = this.coreTexture
    override fun isBuiltInRenderer(): Boolean = false
    override fun isAmbientOcclusion(): Boolean = false
    override fun isGui3d(): Boolean = false
    override fun getOverrides(): ItemOverrideList = ItemOverrideList.NONE
}

fun IExtendedBlockState.getConnectionSet() = mutableSetOf<Direction>().apply {
    if (this@getConnectionSet.getValue(CableBlock.connectionNorth)) this.add(Direction.NORTH)
    if (this@getConnectionSet.getValue(CableBlock.connectionEast)) this.add(Direction.EAST)
    if (this@getConnectionSet.getValue(CableBlock.connectionSouth)) this.add(Direction.SOUTH)
    if (this@getConnectionSet.getValue(CableBlock.connectionWest)) this.add(Direction.WEST)
    if (this@getConnectionSet.getValue(CableBlock.connectionUp)) this.add(Direction.UP)
    if (this@getConnectionSet.getValue(CableBlock.connectionDown)) this.add(Direction.DOWN)
}.toSet()
