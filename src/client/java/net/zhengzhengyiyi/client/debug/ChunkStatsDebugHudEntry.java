package net.zhengzhengyiyi.client.debug;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for chunk statistics.
 */
public class ChunkStatsDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		if (world != null) {
			int loadedChunks = world.getChunkManager().getLoadedChunkCount();
			lines.add(String.format("§aChunks: §e%d §7loaded", loadedChunks));
		}
	}
}
