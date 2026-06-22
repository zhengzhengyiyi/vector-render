package net.zhengzhengyiyi.renderer.api.blaze3d.pipeline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface CompiledRenderPipeline {
   boolean isValid();
}
