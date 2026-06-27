package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for player position (vanilla element).
 */
public class PositionDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getCameraEntity() != null) {
			BlockPos blockPos = client.getCameraEntity().getBlockPos();
			lines.add(String.format("XYZ: %.3f / %.5f / %.3f", 
				client.getCameraEntity().getX(), 
				client.getCameraEntity().getY(), 
				client.getCameraEntity().getZ()));
			lines.add(String.format("Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
		}
	}
}
