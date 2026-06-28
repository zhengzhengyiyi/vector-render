package net.zhengzhengyiyi.client.debug;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for Minecraft version (vanilla element).
 */
public class MinecraftVersionDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		String version = SharedConstants.getGameVersion().getName();
		lines.addPriorityLine(String.format("§bMinecraft §7%s §7(§e%s§7)", version, client.getGameVersion()));
	}
}
