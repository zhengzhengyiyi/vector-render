package net.zhengzhengyiyi.client.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.zhengzhengyiyi.renderer.chunk.ChunkRenderTaskScheduler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$Task")
public abstract class ChunkBuilderTaskMixin implements ChunkRenderTaskScheduler.TaskAccess {
	@Shadow
	protected boolean prioritized;

	@Shadow
	protected AtomicBoolean cancelled;

	@Shadow
	@Final
	net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk field_20837;

	@Override
	public BlockPos getOrigin() {
		return this.field_20837.getOrigin();
	}

	@Override
	public boolean isPrioritized() {
		return this.prioritized;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled.get();
	}

	@Override
	public void cancelTask() {
		this.cancel();
	}

	@Shadow
	public abstract void cancel();
}
