package net.zhengzhengyiyi.client.render.weather;

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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Weather rendering system for 1.21.11-style rendering.
 * Ported from 1.21.11 WeatherRendering with 1.20.4 compatibility.
 */
@SuppressWarnings("unused")
public class WeatherRendering implements AutoCloseable {
	private static final float field_63581 = 0.225F;
	private static final int field_53148 = 10;
	private static final Identifier RAIN_TEXTURE = new Identifier("textures/environment/rain.png");
	private static final Identifier SNOW_TEXTURE = new Identifier("textures/environment/snow.png");
	private static final int field_53152 = 32;
	private static final int field_53153 = 16;
	private int soundChance;
	private final float[] NORMAL_LINE_DX = new float[1024];
	private final float[] NORMAL_LINE_DZ = new float[1024];
	
	private final GpuBuffer rainVertexBuffer;
	private final GpuBuffer snowVertexBuffer;
	private final GpuBuffer indexBuffer;

	public WeatherRendering() {
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				float f = j - 16;
				float g = i - 16;
				float h = MathHelper.sqrt(f * f + g * g);
				this.NORMAL_LINE_DX[i * 32 + j] = f / h;
				this.NORMAL_LINE_DZ[i * 32 + j] = g / h;
			}
		}
		
		// Create index buffer for quad rendering
		java.nio.ByteBuffer indexData = java.nio.ByteBuffer.allocateDirect(6 * 4); // 6 indices, 4 bytes each
		indexData.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);
		indexData.flip();
		this.indexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "Weather index buffer", net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer.USAGE_INDEX, indexData);
		
		// Create rain and snow vertex buffers
		this.rainVertexBuffer = createWeatherVertexBuffer();
		this.snowVertexBuffer = createWeatherVertexBuffer();
	}

	private GpuBuffer createWeatherVertexBuffer() {
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE_COLOR_LIGHT;
		
		GpuBuffer var6;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			// Simple quad for weather rendering
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F).color(0xFFFFFFFF).light(0);
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F).color(0xFFFFFFFF).light(0);
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F).color(0xFFFFFFFF).light(0);
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F).color(0xFFFFFFFF).light(0);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var6 = RenderEngine.tryGetDevice().createBuffer(() -> "Weather vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
		
		return var6;
	}

	public void render(float tickDelta, double cameraX, double cameraY, double cameraZ) {
		// Create a simple model view matrix for weather rendering
		Matrix4f modelView = new Matrix4f();
		modelView.translate((float)cameraX, (float)cameraY, (float)cameraZ);
		
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
			.createRenderPass(() -> "Weather", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(RenderPipelines.WEATHER_NO_DEPTH);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.rainVertexBuffer);
			renderPass.setIndexBuffer(this.indexBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, 6, 1);
		}
	}

	@Override
	public void close() {
		this.rainVertexBuffer.close();
		this.snowVertexBuffer.close();
		this.indexBuffer.close();
	}
}
