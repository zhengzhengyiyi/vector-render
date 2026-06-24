package net.zhengzhengyiyi.renderer.gl;
import net.zhengzhengyiyi.client.render.RenderEngine;

import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuFence;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.GpuDevice;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class MappableRingBuffer implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   // How long (nanoseconds) to wait for a fence before logging a stall warning.
   // 50 ms = one full frame at 20 fps — anything beyond this indicates the GPU
   // is genuinely stalling the render thread.
   private static final long FENCE_WARN_TIMEOUT_NS = 50_000_000L;

   private final GpuBuffer[] buffers = new GpuBuffer[3];
   private final GpuFence[] fences = new GpuFence[3];
   private final int size;
   private int current = 0;
   private final String debugName;

   public MappableRingBuffer(Supplier<String> nameSupplier, @GpuBuffer.Usage int usage, int size) {
      GpuDevice gpuDevice = RenderEngine.getDevice();
      if ((usage & 1) == 0 && (usage & 2) == 0) {
         throw new IllegalArgumentException("MappableRingBuffer requires at least one of USAGE_MAP_READ or USAGE_MAP_WRITE");
      } else {
         this.debugName = nameSupplier.get();
         for (int i = 0; i < 3; i++) {
            int j = i;
            this.buffers[i] = gpuDevice.createBuffer(() -> nameSupplier.get() + " #" + j, usage, size);
            this.fences[i] = null;
         }

         this.size = size;
      }
   }

   public int size() {
      return this.size;
   }

   public GpuBuffer getBlocking() {
      GpuFence gpuFence = this.fences[this.current];
      if (gpuFence != null) {
         // First try a non-blocking poll so we don't stall at all in the common case.
         if (!gpuFence.awaitCompletion(0L)) {
            // GPU hasn't finished yet — wait up to FENCE_WARN_TIMEOUT_NS before logging.
            if (!gpuFence.awaitCompletion(FENCE_WARN_TIMEOUT_NS)) {
               LOGGER.warn(
                  "[MappableRingBuffer] GPU fence stall on '{}' slot {} — " +
                  "GPU is more than {}ms behind the CPU. " +
                  "Waiting indefinitely; consider reducing batch sizes or render distance.",
                  this.debugName, this.current, FENCE_WARN_TIMEOUT_NS / 1_000_000L
               );
               // Now block until the GPU truly finishes; there is no safe alternative.
               gpuFence.awaitCompletion(Long.MAX_VALUE);
            }
         }
         gpuFence.close();
         this.fences[this.current] = null;
      }

      return this.buffers[this.current];
   }

   public void rotate() {
      if (this.fences[this.current] != null) {
         this.fences[this.current].close();
      }

      this.fences[this.current] = RenderEngine.getDevice().createCommandEncoder().createFence();
      this.current = (this.current + 1) % 3;
   }

   @Override
   public void close() {
      for (int i = 0; i < 3; i++) {
         this.buffers[i].close();
         if (this.fences[i] != null) {
            this.fences[i].close();
         }
      }
   }
}
