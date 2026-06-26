package net.zhengzhengyiyi.client.mixin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientChunkManager;
import net.zhengzhengyiyi.client.SectionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds 1.21.11-style section tracking to ClientChunkManager.
 * 
 * <p>1.21.11 tracks which chunk sections are active (non-empty) to optimize
 * rendering and chunk unloading. This mixin adds that functionality to 1.20.4.
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin {
	@Unique
	private LongOpenHashSet renderer$activeSections;

	@Unique
	private void renderer$initSections() {
		if (this.renderer$activeSections == null) {
			this.renderer$activeSections = new LongOpenHashSet();
			SectionTracker.registerManager((ClientChunkManager) (Object) this, this.renderer$activeSections);
		}
	}

	/**
	 * Initialize the sections tracking when the chunk manager is created.
	 */
	@Inject(method = "<init>", at = @At("RETURN"))
	private void renderer$init(CallbackInfo ci) {
		renderer$initSections();
	}

	/**
	 * Expose the active sections set for use by the renderer.
	 */
	@Unique
	public LongOpenHashSet renderer$getActiveSections() {
		renderer$initSections();
		return this.renderer$activeSections;
	}
}
