package net.zhengzhengyiyi.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.zhengzhengyiyi.client.debug.DebugHudProfile;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Shadow
	private Window window;

	@Unique
	private DebugHudProfile debugHudEntryList;

	/**
	 * After vanilla's 1.20.4 RenderSystem.initRenderer(IZ) has run — meaning the GL
	 * context is fully current — create the single GlBackend and register it with
	 * RenderEngine. No second context is ever created; GL.getCapabilities() is reused.
	 */
	@SuppressWarnings("resource")
	@Inject(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V",
			shift = At.Shift.AFTER
		)
	)
	private void afterRenderInit(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		RenderEngine.initGpu(
			this.window.getHandle(),
			client.options.glDebugVerbosity,
			false,
			(id, type) -> null,
			false
		);
		RenderEngine.initClientRendering();
		
		// Initialize debug HUD profile
		this.debugHudEntryList = new DebugHudProfile(client.runDirectory);
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void onStop(CallbackInfo ci) {
		RenderEngine.shutdown();
	}

	@Unique
	public DebugHudProfile getDebugHudEntryList() {
		return this.debugHudEntryList;
	}
}
