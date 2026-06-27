package net.zhengzhengyiyi.client.debug;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for debug HUD entries.
 * Ported from 1.21.11.
 */
public interface DebugHudEntry {
	void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk);

	default boolean canShow(boolean reducedDebugInfo) {
		return !reducedDebugInfo;
	}

	default DebugHudEntryCategory getCategory() {
		return DebugHudEntryCategory.TEXT;
	}
}
