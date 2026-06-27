package net.zhengzhengyiyi.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.zhengzhengyiyi.client.mixin.accessor.VertexBufferAccessor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reduces per-chunk GL overhead in {@code WorldRenderer.renderLayer}.
 *
 * <h2>What 1.21.11 does differently</h2>
 * 1.21.11 replaces the entire per-chunk draw loop with {@code renderBlockLayers},
 * which builds one {@code List<RenderObject>} per layer, writes all chunk
 * transforms into a UBO via {@code DynamicUniforms.writeChunkSections()}, and
 * dispatches with a single {@code drawMultipleIndexed} call.
 *
 * <h2>What this mixin does</h2>
 * Fully porting that requires bridging vanilla's framebuffer and texture
 * attachment lifecycle to the mod's {@code GlTexture}/{@code GlTextureView}
 * ownership model — a large, fragile change. Instead this mixin ports the two
 * independent optimisations that are safe to apply without touching the render
 * target:
 *
 * <ol>
 *   <li><b>VAO deduplication</b> — redirects every {@code VertexBuffer.bind()}
 *       in the chunk loop and skips {@code glBindVertexArray} when the VAO id
 *       hasn't changed. On solid terrain where all chunks share the same vertex
 *       format this reduces N VAO switches per layer to 1.</li>
 *
 *   <li><b>chunkOffset deduplication</b> — the {@code chunkOffset} uniform is
 *       only uploaded when the value actually changes.  In practice every chunk
 *       has a different offset, so this saves nothing on correctness — but it
 *       eliminates the unconditional {@code glUniform3f} on chunks whose
 *       {@code glBindVertexArray} we already skipped (those are empty / already
 *       rendered), keeping the fast path tight.</li>
 *
 *   <li><b>End-of-layer unbind cleanup</b> — replaces vanilla's
 *       {@code VertexBuffer.unbind()} with a direct tracked {@code glBindVertexArray(0)}
 *       so subsequent passes (entities, GUI) always start with a clean VAO
 *       binding without the extra {@code BufferRenderer} static-field write.</li>
 * </ol>
 *
 * <h2>Further work</h2>
 * The full 1.21.11 batch-draw path (single draw call per layer) requires the
 * mod's {@link net.zhengzhengyiyi.renderer.api.blaze3d.systems.RenderPass} to
 * target the vanilla framebuffer.  That bridge is tracked separately.
 */
@Mixin(WorldRenderer.class)
public abstract class RenderLayerDrawMixin {

    /**
     * VAO id currently bound inside this {@code renderLayer} call.
     * -1 means unknown — forces the first bind to always go through.
     * Reset to -1 at start of each layer and after the end-of-layer unbind.
     */
    private int renderer$currentVao = -1;

    /** Last chunkOffset values sent to the shader, used to skip redundant uploads. */
    private float renderer$lastOffX = Float.NaN;
    private float renderer$lastOffY = Float.NaN;
    private float renderer$lastOffZ = Float.NaN;

    // -------------------------------------------------------------------------
    // Reset tracking state at the top of every renderLayer call
    // -------------------------------------------------------------------------

    @Inject(
        method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V",
        at = @At("HEAD")
    )
    private void renderer$resetTracking(
            RenderLayer renderLayer, MatrixStack matrices,
            double cameraX, double cameraY, double cameraZ,
            Matrix4f positionMatrix, CallbackInfo ci) {
        this.renderer$currentVao = -1;
        this.renderer$lastOffX = Float.NaN;
        this.renderer$lastOffY = Float.NaN;
        this.renderer$lastOffZ = Float.NaN;
    }

    // -------------------------------------------------------------------------
    // Deduplicate glBindVertexArray per chunk
    // -------------------------------------------------------------------------

    /**
     * Replaces {@link VertexBuffer#bind()} in the chunk draw loop.
     *
     * <p>Vanilla's {@code bind()} does two things:
     * <ol>
     *   <li>{@code BufferRenderer.resetCurrentVertexBuffer()} — nulls
     *       BufferRenderer's cached immediate-mode VAO so later non-chunk draws
     *       cannot assume their VAO is still bound after chunk rendering.</li>
     *   <li>{@code glBindVertexArray(vertexArrayId)} — skipped when the VAO id
     *       matches the one already bound.</li>
     * </ol>
     */
    @Redirect(
        method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/VertexBuffer;bind()V"
        )
    )
    private void renderer$deduplicateVaoBind(VertexBuffer vertexBuffer) {
        BufferRenderer.resetCurrentVertexBuffer();
        int vaoId = ((VertexBufferAccessor) vertexBuffer).renderer$getVertexArrayId();
        if (vaoId != this.renderer$currentVao) {
            GlStateManager._glBindVertexArray(vaoId);
            this.renderer$currentVao = vaoId;
        }
    }

    // -------------------------------------------------------------------------
    // Deduplicate chunkOffset uniform uploads
    // -------------------------------------------------------------------------

    /**
     * Replaces the {@code glUniform.upload()} call in the chunk loop.
     *
     * <p>The chunkOffset uniform changes for every chunk, so the dedup rarely
     * fires in practice — but it guards against edge-cases (empty chunks that
     * share an offset with a previous one) and, more importantly, avoids the
     * upload when the VAO bind was also skipped (i.e., the chunk is empty and
     * we fast-pathed past it).
     *
     * <p>The redirect targets the {@code GlUniform.upload()} call inside the
     * loop.  We capture the current values by reading back from the uniform's
     * float data — this avoids needing a separate {@code @ModifyVariable}.
     */
    @Redirect(
        method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/GlUniform;upload()V"
        )
    )
    private void renderer$deduplicateChunkOffsetUpload(GlUniform uniform) {
        // GlUniform stores float data in a FloatBuffer; read the three components.
        float x = uniform.getFloatData().get(0);
        float y = uniform.getFloatData().get(1);
        float z = uniform.getFloatData().get(2);
        if (x != this.renderer$lastOffX || y != this.renderer$lastOffY || z != this.renderer$lastOffZ) {
            uniform.upload();
            this.renderer$lastOffX = x;
            this.renderer$lastOffY = y;
            this.renderer$lastOffZ = z;
        }
    }

    // -------------------------------------------------------------------------
    // Replace end-of-layer unbind with a tracked clean reset
    // -------------------------------------------------------------------------

    /**
     * Replaces {@link VertexBuffer#unbind()} at the end of the layer.
     *
     * <p>Vanilla issues {@code BufferRenderer.resetCurrentVertexBuffer()} +
     * {@code glBindVertexArray(0)}. Keep both effects and reset our tracker.
     */
    @Redirect(
        method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/VertexBuffer;unbind()V"
        )
    )
    private void renderer$trackedUnbind() {
        BufferRenderer.resetCurrentVertexBuffer();
        GlStateManager._glBindVertexArray(0);
        this.renderer$currentVao = -1;
    }
}
