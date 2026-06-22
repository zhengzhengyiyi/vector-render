package net.zhengzhengyiyi.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.zhengzhengyiyi.renderer.chunk.ChunkRenderTaskScheduler;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(net.minecraft.client.render.chunk.ChunkBuilder.class)
public abstract class ChunkBuilderMixin {
	@Shadow
	private int queuedTaskCount;

	@Shadow
	private volatile boolean stopped;

	@Shadow
	private net.minecraft.util.thread.TaskExecutor<Runnable> mailbox;

	@Shadow
	protected abstract void scheduleRunTasks();

	@Shadow
	public abstract Vec3d getCameraPosition();

	@Unique
	private final ChunkRenderTaskScheduler renderer$scheduler = new ChunkRenderTaskScheduler();

	/**
	 * Redirect task enqueuing into the 1.21.11-style proximity scheduler instead of
	 * 1.20.4's separate priority/regular blocking queues.
	 */
	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void renderer$enqueueTask(@Coerce Object task, CallbackInfo ci) {
		if (!this.stopped) {
			this.mailbox.send(() -> {
				if (!this.stopped) {
					this.renderer$scheduler.enqueue(task);
					this.queuedTaskCount = this.renderer$scheduler.size();
					this.scheduleRunTasks();
				}
			});
		}
		ci.cancel();
	}

	/**
	 * Replace 1.20.4's dual-queue pollTask with a proximity-aware dequeue.
	 */
	@Inject(method = "pollTask", at = @At("HEAD"), cancellable = true)
	private void renderer$pollNearestTask(CallbackInfoReturnable<Object> cir) {
		Object task = this.renderer$scheduler.dequeueNearest(this.getCameraPosition());
		this.queuedTaskCount = this.renderer$scheduler.size();
		cir.setReturnValue(task);
		cir.cancel();
	}

	/**
	 * Cancel all pending tasks in the proximity scheduler when the chunk builder is cleared.
	 */
	@Inject(method = "clear", at = @At("HEAD"))
	private void renderer$clearScheduler(CallbackInfo ci) {
		this.renderer$scheduler.cancelAll();
	}

	@Inject(method = "getDebugString", at = @At("RETURN"), cancellable = true)
	private void renderer$appendQueueMarker(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(cir.getReturnValue() + " [1.21Q]");
	}
}
