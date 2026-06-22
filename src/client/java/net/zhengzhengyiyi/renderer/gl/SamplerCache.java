package net.zhengzhengyiyi.renderer.gl;
import net.zhengzhengyiyi.client.render.RenderEngine;

import com.google.common.annotations.VisibleForTesting;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.GpuDevice;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.AddressMode;
import net.zhengzhengyiyi.renderer.api.blaze3d.textures.FilterMode;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SamplerCache {
   private final GpuSampler[] samplers = new GpuSampler[32];

   public SamplerCache() {
   }

   public void init() {
      GpuDevice gpuDevice = RenderEngine.getDevice();
      if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
         for (AddressMode addressMode : AddressMode.values()) {
            for (AddressMode addressMode2 : AddressMode.values()) {
               for (FilterMode filterMode : FilterMode.values()) {
                  for (FilterMode filterMode2 : FilterMode.values()) {
                     for (boolean bl : new boolean[]{true, false}) {
                        this.samplers[toIndex(addressMode, addressMode2, filterMode, filterMode2, bl)] = gpuDevice.createSampler(
                           addressMode, addressMode2, filterMode, filterMode2, 1, bl ? OptionalDouble.empty() : OptionalDouble.of(0.0)
                        );
                     }
                  }
               }
            }
         }
      } else {
         throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
      }
   }

   public GpuSampler get(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilterMode, FilterMode magFilterMode, boolean defaultLineOfDetail) {
      return this.samplers[toIndex(addressModeU, addressModeV, minFilterMode, magFilterMode, defaultLineOfDetail)];
   }

   public GpuSampler get(FilterMode filterMode) {
      return this.get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filterMode, filterMode, false);
   }

   public GpuSampler get(FilterMode filterMode, boolean defaultLineOfDetail) {
      return this.get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filterMode, filterMode, defaultLineOfDetail);
   }

   public GpuSampler getRepeated(FilterMode filterMode) {
      return this.get(AddressMode.REPEAT, AddressMode.REPEAT, filterMode, filterMode, false);
   }

   public GpuSampler getRepeated(FilterMode filterMode, boolean defaultLineOfDetail) {
      return this.get(AddressMode.REPEAT, AddressMode.REPEAT, filterMode, filterMode, defaultLineOfDetail);
   }

   public void close() {
      for (GpuSampler gpuSampler : this.samplers) {
         gpuSampler.close();
      }
   }

   @VisibleForTesting
   static int toIndex(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilterMode, FilterMode magFilterMode, boolean bl) {
      int i = 0;
      i |= addressModeU.ordinal() & 1;
      i |= (addressModeV.ordinal() & 1) << 1;
      i |= (minFilterMode.ordinal() & 1) << 2;
      i |= (magFilterMode.ordinal() & 1) << 3;
      if (bl) {
         i |= 16;
      }

      return i;
   }
}
