package net.zhengzhengyiyi.client.mixin;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.zhengzhengyiyi.renderer.fog.FogData;
import net.zhengzhengyiyi.renderer.fog.FogRenderer;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DebugHud.class)
public class DebugHudMixin {
	@Inject(method = "getLeftText", at = @At("RETURN"))
	private void renderer$append211RendererStats(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		FogRenderer fogRenderer = RenderEngine.tryGetFogRenderer();
		if (fogRenderer != null) {
			FogData fog = fogRenderer.getLastFogData();
			lines.add(String.format(
				"vector-render fog: env %.0f-%.0f chunk %.0f-%.0f",
				fog.environmentalStart,
				fog.environmentalEnd,
				fog.renderDistanceStart,
				fog.renderDistanceEnd
			));
		}
	}

	@Inject(method = "getRightText", at = @At("RETURN"))
	private void renderer$append211RendererInUse(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		FogRenderer fogRenderer = RenderEngine.tryGetFogRenderer();
		if (fogRenderer != null) {
			lines.add(5, "§9" + String.format(
				"vector-render §6 %s",
				MinecraftClient.getInstance().getGameVersion()
			));
		}
	}
}
