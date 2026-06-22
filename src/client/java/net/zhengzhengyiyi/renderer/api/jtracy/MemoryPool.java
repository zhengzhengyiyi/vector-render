package net.zhengzhengyiyi.renderer.api.jtracy;

public final class MemoryPool {
	public final String name;

	MemoryPool(String name) {
		this.name = name;
	}

	public void malloc(int id, long size) {
	}

	public void free(int id) {
	}
}
