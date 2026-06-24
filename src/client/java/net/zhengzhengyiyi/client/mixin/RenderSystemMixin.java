package net.zhengzhengyiyi.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

	/**
	 * Issue {@code glFlush()} before {@code glfwSwapBuffers()} so the driver
	 * starts executing queued GPU commands immediately rather than holding them
	 * until swap forces a hard sync.
	 *
	 * <p>Without this, all commands accumulated during the frame are submitted
	 * at once at swap time, causing the CPU to stall inside
	 * {@code glfwSwapBuffers} waiting for the GPU to catch up. The profiler
	 * shows this as ~28% of frame time in {@code Window.swapBuffers()}.
	 *
	 * <p>{@code glFlush()} is non-blocking — it tells the driver "start
	 * executing what you have so far" and returns immediately. The GPU then
	 * works through the command queue in parallel while the CPU finishes the
	 * rest of the frame (UI, audio, tick), so by the time swap arrives the GPU
	 * is much closer to done and the stall shrinks substantially.
	 */
	@Inject(
		method = "flipFrame",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"
		)
	)
	private static void renderer$flushBeforeSwap(long window, CallbackInfo ci) {
		GL11.glFlush();
	}

	/**
	 * At the end of every frame (after glfwSwapBuffers), mirror what 1.21.11's
	 * RenderSystem.flipFrame does:
	 *
	 * 1. {@code dynamicUniforms.clear()} — rotates the triple-buffered UBO ring
	 *    and resets the write cursor. Without this the DynamicUniformStorage grows
	 *    unboundedly and forces GPU-CPU synchronisation on every write.
	 *
	 * 2. {@code executePendingTasks()} — polls non-blocking GpuFences and fires
	 *    callbacks (e.g. texture readbacks) without stalling the render thread.
	 */
	@Inject(method = "flipFrame", at = @At("TAIL"))
	private static void renderer$onFlipFrame(long window, CallbackInfo ci) {
		if (RenderEngine.tryGetDevice() == null) {
			return; // not yet initialised
		}
		RenderEngine.getDynamicUniforms().clear();
		RenderEngine.executePendingTasks();
	}
}
