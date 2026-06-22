package net.zhengzhengyiyi.client.render;

import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBuffer;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuBufferSlice;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.GpuFence;
import net.zhengzhengyiyi.renderer.api.blaze3d.buffers.Std140SizeCalculator;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.GpuDevice;
import net.zhengzhengyiyi.renderer.gl.DynamicUniforms;
import net.zhengzhengyiyi.renderer.gl.GlBackend;
import net.zhengzhengyiyi.renderer.gl.SamplerCache;
import net.zhengzhengyiyi.renderer.gl.ShaderSourceGetter;
import net.zhengzhengyiyi.renderer.fog.FogRenderer;
import net.minecraft.util.collection.ArrayListDeque;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight 1.21.11 rendering hooks.
 *
 * <p>Holds the single {@link GpuDevice} for 1.21.11-style rendering. It is created
 * once in {@link #initGpu} by reusing the GL context that vanilla's
 * {@code RenderSystem.initRenderer} already made current — {@code GL.getCapabilities()}
 * is never called a second time. Only one {@code GlBackend} ever exists.
 */
public final class RenderEngine {
	public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();

	private static @Nullable GpuDevice device;
	private static @Nullable DynamicUniforms dynamicUniforms;
	private static @Nullable GpuBuffer globalSettingsUniform;
	private static @Nullable GpuBufferSlice shaderFog;
	private static @Nullable FogRenderer fogRenderer;
	private static @Nullable SamplerCache samplerCache;
	private static final ArrayListDeque<FencedTask> pendingFences = new ArrayListDeque<>();

	private RenderEngine() {
	}

	// -------------------------------------------------------------------------
	// Lifecycle
	// -------------------------------------------------------------------------

	/**
	 * Create the single {@link GpuDevice}. Must be called after vanilla's
	 * {@code RenderSystem.initRenderer(IZ)} so that the GL context is already
	 * current. {@link GlBackend} will call {@code GL.getCapabilities()} which
	 * returns the existing capabilities — no second context is created.
	 */
	public static void initGpu(
		long windowHandle,
		int debugVerbosity,
		boolean sync,
		ShaderSourceGetter shaderSourceGetter,
		boolean renderDebugLabels
	) {
		if (device != null) {
			return; // already initialised (guard against double-call)
		}
		device = new GlBackend(windowHandle, debugVerbosity, sync, shaderSourceGetter, renderDebugLabels);
	}

	/** Called after {@link #initGpu} to initialise sub-systems that need the device. */
	public static void initClientRendering() {
		fogRenderer = new FogRenderer();
		dynamicUniforms = new DynamicUniforms();
		samplerCache = new SamplerCache();
		samplerCache.init();
	}

	/** Called on client shutdown. */
	public static void shutdown() {
		if (samplerCache != null) {
			samplerCache.close();
			samplerCache = null;
		}
		if (fogRenderer != null) {
			fogRenderer.close();
			fogRenderer = null;
		}
		if (device != null) {
			device.close();
			device = null;
		}
		dynamicUniforms = null;
		globalSettingsUniform = null;
		shaderFog = null;
		pendingFences.clear();
	}

	// -------------------------------------------------------------------------
	// GPU device
	// -------------------------------------------------------------------------

	/**
	 * Returns the single {@link GpuDevice}. Throws if {@link #initGpu} has not
	 * been called yet.
	 */
	public static GpuDevice getDevice() {
		if (device == null) {
			throw new IllegalStateException("RenderEngine GPU not initialised — initGpu() was not called");
		}
		return device;
	}

	public static @Nullable GpuDevice tryGetDevice() {
		return device;
	}

	// -------------------------------------------------------------------------
	// Sub-systems
	// -------------------------------------------------------------------------

	public static DynamicUniforms getDynamicUniforms() {
		if (dynamicUniforms == null) {
			throw new IllegalStateException("RenderEngine not initialised");
		}
		return dynamicUniforms;
	}

	public static SamplerCache getSamplerCache() {
		if (samplerCache == null) {
			throw new IllegalStateException("RenderEngine not initialised");
		}
		return samplerCache;
	}

	public static FogRenderer getFogRenderer() {
		if (fogRenderer == null) {
			throw new IllegalStateException("RenderEngine fog renderer not initialized");
		}
		return fogRenderer;
	}

	public static @Nullable FogRenderer tryGetFogRenderer() {
		return fogRenderer;
	}

	// -------------------------------------------------------------------------
	// Shared uniform state
	// -------------------------------------------------------------------------

	public static void setGlobalSettingsUniform(GpuBuffer buffer) {
		globalSettingsUniform = buffer;
	}

	public static @Nullable GpuBuffer getGlobalSettingsUniform() {
		return globalSettingsUniform;
	}

	public static void setShaderFog(@Nullable GpuBufferSlice fog) {
		shaderFog = fog;
	}

	public static @Nullable GpuBufferSlice getShaderFog() {
		return shaderFog;
	}

	// -------------------------------------------------------------------------
	// Fenced async tasks
	// -------------------------------------------------------------------------

	public static void queueFencedTask(Runnable task) {
		pendingFences.addLast(new FencedTask(task, getDevice().createCommandEncoder().createFence()));
	}

	public static void executePendingTasks() {
		if (pendingFences.isEmpty()) {
			return;
		}
		for (FencedTask task = pendingFences.peekFirst(); task != null; task = pendingFences.peekFirst()) {
			if (!task.fence.awaitCompletion(0L)) {
				return;
			}
			try {
				task.callback.run();
			} finally {
				task.fence.close();
			}
			pendingFences.removeFirst();
		}
	}

	private record FencedTask(Runnable callback, GpuFence fence) {
	}
}
