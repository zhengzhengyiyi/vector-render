package net.zhengzhengyiyi.client.debug;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.zhengzhengyiyi.renderer.fog.FogData;
import net.zhengzhengyiyi.renderer.fog.FogRenderer;
import net.zhengzhengyiyi.client.render.RenderEngine;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for fog information.
 */
public class FogInfoDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
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
}
