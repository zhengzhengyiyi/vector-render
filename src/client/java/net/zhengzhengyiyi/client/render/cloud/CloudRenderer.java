package net.zhengzhengyiyi.client.render.cloud;

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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Cloud rendering system for 1.21.11-style rendering.
 * Ported from 1.21.11 CloudRenderer with 1.20.4 compatibility.
 */
@SuppressWarnings("unused")
public class CloudRenderer implements AutoCloseable {
	private static final int field_60075 = 16;
	private static final int field_60076 = 32;
	private static final float field_53043 = 12.0F;
	private static final int field_64448 = 400;
	private static final float field_53045 = 0.6F;
	private static final Identifier CLOUD_TEXTURE = new Identifier("textures/environment/clouds.png");
	
	private final GpuBuffer cloudVertexBuffer;
	private final GpuBuffer indexBuffer;

	public CloudRenderer() {
		// Create index buffer for quad rendering
		java.nio.ByteBuffer indexData = java.nio.ByteBuffer.allocateDirect(6 * 4); // 6 indices, 4 bytes each
		indexData.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);
		indexData.flip();
		this.indexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "Cloud index buffer", net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer.USAGE_INDEX, indexData);
		
		// Create cloud vertex buffer
		this.cloudVertexBuffer = createCloudVertexBuffer();
	}

	private GpuBuffer createCloudVertexBuffer() {
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE;
		
		GpuBuffer var6;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			// Simple quad for cloud rendering
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var6 = RenderEngine.tryGetDevice().createBuffer(() -> "Cloud vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
		
		return var6;
	}

	public void render(MatrixStack matrices, float tickDelta) {
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
			.createRenderPass(() -> "Clouds", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_END_SKY);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.cloudVertexBuffer);
			renderPass.setIndexBuffer(this.indexBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, 6, 1);
		}
	}

	@Override
	public void close() {
		this.cloudVertexBuffer.close();
		this.indexBuffer.close();
	}
}
