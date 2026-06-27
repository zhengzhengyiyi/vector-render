package net.zhengzhengyiyi.client.render.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug rendering system for 1.21.11-style rendering.
 * Simplified implementation for 1.20.4 compatibility.
 */
public class DebugRenderer implements AutoCloseable {
	private final List<Renderer> renderers = new ArrayList<>();
	private long currentVersion;

	public DebugRenderer() {
		this.initRenderers();
	}

	public void initRenderers() {
		this.renderers.clear();
		// Initialize debug renderers based on 1.21.11 structure
		// For 1.20.4 compatibility, we'll add renderers as needed
	}

	public void render() {
		this.currentVersion++;
		for (Renderer renderer : this.renderers) {
			renderer.render(this.currentVersion);
		}
	}

	@Override
	public void close() {
		this.renderers.clear();
	}

	public interface Renderer {
		void render(long version);
	}
}
