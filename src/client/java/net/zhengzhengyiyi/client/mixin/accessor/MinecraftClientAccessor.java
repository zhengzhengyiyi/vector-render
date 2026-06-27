package net.zhengzhengyiyi.client.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.zhengzhengyiyi.client.debug.DebugHudProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
	@Accessor("debugHudEntryList")
	DebugHudProfile getDebugHudEntryList();
}
