package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for section position.
 */
public class SectionPositionDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			int sectionX = ChunkSectionPos.getSectionCoord(client.player.getBlockX());
			int sectionY = ChunkSectionPos.getSectionCoord(client.player.getBlockY());
			int sectionZ = ChunkSectionPos.getSectionCoord(client.player.getBlockZ());
			lines.add(String.format("Section: %d %d %d", sectionX, sectionY, sectionZ));
		}
	}
}
