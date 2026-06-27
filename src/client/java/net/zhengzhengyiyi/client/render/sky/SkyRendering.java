package net.zhengzhengyiyi.client.render.sky;

import net.zhengzhengyiyi.client.render.RenderEngine;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBufferSlice;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.RenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.RenderPass;
import net.zhengzhengyiyi.renderer.api.blaze3d.vertex.VertexFormat;
import net.zhengzhengyiyi.renderer.gl.RenderPipelines;
import net.zhengzhengyiyi.renderer.v211.render.BufferBuilder;
import net.zhengzhengyiyi.renderer.v211.render.BufferAllocator;
import net.zhengzhengyiyi.renderer.v211.render.VertexFormats211;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Sky rendering system for 1.21.11-style rendering.
 * Ported from 1.21.11 SkyRendering.
 */
@SuppressWarnings("unused")
public class SkyRendering implements AutoCloseable {
	private static final float field_53144 = 512.0F;
	private static final int field_57932 = 10;
	private static final int field_57933 = 1500;
	private static final float field_62950 = 30.0F;
	private static final float field_62951 = 100.0F;
	private static final float field_62952 = 20.0F;
	private static final float field_62953 = 100.0F;
	private static final int field_62954 = 16;
	private static final int field_57934 = 6;
	private static final float field_62955 = 100.0F;
	private static final float field_62956 = 60.0F;
	
	private final GpuBuffer starVertexBuffer;
	private final GpuBuffer topSkyVertexBuffer;
	private final GpuBuffer bottomSkyVertexBuffer;
	private final GpuBuffer endSkyVertexBuffer;
	private final GpuBuffer sunVertexBuffer;
	private final GpuBuffer moonPhaseVertexBuffer;
	private final GpuBuffer sunRiseVertexBuffer;
	private final GpuBuffer endFlashVertexBuffer;
	private final GpuBuffer indexBuffer;
	private int starIndexCount;

