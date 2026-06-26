package net.zhengzhengyiyi.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.zhengzhengyiyi.client.SectionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks section status changes to notify ClientChunkManager.
 * 
 * <p>In 1.21.11, WorldChunk notifies ClientChunkManager when sections become
 * empty or non-empty. This mixin adds that notification to 1.20.4.
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
	/**
	 * Notify ClientChunkManager when a section's empty status changes.
	 * This is called when blocks are added/removed from a section.
	 */
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void renderer$notifySectionStatusChange(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state, boolean moved, CallbackInfoReturnable<Boolean> ci) {
		WorldChunk self = (WorldChunk) (Object) this;
		if (self.getWorld() instanceof net.minecraft.client.world.ClientWorld clientWorld) {
			net.minecraft.client.world.ClientChunkManager chunkManager = clientWorld.getChunkManager();
			int sectionY = ChunkSectionPos.getSectionCoord(pos.getY());
			
			// Validate section index is within bounds to prevent ArrayIndexOutOfBoundsException
			if (sectionY < 0 || sectionY >= self.getSectionArray().length) {
				return;
			}
			
			long sectionPos = ChunkSectionPos.asLong(self.getPos().x, sectionY, self.getPos().z);
			
			// Check if the section is empty after the block change
			ChunkSection section = self.getSection(sectionY);
			boolean isEmpty = section.isEmpty();
			
			// Call the tracking method via SectionTracker
			SectionTracker.trackSection(chunkManager, sectionPos, isEmpty);
		}
	}
}
