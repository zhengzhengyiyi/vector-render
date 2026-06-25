package net.zhengzhengyiyi.client;

import net.fabricmc.api.ClientModInitializer;

public class RendererClient implements ClientModInitializer{
	@Override
	public void onInitializeClient() {
		// GPU backend bootstraps from MinecraftClientMixin after RenderSystem.initRenderer.
	}
}
