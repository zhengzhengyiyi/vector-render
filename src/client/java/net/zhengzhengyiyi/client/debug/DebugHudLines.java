package net.zhengzhengyiyi.client.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for rendering debug HUD lines.
 * Ported from 1.21.11.
 */
public class DebugHudLines {
	private final List<String> lines = new ArrayList<>();
	private final List<String> priorityLines = new ArrayList<>();

	public void add(String line) {
		this.lines.add(line);
	}

	public void addPriorityLine(String line) {
		this.priorityLines.add(line);
	}

	public List<String> getLines() {
		return this.lines;
	}

	public List<String> getPriorityLines() {
		return this.priorityLines;
	}
}
