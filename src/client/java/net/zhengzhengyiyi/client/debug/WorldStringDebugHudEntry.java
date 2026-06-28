package net.zhengzhengyiyi.client.debug;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for world string (vanilla element).
 */
public class WorldStringDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		if (world != null) {
			lines.add("§5" + world.asString());
		}
	}
}
