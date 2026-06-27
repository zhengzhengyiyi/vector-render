package net.zhengzhengyiyi.client.debug;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for memory usage.
 */
public class MemoryDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
		long totalMemory = runtime.totalMemory() / 1024 / 1024;
		long maxMemory = runtime.maxMemory() / 1024 / 1024;
		lines.add(String.format("Memory: %dMB / %dMB (max: %dMB)", usedMemory, totalMemory, maxMemory));
	}
}
