package net.zhengzhengyiyi.client.render.debug;

import com.google.common.collect.Maps;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * Game test debug rendering system for 1.21.11-style rendering.
 * Simplified implementation for 1.20.4 compatibility.
 */
@SuppressWarnings("unused")
public class GameTestDebugRenderer implements AutoCloseable {
	private static final int MARKER_LIFESPAN_MS = 10000;
	private final Map<BlockPos, Marker> markers = Maps.newHashMap();

	public GameTestDebugRenderer() {
	}

	public void addMarker(BlockPos absolutePos, BlockPos relativePos) {
		String string = relativePos.toShortString();
		this.markers.put(absolutePos, new Marker(1610678016, string, Util.getMeasuringTimeMs() + 10000L));
	}

	public void clear() {
		this.markers.clear();
	}

	public void render() {
		long l = Util.getMeasuringTimeMs();
		this.markers.entrySet().removeIf(marker -> l > marker.getValue().removalTime);
		// Rendering would be done here with 1.20.4 compatible debug rendering
		// For now, markers are tracked and expired
	}

	@Override
	public void close() {
		this.markers.clear();
	}

	public record Marker(int color, String message, long removalTime) {
	}
}
