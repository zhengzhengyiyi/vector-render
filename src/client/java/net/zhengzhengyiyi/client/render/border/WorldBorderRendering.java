package net.zhengzhengyiyi.client.render.border;

import net.zhengzhengyiyi.client.render.RenderEngine;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBufferSlice;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.RenderPass;
import net.zhengzhengyiyi.renderer.api.blaze3d.vertex.VertexFormat;
import net.zhengzhengyiyi.renderer.gl.RenderPipelines;
import net.zhengzhengyiyi.renderer.v211.render.BufferBuilder;
import net.zhengzhengyiyi.renderer.v211.render.BufferAllocator;
import net.zhengzhengyiyi.renderer.v211.render.VertexFormats211;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * World border rendering system for 1.21.11-style rendering.
 * Ported from 1.21.11 WorldBorderRendering with 1.20.4 compatibility.
 */
@SuppressWarnings("unused")
public class WorldBorderRendering implements AutoCloseable {
	public static final Identifier FORCEFIELD = new Identifier("textures/misc/forcefield.png");
	private boolean forceRefreshBuffers = true;
	private double lastUploadedBoundWest;
	private double lastUploadedBoundNorth;
	private double lastXMin;
	private double lastXMax;
	private double lastZMin;
	private double lastZMax;
	
	private final GpuBuffer borderVertexBuffer;
	private final GpuBuffer indexBuffer;

	public WorldBorderRendering() {
		// Create index buffer for quad rendering
		java.nio.ByteBuffer indexData = java.nio.ByteBuffer.allocateDirect(6 * 4); // 6 indices, 4 bytes each
		indexData.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);
		indexData.flip();
		this.indexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "World border index buffer", net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer.USAGE_INDEX, indexData);
		
		// Create world border vertex buffer
		this.borderVertexBuffer = createBorderVertexBuffer();
	}

	private GpuBuffer createBorderVertexBuffer() {
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE;
		
		GpuBuffer var6;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			// Simple quad for world border rendering
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var6 = RenderEngine.tryGetDevice().createBuffer(() -> "World border vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
		
		return var6;
	}

	public void render(Camera camera) {
		// Create a simple model view matrix for world border rendering
		MatrixStack matrices = new MatrixStack();
		matrices.translate(camera.getPos().x, camera.getPos().y, camera.getPos().z);
		Matrix4f modelView = matrices.peek().getPositionMatrix();
		
		int colorTexId = MinecraftClient.getInstance().getFramebuffer().getColorAttachment();
		int depthTexId = MinecraftClient.getInstance().getFramebuffer().getDepthAttachment();
		
		// Wrap vanilla framebuffer textures into GpuTextureView
		net.zhengzhengyiyi.renderer.texture.GlTexture colorTexture = new net.zhengzhengyiyi.renderer.texture.GlTexture(
			net.zhengzhengyiyi.renderer.api.blaze3d.textures.GpuTexture.USAGE_RENDER_ATTACHMENT,
			"Framebuffer color",
			net.zhengzhengyiyi.renderer.api.blaze3d.textures.TextureFormat.RGBA8,
			MinecraftClient.getInstance().getWindow().getFramebufferWidth(),
			MinecraftClient.getInstance().getWindow().getFramebufferHeight(),
			1,
			1,
			colorTexId
		);
		net.zhengzhengyiyi.renderer.texture.GlTextureView colorTextureView = new net.zhengzhengyiyi.renderer.texture.GlTextureView(colorTexture, 0, 1);
		
		net.zhengzhengyiyi.renderer.texture.GlTexture depthTexture = new net.zhengzhengyiyi.renderer.texture.GlTexture(
			net.zhengzhengyiyi.renderer.api.blaze3d.textures.GpuTexture.USAGE_RENDER_ATTACHMENT,
			"Framebuffer depth",
			net.zhengzhengyiyi.renderer.api.blaze3d.textures.TextureFormat.DEPTH32,
			MinecraftClient.getInstance().getWindow().getFramebufferWidth(),
			MinecraftClient.getInstance().getWindow().getFramebufferHeight(),
			1,
			1,
			depthTexId
		);
		net.zhengzhengyiyi.renderer.texture.GlTextureView depthTextureView = new net.zhengzhengyiyi.renderer.texture.GlTextureView(depthTexture, 0, 1);
		
		GpuBufferSlice gpuBufferSlice = RenderEngine.getDynamicUniforms()
			.write(modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
		
		try (RenderPass renderPass = RenderEngine.tryGetDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "World border", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(RenderPipelines.RENDERTYPE_WORLD_BORDER);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.borderVertexBuffer);
			renderPass.setIndexBuffer(this.indexBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, 6, 1);
		}
	}

	public void close() {
		this.borderVertexBuffer.close();
		this.indexBuffer.close();
	}
}
