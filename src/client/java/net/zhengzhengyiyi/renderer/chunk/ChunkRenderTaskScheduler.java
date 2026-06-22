package net.zhengzhengyiyi.renderer.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.ListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Proximity-aware chunk build task scheduler, ported from 1.21.11.
 *
 * <p>Uses a single {@link ListIterator} pass per {@link #dequeueNearest} call to
 * simultaneously compact cancelled tasks and find the nearest regular/prioritized
 * task. This matches the vanilla 1.21.11 implementation exactly and avoids the
 * O(n²) double-scan that the previous version suffered from.
 */
@Environment(EnvType.CLIENT)
public class ChunkRenderTaskScheduler {
	private static final int MAX_CONSECUTIVE_PRIORITIZED = 2;
	private int remainingPrioritizableTasks = MAX_CONSECUTIVE_PRIORITIZED;
	private final List<Object> queue = new ObjectArrayList<>();

	public synchronized void enqueue(Object task) {
		this.queue.add(task);
	}

	public synchronized @Nullable Object dequeueNearest(Vec3d pos) {
		int regularIdx = -1;
		int prioritizedIdx = -1;
		double nearestRegularDist = Double.MAX_VALUE;
		double nearestPrioritizedDist = Double.MAX_VALUE;

		ListIterator<Object> it = this.queue.listIterator();
		while (it.hasNext()) {
			int k = it.nextIndex();
			Object task = it.next();
			TaskAccess access = TaskAccess.of(task);
			if (access.isCancelled()) {
				it.remove();
			} else {
				double dist = access.getOrigin().getSquaredDistance(pos);
				if (!access.isPrioritized() && dist < nearestRegularDist) {
					nearestRegularDist = dist;
					regularIdx = k;
				}
				if (access.isPrioritized() && dist < nearestPrioritizedDist) {
					nearestPrioritizedDist = dist;
					prioritizedIdx = k;
				}
			}
		}

		boolean hasPrioritized = prioritizedIdx >= 0;
		boolean hasRegular = regularIdx >= 0;
		if (!hasPrioritized || hasRegular && (this.remainingPrioritizableTasks <= 0 || !(nearestPrioritizedDist < nearestRegularDist))) {
			this.remainingPrioritizableTasks = MAX_CONSECUTIVE_PRIORITIZED;
			return this.remove(regularIdx);
		} else {
			this.remainingPrioritizableTasks--;
			return this.remove(prioritizedIdx);
		}
	}

	public int size() {
		return this.queue.size();
	}

	private @Nullable Object remove(int index) {
		return index >= 0 ? this.queue.remove(index) : null;
	}

	public synchronized void cancelAll() {
		for (Object task : this.queue) {
			TaskAccess.of(task).cancelTask();
		}
		this.queue.clear();
	}

	@Environment(EnvType.CLIENT)
	public interface TaskAccess {
		BlockPos getOrigin();

		boolean isPrioritized();

		boolean isCancelled();

		void cancelTask();

		static TaskAccess of(Object task) {
			return (TaskAccess) task;
		}
	}
}
