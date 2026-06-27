package net.zhengzhengyiyi.client.debug;

import net.minecraft.util.StringIdentifiable;

/**
 * Visibility settings for debug HUD entries.
 * Ported from 1.21.11.
 */
public enum DebugHudEntryVisibility implements StringIdentifiable {
	ALWAYS_ON("alwaysOn"),
	IN_OVERLAY("inOverlay"),
	NEVER("never");

	private final String id;

	DebugHudEntryVisibility(String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return this.id;
	}
}
