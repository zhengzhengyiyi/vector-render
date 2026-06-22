package net.zhengzhengyiyi.renderer.api.blaze3d.systems;

import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.CompiledRenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.RenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.AddressMode;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.FilterMode;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.GpuTexture;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.GpuTextureView;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.zhengzhengyiyi.renderer.gl.GpuSampler;
import net.zhengzhengyiyi.renderer.gl.ShaderSourceGetter;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuDevice {
   CommandEncoder createCommandEncoder();

   GpuSampler createSampler(
      AddressMode addressModeU,
      AddressMode addressModeV,
      FilterMode minFilterMode,
      FilterMode magFilterMode,
      int maxAnisotropy,
      OptionalDouble maxLevelOfDetail
   );

   GpuTexture createTexture(
      @Nullable Supplier<String> labelGetter, @GpuTexture.Usage int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels
   );

   GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels);

   GpuTextureView createTextureView(GpuTexture texture);

   GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels);

   GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, long size);

   GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, ByteBuffer data);

   String getImplementationInformation();

   List<String> getLastDebugMessages();

   boolean isDebuggingEnabled();

   String getVendor();

   String getBackendName();

   String getVersion();

   String getRenderer();

   int getMaxTextureSize();

   int getUniformOffsetAlignment();

   default CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
      return this.precompilePipeline(pipeline, null);
   }

   CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSourceGetter sourceGetter);

   void clearPipelineCache();

   List<String> getEnabledExtensions();

   int getMaxSupportedAnisotropy();

   void close();
}
