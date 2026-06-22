package net.zhengzhengyiyi.renderer.api.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum AddressMode {
   REPEAT,
   CLAMP_TO_EDGE;

   private AddressMode() {
   }
}
