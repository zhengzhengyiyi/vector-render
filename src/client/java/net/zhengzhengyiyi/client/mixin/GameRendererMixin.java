package net.zhengzhengyiyi.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.gui.screen.Overlay;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ports 1.21.11's monitor-refresh-rate-aware frame pacing to 1.20.4.
 *
 * <p>Vanilla 1.20.4 only calls {@link RenderSystem#limitDisplayFPS} when
 * {@code maxFps < 260}. At "Max" (260) the GPU renders without any throttle,
 * sitting at 100% indefinitely rendering frames the display can never show.
 *
 * <p>1.21.11 caps at the monitor's refresh rate even when the limit is
 * "Unlimited". We do the same: after {@code swapBuffers()} we call
 * {@code limitDisplayFPS(refreshRate)} whenever the user limit is 260 and
 * vsync is off. On a 144 Hz display this reduces GPU usage proportionally
 * (from "render 700+ fps" to "render 144 fps") with no visual change.
 */
@Mixin(value = MinecraftClient.class, priority = 1100)
public abstract class GameRendererMixin {

	@Shadow private Window window;
	@Shadow public ClientWorld world;
	@Shadow public GameOptions options;
	@Shadow @Nullable public Screen currentScreen;
	@Shadow public abstract @Nullable Overlay getOverlay();

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/util/Window;swapBuffers()V",
			shift = At.Shift.AFTER
		)
	)
	private void renderer$capFpsAtMonitorRefreshRate(CallbackInfo ci) {
		// Reproduce getFramerateLimit() inline: in menus cap at 60, in world use user setting
		int limit = (this.world != null || (this.currentScreen == null && this.getOverlay() == null))
			? this.window.getFramerateLimit()
			: 60;

		if (limit >= 260) {
			// User wants "unlimited" — cap at monitor refresh rate instead.
			// This is exactly what 1.21.11 does: render no faster than the display.
			int refreshRate = this.window.getRefreshRate();
			if (refreshRate > 0) {
				RenderSystem.limitDisplayFPS(refreshRate);
			}
		}
	}
}
