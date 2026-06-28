package net.zhengzhengyiyi.client.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Manages debug HUD profile and entry visibility.
 * Ported from 1.21.11.
 */
public class DebugHudProfile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private Map<Identifier, DebugHudEntryVisibility> visibilityMap = new HashMap<>();
	private final List<Identifier> visibleEntries = new ArrayList<>();
	private boolean f3Enabled = false;
	private final File file;

	public DebugHudProfile(File file) {
		this.file = new File(file, "debug-profile.json");
		readProfileFile();
	}

	public void readProfileFile() {
		try {
			if (!this.file.isFile()) {
				setToDefault();
				updateVisibleEntries();
				return;
			}
			
			FileReader reader = new FileReader(this.file);
			JsonObject json = GSON.fromJson(reader, JsonObject.class);
			reader.close();
			
			if (json != null) {
				this.f3Enabled = json.has("f3Enabled") ? json.get("f3Enabled").getAsBoolean() : false;
				
				if (json.has("visibility")) {
					JsonObject visibilityJson = json.getAsJsonObject("visibility");
					for (String key : visibilityJson.keySet()) {
						try {
							Identifier id = Identifier.tryParse(key);
							String visibilityStr = visibilityJson.get(key).getAsString();
							DebugHudEntryVisibility visibility = DebugHudEntryVisibility.valueOf(visibilityStr);
							if (id != null) {
								this.visibilityMap.put(id, visibility);
							}
						} catch (Exception e) {
							// Skip invalid entries
						}
					}
				}
			}
		} catch (Exception e) {
			setToDefault();
		}
		
		// Ensure all registered entries have a visibility setting
		for (Identifier id : DebugHudEntries.getEntries().keySet()) {
			if (!this.visibilityMap.containsKey(id)) {
				this.visibilityMap.put(id, DebugHudEntryVisibility.IN_OVERLAY);
			}
		}
		
		updateVisibleEntries();
	}

	private void setToDefault() {
		this.visibilityMap = new HashMap<>();
		// Set default visibility for all registered entries to ALWAYS_ON
		for (Identifier id : DebugHudEntries.getEntries().keySet()) {
			this.visibilityMap.put(id, DebugHudEntryVisibility.ALWAYS_ON);
		}
	}

	public DebugHudEntryVisibility getVisibility(Identifier entryId) {
		DebugHudEntryVisibility visibility = this.visibilityMap.get(entryId);
		return visibility == null ? DebugHudEntryVisibility.NEVER : visibility;
	}

	public boolean isEntryVisible(Identifier entryId) {
		return this.visibleEntries.contains(entryId);
	}

	public void setEntryVisibility(Identifier entryId, DebugHudEntryVisibility visibility) {
		this.visibilityMap.put(entryId, visibility);
		updateVisibleEntries();
		saveProfileFile();
	}

	public boolean toggleVisibility(Identifier entryId) {
		DebugHudEntryVisibility current = this.visibilityMap.get(entryId);
		if (current == null) {
			setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
			return true;
		}

		switch (current) {
			case ALWAYS_ON:
				setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
				return false;
			case IN_OVERLAY:
				if (this.f3Enabled) {
					setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
					return false;
				} else {
					setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
					return true;
				}
			case NEVER:
				if (this.f3Enabled) {
					setEntryVisibility(entryId, DebugHudEntryVisibility.IN_OVERLAY);
				} else {
					setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				}
				return true;
			default:
				setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				return true;
		}
	}

	public Collection<Identifier> getVisibleEntries() {
		return this.visibleEntries;
	}

	public void toggleF3Enabled() {
		setF3Enabled(!this.f3Enabled);
	}

	public void setF3Enabled(boolean f3Enabled) {
		if (this.f3Enabled != f3Enabled) {
			this.f3Enabled = f3Enabled;
			updateVisibleEntries();
			saveProfileFile();
		}
	}

	public boolean isF3Enabled() {
		return this.f3Enabled;
	}

	public void updateVisibleEntries() {
		this.visibleEntries.clear();
		boolean reducedDebugInfo = MinecraftClient.getInstance().hasReducedDebugInfo();

		for (Map.Entry<Identifier, DebugHudEntryVisibility> entry : this.visibilityMap.entrySet()) {
			if (entry.getValue() == DebugHudEntryVisibility.ALWAYS_ON ||
				(this.f3Enabled && entry.getValue() == DebugHudEntryVisibility.IN_OVERLAY)) {
				DebugHudEntry debugHudEntry = DebugHudEntries.get(entry.getKey());
				if (debugHudEntry != null && debugHudEntry.canShow(reducedDebugInfo)) {
					this.visibleEntries.add(entry.getKey());
				}
			}
		}

		this.visibleEntries.sort(Identifier::compareTo);
	}

	public void saveProfileFile() {
		try {
			JsonObject json = new JsonObject();
			json.addProperty("f3Enabled", this.f3Enabled);
			
			JsonObject visibilityJson = new JsonObject();
			for (Map.Entry<Identifier, DebugHudEntryVisibility> entry : this.visibilityMap.entrySet()) {
				visibilityJson.addProperty(entry.getKey().toString(), entry.getValue().name());
			}
			json.add("visibility", visibilityJson);
			
			FileWriter writer = new FileWriter(this.file);
			GSON.toJson(json, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
