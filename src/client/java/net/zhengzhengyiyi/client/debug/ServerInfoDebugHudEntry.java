package net.zhengzhengyiyi.client.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Debug HUD entry for server info (vanilla element).
 */
public class ServerInfoDebugHudEntry implements DebugHudEntry {
	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
		MinecraftClient client = MinecraftClient.getInstance();
		IntegratedServer integratedServer = client.getServer();
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		ClientConnection connection = networkHandler.getConnection();
		float tx = connection.getAveragePacketsSent();
		float rx = connection.getAveragePacketsReceived();
		
		if (integratedServer != null) {
			lines.add(String.format("§cIntegrated server §7@ §e%.1f ms§7, §e%.0f tx§7, §e%.0f rx§7", 
				integratedServer.getAverageTickTime(), tx, rx));
		} else {
			lines.add(String.format("§c\"%s\" §7server, §e%.0f tx§7, §e%.0f rx§7", 
				networkHandler.getBrand(), tx, rx));
		}
	}
}
