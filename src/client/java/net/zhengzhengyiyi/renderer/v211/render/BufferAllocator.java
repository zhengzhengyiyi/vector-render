package net.zhengzhengyiyi.renderer.v211.render;

import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;

@Environment(EnvType.CLIENT)
@SuppressWarnings("unused")
public class BufferAllocator implements AutoCloseable {
	private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
	private static final long MAX_SIZE = 4294967295L;
	private static final int MIN_GROWTH = 2097152;
	private static final int CLOSED = -1;
	long pointer;
	private long size;
	private final long maxSize;
	private long offset;
	private long lastOffset;
	private int refCount;
	private int clearCount;

	public BufferAllocator(int size, long maxSize) {
		this.size = size;
		this.maxSize = maxSize;
		this.pointer = ALLOCATOR.malloc(size);
		if (this.pointer == 0L) {
			throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
		}
	}

	public BufferAllocator(int size) {
		this(size, 4294967295L);
	}

	public static BufferAllocator fixedSized(int size) {
		return new BufferAllocator(size, size);
	}

	public long allocate(int size) {
		long l = this.offset;
		long m = Math.addExact(l, (long) size);
		this.growIfNecessary(m);
		this.offset = m;
		return Math.addExact(this.pointer, l);
	}

	private void growIfNecessary(long newSize) {
		if (newSize > this.size) {
			if (newSize > this.maxSize) {
				throw new IllegalArgumentException("Maximum capacity of BufferAllocator (" + this.maxSize + ") exceeded, required " + newSize);
			}

			long l = Math.min(this.size, 2097152L);
			long m = MathHelper.clamp(this.size + l, newSize, this.maxSize);
			this.grow(m);
		}
	}

	private void grow(long newSize) {
		this.pointer = ALLOCATOR.realloc(this.pointer, newSize);
		if (this.pointer == 0L) {
			throw new OutOfMemoryError("Failed to resize buffer from " + this.size + " bytes to " + newSize + " bytes");
		} else {
			this.size = newSize;
		}
	}

	public CloseableBuffer getAllocated() {
		this.ensureNotFreed();
		long l = this.lastOffset;
		long m = this.offset - l;
		if (m == 0L) {
			return null;
		} else if (m > 2147483647L) {
			throw new IllegalStateException("Cannot build buffer larger than 2147483647 bytes (was " + m + ")");
		} else {
			this.lastOffset = this.offset;
			this.refCount++;
			return new CloseableBuffer(l, (int) m, this.clearCount);
		}
	}

	public void clear() {
		if (this.refCount > 0) {
			System.err.println("Clearing BufferAllocator with unused batches");
		}

		this.reset();
	}

	public void reset() {
		this.ensureNotFreed();
		if (this.refCount > 0) {
			this.forceClear();
			this.refCount = 0;
		}
	}

	boolean clearCountEquals(int clearCount) {
		return clearCount == this.clearCount;
	}

	void clearIfUnreferenced() {
		if (--this.refCount <= 0) {
			this.forceClear();
		}
	}

	private void forceClear() {
		long l = this.offset - this.lastOffset;
		if (l > 0L) {
			MemoryUtil.memCopy(this.pointer + this.lastOffset, this.pointer, l);
		}

		this.offset = l;
		this.lastOffset = 0L;
		this.clearCount++;
	}

	@Override
	public void close() {
		if (this.pointer != 0L) {
			ALLOCATOR.free(this.pointer);
			this.pointer = 0L;
			this.clearCount = -1;
		}
	}

	private void ensureNotFreed() {
		if (this.pointer == 0L) {
			throw new IllegalStateException("Buffer has been freed");
		}
	}

	@Environment(EnvType.CLIENT)
	public class CloseableBuffer implements AutoCloseable {
		private final long offset;
		private final int size;
		private final int clearCount;

		CloseableBuffer(long offset, int size, int clearCount) {
			this.offset = offset;
			this.size = size;
			this.clearCount = clearCount;
		}

		public ByteBuffer getBuffer() {
			return MemoryUtil.memByteBuffer(BufferAllocator.this.pointer + this.offset, this.size);
		}

		public int getSize() {
			return this.size;
		}

		@Override
		public void close() {
			BufferAllocator.this.clearIfUnreferenced();
		}
	}
}
