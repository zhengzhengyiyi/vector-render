package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for entity count (vanilla element).
 */
public class EntitiesDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world != null) {
			String particleDebug = client.particleManager.getDebugString();
			int entityCount = client.world.getRegularEntityCount();
			lines.add(String.format("§dP: §e%s§7. §dT: §e%d", particleDebug, entityCount));
		}
	}
}
