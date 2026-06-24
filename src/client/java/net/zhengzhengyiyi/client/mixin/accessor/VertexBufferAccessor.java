package net.zhengzhengyiyi.client.mixin.accessor;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the raw GL object IDs and draw parameters inside vanilla's
 * {@link VertexBuffer} so that the batch-draw mixin can wrap them as
 * {@code GlGpuBuffer} instances for the mod's pipeline/command-encoder.
 */
@Mixin(VertexBuffer.class)
public interface VertexBufferAccessor {
    @Accessor("vertexArrayId")
    int renderer$getVertexArrayId();

    @Accessor("vertexBufferId")
    int renderer$getVertexBufferId();

    @Accessor("indexBufferId")
    int renderer$getIndexBufferId();

    @Accessor("indexCount")
    int renderer$getIndexCount();

    @Accessor("indexType")
    VertexFormat.IndexType renderer$getIndexType();

    @Accessor("drawMode")
    VertexFormat.DrawMode renderer$getDrawMode();
}
