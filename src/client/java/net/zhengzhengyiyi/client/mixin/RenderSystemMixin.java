package net.zhengzhengyiyi.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

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
