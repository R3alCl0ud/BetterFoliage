package mods.betterfoliage.client.integration

import mods.betterfoliage.client.Client
import mods.betterfoliage.client.config.Config
import mods.betterfoliage.loader.Refs
import mods.octarinecore.metaprog.allAvailable
import net.minecraft.block.Block
import net.minecraft.block.BlockTallGrass
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.init.Blocks
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.apache.logging.log4j.Level.INFO

/**
 * Integration for ShadersMod.
 */
@SideOnly(Side.CLIENT)
object ShadersModIntegration {

    @JvmStatic var isAvailable = allAvailable(Refs.sVertexBuilder, Refs.pushEntity_state, Refs.pushEntity_num, Refs.popEntity)
    @JvmStatic val tallGrassEntityData = entityDataFor(Blocks.TALLGRASS.defaultState.withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS))
    @JvmStatic val leavesEntityData = entityDataFor(Blocks.LEAVES.defaultState)

    fun entityDataFor(blockState: IBlockState) =
        (Block.REGISTRY.getIDForObject(blockState.block).toLong() and 65535) or
        ((blockState.renderType.ordinal.toLong() and 65535) shl 16) or
        (blockState.block.getMetaFromState(blockState).toLong() shl 32)


    /**
     * Called from transformed ShadersMod code.
     * @see mods.betterfoliage.loader.BetterFoliageTransformer
     */
    @JvmStatic fun getBlockIdOverride(original: Long, blockState: IBlockState): Long {
        if (Config.blocks.leavesClasses.matchesClass(blockState.block)) return leavesEntityData
        if (Config.blocks.crops.matchesClass(blockState.block)) return tallGrassEntityData
        return original
    }

    init {
        Client.log(INFO, "ShadersMod integration is ${if (isAvailable) "enabled" else "disabled" }")
    }

    /** Quads rendered inside this block will use the given block entity data in shader programs. */
    inline fun renderAs(blockEntityData: Long, renderer: BufferBuilder, enabled: Boolean = true, func: ()->Unit) {
        if ((isAvailable && enabled)) {
            val vertexBuilder = Refs.sVertexBuilder.get(renderer)!!
            Refs.pushEntity_num.invoke(vertexBuilder, blockEntityData)
            func()
            Refs.popEntity.invoke(vertexBuilder)
        } else {
            func()
        }
    }

    /** Quads rendered inside this block will use the given block entity data in shader programs. */
    inline fun renderAs(state: IBlockState, renderer: BufferBuilder, enabled: Boolean = true, func: ()->Unit) =
        renderAs(entityDataFor(state), renderer, enabled, func)

    /** Quads rendered inside this block will behave as tallgrass blocks in shader programs. */
    inline fun grass(renderer: BufferBuilder, enabled: Boolean = true, func: ()->Unit) =
        renderAs(tallGrassEntityData, renderer, enabled, func)

    /** Quads rendered inside this block will behave as leaf blocks in shader programs. */
    inline fun leaves(renderer: BufferBuilder, enabled: Boolean = true, func: ()->Unit) =
        renderAs(leavesEntityData, renderer, enabled, func)
}
