package net.zhengzhengyiyi.renderer.api.jtracy;

public final class TracyClient {
	private TracyClient() {
	}

	public static boolean isAvailable() {
		return false;
	}

	public static void beginZone(String name) {
	}

	public static void endZone() {
	}

	public static MemoryPool createMemoryPool(String name) {
		return new MemoryPool(name);
	}

	public static Plot createPlot(String name) {
		return new Plot(name);
	}
}
