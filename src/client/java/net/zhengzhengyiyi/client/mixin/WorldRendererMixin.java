package net.zhengzhengyiyi.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.zhengzhengyiyi.client.render.RenderEngine;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/BackgroundRenderer;render(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)V"
		)
	)
	private void renderer$apply211Fog(
		Camera camera,
		float tickDelta,
		ClientWorld world,
		int viewDistance,
		float skyDarkness
	) {
		boolean thickFog = world.getDimensionEffects().useThickFog(
			MathHelper.floor(camera.getPos().x),
			MathHelper.floor(camera.getPos().z)
		) || this.client.inGameHud.getBossBarHud().shouldThickenFog();
		RenderEngine.getFogRenderer().applyFog(camera, viewDistance, tickDelta, skyDarkness, world, thickFog);
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/BackgroundRenderer;applyFogColor()V"
		)
	)
	private void renderer$skipDuplicateFogColor() {
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"
		)
	)
	private void renderer$renderSky(
		WorldRenderer worldRenderer,
		MatrixStack matrices,
		Matrix4f projectionMatrix,
		float tickDelta,
		Camera camera,
		boolean thickFog,
		Runnable fogCallback
	) {
		if (RenderEngine.getSkyRendering() != null) {
			RenderEngine.getSkyRendering().render(matrices, tickDelta, tickDelta, thickFog, fogCallback);
		}
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V"
		)
	)
	private void renderer$renderClouds(
		WorldRenderer worldRenderer,
		MatrixStack matrices,
		Matrix4f projectionMatrix,
		float tickDelta,
		double cameraX,
		double cameraY,
		double cameraZ
	) {
		if (RenderEngine.getCloudRenderer() != null) {
			RenderEngine.getCloudRenderer().render((net.minecraft.client.util.math.MatrixStack) matrices, tickDelta);
		}
	}

	// @Redirect(
	// 	method = "render",
	// 	at = @At(
	// 		value = "INVOKE",
	// 		target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V"
	// 	)
	// )
	@Inject(method="renderWeather", at=@At("HEAD"), cancellable = true)
	private void renderer$renderWeather(
		LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ,
		CallbackInfo ci
	) {
		ci.cancel();
		if (RenderEngine.getWeatherRendering() != null) {
			RenderEngine.getWeatherRendering().render(tickDelta, cameraX, cameraY, cameraZ);
		}
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/WorldRenderer;renderWorldBorder(Lnet/minecraft/client/render/Camera;)V"
		)
	)
	private void renderer$renderWorldBorder(
		WorldRenderer worldRenderer,
		Camera camera
	) {
		if (RenderEngine.getWorldBorderRendering() != null) {
			RenderEngine.getWorldBorderRendering().render(camera);
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void renderer$renderDebug(CallbackInfo ci) {
		if (RenderEngine.getDebugRenderer() != null) {
			RenderEngine.getDebugRenderer().render();
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void renderer$renderGameTestDebug(CallbackInfo ci) {
		if (RenderEngine.getGameTestDebugRenderer() != null) {
			RenderEngine.getGameTestDebugRenderer().render();
		}
	}
}