	public SkyRendering(TextureManager textureManager) {
		// Create index buffer for quad rendering
		java.nio.ByteBuffer indexData = java.nio.ByteBuffer.allocateDirect(6 * 4); // 6 indices, 4 bytes each
		indexData.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);
		indexData.flip();
		this.indexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "Quad index buffer", net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer.USAGE_INDEX, indexData);
		
		this.starVertexBuffer = this.createStars();
		this.endSkyVertexBuffer = createEndSky();
		this.endFlashVertexBuffer = createEndFlash();
		this.sunVertexBuffer = createSun();
		this.moonPhaseVertexBuffer = createMoonPhases();
		this.sunRiseVertexBuffer = this.createSunRise();
		
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(20 * VertexFormats211.POSITION.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats211.POSITION);
			this.createSky(bufferBuilder, 16.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.topSkyVertexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "Top sky vertex buffer", 32, builtBuffer.getBuffer());
			}
			
			bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats211.POSITION);
			this.createSky(bufferBuilder, -16.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.bottomSkyVertexBuffer = RenderEngine.tryGetDevice().createBuffer(() -> "Bottom sky vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
	}

	private GpuBuffer createSunRise() {
		int j = VertexFormats211.POSITION_COLOR.getVertexSize();
		
		GpuBuffer var16;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(18 * j)) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats211.POSITION_COLOR);
			int k = 0xFFFFFFFF; // Full white (ABGR: 255 alpha, 255 blue, 255 green, 255 red)
			int l = 0x00000000; // Full transparent
			bufferBuilder.vertex(0.0F, 100.0F, 0.0F).color(k);
			
			for (int m = 0; m <= 16; m++) {
				float f = m * (float) (Math.PI * 2) / 16.0F;
				float g = MathHelper.sin(f);
				float h = MathHelper.cos(f);
				bufferBuilder.vertex(g * 120.0F, h * 120.0F, -h * 40.0F).color(l);
			}
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var16 = RenderEngine.tryGetDevice().createBuffer(() -> "Sunrise/Sunset fan", 32, builtBuffer.getBuffer());
			}
		}
		
		return var16;
	}

	private GpuBuffer createSun() {
		// In 1.20.4, use direct texture identifier instead of sprite atlas
		Identifier sunTexture = new Identifier("textures/environment/sun.png");
		return createQuadVertexBuffer("Sun quad", sunTexture);
	}

	private GpuBuffer createEndFlash() {
		// In 1.20.4, use direct texture identifier instead of sprite atlas
		Identifier endFlashTexture = new Identifier("textures/environment/end_sky.png");
		return createQuadVertexBuffer("End flash quad", endFlashTexture);
	}

	private GpuBuffer createQuadVertexBuffer(String description, Identifier textureId) {
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE;
		
		GpuBuffer var6;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			// Use full texture coordinates (0.0 to 1.0) for direct texture binding
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var6 = RenderEngine.tryGetDevice().createBuffer(() -> description, 32, builtBuffer.getBuffer());
			}
		}
		
		return var6;
	}

	private GpuBuffer createMoonPhases() {
		// In 1.20.4, create 8 moon phase quads (standard moon phases)
		int moonPhaseCount = 8;
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE;
		
		GpuBuffer var15;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(moonPhaseCount * 4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			
			for (int phaseIndex = 0; phaseIndex < moonPhaseCount; phaseIndex++) {
				// Use full texture coordinates (0.0 to 1.0) for direct texture binding
				bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F);
				bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F);
				bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F);
				bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F);
			}
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var15 = RenderEngine.tryGetDevice().createBuffer(() -> "Moon phases", 32, builtBuffer.getBuffer());
			}
		}
		
		return var15;
	}

	private GpuBuffer createStars() {
		Random random = Random.create(10842L);
		float f = 100.0F;
		
		GpuBuffer var19;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(VertexFormats211.POSITION.getVertexSize() * 1500 * 4)) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats211.POSITION);
			
			for (int i = 0; i < 1500; i++) {
				float g = random.nextFloat() * 2.0F - 1.0F;
				float h = random.nextFloat() * 2.0F - 1.0F;
				float j = random.nextFloat() * 2.0F - 1.0F;
				float k = 0.15F + random.nextFloat() * 0.1F;
				float l = (float)MathHelper.magnitude((double)g, (double)h, (double)j);
				
				if (!(l <= 0.010000001F) && !(l >= 1.0F)) {
					Vector3f vector3f = new Vector3f(g, h, j).normalize(100.0F);
					float m = (float) (random.nextDouble() * (float) Math.PI * 2.0);
					Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-m);
					bufferBuilder.vertex(new Vector3f(k, -k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(k, k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(-k, k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(-k, -k, 0.0F).mul(matrix3f).add(vector3f));
				}
			}
			
			BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end();
			this.starIndexCount = builtBuffer.getIndexCount();
			var19 = RenderEngine.tryGetDevice().createBuffer(() -> "Stars", 32, builtBuffer.getBuffer());
		}
		
		return var19;
	}

	private void createSky(BufferBuilder bufferBuilder, float f) {
		for (int i = 0; i <= 16; i++) {
			float g = i * (float) Math.PI * 2.0F / 16.0F;
			float h = MathHelper.sin(g);
			float j = MathHelper.cos(g);
			bufferBuilder.vertex(h * f, 0.0F, j * f);
		}
	}

	private static GpuBuffer createEndSky() {
		VertexFormat vertexFormat = VertexFormats211.POSITION_TEXTURE;
		
		GpuBuffer var2;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(0.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(1.0F, 0.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(0.0F, 1.0F);
			
			try (BufferBuilder.BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var2 = RenderEngine.tryGetDevice().createBuffer(() -> "End sky", 32, builtBuffer.getBuffer());
			}
		}
		
		return var2;
	}

	public void render(MatrixStack matrices, float tickDelta, float solarAngle, boolean thickFog, Runnable fogCallback) {
		net.minecraft.client.world.ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			return;
		}
		
		// Basic sky rendering - can be expanded with proper 1.20.4 world state checks
		// For now, render stars and sky effects based on dimension
		net.minecraft.world.dimension.DimensionType dimensionType = world.getDimension();
		
		// Render stars for overworld dimensions
		if (dimensionType.hasSkyLight()) {
			float brightness = 1.0F; // Simplified brightness calculation
			this.renderStars(matrices, brightness);
		}
		
		// Render end sky for end dimension (check by registry key)
		if (world.getRegistryKey().equals(net.minecraft.world.World.END)) {
			this.renderEndSky();
		}
	}

	public void renderStars(MatrixStack matrices, float brightness) {
		Matrix4f modelView = matrices.peek().getPositionMatrix();
		RenderPipeline renderPipeline = RenderPipelines.POSITION_STARS;
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
		
		GpuBuffer gpuBuffer = this.indexBuffer;
		GpuBufferSlice gpuBufferSlice = RenderEngine.getDynamicUniforms()
			.write(modelView, new Vector4f(brightness, brightness, brightness, brightness), new Vector3f(), new Matrix4f());
		
		try (RenderPass renderPass = RenderEngine.tryGetDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "Stars", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(renderPipeline);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.starVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, this.starIndexCount, 1);
		}
	}

	public void renderGlowingSky(MatrixStack matrices, float solarAngle, int color) {
		float f = ((color >> 24) & 0xFF) / 255.0F;
		if (!(f <= 0.001F)) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
			float g = MathHelper.sin(solarAngle) < 0.0F ? 180.0F : 0.0F;
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g + 90.0F));
			Matrix4f modelView = matrices.peek().getPositionMatrix();
			modelView.scale(1.0F, 1.0F, f);
			Vector4f colorVec = new Vector4f(
				((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F,
				(color & 0xFF) / 255.0F,
				f
			);
			GpuBufferSlice gpuBufferSlice = RenderEngine.getDynamicUniforms()
				.write(modelView, colorVec, new Vector3f(), new Matrix4f());
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
			
			try (RenderPass renderPass = RenderEngine.tryGetDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "Sunrise sunset", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
				renderPass.setPipeline(RenderPipelines.POSITION_COLOR_SUNRISE_SUNSET);
				RenderEngine.bindDefaultUniforms(renderPass);
				renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
				renderPass.setVertexBuffer(0, this.sunRiseVertexBuffer);
				renderPass.draw(0, 18);
			}
			
			matrices.pop();
		}
	}

	public void renderEndSky() {
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
			.write(new Matrix4f(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
		
		try (RenderPass renderPass = RenderEngine.tryGetDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "End sky", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_END_SKY);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.endSkyVertexBuffer);
			renderPass.setIndexBuffer(this.indexBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, 36, 1);
		}
	}

	public void drawEndLightFlash(MatrixStack matrices, float intensity, float pitch, float yaw) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - pitch));
		Matrix4f modelView = matrices.peek().getPositionMatrix();
		modelView.translate(0.0F, 100.0F, 0.0F);
		modelView.scale(60.0F, 1.0F, 60.0F);
		GpuBufferSlice gpuBufferSlice = RenderEngine.getDynamicUniforms()
			.write(modelView, new Vector4f(intensity, intensity, intensity, intensity), new Vector3f(), new Matrix4f());
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
		
		try (RenderPass renderPass = RenderEngine.tryGetDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "End flash", colorTextureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_CELESTIAL);
			RenderEngine.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.endFlashVertexBuffer);
			renderPass.setIndexBuffer(this.indexBuffer, VertexFormat.IndexType.INT);
			renderPass.drawIndexed(0, 0, 6, 1);
		}
	}

	@Override
	public void close() {
		this.sunVertexBuffer.close();
		this.moonPhaseVertexBuffer.close();
		this.starVertexBuffer.close();
		this.topSkyVertexBuffer.close();
		this.bottomSkyVertexBuffer.close();
		this.endSkyVertexBuffer.close();
		this.sunRiseVertexBuffer.close();
		this.endFlashVertexBuffer.close();
		this.indexBuffer.close();
	}
}
