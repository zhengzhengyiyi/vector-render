package net.zhengzhengyiyi.renderer.api.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum LogicOp {
   NONE,
   OR_REVERSE;

   private LogicOp() {
   }
}
