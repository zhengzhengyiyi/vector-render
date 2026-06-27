package net.zhengzhengyiyi.client.debug;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for debug HUD entries.
 * Ported from 1.21.11.
 */
public class DebugHudEntries {
	private static final Map<Identifier, DebugHudEntry> ENTRIES = new HashMap<>();

	// Custom renderer entries
	public static final Identifier GAME_VERSION = register("game_version", new GameVersionDebugHudEntry());
	public static final Identifier SECTION_POSITION = register("section_position", new SectionPositionDebugHudEntry());
	public static final Identifier FOG_INFO = register("fog_info", new FogInfoDebugHudEntry());
	
	// Vanilla-style entries (replicating existing F3 elements)
	public static final Identifier MINECRAFT_VERSION = register("minecraft_version", new MinecraftVersionDebugHudEntry());
	public static final Identifier FPS = register("fps", new FpsDebugHudEntry());
	public static final Identifier SERVER_INFO = register("server_info", new ServerInfoDebugHudEntry());
	public static final Identifier MEMORY = register("memory", new MemoryDebugHudEntry());
	public static final Identifier CHUNK_STATS = register("chunk_stats", new ChunkStatsDebugHudEntry());
	public static final Identifier POSITION = register("position", new PositionDebugHudEntry());
	public static final Identifier ENTITIES = register("entities", new EntitiesDebugHudEntry());
	public static final Identifier WORLD_STRING = register("world_string", new WorldStringDebugHudEntry());
	public static final Identifier CHUNK_INFO = register("chunk_info", new ChunkInfoDebugHudEntry());

	private DebugHudEntries() {}

	private static Identifier register(String id, DebugHudEntry entry) {
		return register(new Identifier("renderer", id), entry);
	}

	private static Identifier register(Identifier id, DebugHudEntry entry) {
		ENTRIES.put(id, entry);
		return id;
	}

	public static Map<Identifier, DebugHudEntry> getEntries() {
		return Map.copyOf(ENTRIES);
	}

	public static DebugHudEntry get(Identifier id) {
		return ENTRIES.get(id);
	}
}
