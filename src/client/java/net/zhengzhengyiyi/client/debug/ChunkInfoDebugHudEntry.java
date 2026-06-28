package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for chunk info (vanilla element).
 */
public class ChunkInfoDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getCameraEntity() != null && world != null) {
			BlockPos blockPos = client.getCameraEntity().getBlockPos();
			ChunkPos chunkPos = new ChunkPos(blockPos);
			
			lines.add(String.format("§fBlock: §6%d §7§6%d §7§6%d §7[§a%d §7§a%d §7§a%d§7]", 
				blockPos.getX(), blockPos.getY(), blockPos.getZ(),
				blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
			lines.add(String.format("§fChunk: §9%d §7§9%d §7§9%d §7[§a%d §7§a%d §7in §cr.§c%d§7.§c%d§7.mca§7]",
				chunkPos.x, ChunkSectionPos.getSectionCoord(blockPos.getY()), chunkPos.z,
				chunkPos.getRegionRelativeX(), chunkPos.getRegionRelativeZ(),
				chunkPos.getRegionX(), chunkPos.getRegionZ()));
		}
	}
}
