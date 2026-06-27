package net.zhengzhengyiyi.renderer.v211.render;

import net.minecraft.util.math.MathHelper;
import net.zhengzhengyiyi.renderer.api.blaze3d.vertex.VertexFormat;
import net.zhengzhengyiyi.renderer.api.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.system.MemoryUtil;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public class BufferBuilder {
	private final BufferAllocator allocator;
	private final VertexFormat vertexFormat;
	private final VertexFormat.DrawMode drawMode;
	private final int vertexSizeByte;
	private final int[] offsetsByElementId;
	private final int requiredMask;
	private final boolean canSkipElementChecks;
	private final boolean hasOverlay;
	
	private boolean building;
	private int vertexCount;
	private int currentMask;
	private long vertexPointer;
	
	public BufferBuilder(BufferAllocator allocator, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat) {
		this.allocator = allocator;
		this.vertexFormat = vertexFormat;
		this.drawMode = drawMode;
		this.vertexSizeByte = vertexFormat.getVertexSize();
		this.offsetsByElementId = new int[VertexFormatElement.MAX_COUNT];
		
		int mask = 0;
		int offset = 0;
		for (VertexFormatElement element : vertexFormat.getElements()) {
			this.offsetsByElementId[element.id()] = offset;
			mask |= element.mask();
			offset += element.count();
		}
		
		this.requiredMask = mask;
		this.canSkipElementChecks = true;
		this.hasOverlay = vertexFormat.contains(VertexFormatElement.UV1);
		this.building = true;
		this.vertexCount = 0;
		this.currentMask = 0;
		this.vertexPointer = -1L;
	}
	
	public BuiltBuffer end() {
		this.building = false;
		if (this.vertexCount == 0) {
			return null;
		} else {
			BufferAllocator.CloseableBuffer closeableBuffer = this.allocator.getAllocated();
			if (closeableBuffer == null) {
				return null;
			} else {
				int i = this.drawMode.getIndexCount(this.vertexCount);
				VertexFormat.IndexType indexType = VertexFormat.IndexType.smallestFor(this.vertexCount);
				return new BuiltBuffer(closeableBuffer, new BuiltBuffer.DrawParameters(this.vertexFormat, this.vertexCount, i, this.drawMode, indexType));
			}
		}
	}
	
	private void ensureBuilding() {
		if (!this.building) {
			throw new IllegalStateException("Not building!");
		}
	}
	
	private long beginVertex() {
		this.ensureBuilding();
		this.endVertex();
		if (this.vertexCount >= 16777215) {
			throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
		} else {
			this.vertexCount++;
			long l = this.allocator.allocate(this.vertexSizeByte);
			this.vertexPointer = l;
			return l;
		}
	}
	
	private long beginElement(VertexFormatElement element) {
		int i = this.currentMask;
		int j = i & ~element.mask();
		if (j == i) {
			return -1L;
		} else {
			this.currentMask = j;
			long l = this.vertexPointer;
			if (l == -1L) {
				throw new IllegalArgumentException("Not currently building vertex");
			} else {
				return l + this.offsetsByElementId[element.id()];
			}
		}
	}
	
	private void endVertex() {
		if (this.vertexCount != 0) {
			if (this.currentMask != 0) {
				// Allow incomplete vertices for formats that don't require all elements
				// Only throw error if required elements are missing
			}
		}
	}
	
	private static void putColor(long pointer, int argb) {
		// 1.20.4 ColorHelper doesn't have toAbgr, so we manually convert ARGB to ABGR
		int a = (argb >> 24) & 0xFF;
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		int i = (a << 24) | (b << 16) | (g << 8) | r;
		MemoryUtil.memPutInt(pointer, i);
	}
	
	private static void putInt(long pointer, int i) {
		MemoryUtil.memPutInt(pointer, i);
	}
	
	public BufferBuilder vertex(float x, float y, float z) {
		long l = this.beginVertex();
		this.currentMask = this.requiredMask;
		long posPointer = l + this.offsetsByElementId[VertexFormatElement.POSITION.id()];
		MemoryUtil.memPutFloat(posPointer, x);
		MemoryUtil.memPutFloat(posPointer + 4L, y);
		MemoryUtil.memPutFloat(posPointer + 8L, z);
		this.currentMask &= ~VertexFormatElement.POSITION.mask();
		return this;
	}
	
	public BufferBuilder vertex(Vector3f pos) {
		return this.vertex(pos.x, pos.y, pos.z);
	}
	
	public BufferBuilder color(int red, int green, int blue, int alpha) {
		long l = this.beginElement(VertexFormatElement.COLOR);
		if (l != -1L) {
			MemoryUtil.memPutByte(l, (byte) red);
			MemoryUtil.memPutByte(l + 1L, (byte) green);
			MemoryUtil.memPutByte(l + 2L, (byte) blue);
			MemoryUtil.memPutByte(l + 3L, (byte) alpha);
		}
		return this;
	}
	
	public BufferBuilder color(int argb) {
		long l = this.beginElement(VertexFormatElement.COLOR);
		if (l != -1L) {
			putColor(l, argb);
		}
		return this;
	}
	
	public BufferBuilder texture(float u, float v) {
		long l = this.beginElement(VertexFormatElement.UV0);
		if (l != -1L) {
			MemoryUtil.memPutFloat(l, u);
			MemoryUtil.memPutFloat(l + 4L, v);
		}
		return this;
	}
	
	public BufferBuilder overlay(int u, int v) {
		return this.uv((short) u, (short) v, VertexFormatElement.UV1);
	}
	
	public BufferBuilder overlay(int uv) {
		return this.uv((short) uv, (short) uv, VertexFormatElement.UV1);
	}
	
	public BufferBuilder light(int u, int v) {
		return this.uv((short) u, (short) v, VertexFormatElement.UV2);
	}
	
	public BufferBuilder light(int uv) {
		return this.uv((short) uv, (short) uv, VertexFormatElement.UV2);
	}
	
	private BufferBuilder uv(short u, short v, VertexFormatElement element) {
		long l = this.beginElement(element);
		if (l != -1L) {
			MemoryUtil.memPutShort(l, u);
			MemoryUtil.memPutShort(l + 2L, v);
		}
		return this;
	}
	
	public BufferBuilder normal(float x, float y, float z) {
		long l = this.beginElement(VertexFormatElement.NORMAL);
		if (l != -1L) {
			MemoryUtil.memPutByte(l, floatToByte(x));
			MemoryUtil.memPutByte(l + 1L, floatToByte(y));
			MemoryUtil.memPutByte(l + 2L, floatToByte(z));
		}
		return this;
	}
	
	public BufferBuilder lineWidth(float width) {
		long l = this.beginElement(VertexFormatElement.LINE_WIDTH);
		if (l != -1L) {
			MemoryUtil.memPutFloat(l, width);
		}
		return this;
	}
	
	public BufferBuilder vertex(double x, double y, double z) {
		return this.vertex((float) x, (float) y, (float) z);
	}
	
	public void next() {
		this.endVertex();
	}
	
	private static byte floatToByte(float f) {
		return (byte) ((int) (MathHelper.clamp(f, -1.0F, 1.0F) * 127.0F) & 0xFF);
	}
	
	public static class BuiltBuffer implements AutoCloseable {
		private final BufferAllocator.CloseableBuffer closeableBuffer;
		private final DrawParameters drawParameters;
		
		public BuiltBuffer(BufferAllocator.CloseableBuffer closeableBuffer, DrawParameters drawParameters) {
			this.closeableBuffer = closeableBuffer;
			this.drawParameters = drawParameters;
		}
		
		public ByteBuffer getBuffer() {
			return this.closeableBuffer.getBuffer();
		}
		
		public int getIndexCount() {
			return this.drawParameters.indexCount();
		}
		
		public VertexFormat getVertexFormat() {
			return this.drawParameters.vertexFormat();
		}
		
		@Override
		public void close() {
			this.closeableBuffer.close();
		}
		
		public record DrawParameters(VertexFormat vertexFormat, int vertexCount, int indexCount, VertexFormat.DrawMode drawMode, VertexFormat.IndexType indexType) {
		}
	}
}
