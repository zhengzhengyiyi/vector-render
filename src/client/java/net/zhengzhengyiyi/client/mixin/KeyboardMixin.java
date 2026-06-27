package net.zhengzhengyiyi.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.zhengzhengyiyi.client.screen.DebugOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds key bindings for debug options and Control+Q drop stack.
 * 
 * <p>In 1.21.11, debug options are opened via a dedicated key binding (F6 by default).
 * This mixin adds that functionality to 1.20.4.
 * 
 * <p>Control+Q drops a stack of items (works on all systems).
 */
@Environment(EnvType.CLIENT)
@Mixin(Keyboard.class)
public class KeyboardMixin {
	private static final KeyBinding DEBUG_OPTIONS_KEY = new KeyBinding(
		"key.renderer.debug_options",
		InputUtil.Type.KEYSYM,
		InputUtil.GLFW_KEY_F6, // F6
		"key.categories.debug"
	);

	@Inject(method = "setup", at = @At("RETURN"))
	private void renderer$registerKeyBindings(long window, CallbackInfo ci) {
		KeyBinding.updateKeysByCode();
	}

	@Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
	private void renderer$handleControlQ(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (action == 1 && key == InputUtil.GLFW_KEY_Q && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_CONTROL)) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && !client.player.isSpectator()) {
				// Drop one stack of items
				client.player.dropSelectedItem(true);
				ci.cancel();
			}
		}
	}

	@Inject(method = "onKey", at = @At("HEAD"))
	private void renderer$handleDebugOptionsKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (action == 1 && DEBUG_OPTIONS_KEY.matchesKey(key, scancode)) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && client.getDebugHud() != null) {
				// Open debug options screen
				if (client.currentScreen instanceof DebugOptionsScreen) {
					client.currentScreen.close();
				} else {
					client.setScreen(new DebugOptionsScreen(client.currentScreen));
				}
			}
		}
	}
}
