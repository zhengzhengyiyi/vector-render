package net.zhengzhengyiyi.renderer.gl;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/** Minimal post-effect pipeline holder until full post-processing backport lands. */
@Environment(EnvType.CLIENT)
public record PostEffectPipeline(Map<Identifier, Object> internalTargets, List<Object> passes) {
	public static final Codec<PostEffectPipeline> CODEC = Codec.unit(new PostEffectPipeline(Map.of(), List.of()));
}
