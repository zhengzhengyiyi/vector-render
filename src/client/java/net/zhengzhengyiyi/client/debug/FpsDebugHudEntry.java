package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for FPS display.
 */
public class FpsDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		int fps = client.fpsDebugString.isEmpty() ? 0 : Integer.parseInt(client.fpsDebugString.split(" ")[0]);
		lines.add(String.format("FPS: %d", fps));
	}
}
