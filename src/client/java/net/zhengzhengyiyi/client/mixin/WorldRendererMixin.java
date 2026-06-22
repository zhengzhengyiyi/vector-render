package net.zhengzhengyiyi.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
}
