package net.zhengzhengyiyi.renderer.gl;
import net.zhengzhengyiyi.client.render.RenderEngine;

import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBufferSlice;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.GpuDevice;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.Uploadable> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
   private final int blockSize;
   private MappableRingBuffer buffer;
   private int size;
   private int capacity;
   @Nullable
   private T lastWrittenValue;
   private final String name;

   public DynamicUniformStorage(String name, int blockSize, int capacity) {
      GpuDevice gpuDevice = RenderEngine.getDevice();
      this.blockSize = MathHelper.roundUpToMultiple(blockSize, gpuDevice.getUniformOffsetAlignment());
      this.capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
      this.size = 0;
      this.buffer = new MappableRingBuffer(() -> name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
      this.name = name;
   }

   public void clear() {
      this.size = 0;
      this.lastWrittenValue = null;
      this.buffer.rotate();
      if (!this.oldBuffers.isEmpty()) {
         for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
            mappableRingBuffer.close();
         }

         this.oldBuffers.clear();
      }
   }

   private void growBuffer(int capacity) {
      this.capacity = capacity;
      this.size = 0;
      this.lastWrittenValue = null;
      this.oldBuffers.add(this.buffer);
      this.buffer = new MappableRingBuffer(() -> this.name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
      // Place a fence on the freshly-created ring's current slot before any
      // writes happen. This prevents a write-after-read race if this method is
      // called mid-frame: the GPU may still be processing commands that
      // reference the old buffer, and until that fence signals we must not
      // hand out slot 0 of the new buffer to another consumer that the GPU
      // might also be reading at the same time.
      //
      // In practice the new ring buffer's GPU memory is unused so the fence
      // signals immediately on the next getBlocking() poll; the cost is
      // negligible but the correctness guarantee is real.
      this.buffer.rotate(); // advances to slot 1, leaves a signalled fence on slot 0
   }

   public GpuBufferSlice write(T value) {
      if (this.lastWrittenValue != null && this.lastWrittenValue.equals(value)) {
         return this.buffer.getBlocking().slice((this.size - 1) * this.blockSize, this.blockSize);
      } else {
         if (this.size >= this.capacity) {
            int i = this.capacity * 2;
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.name, this.capacity, i});
            this.growBuffer(i);
         }

         int i = this.size * this.blockSize;

         try (GpuBuffer.MappedView mappedView = RenderEngine.getDevice()
               .createCommandEncoder()
               .mapBuffer(this.buffer.getBlocking().slice(i, this.blockSize), false, true)) {
            value.write(mappedView.data());
         }

         this.size++;
         this.lastWrittenValue = value;
         return this.buffer.getBlocking().slice(i, this.blockSize);
      }
   }

   public GpuBufferSlice[] writeAll(T[] values) {
      if (values.length == 0) {
         return new GpuBufferSlice[0];
      } else {
         if (this.size + values.length > this.capacity) {
            int i = MathHelper.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, values.length));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.name, this.capacity, i});
            this.growBuffer(i);
         }

         int i = this.size * this.blockSize;
         GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[values.length];

         try (GpuBuffer.MappedView mappedView = RenderEngine.getDevice()
               .createCommandEncoder()
               .mapBuffer(this.buffer.getBlocking().slice(i, values.length * this.blockSize), false, true)) {
            ByteBuffer byteBuffer = mappedView.data();

            for (int j = 0; j < values.length; j++) {
               T uploadable = values[j];
               gpuBufferSlices[j] = this.buffer.getBlocking().slice(i + j * this.blockSize, this.blockSize);
               byteBuffer.position(j * this.blockSize);
               uploadable.write(byteBuffer);
            }
         }

         this.size += values.length;
         this.lastWrittenValue = values[values.length - 1];
         return gpuBufferSlices;
      }
   }

   @Override
   public void close() {
      for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
         mappableRingBuffer.close();
      }

      this.buffer.close();
   }

   @Environment(EnvType.CLIENT)
   public interface Uploadable {
      void write(ByteBuffer buffer);
   }
}
