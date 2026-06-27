package net.zhengzhengyiyi.client.mixin;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.zhengzhengyiyi.client.debug.DebugHudEntries;
import net.zhengzhengyiyi.client.debug.DebugHudLines;
import net.zhengzhengyiyi.client.debug.DebugHudProfile;
import net.zhengzhengyiyi.client.mixin.accessor.MinecraftClientAccessor;
import net.zhengzhengyiyi.client.render.RenderEngine;
import net.zhengzhengyiyi.renderer.fog.FogRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public class DebugHudMixin {
	@Inject(method = "getLeftText", at = @At("RETURN"))
	private void renderer$render211DebugEntries(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		MinecraftClient client = MinecraftClient.getInstance();
		DebugHudProfile profile = ((MinecraftClientAccessor) client).getDebugHudEntryList();
		
		if (profile == null) {
			return;
		}
		
		// Don't clear vanilla lines - append our configurable entries
		// This allows vanilla debug elements to still show if not overridden
		
		DebugHudLines hudLines = new DebugHudLines();
		World world = client.world;
		WorldChunk clientChunk = null;
		if (world != null && client.player != null) {
			var chunk = world.getChunk(client.player.getBlockPos());
			if (chunk instanceof WorldChunk worldChunk) {
				clientChunk = worldChunk;
			}
		}
		WorldChunk chunk = clientChunk;
		
		// Render all visible debug entries
		for (var entryId : profile.getVisibleEntries()) {
			var entry = DebugHudEntries.get(entryId);
			if (entry != null) {
				entry.render(hudLines, world, clientChunk, chunk);
			}
		}
		
		// Add priority lines first
		for (String line : hudLines.getPriorityLines()) {
			lines.add(line);
		}
		
		// Add regular lines
		lines.addAll(hudLines.getLines());
	}

	@Inject(method = "getRightText", at = @At("RETURN"))
	private void renderer$append211RendererInUse(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		FogRenderer fogRenderer = RenderEngine.tryGetFogRenderer();
		if (fogRenderer != null) {
			lines.add(5, "§3" + String.format(
				"vector-render §6 %s",
				MinecraftClient.getInstance().getGameVersion()
			));
		}
	}
}
