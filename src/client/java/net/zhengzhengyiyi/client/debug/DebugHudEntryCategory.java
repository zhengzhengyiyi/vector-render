package net.zhengzhengyiyi.client.debug;

import net.minecraft.text.Text;

/**
 * Category for debug HUD entries.
 * Ported from 1.21.11.
 */
public enum DebugHudEntryCategory {
	TEXT(0, Text.translatable("debug.category.text")),
	RENDERING(1, Text.translatable("debug.category.rendering")),
	NETWORK(2, Text.translatable("debug.category.network"));

	private final int sortKey;
	private final Text label;

	DebugHudEntryCategory(int sortKey, Text label) {
		this.sortKey = sortKey;
		this.label = label;
	}

	public int sortKey() {
		return this.sortKey;
	}

	public Text label() {
		return this.label;
	}
}
