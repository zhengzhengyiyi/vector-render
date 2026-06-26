package net.zhengzhengyiyi.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.world.ClientChunkManager;

/**
 * Utility class to track active chunk sections across ClientChunkManager instances.
 * This implements 1.21.11-style section tracking for 1.20.4.
 */
public class SectionTracker {
	private static final Object2ObjectOpenHashMap<ClientChunkManager, LongOpenHashSet> ACTIVE_SECTIONS_MAP = new Object2ObjectOpenHashMap<>();

	public static void trackSection(ClientChunkManager chunkManager, long sectionPos, boolean isEmpty) {
		LongOpenHashSet sections = ACTIVE_SECTIONS_MAP.get(chunkManager);
		if (sections != null) {
			if (isEmpty) {
				sections.add(sectionPos);
			} else {
				sections.remove(sectionPos);
			}
		}
	}

	public static LongOpenHashSet getActiveSections(ClientChunkManager chunkManager) {
		return ACTIVE_SECTIONS_MAP.get(chunkManager);
	}

	public static void registerManager(ClientChunkManager chunkManager, LongOpenHashSet sections) {
		ACTIVE_SECTIONS_MAP.put(chunkManager, sections);
	}
}
