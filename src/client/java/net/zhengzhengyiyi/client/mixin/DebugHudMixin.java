package net.zhengzhengyiyi.client.mixin;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.zhengzhengyiyi.client.debug.DebugHudEntries;
import net.zhengzhengyiyi.client.debug.DebugHudEntryVisibility;
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
		
		// Clear vanilla debug lines and replace with our configurable entries
		lines.clear();
		
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
		
		// Render entries based on F3 state
		boolean reducedDebugInfo = client.hasReducedDebugInfo();
		if (profile.isF3Enabled()) {
			// Render all visible debug entries when F3 is pressed
			for (var entryId : profile.getVisibleEntries()) {
				var entry = DebugHudEntries.get(entryId);
				if (entry != null) {
					entry.render(hudLines, world, clientChunk, chunk);
				}
			}
		} else {
			// Render only ALWAYS_ON entries when F3 is not pressed
			for (var entryId : DebugHudEntries.getEntries().keySet()) {
				if (profile.getVisibility(entryId) == DebugHudEntryVisibility.ALWAYS_ON) {
					var entry = DebugHudEntries.get(entryId);
					if (entry != null && entry.canShow(reducedDebugInfo)) {
						entry.render(hudLines, world, clientChunk, chunk);
					}
				}
			}
		}
		
		// Add priority lines first (version, FPS, etc.)
		for (String line : hudLines.getPriorityLines()) {
			lines.add(line);
		}
		
		// Add regular lines (position, chunk info, etc.)
		lines.addAll(hudLines.getLines());
	}

	@Inject(method = "getRightText", at = @At("RETURN"))
	private void renderer$append211RendererInUse(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		MinecraftClient client = MinecraftClient.getInstance();
		DebugHudProfile profile = ((MinecraftClientAccessor) client).getDebugHudEntryList();
		
		if (profile == null) {
			return;
		}
		
		// Only show right text when F3 is enabled
		if (!profile.isF3Enabled()) {
			lines.clear();
			return;
		}
		
		// Clear vanilla right text and replace with fog info
		lines.clear();
		
		// Add vector-render branding
		FogRenderer fogRenderer = RenderEngine.tryGetFogRenderer();
		if (fogRenderer != null) {
			lines.add("§3" + String.format(
				"vector-render §6 %s",
				client.getGameVersion()
			));
			
			// Add fog information (replaces chunk waiting text)
			net.zhengzhengyiyi.renderer.fog.FogData fog = fogRenderer.getLastFogData();
			if (fog != null) {
				lines.add(String.format(
					"Fog: env %.0f-%.0f chunk %.0f-%.0f",
					fog.environmentalStart,
					fog.environmentalEnd,
					fog.renderDistanceStart,
					fog.renderDistanceEnd
				));
			}
		}
	}
}
